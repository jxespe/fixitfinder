<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_login();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $reportId = (int) ($_POST['report_id'] ?? 0);
    $status = trim($_POST['status'] ?? '');
    if ($reportId > 0 && in_array($status, ['Open', 'In Review', 'Resolved'], true)) {
        $stmt = $pdo->prepare("UPDATE reports SET status = :status WHERE id = :id");
        $stmt->execute(['status' => $status, 'id' => $reportId]);
    }
}

$reports = $pdo->query("SELECT * FROM reports ORDER BY id DESC")->fetchAll();
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Bugs & Reports</title>
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
          <a class="nav-pill" href="./user-overview.php">Users</a>
          <a class="nav-pill" href="./provider-overview.php">Service Providers</a>
          <a class="nav-pill is-active" href="./reports-management.php">Reports</a>
          <a class="nav-pill" href="./provider-documents.php">Documents</a>
        </nav>
        <a class="logout-pill" href="./logout.php">Log Out <span>⎋</span></a>
      </header>

      <div class="container">
        <div class="card">
          <div class="section-title">Managing Bugs and Reports</div>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Type</th>
                <th>Title</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Update</th>
              </tr>
            </thead>
            <tbody>
              <?php if (!$reports) : ?>
              <tr>
                <td colspan="6">No reports available.</td>
              </tr>
              <?php else : ?>
              <?php foreach ($reports as $report) : ?>
              <tr>
                <td><?php echo (int) $report['id']; ?></td>
                <td><?php echo htmlspecialchars($report['report_type'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($report['title'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($report['priority'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($report['status'], ENT_QUOTES); ?></td>
                <td>
                  <form method="post" style="display: flex; gap: 8px; align-items: center;">
                    <input type="hidden" name="report_id" value="<?php echo (int) $report['id']; ?>" />
                    <select name="status">
                      <option <?php echo $report['status'] === 'Open' ? 'selected' : ''; ?>>Open</option>
                      <option <?php echo $report['status'] === 'In Review' ? 'selected' : ''; ?>>In Review</option>
                      <option <?php echo $report['status'] === 'Resolved' ? 'selected' : ''; ?>>Resolved</option>
                    </select>
                    <button class="btn small" type="submit">Save</button>
                  </form>
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
