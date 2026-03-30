<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_once __DIR__ . "/includes/firebase.php";
require_login();

$providers = [];
$usingFirestore = false;
$documentsByProvider = [];
try {
    $firestoreProviders = firestore_list_collection('providers', 'createdAt');
    if (!empty($firestoreProviders)) {
        foreach ($firestoreProviders as $provider) {
            $providers[] = [
                'id' => $provider['id'] ?? '',
                'name' => $provider['fullName'] ?? ($provider['name'] ?? 'Provider'),
                'service_type' => $provider['serviceCategory'] ?? ($provider['serviceType'] ?? 'Service'),
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
    $providers = $pdo->query("SELECT * FROM providers ORDER BY id ASC")->fetchAll();
}
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Service Provider Overview</title>
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
          <a class="nav-pill is-active" href="./provider-overview.php">Service Providers</a>
          <a class="nav-pill" href="./reports-management.php">Reports</a>
          <a class="nav-pill" href="./provider-documents.php">Documents</a>
        </nav>
        <a class="logout-pill" href="./logout.php">Log Out <span>⎋</span></a>
      </header>

      <div class="container">
        <div class="card">
          <div class="section-title">Service Provider Overview</div>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Provider</th>
                <th>Service</th>
                <th>Branch</th>
                <th>Status</th>
                <th>Verified</th>
                <th>Profile</th>
                <th>Registration</th>
              </tr>
            </thead>
            <tbody>
              <?php if (!$providers) : ?>
              <tr>
                <td colspan="8">No providers available.</td>
              </tr>
              <?php else : ?>
              <?php foreach ($providers as $provider) : ?>
              <tr>
                <td><?php echo htmlspecialchars((string) $provider['id'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($provider['name'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($provider['service_type'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($provider['branch'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($provider['status'], ENT_QUOTES); ?></td>
                <td><?php echo !empty($provider['verified']) ? 'Yes' : 'No'; ?></td>
                <td>
                  <a
                    class="status-open"
                    href="./provider-profile.php?<?php echo $usingFirestore ? 'uid=' . urlencode((string) $provider['id']) : 'id=' . (int) $provider['id']; ?>"
                  >
                    Open
                  </a>
                </td>
                <td>
                  <?php if ($usingFirestore) : ?>
                    <a
                      class="status-open"
                      href="./provider-documents.php?provider_uid=<?php echo urlencode((string) $provider['id']); ?>"
                    >
                      <?php echo !empty($documentsByProvider[(string) $provider['id']]) ? 'Submitted' : 'Missing'; ?>
                    </a>
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
