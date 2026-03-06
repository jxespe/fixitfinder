<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";

$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH) ?? '';
$isRegistration = (bool) preg_match('#/admin-registration(\.php)?$#', $path);
$isDashboard = (bool) preg_match('#/dashboard(\.php)?$#', $path);
$routeMap = [
    'dashboard' => 'dashboard.php',
    'users' => 'users.php',
    'user-overview' => 'user-overview.php',
    'user-detail' => 'user-detail.php',
    'service-providers' => 'service-providers.php',
    'provider-overview' => 'provider-overview.php',
    'provider-profile' => 'provider-profile.php',
    'provider-registration' => 'provider-registration.php',
    'technicians' => 'technicians.php',
    'reports' => 'reports.php',
    'reports-management' => 'reports-management.php',
    'receipts' => 'receipts.php',
    'provider-documents' => 'provider-documents.php',
];

$admin = current_admin($pdo);

if ($isDashboard) {
    require __DIR__ . "/dashboard.php";
    exit;
}

foreach ($routeMap as $slug => $file) {
    if (preg_match('#/' . preg_quote($slug, '#') . '(\.php)?$#', $path)) {
        require __DIR__ . "/" . $file;
        exit;
    }
}

$loginError = null;
$registrationError = null;
$registrationValues = [
    'first_name' => '',
    'last_name' => '',
    'email' => '',
    'phone' => '',
    'admin_id' => '',
    'role' => 'Super Admin',
    'setup_code' => '',
];

