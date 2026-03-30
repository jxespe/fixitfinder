<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_once __DIR__ . "/includes/firebase.php";
require_login();

$users = [];
$usingFirestore = false;
try {
    $firestoreUsers = firestore_list_collection('users', 'createdAt');
    if (!empty($firestoreUsers)) {
        foreach ($firestoreUsers as $user) {
            $users[] = [
                'id' => $user['id'] ?? '',
                'full_name' => $user['fullName'] ?? ($user['name'] ?? 'User'),
            ];
        }
        $usingFirestore = true;
    }
} catch (RuntimeException $e) {
    $usingFirestore = false;
}

if (!$usingFirestore) {
    $users = $pdo->query("SELECT * FROM users ORDER BY id ASC")->fetchAll();
}
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Users</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      .users-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 40px;
        padding: 40px 0 20px;
      }

      .user-avatar {
        width: 140px;
        height: 140px;
        border-radius: 50%;
        background: #efefef;
        display: grid;
        place-items: center;
        font-weight: 600;
        color: #444;
        font-size: 28px;
        margin: 0 auto;
      }

      @media (max-width: 900px) {
        .users-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
      }

      @media (max-width: 600px) {
        .users-grid {
          grid-template-columns: 1fr;
        }
      }
    </style>
  </head>
  <body>
    <div class="page">
      <header class="topbar">
        <div class="brand">
          <img src="./assets/ic_banner.png" alt="Fix It Finder" />
          <div class="brand-title">
            <span>Admin Panel</span>
            <span>Fix It Finder</span>
          </div>
        </div>
        <nav class="nav-pills">
          <a class="nav-pill" href="./dashboard.php">Dashboard</a>
          <a class="nav-pill is-active" href="./users.php">Users</a>
          <a class="nav-pill" href="./service-providers.php">Service Providers</a>
          <a class="nav-pill" href="./technicians.php">Technicians</a>
          <a class="nav-pill" href="./reports.php">Report</a>
        </nav>
        <a class="logout-pill" href="./logout.php">Log Out <span>⎋</span></a>
      </header>

      <div class="container">
        <div class="card">
          <div class="users-grid">
            <?php foreach ($users as $user) : ?>
            <a
              href="./user-detail.php?<?php echo $usingFirestore ? 'uid=' . urlencode((string) $user['id']) : 'id=' . (int) $user['id']; ?>"
              class="user-avatar"
            >
              <?php echo htmlspecialchars(get_initials($user['full_name']), ENT_QUOTES); ?>
            </a>
            <?php endforeach; ?>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
