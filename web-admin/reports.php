<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_login();

$firstUserId = (int) $pdo->query("SELECT id FROM users ORDER BY id ASC LIMIT 1")->fetchColumn();
$firstProviderId = (int) $pdo->query("SELECT id FROM providers ORDER BY id ASC LIMIT 1")->fetchColumn();
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
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 20px;
      }

      @media (max-width: 900px) {
        .report-grid {
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
            <div class="section-title">Receipts for Users</div>
            <p class="subtle">
              Manage registration receipts and incomplete status per user. This section matches the
              design used for registration completion.
            </p>
            <div style="margin-top: 16px;">
              <a class="btn" href="./receipts.php?id=<?php echo $firstUserId; ?>">Open Receipts</a>
            </div>
          </div>
          <div class="card">
            <div class="section-title">Provider Registration Slip</div>
            <p class="subtle">
              View submitted registration forms for service providers and verify documentation.
            </p>
            <div style="margin-top: 16px;">
              <a class="btn" href="./provider-registration.php?id=<?php echo $firstProviderId; ?>">Open Registration</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
