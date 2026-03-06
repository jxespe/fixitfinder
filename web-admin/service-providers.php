<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_login();

$providers = $pdo->query("SELECT * FROM providers ORDER BY id ASC")->fetchAll();
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Service Providers</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      .provider-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 40px;
        padding: 40px 10px;
        place-items: center;
      }

      .provider-logo {
        width: 140px;
        height: 140px;
        border-radius: 50%;
        border: 1px solid #cfcfcf;
        display: grid;
        place-items: center;
        background: #fff;
      }

      .provider-logo img {
        width: 92px;
        height: 92px;
        object-fit: contain;
      }

      @media (max-width: 900px) {
        .provider-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
      }

      @media (max-width: 600px) {
        .provider-grid {
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
        <div class="card">
          <div class="section-title">Service Providers</div>
          <div class="provider-grid">
            <?php foreach ($providers as $provider) : ?>
            <a
              class="provider-logo"
              href="./provider-profile.php?id=<?php echo (int) $provider['id']; ?>"
              style="<?php echo $provider['name'] === 'Craft Fix' ? 'width: 170px; height: 170px;' : ''; ?>"
            >
              <img
                src="./assets/<?php echo htmlspecialchars($provider['logo'], ENT_QUOTES); ?>"
                alt="<?php echo htmlspecialchars($provider['name'], ENT_QUOTES); ?>"
              />
            </a>
            <?php endforeach; ?>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
