<?php
declare(strict_types=1);

if (session_status() === PHP_SESSION_NONE) {
    $isHttps = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || (($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '') === 'https');
    ini_set('session.save_path', '/tmp');
    session_set_cookie_params([
        'lifetime' => 0,
        'path' => '/',
        'secure' => $isHttps,
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
    session_start();
}

// Prevent client-hint restart loops in Chrome.
header("Permissions-Policy: ch-ua=(), ch-ua-platform=(), ch-ua-mobile=()");
header("Accept-CH:");
header("Critical-CH:");

$defaultPath = __DIR__ . "/../data/app.db";
$dbPath = getenv('ADMIN_DB_PATH') ?: $defaultPath;
$dbDir = dirname($dbPath);
if (!is_dir($dbDir) && $dbPath === $defaultPath) {
    mkdir($dbDir, 0775, true);
}

if (!is_writable($dbDir)) {
    $dbPath = "/tmp/app.db";
}

$pdo = new PDO("sqlite:" . $dbPath);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);

$pdo->exec(
    "CREATE TABLE IF NOT EXISTS admins (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        first_name TEXT NOT NULL,
        last_name TEXT NOT NULL,
        email TEXT NOT NULL UNIQUE,
        phone TEXT,
        admin_id TEXT NOT NULL UNIQUE,
        role TEXT NOT NULL,
        password_hash TEXT NOT NULL,
        created_at TEXT NOT NULL
    )"
);

$pdo->exec(
    "CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        full_name TEXT NOT NULL,
        email TEXT,
        phone TEXT,
        verified INTEGER NOT NULL DEFAULT 0,
        avatar TEXT,
        created_at TEXT NOT NULL
    )"
);

$pdo->exec(
    "CREATE TABLE IF NOT EXISTS user_pending_items (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL,
        description TEXT NOT NULL,
        amount REAL,
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    )"
);

$pdo->exec(
    "CREATE TABLE IF NOT EXISTS user_transactions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL,
        date TEXT NOT NULL,
        amount REAL NOT NULL,
        type TEXT NOT NULL,
        transaction_id TEXT NOT NULL,
        verification_status TEXT NOT NULL,
        receipt TEXT,
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    )"
);

$pdo->exec(
    "CREATE TABLE IF NOT EXISTS providers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        service_type TEXT NOT NULL,
        branch TEXT NOT NULL,
        phone TEXT,
        email TEXT,
        address TEXT,
        ranking TEXT,
        status TEXT,
        verified INTEGER NOT NULL DEFAULT 0,
        rating REAL,
        logo TEXT,
        pending_count INTEGER NOT NULL DEFAULT 0,
        completed_count INTEGER NOT NULL DEFAULT 0,
        report_count INTEGER NOT NULL DEFAULT 0,
        business_hours TEXT,
        created_at TEXT NOT NULL
    )"
);

$pdo->exec(
    "CREATE TABLE IF NOT EXISTS reports (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        report_type TEXT NOT NULL,
        title TEXT NOT NULL,
        description TEXT,
        status TEXT NOT NULL,
        priority TEXT NOT NULL,
        created_at TEXT NOT NULL
    )"
);

$pdo->exec(
    "CREATE TABLE IF NOT EXISTS provider_documents (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        provider_id INTEGER NOT NULL,
        doc_type TEXT NOT NULL,
        file_name TEXT NOT NULL,
        status TEXT NOT NULL,
        submitted_at TEXT NOT NULL,
        FOREIGN KEY(provider_id) REFERENCES providers(id) ON DELETE CASCADE
    )"
);

function set_flash(string $key, string $message): void
{
    $_SESSION['flash'][$key] = $message;
}

function get_flash(string $key): ?string
{
    if (!empty($_SESSION['flash'][$key])) {
        $message = $_SESSION['flash'][$key];
        unset($_SESSION['flash'][$key]);
        return $message;
    }
    return null;
}

