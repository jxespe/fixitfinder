<?php
declare(strict_types=1);

require_once __DIR__ . "/config.php";
require_once __DIR__ . "/firebase.php";

function login_admin_firebase(string $uid, array $profile): void
{
    $_SESSION['admin_uid'] = $uid;
    $_SESSION['admin_email'] = $profile['email'] ?? '';
    $_SESSION['admin_name'] = $profile['full_name'] ?? '';
    set_admin_cookie($uid, (string) ($profile['email'] ?? ''));
}

function logout_admin(): void
{
    $_SESSION = [];
    if (ini_get("session.use_cookies")) {
        $params = session_get_cookie_params();
        setcookie(session_name(), "", time() - 42000, $params["path"], $params["domain"], $params["secure"], $params["httponly"]);
    }
    session_destroy();
    clear_admin_cookie();
}

function current_admin(PDO $pdo): ?array
{
    if (empty($_SESSION['admin_uid'])) {
        $rehydrated = try_rehydrate_session();
        if (!$rehydrated) {
            return null;
        }
    }
    $uid = (string) $_SESSION['admin_uid'];
    $profile = firestore_get_admin($uid);
    if (!$profile) {
        return null;
    }
    $profile['uid'] = $uid;
    return $profile;
}

function require_login(): void
{
    if (empty($_SESSION['admin_uid'])) {
        $rehydrated = try_rehydrate_session();
        if (!$rehydrated) {
            header("Location: index.php");
            exit;
        }
    }
}

function set_admin_cookie(string $uid, string $email): void
{
    $exp = time() + 86400;
    $payload = $uid . '|' . $email . '|' . $exp;
    $sig = hash_hmac('sha256', $payload, session_signing_key());
    $token = base64_encode($payload . '|' . $sig);
    setcookie('admin_session', $token, [
        'expires' => $exp,
        'path' => '/',
        'secure' => true,
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
}

function clear_admin_cookie(): void
{
    setcookie('admin_session', '', [
        'expires' => time() - 3600,
        'path' => '/',
        'secure' => true,
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
}

function try_rehydrate_session(): bool
{
    $token = $_COOKIE['admin_session'] ?? '';
    if (!is_string($token) || $token === '') {
        return false;
    }
    $decoded = base64_decode($token, true);
    if (!is_string($decoded)) {
        return false;
    }
    $parts = explode('|', $decoded);
    if (count($parts) !== 4) {
        return false;
    }
    [$uid, $email, $exp, $sig] = $parts;
    if ((int) $exp < time()) {
        return false;
    }
    $payload = $uid . '|' . $email . '|' . $exp;
    $expected = hash_hmac('sha256', $payload, session_signing_key());
    if (!hash_equals($expected, $sig)) {
        return false;
    }
    $_SESSION['admin_uid'] = $uid;
    $_SESSION['admin_email'] = $email;
    return true;
}
