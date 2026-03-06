<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_login();

$providerId = (int) ($_GET['id'] ?? 0);
if ($providerId <= 0) {
    $providerId = (int) $pdo->query("SELECT id FROM providers ORDER BY id ASC LIMIT 1")->fetchColumn();
}

$provider = null;
if ($providerId > 0) {
    $stmt = $pdo->prepare("SELECT * FROM providers WHERE id = :id");
    $stmt->execute(['id' => $providerId]);
    $provider = $stmt->fetch();
}

if (!$provider) {
    header("Location: service-providers.php");
    exit;
}
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Provider Profile</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      .profile-grid {
        display: grid;
        grid-template-columns: 1fr 1.5fr;
        gap: 20px;
      }

      .profile-card img {
        width: 120px;
        height: 120px;
        object-fit: contain;
        margin-bottom: 10px;
      }

      .info-line {
        font-size: 12px;
        margin-bottom: 8px;
      }

      .stat-row {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 12px;
        margin-bottom: 20px;
      }

      .stat-pill {
        text-align: center;
        border-radius: 14px;
        padding: 12px;
        background: #f6f6f6;
        border: 1px solid #e2e2e2;
        font-size: 12px;
      }

      .stat-pill span {
        display: block;
        font-weight: 600;
        margin-top: 4px;
      }

      .metric-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 16px;
      }

      .metric-card {
        border-radius: 16px;
        background: var(--primary);
        color: #fff;
        padding: 18px;
        text-align: center;
        font-weight: 600;
        box-shadow: 0 8px 18px rgba(0, 0, 0, 0.12);
      }

      .metric-card.large {
        font-size: 32px;
      }

      .metric-card.small {
        font-size: 16px;
      }

      .metric-label {
        margin-top: 10px;
        font-size: 12px;
        font-weight: 500;
      }

      @media (max-width: 980px) {
        .profile-grid {
          grid-template-columns: 1fr;
        }

        .stat-row {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .metric-grid {
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
          <a class="nav-pill is-active" href="./service-providers.php">Service Providers</a>
          <a class="nav-pill" href="./technicians.php">Technicians</a>
          <a class="nav-pill" href="./reports.php">Report</a>
        </nav>
        <a class="logout-pill" href="./logout.php">Log Out <span>⎋</span></a>
      </header>

      <div class="container">
        <div class="profile-grid">
          <div class="card profile-card">
            <img
              src="./assets/<?php echo htmlspecialchars($provider['logo'], ENT_QUOTES); ?>"
              alt="<?php echo htmlspecialchars($provider['service_type'], ENT_QUOTES); ?>"
            />
            <h3 style="font-size: 16px; margin-bottom: 8px;">
              <?php echo htmlspecialchars($provider['name'], ENT_QUOTES); ?>
            </h3>
            <div class="info-line">Services Type: <?php echo htmlspecialchars($provider['service_type'], ENT_QUOTES); ?></div>
            <div class="info-line">Branch: <?php echo htmlspecialchars($provider['branch'], ENT_QUOTES); ?></div>
            <div class="soft-divider"></div>
            <div class="info-line">Phone: <?php echo htmlspecialchars((string) $provider['phone'], ENT_QUOTES); ?></div>
            <div class="info-line">Email: <?php echo htmlspecialchars((string) $provider['email'], ENT_QUOTES); ?></div>
            <div class="soft-divider"></div>
            <div class="info-line">Address:</div>
            <div class="info-line"><?php echo htmlspecialchars((string) $provider['address'], ENT_QUOTES); ?></div>
            <button class="btn" style="margin-top: 18px; display: inline-flex; gap: 8px;">
              ✎ Request Edit
            </button>
          </div>
          <div>
            <div class="card" style="margin-bottom: 18px;">
              <div class="stat-row">
                <div class="stat-pill">Ranks <span><?php echo htmlspecialchars((string) $provider['ranking'], ENT_QUOTES); ?></span></div>
                <div class="stat-pill">Status <span><?php echo htmlspecialchars((string) $provider['status'], ENT_QUOTES); ?></span></div>
                <div class="stat-pill">Verified <span><?php echo (int) $provider['verified'] === 1 ? '★' : '○'; ?></span></div>
                <div class="stat-pill">Ratings <span><?php echo number_format((float) $provider['rating'], 2); ?></span></div>
              </div>
            </div>
            <div class="metric-grid">
              <div class="metric-card large">
                <?php echo (int) $provider['pending_count']; ?>
                <div class="metric-label">Service Pendings</div>
              </div>
              <div class="metric-card large">
                <?php echo (int) $provider['completed_count']; ?>
                <div class="metric-label">Completed Services To Users</div>
              </div>
              <div class="metric-card large">
                <?php echo (int) $provider['report_count']; ?>
                <div class="metric-label">Report</div>
              </div>
              <div class="metric-card small">
                <?php echo htmlspecialchars((string) $provider['business_hours'], ENT_QUOTES); ?>
                <div class="metric-label">Business Hours</div>
              </div>
              <div class="metric-card small" style="grid-column: span 2;">
                File Documentation
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
