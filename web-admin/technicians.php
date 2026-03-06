<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_login();
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Technicians</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      .empty-state {
        text-align: center;
        padding: 60px 20px;
        color: var(--text-muted);
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
          <a class="nav-pill is-active" href="./technicians.php">Technicians</a>
          <a class="nav-pill" href="./reports.php">Report</a>
        </nav>
        <a class="logout-pill" href="./logout.php">Log Out <span>⎋</span></a>
      </header>

      <div class="container">
        <div class="card empty-state">
          <div class="section-title">Technicians</div>
          <p>Technician management list will appear here.</p>
        </div>
      </div>
    </div>
  </body>
</html>
