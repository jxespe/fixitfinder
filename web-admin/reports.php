<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_login();

$firstReportId = (int) $pdo->query("SELECT id FROM reports ORDER BY id ASC LIMIT 1")->fetchColumn();
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Reports</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      .report-grid {
        display: grid;
        grid-template-columns: minmax(0, 1fr);
        gap: 20px;
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
          <a class="nav-pill" href="./users.php">Users</a>
          <a class="nav-pill" href="./service-providers.php">Service Providers</a>
          <a class="nav-pill" href="./technicians.php">Technicians</a>
          <a class="nav-pill is-active" href="./reports.php">Report</a>
        </nav>
        <a class="logout-pill" href="./logout.php">Log Out <span>⎋</span></a>
      </header>

      <div class="container">
        <div class="report-grid">
          <div class="card">
            <div class="section-title">User & Provider Reports</div>
            <p class="subtle">
              Review bug reports and issue submissions from users and providers.
            </p>
            <div style="margin-top: 16px;">
              <a class="btn" href="./reports-management.php<?php echo $firstReportId > 0 ? '' : ''; ?>">
                Open Reports
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
