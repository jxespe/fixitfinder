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

if ($isRegistration) {
    require __DIR__ . "/admin-registration.php";
    exit;
}

if ($admin && ($path === '' || $path === '/' || $path === '/index.php')) {
    header("Location: dashboard.php");
    exit;
}

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

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
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
        } catch (Throwable $e) {
            $profile = null;
            try {
                $profile = firestore_find_admin_by_email($email);
            } catch (Throwable $ignored) {
                $profile = null;
            }
            $hash = is_array($profile) ? ($profile['password_hash'] ?? '') : '';
            if (!is_string($hash) || $hash === '' || !password_verify($password, $hash)) {
                throw $e;
            }
            $uid = (string) ($profile['uid'] ?? $profile['id'] ?? '');
            if ($uid === '') {
                throw $e;
            }
        }
        $_SESSION['login_attempts'] = 0;
        $_SESSION['login_locked_until'] = 0;
        login_admin_firebase($uid, $profile);
        set_flash('login_success', 'Login successful. Redirecting to dashboard.');
        header("Location: dashboard.php");
        exit;
    }
}

$success = get_flash('success');
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Login</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      body {
        background: #1f1f1f;
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

      }
    </style>
  </head>
  <body>
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
            <?php $loginFlash = get_flash('login_success'); ?>
            <?php if (!empty($loginFlash)) : ?>
            <div class="message success"><?php echo htmlspecialchars($loginFlash, ENT_QUOTES); ?></div>
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
  </body>
</html>
