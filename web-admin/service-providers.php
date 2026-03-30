<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_once __DIR__ . "/includes/firebase.php";
require_login();

$providers = [];
$categories = [];
$usingFirestore = false;
try {
    $firestoreProviders = firestore_list_collection('providers', 'createdAt');
    if (!empty($firestoreProviders)) {
        foreach ($firestoreProviders as $provider) {
            $serviceType = (string) ($provider['serviceCategory'] ?? ($provider['serviceType'] ?? 'Service'));
            $providers[] = [
                'id' => $provider['id'] ?? '',
                'name' => $provider['fullName'] ?? ($provider['name'] ?? 'Provider'),
                'service_type' => $serviceType,
            ];
            if ($serviceType !== '') {
                if (!isset($categories[$serviceType])) {
                    $categories[$serviceType] = 0;
                }
                $categories[$serviceType]++;
            }
        }
        $usingFirestore = true;
    }
} catch (RuntimeException $e) {
    $usingFirestore = false;
}

if (!$usingFirestore) {
    $providers = $pdo->query("SELECT * FROM providers ORDER BY id ASC")->fetchAll();
    foreach ($providers as $provider) {
        $serviceType = (string) ($provider['service_type'] ?? 'Service');
        if (!isset($categories[$serviceType])) {
            $categories[$serviceType] = 0;
        }
        $categories[$serviceType]++;
    }
}

ksort($categories, SORT_NATURAL | SORT_FLAG_CASE);
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Service Providers</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      .category-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 24px;
        padding: 24px 10px;
      }

      .category-card {
        border-radius: 18px;
        border: 1px solid #e3e3e3;
        background: #fff;
        padding: 20px;
        display: flex;
        flex-direction: column;
        gap: 10px;
        text-decoration: none;
        color: inherit;
        box-shadow: 0 10px 24px rgba(0, 0, 0, 0.06);
      }

      .category-title {
        font-size: 16px;
        font-weight: 600;
      }

      .category-count {
        font-size: 12px;
        color: var(--text-muted);
      }

      @media (max-width: 900px) {
        .category-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
      }

      @media (max-width: 600px) {
        .category-grid {
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
          <div class="section-title">Service Provider Categories</div>
          <div class="category-grid">
            <?php if (!$categories) : ?>
              <div class="subtle">No provider categories available.</div>
            <?php else : ?>
              <?php foreach ($categories as $category => $count) : ?>
                <a class="category-card" href="./provider-category.php?category=<?php echo urlencode((string) $category); ?>">
                  <div class="category-title"><?php echo htmlspecialchars((string) $category, ENT_QUOTES); ?></div>
                  <div class="category-count"><?php echo (int) $count; ?> provider(s)</div>
                </a>
              <?php endforeach; ?>
            <?php endif; ?>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
