<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_login();

$userId = (int) ($_GET['id'] ?? 0);
if ($userId <= 0) {
    $userId = (int) $pdo->query("SELECT id FROM users ORDER BY id ASC LIMIT 1")->fetchColumn();
}

$user = null;
if ($userId > 0) {
    $stmt = $pdo->prepare("SELECT * FROM users WHERE id = :id");
    $stmt->execute(['id' => $userId]);
    $user = $stmt->fetch();
}

if (!$user) {
    header("Location: users.php");
    exit;
}
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Receipts</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      .receipt-header {
        display: grid;
        grid-template-columns: 1fr auto 1fr;
        gap: 20px;
        align-items: center;
      }

      .divider {
        width: 1px;
        height: 100%;
        background: var(--border);
      }

      .receipt-form {
        margin-top: 16px;
      }

      .form-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 16px;
        align-items: center;
        margin-bottom: 12px;
      }

      .option-row {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 12px;
      }

      @media (max-width: 980px) {
        .receipt-header {
          grid-template-columns: 1fr;
        }

        .divider {
          display: none;
        }

        .form-row {
          grid-template-columns: 1fr;
        }

        .option-row {
          grid-template-columns: repeat(2, minmax(0, 1fr));
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
        <div class="card receipt-header">
          <div>
            <div>Name: <?php echo htmlspecialchars($user['full_name'], ENT_QUOTES); ?></div>
            <div>Roll: 200XXXXXX</div>
            <div>Course: B.Tech</div>
            <div>Batch: 2020-24</div>
            <div>Branch: CSE</div>
          </div>
          <div class="divider"></div>
          <div style="text-align: right;">
            <div><strong>Registration Status</strong></div>
            <div>Current Semester: 6th (2023 Spring)</div>
            <div>Fee Payment: Completed</div>
            <div>Active Backlog: 0</div>
            <div>
              Current Registration Status:
              <span style="color: #f1b502; font-weight: 600;">Incomplete</span>
            </div>
          </div>
        </div>

        <div class="card receipt-form">
          <div class="section-title">Complete your Registration:</div>
          <div class="form-row">
            <div class="subtle">Choose Semester:</div>
            <select>
              <option>7th Semester (2023 - 24 Autumn)</option>
              <option>8th Semester (2024 - 25 Spring)</option>
            </select>
          </div>

          <div class="form-row">
            <div class="subtle">Electives Subjects: (Choose Priority Wise)</div>
            <div class="option-row">
              <select>
                <option>A. Choose</option>
              </select>
              <select>
                <option>B. Choose</option>
              </select>
              <select>
                <option>C. Choose</option>
              </select>
              <select>
                <option>D. Choose</option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="subtle">Apply for Minor:</div>
            <div class="option-row" style="grid-template-columns: repeat(3, minmax(0, 1fr));">
              <select>
                <option>A. Choose</option>
              </select>
              <select>
                <option>B. Choose</option>
              </select>
              <select>
                <option>C. Choose</option>
              </select>
            </div>
          </div>

          <div class="subtle" style="font-size: 11px; margin-top: 12px;">
            *By clicking on submit button you agree that all the information provided by you is
            True. If its found to be false your registration will not be considered.
          </div>
          <div style="display: flex; justify-content: flex-end; margin-top: 14px;">
            <button class="btn">Submit Now</button>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
