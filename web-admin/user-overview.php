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
                'email' => $user['email'] ?? '',
                'phone' => $user['phone'] ?? '',
                'verified' => $user['verified'] ?? ($user['phoneVerified'] ?? false),
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
    <title>Fix It Finder Admin - User Overview</title>
    <link rel="stylesheet" href="./styles.css" />
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
          <a class="nav-pill is-active" href="./user-overview.php">Users</a>
          <a class="nav-pill" href="./provider-overview.php">Service Providers</a>
          <a class="nav-pill" href="./reports-management.php">Reports</a>
          <a class="nav-pill" href="./provider-documents.php">Documents</a>
        </nav>
        <a class="logout-pill" href="./logout.php">Log Out <span>⎋</span></a>
      </header>

      <div class="container">
        <div class="card">
          <div class="section-title">User Overview</div>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Status</th>
                <th>Profile</th>
              </tr>
            </thead>
            <tbody>
              <?php if (!$users) : ?>
              <tr>
                <td colspan="6">No users available.</td>
              </tr>
              <?php else : ?>
              <?php foreach ($users as $user) : ?>
              <tr>
                <td><?php echo htmlspecialchars((string) $user['id'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($user['full_name'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars((string) $user['email'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars((string) $user['phone'], ENT_QUOTES); ?></td>
                <td><?php echo !empty($user['verified']) ? 'Verified' : 'Pending'; ?></td>
                <td>
                  <a
                    class="status-open"
                    href="./user-detail.php?<?php echo $usingFirestore ? 'uid=' . urlencode((string) $user['id']) : 'id=' . (int) $user['id']; ?>"
                  >Open</a>
                </td>
              </tr>
              <?php endforeach; ?>
              <?php endif; ?>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </body>
</html>