if ($_SERVER['REQUEST_METHOD'] === 'POST' && !$isRegistration) {
    $email = trim($_POST['email'] ?? '');
    $password = (string) ($_POST['password'] ?? '');
    $token = $_POST['csrf_token'] ?? '';

    $attempts = (int) ($_SESSION['login_attempts'] ?? 0);
    $lockedUntil = (int) ($_SESSION['login_locked_until'] ?? 0);
    if ($lockedUntil > time()) {
        $loginError = "Too many attempts. Try again in a few minutes.";
    } elseif (!csrf_valid((string) $token)) {
        $loginError = "Security check failed. Refresh and try again.";
    } elseif ($email === '' || $password === '') {
        $loginError = "Please enter your email and password.";
    } else {
        try {
            $authResult = firebase_sign_in($email, $password);
            $uid = (string) ($authResult['localId'] ?? '');
            if ($uid === '') {
                throw new RuntimeException("Auth failed.");
            }
            $profile = firestore_get_admin($uid);
            if (!$profile) {
                throw new RuntimeException("Admin profile not found in Firestore.");
            }
            $_SESSION['login_attempts'] = 0;
            $_SESSION['login_locked_until'] = 0;
            login_admin_firebase($uid, $profile);
            header("Location: dashboard.php");
            exit;
        } catch (Throwable $e) {
            $attempts++;
            $_SESSION['login_attempts'] = $attempts;
            if ($attempts >= 5) {
                $_SESSION['login_locked_until'] = time() + 300;
            }
            $loginError = $e->getMessage();
        }
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $isRegistration) {
    $registrationValues = [
        'first_name' => trim($_POST['first_name'] ?? ''),
        'last_name' => trim($_POST['last_name'] ?? ''),
        'email' => trim($_POST['email'] ?? ''),
        'phone' => trim($_POST['phone'] ?? ''),
        'admin_id' => trim($_POST['admin_id'] ?? ''),
        'role' => trim($_POST['role'] ?? 'Super Admin'),
        'setup_code' => (string) ($_POST['setup_code'] ?? ''),
    ];

    $firstName = $registrationValues['first_name'];
    $lastName = $registrationValues['last_name'];
    $email = $registrationValues['email'];
    $phone = $registrationValues['phone'];
    $adminId = $registrationValues['admin_id'];
    $role = $registrationValues['role'];
    $setupCode = $registrationValues['setup_code'];
    $password = (string) ($_POST['password'] ?? '');
    $confirmPassword = (string) ($_POST['confirm_password'] ?? '');
    $token = $_POST['csrf_token'] ?? '';

    if (!csrf_valid((string) $token)) {
        $registrationError = "Security check failed. Refresh and try again.";
    } elseif ($firstName === '' || $lastName === '' || $email === '' || $adminId === '' || $password === '') {
        $registrationError = "Please fill in all required fields.";
    } elseif (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        $registrationError = "Please enter a valid email address.";
    } elseif ($password !== $confirmPassword) {
        $registrationError = "Passwords do not match.";
    } elseif ($setupCode !== admin_setup_code()) {
        $registrationError = "Invalid admin setup code.";
    } elseif (!password_strong($password)) {
        $registrationError = "Password must be at least 8 characters and include upper, lower, and a number.";
    } else {
        try {
            $displayName = trim($firstName . ' ' . $lastName);
            $authResult = firebase_create_user($email, $password, $displayName, $phone);
            $uid = (string) ($authResult['localId'] ?? '');
            if ($uid === '') {
                throw new RuntimeException("Auth creation failed.");
            }
            firestore_save_admin($uid, [
                'uid' => $uid,
                'full_name' => $displayName,
                'first_name' => $firstName,
                'last_name' => $lastName,
                'email' => $email,
                'phone' => $phone,
                'admin_id' => $adminId,
                'role' => $role,
                'created_at' => date('c'),
            ]);
            set_flash('success', 'Admin account created. You can now log in.');
            header("Location: index.php");
            exit;
        } catch (Throwable $e) {
            $registrationError = $e->getMessage();
        }
    }
}

$success = get_flash('success');
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>
      Fix It Finder Admin - <?php echo $isRegistration ? 'Registration' : 'Login'; ?>
    </title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      body {
        background: <?php echo $isRegistration ? '#f4f4f4' : '#1f1f1f'; ?>;
      }

      .login-hero {
        min-height: 100vh;
        display: grid;
        place-items: center;
        padding: 24px;
      }

      .login-card {
        width: min(1120px, 94vw);
        min-height: 600px;
        border-radius: 18px;
        overflow: hidden;
        background: linear-gradient(180deg, rgba(0, 0, 0, 0.35), rgba(0, 0, 0, 0.35)),
          url("./assets/ic_banner.png") center/cover no-repeat;
        border: 6px solid #242424;
        display: grid;
        grid-template-columns: 1.1fr 0.9fr;
        position: relative;
      }

      .login-left {
        padding: 48px;
        color: #fff;
      }

      .login-left h1 {
        font-size: 28px;
        margin-bottom: 10px;
      }

      .login-left h2 {
        font-weight: 500;
        font-size: 18px;
        margin-bottom: 12px;
      }

      .login-left p {
        font-size: 13px;
        line-height: 1.6;
        max-width: 420px;
        color: rgba(255, 255, 255, 0.9);
      }

      .login-right {
        display: flex;
        justify-content: center;
        align-items: center;
        padding: 32px;
      }

      .login-panel {
        width: min(360px, 100%);
        background: rgba(255, 255, 255, 0.76);
        border-radius: 14px;
        padding: 22px;
        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
        backdrop-filter: blur(6px);
      }

      .login-panel h3 {
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 6px;
      }

      .login-panel span {
        font-size: 12px;
        color: #5f5f5f;
      }

      .form-group {
        margin-top: 14px;
      }

      .form-group label {
        font-size: 12px;
        display: block;
        margin-bottom: 6px;
      }

      .form-footer {
        margin-top: 10px;
        text-align: right;
        font-size: 11px;
        color: #6a6a6a;
      }

      .login-btn {
        width: 100%;
        margin-top: 16px;
        border-radius: 10px;
      }

      .message {
        margin-top: 10px;
        padding: 8px 10px;
        border-radius: 8px;
        font-size: 12px;
      }

      .message.error {
        background: #ffe4e6;
        color: #991b1b;
      }

      .message.success {
        background: #dcfce7;
        color: #166534;
      }

      .register-link {
        margin-top: 12px;
        font-size: 12px;
        text-align: center;
        color: #6a6a6a;
      }

      .register-link a {
        color: var(--primary);
        font-weight: 600;
      }

      .register-wrap {
        min-height: 100vh;
        display: grid;
        place-items: center;
        padding: 32px 16px;
      }

      .register-card {
        width: min(980px, 94vw);
        border-radius: 20px;
        background: #ffffff;
        border: 1px solid var(--border);
        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
        overflow: hidden;
      }

      .register-header {
        padding: 24px 32px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        border-bottom: 1px solid var(--border);
        background: #fafafa;
      }

      .register-header .brand {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .register-header img {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        object-fit: cover;
      }

      .register-body {
        display: grid;
        grid-template-columns: 0.9fr 1.1fr;
        gap: 20px;
        padding: 28px 32px 32px;
      }

      .register-left h1 {
        font-size: 22px;
        margin-bottom: 8px;
        color: var(--primary);
      }

      .register-left p {
        font-size: 13px;
        line-height: 1.6;
        color: var(--text-muted);
      }

      .register-right h2 {
        font-size: 18px;
        margin-bottom: 6px;
      }

      .form-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 14px;
        margin-top: 16px;
      }

      .message {
        margin-top: 10px;
        padding: 8px 10px;
        border-radius: 8px;
        font-size: 12px;
      }

      .message.error {
        background: #ffe4e6;
        color: #991b1b;
      }

      .message.success {
        background: #dcfce7;
        color: #166534;
      }

      @media (max-width: 900px) {
        .login-card {
          grid-template-columns: 1fr;
        }

        .login-left {
          padding: 36px 28px;
        }

        .register-body {
          grid-template-columns: 1fr;
        }

        .form-grid {
          grid-template-columns: 1fr;
        }
      }
    </style>
  </head>
  <body>
    <?php if ($isRegistration) : ?>
    <main class="register-wrap">
      <section class="register-card">
        <div class="register-header">
          <div class="brand">
            <img src="./assets/ic_banner.png" alt="Fix It Finder" />
            <div>
              <div style="font-weight: 600;">Fix It Finder Admin</div>
              <div class="subtle">Admin Registration</div>
            </div>
          </div>
          <a class="btn secondary" href="./index.php">Back to Login</a>
        </div>
        <div class="register-body">
          <div class="register-left">
            <h1>Create Admin Account</h1>
            <p>
              Use this form to register an admin user. The account will have access to dashboards,
              verification modules, and reporting tools.
            </p>
            <div class="card" style="margin-top: 16px;">
              <div class="section-title">Security Requirements</div>
              <div class="subtle">- Minimum 8 characters</div>
              <div class="subtle">- At least 1 uppercase and 1 lowercase letter</div>
              <div class="subtle">- At least 1 number</div>
            </div>
          </div>
          <div class="register-right">
            <h2>Admin Registration</h2>
            <p class="subtle">Fill out all required information.</p>
            <?php if (!empty($registrationError)) : ?>
            <div class="message error">
              <?php echo htmlspecialchars($registrationError, ENT_QUOTES); ?>
            </div>
            <?php endif; ?>
            <form method="post" action="./index.php">
              <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars(csrf_token(), ENT_QUOTES); ?>" />
              <div class="form-grid">
                <div class="form-group">
                  <label>First Name</label>
                  <input
                    class="input"
                    name="first_name"
                    type="text"
                    placeholder="Enter first name"
                    value="<?php echo htmlspecialchars($registrationValues['first_name'], ENT_QUOTES); ?>"
                  />
                </div>
                <div class="form-group">
                  <label>Last Name</label>
                  <input
                    class="input"
                    name="last_name"
                    type="text"
                    placeholder="Enter last name"
                    value="<?php echo htmlspecialchars($registrationValues['last_name'], ENT_QUOTES); ?>"
                  />
                </div>
                <div class="form-group">
                  <label>Email Address</label>
                  <input
                    class="input"
                    name="email"
                    type="email"
                    placeholder="Enter email"
                    value="<?php echo htmlspecialchars($registrationValues['email'], ENT_QUOTES); ?>"
                  />
                </div>
                <div class="form-group">
                  <label>Phone Number</label>
                  <input
                    class="input"
                    name="phone"
                    type="text"
                    placeholder="Enter phone number"
                    value="<?php echo htmlspecialchars($registrationValues['phone'], ENT_QUOTES); ?>"
                  />
                </div>
                <div class="form-group">
                  <label>Admin ID</label>
                  <input
                    class="input"
                    name="admin_id"
                    type="text"
                    placeholder="Enter admin ID"
                    value="<?php echo htmlspecialchars($registrationValues['admin_id'], ENT_QUOTES); ?>"
                  />
                </div>
                <div class="form-group">
                  <label>Role</label>
                  <select name="role">
                    <option <?php echo $registrationValues['role'] === 'Super Admin' ? 'selected' : ''; ?>>Super Admin</option>
                    <option <?php echo $registrationValues['role'] === 'Moderator' ? 'selected' : ''; ?>>Moderator</option>
                    <option <?php echo $registrationValues['role'] === 'Support' ? 'selected' : ''; ?>>Support</option>
                  </select>
                </div>
                <div class="form-group">
                  <label>Admin Setup Code</label>
                  <input
                    class="input"
                    name="setup_code"
                    type="password"
                    placeholder="Enter setup code"
                    value="<?php echo htmlspecialchars($registrationValues['setup_code'], ENT_QUOTES); ?>"
                  />
                </div>
                <div class="form-group">
                  <label>Password</label>
                  <input class="input" name="password" type="password" placeholder="Create password" />
                </div>
                <div class="form-group">
                  <label>Confirm Password</label>
                  <input class="input" name="confirm_password" type="password" placeholder="Confirm password" />
                </div>
              </div>
              <div class="actions">
                <a class="btn secondary" href="./index.php">Cancel</a>
                <button class="btn" type="submit">Create Admin</button>
              </div>
            </form>
          </div>
        </div>
      </section>
    </main>
    <?php else : ?>
    <main class="login-hero">
      <section class="login-card">
        <div class="login-left">
          <h1>Fix It Finder Admin</h1>
          <h2>Welcome to Fix It Finder</h2>
          <p>
            Fix It Finder Services is your reliable partner for fast, efficient, and professional
            repair and maintenance solutions. We connect you with skilled experts ready to handle
            everything from simple fixes to complex projects - making home and workplace repairs
            easy, convenient, and stress-free. With quality service and customer satisfaction at our
            core, we help you get things fixed right the first time.
          </p>
        </div>
        <div class="login-right">
          <div class="login-panel">
            <h3>Login</h3>
            <span>Welcome onboard with us!</span>
            <?php if (!empty($admin)) : ?>
            <div class="message success">
              You are already logged in. <a href="./dashboard.php">Go to Dashboard</a>
            </div>
            <?php endif; ?>
            <?php if (!empty($loginError)) : ?>
            <div class="message error"><?php echo htmlspecialchars($loginError, ENT_QUOTES); ?></div>
            <?php endif; ?>
            <?php if (!empty($success)) : ?>
            <div class="message success"><?php echo htmlspecialchars($success, ENT_QUOTES); ?></div>
            <?php endif; ?>
            <form method="post">
              <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars(csrf_token(), ENT_QUOTES); ?>" />
              <div class="form-group">
                <label>Admin Email ID</label>
                <input class="input" type="email" name="email" placeholder="Enter your username" />
              </div>
              <div class="form-group">
                <label>Password</label>
                <input class="input" type="password" name="password" placeholder="Enter your password" />
              </div>
              <div class="form-footer">Forgot Password?</div>
              <button class="btn login-btn" type="submit">LogIn as Admin</button>
            </form>
            <div class="register-link">
              New admin? <a href="./admin-registration.php">Create account</a>
            </div>
          </div>
        </div>
      </section>
    </main>
    <?php endif; ?>
  </body>
</html>
