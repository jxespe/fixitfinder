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
    <title>Fix It Finder Admin - Provider Registration</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      .registration-grid {
        display: grid;
        grid-template-columns: 1.7fr 0.9fr;
        gap: 20px;
      }

      .form-header {
        text-align: center;
        font-weight: 600;
        font-size: 14px;
        margin-bottom: 12px;
        border-bottom: 1px solid var(--border);
        padding-bottom: 10px;
      }

      .subtle {
        font-size: 12px;
        color: var(--text-muted);
      }

      .info-columns {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 14px;
        font-size: 12px;
      }

      .section-label {
        font-weight: 600;
        margin-bottom: 6px;
      }

      .signature-row {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 20px;
        margin-top: 16px;
        font-size: 11px;
        color: var(--text-muted);
      }

      .id-card {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 20px;
        margin-top: 18px;
      }

      .id-card img {
        width: 100%;
        border-radius: 12px;
        border: 1px solid var(--border);
      }

      @media (max-width: 980px) {
        .registration-grid {
          grid-template-columns: 1fr;
        }

        .info-columns {
          grid-template-columns: 1fr;
        }

        .id-card {
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
        <div class="registration-grid">
          <div class="card">
            <div class="form-header">
              <?php echo htmlspecialchars($provider['name'], ENT_QUOTES); ?> Registration Form: 2023-24
            </div>
            <div class="info-columns">
              <div>
                <div>Name: Mr. <?php echo htmlspecialchars($provider['name'], ENT_QUOTES); ?></div>
                <div>Year: 2024</div>
                <div>Branch: <?php echo htmlspecialchars($provider['branch'], ENT_QUOTES); ?></div>
                <div>Type: <?php echo htmlspecialchars($provider['service_type'], ENT_QUOTES); ?></div>
              </div>
              <div>
                <div>Roll: 200XXXX</div>
                <div>Ranking: <?php echo htmlspecialchars((string) $provider['ranking'], ENT_QUOTES); ?></div>
                <div>Report: <?php echo (int) $provider['report_count']; ?></div>
              </div>
              <div style="text-align: right;">
                <img
                  src="./assets/<?php echo htmlspecialchars($provider['logo'], ENT_QUOTES); ?>"
                  alt="<?php echo htmlspecialchars($provider['service_type'], ENT_QUOTES); ?>"
                  style="width: 70px; height: 70px; object-fit: contain;"
                />
              </div>
            </div>
            <div class="soft-divider"></div>
            <div>
              <div class="section-label">Vision</div>
              <p class="subtle">
                To become the most trusted and reliable plumbing service provider in the community,
                known for quality workmanship, fast response, and long-lasting solutions that ensure
                safe and efficient water systems for every home and business.
              </p>
            </div>
            <div style="margin-top: 16px;">
              <div class="section-label">Mission</div>
              <p class="subtle">At J Plumbing Services, our mission is to:</p>
              <ul class="subtle" style="padding-left: 16px; margin-top: 8px;">
                <li>Deliver high-quality plumbing solutions with precision and professionalism.</li>
                <li>Provide prompt, dependable, and affordable services to residential and commercial clients.</li>
                <li>Maintain consistent communication, safety, and environmental responsibility in all projects.</li>
              </ul>
            </div>
            <div class="soft-divider"></div>
            <div style="display: flex; justify-content: space-between; font-size: 12px;">
              <div>Registration Status: Complete ✓</div>
              <div>Payment Status: Verified ✓</div>
            </div>
            <div class="signature-row">
              <div>Signature of Administration</div>
              <div>Signature of Chief Technology Officer</div>
            </div>
            <div class="soft-divider"></div>
            <div class="section-label">Identification Card</div>
            <div class="subtle">
              <?php echo htmlspecialchars($provider['name'], ENT_QUOTES); ?> | ROLL: 200XXXX | 2020-24
            </div>
            <div class="id-card">
              <img src="./assets/ic_electrical_repair.png" alt="ID 1" />
              <img src="./assets/ic_electronics_repair.png" alt="ID 2" />
            </div>
          </div>
          <div>
            <div class="card" style="margin-bottom: 16px;">
              <div class="section-title">Specific Search:</div>
              <div style="display: grid; gap: 10px;">
                <input class="input" type="text" value="<?php echo (int) $provider['id']; ?>" />
                <input class="input" type="text" value="<?php echo htmlspecialchars($provider['name'], ENT_QUOTES); ?>" />
                <input class="input" type="text" value="<?php echo htmlspecialchars($provider['service_type'], ENT_QUOTES); ?>" />
                <button class="btn small">Apply</button>
              </div>
            </div>
            <div class="card" style="margin-bottom: 16px;">
              <div class="section-title">Contact:</div>
              <div class="subtle">Phone: <?php echo htmlspecialchars((string) $provider['phone'], ENT_QUOTES); ?></div>
              <div class="subtle">Email: <?php echo htmlspecialchars((string) $provider['email'], ENT_QUOTES); ?></div>
            </div>
            <div class="card" style="text-align: center;">
              <button class="btn">Print Registration Slip</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
