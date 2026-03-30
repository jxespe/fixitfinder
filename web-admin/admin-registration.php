<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";

$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH) ?? '';
$isRegistration = (bool) preg_match('#/admin-registration(\.php)?$#', $path);

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
            // Validate Firebase config early for clearer errors.
            firebase_project_id();
            firebase_api_key();
            firebase_credentials_path();
            $displayName = trim($firstName . ' ' . $lastName);
            $authResult = firebase_create_user($email, $password, $displayName, $phone);
            $uid = (string) ($authResult['localId'] ?? '');
            if ($uid === '') {
                throw new RuntimeException("Auth creation failed.");
            }
            $profile = [
                'uid' => $uid,
                'full_name' => $displayName,
                'first_name' => $firstName,
                'last_name' => $lastName,
                'email' => $email,
                'phone' => $phone,
                'admin_id' => $adminId,
                'role' => $role,
                'password_hash' => password_hash($password, PASSWORD_DEFAULT),
                'created_at' => date('c'),
            ];
            firestore_save_admin($uid, $profile);
            $stored = firestore_get_admin($uid);
            if (!$stored) {
                throw new RuntimeException("Firestore admin profile was not created.");
            }
            set_flash('success', 'Admin account created. You can now log in.');
            header("Location: index.php");
            exit;
        } catch (Throwable $e) {
            error_log('Admin registration failed: ' . $e->getMessage());
            $registrationError = $e->getMessage();
        }
    }
}
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Registration</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      body {
        background: #f4f4f4;
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

      @media (max-width: 900px) {
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
            <form method="post" action="./admin-registration.php">
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
  </body>
</html>