function csrf_token(): string
{
    if (empty($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf_token'];
}

function csrf_valid(?string $token): bool
{
    return !empty($_SESSION['csrf_token']) && is_string($token) && hash_equals($_SESSION['csrf_token'], $token);
}

function password_strong(string $password): bool
{
    if (strlen($password) < 8) {
        return false;
    }
    if (!preg_match('/[A-Z]/', $password)) {
        return false;
    }
    if (!preg_match('/[a-z]/', $password)) {
        return false;
    }
    if (!preg_match('/\d/', $password)) {
        return false;
    }
    return true;
}

function admin_setup_code(): string
{
    $envCode = getenv('ADMIN_SETUP_CODE');
    if (is_string($envCode) && trim($envCode) !== '') {
        return trim($envCode);
    }
    return 'FIXIT-ADMIN-2026';
}

function session_signing_key(): string
{
    $key = getenv('ADMIN_SESSION_KEY');
    if (is_string($key) && trim($key) !== '') {
        return trim($key);
    }
    return 'CHANGE_ME_ADMIN_SESSION_KEY';
}

function get_initials(string $name): string
{
    $parts = preg_split('/\s+/', trim($name));
    if (!$parts) {
        return '';
    }
    $first = strtoupper($parts[0][0] ?? '');
    $last = strtoupper($parts[count($parts) - 1][0] ?? '');
    return $first . $last;
}

function seed_demo_data(PDO $pdo): void
{
    $userCount = (int) $pdo->query("SELECT COUNT(*) FROM users")->fetchColumn();
    if ($userCount === 0) {
        $pdo->exec(
            "INSERT INTO users (full_name, email, phone, verified, avatar, created_at) VALUES
            ('John N. Nabaldog', 'john@example.com', '+63 901 234 5678', 1, NULL, datetime('now')),
            ('Maria J. Cruz', 'maria@example.com', '+63 912 555 7788', 1, NULL, datetime('now')),
            ('Kevin L. Santos', 'kevin@example.com', '+63 921 111 2233', 0, NULL, datetime('now')),
            ('Rhea A. Dizon', 'rhea@example.com', '+63 930 444 8899', 1, NULL, datetime('now'))"
        );

        $pdo->exec(
            "INSERT INTO user_pending_items (user_id, description, amount) VALUES
            (1, 'Plumbing', 480),
            (1, 'App Charged', 320),
            (1, 'Checkups', 100),
            (1, 'Other Fee', NULL)"
        );

        $pdo->exec(
            "INSERT INTO user_transactions (user_id, date, amount, type, transaction_id, verification_status, receipt) VALUES
            (1, '2022-08-02', 800, 'Maintenance', 'UITX 1003343434', 'Pending', '6-SEM-01.pdf'),
            (1, '2022-08-02', 600, 'Repair', 'UITX 1003343434', 'Verified', ''),
            (1, '2022-08-02', 1600, 'Carpenter', 'UITX 1003343434', 'Verified', ''),
            (1, '2022-08-02', 4300, 'Installation', 'UITX 1003343434', 'Verified', ''),
            (1, '2022-08-02', 900, 'Cleaning', 'UPT 1003343434', 'Verified', '')"
        );
    }

    $providerCount = (int) $pdo->query("SELECT COUNT(*) FROM providers")->fetchColumn();
    if ($providerCount === 0) {
        $pdo->exec(
            "INSERT INTO providers
                (name, service_type, branch, phone, email, address, ranking, status, verified, rating, logo, pending_count, completed_count, report_count, business_hours, created_at)
             VALUES
                ('J Plumbing Services', 'Plumbing', 'Manila', '+91 XXXXXXXXXX', 'premXXXXXXXXXXXXXX', 'San Miguel, Manila', '6th', 'Active', 1, 7.52, 'ic_plumbing.png', 6, 5, 3, '8:00 AM - 5:00 PM', datetime('now')),
                ('Craft Fix', 'Carpentry', 'Caloocan', '+63 922 111 2222', 'craftfix@example.com', 'Caloocan City', '9th', 'Active', 1, 7.30, 'ic_carpentry.png', 3, 8, 1, '9:00 AM - 6:00 PM', datetime('now')),
                ('CoolAir', 'Aircon', 'Makati', '+63 933 222 3333', 'coolair@example.com', 'Makati City', '8th', 'Active', 1, 7.12, 'ic_aircon_repairs.png', 2, 4, 0, '8:30 AM - 6:30 PM', datetime('now')),
                ('HomeHero', 'Handy Services', 'Taguig', '+63 944 333 4444', 'homehero@example.com', 'Taguig City', '10th', 'Offline', 0, 6.90, 'ic_appliance_repair.png', 0, 2, 0, '8:00 AM - 5:00 PM', datetime('now')),
                ('Sparkle', 'Clean Services', 'Pasay', '+63 955 444 5555', 'sparkle@example.com', 'Pasay City', '7th', 'Active', 1, 7.40, 'ic_electronics_repair.png', 1, 6, 0, '7:00 AM - 4:00 PM', datetime('now')),
                ('LockPro', 'Locksmith', 'BGC', '+63 966 555 6666', 'lockpro@example.com', 'BGC, Taguig', '11th', 'Offline', 1, 6.80, 'ic_internet_technician.png', 0, 1, 0, '9:00 AM - 5:00 PM', datetime('now'))"
        );
    }

    $reportCount = (int) $pdo->query("SELECT COUNT(*) FROM reports")->fetchColumn();
    if ($reportCount === 0) {
        $pdo->exec(
            "INSERT INTO reports (report_type, title, description, status, priority, created_at) VALUES
            ('Bug', 'Login button unresponsive', 'Admin login button does not respond on Safari.', 'Open', 'High', datetime('now')),
            ('Report', 'Service provider complaint', 'User reported delayed response from provider.', 'In Review', 'Medium', datetime('now')),
            ('Bug', 'Dashboard table alignment', 'Table headers misaligned on mobile.', 'Open', 'Low', datetime('now'))"
        );
    }

    $docCount = (int) $pdo->query("SELECT COUNT(*) FROM provider_documents")->fetchColumn();
    if ($docCount === 0) {
        $pdo->exec(
            "INSERT INTO provider_documents (provider_id, doc_type, file_name, status, submitted_at) VALUES
            (1, 'Business Permit', 'permit_jplumbing.pdf', 'Pending', datetime('now')),
            (2, 'Government ID', 'id_craftfix.png', 'Approved', datetime('now')),
            (3, 'Insurance', 'insurance_coolair.pdf', 'Pending', datetime('now'))"
        );
    }
}

seed_demo_data($pdo);
