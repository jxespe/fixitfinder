<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_once __DIR__ . "/includes/firebase.php";
require_login();

$category = trim((string) ($_GET['category'] ?? ''));
if ($category === '') {
    header("Location: service-providers.php");
    exit;
}

$providers = [];
$usingFirestore = false;
$documentsByProvider = [];
try {
    $firestoreProviders = firestore_list_collection('providers', 'createdAt');
    if (!empty($firestoreProviders)) {
        $normalized = strtolower($category);
        foreach ($firestoreProviders as $provider) {
            $serviceCategory = (string) ($provider['serviceCategory'] ?? ($provider['serviceType'] ?? ''));
            $serviceCategoryLower = (string) ($provider['serviceCategoryLower'] ?? strtolower($serviceCategory));
            if ($serviceCategoryLower !== $normalized) {
                continue;
            }
            $providers[] = [
                'id' => $provider['id'] ?? '',
                'name' => $provider['fullName'] ?? ($provider['name'] ?? 'Provider'),
                'branch' => $provider['branch'] ?? ($provider['address'] ?? 'N/A'),
                'status' => $provider['status'] ?? 'Active',
                'verified' => $provider['verified'] ?? ($provider['phoneVerified'] ?? false),
            ];
        }
        $usingFirestore = true;
        $docs = firestore_list_collection('provider_documents', 'submittedAt');
        foreach ($docs as $doc) {
            $providerId = (string) ($doc['providerId'] ?? '');
            if ($providerId !== '') {
                $documentsByProvider[$providerId] = true;
            }
        }
    }
} catch (RuntimeException $e) {
    $usingFirestore = false;
}

if (!$usingFirestore) {
    $stmt = $pdo->prepare("SELECT * FROM providers WHERE service_type = :category ORDER BY id ASC");
    $stmt->execute(['category' => $category]);
    $providers = $stmt->fetchAll();
}
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - <?php echo htmlspecialchars($category, ENT_QUOTES); ?></title>
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
          <a class="nav-pill" href="./users.php">Users</a>
          <a class="nav-pill is-active" href="./service-providers.php">Service Providers</a>
          <a class="nav-pill" href="./technicians.php">Technicians</a>
          <a class="nav-pill" href="./reports.php">Report</a>
        </nav>
        <a class="logout-pill" href="./logout.php">Log Out <span>⎋</span></a>
      </header>

      <div class="container">
        <div class="card">
          <div class="section-title">
            <?php echo htmlspecialchars($category, ENT_QUOTES); ?> Providers
          </div>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Provider</th>
                <th>Branch</th>
                <th>Status</th>
                <th>Verified</th>
                <th>Registration</th>
              </tr>
            </thead>
            <tbody>
              <?php if (!$providers) : ?>
              <tr>
                <td colspan="6">No providers available for this category.</td>
              </tr>
              <?php else : ?>
              <?php foreach ($providers as $provider) : ?>
              <tr>
                <td><?php echo htmlspecialchars((string) $provider['id'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($provider['name'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($provider['branch'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($provider['status'], ENT_QUOTES); ?></td>
                <td><?php echo !empty($provider['verified']) ? 'Yes' : 'No'; ?></td>
                <td>
                  <?php if ($usingFirestore) : ?>
                    <?php echo !empty($documentsByProvider[(string) $provider['id']]) ? 'Submitted' : 'Missing'; ?>
                  <?php else : ?>
                    <a class="status-open" href="./provider-documents.php?provider_id=<?php echo (int) $provider['id']; ?>">
                      Registration
                    </a>
                  <?php endif; ?>
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
