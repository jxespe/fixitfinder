<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_login();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $docId = (int) ($_POST['doc_id'] ?? 0);
    $status = trim($_POST['status'] ?? '');
    if ($docId > 0 && in_array($status, ['Pending', 'Approved', 'Rejected'], true)) {
        $stmt = $pdo->prepare("UPDATE provider_documents SET status = :status WHERE id = :id");
        $stmt->execute(['status' => $status, 'id' => $docId]);
    }
}

$providerFilter = (int) ($_GET['provider_id'] ?? 0);
$sql = "SELECT provider_documents.*, providers.name AS provider_name
        FROM provider_documents
        JOIN providers ON providers.id = provider_documents.provider_id";
$params = [];
if ($providerFilter > 0) {
    $sql .= " WHERE provider_documents.provider_id = :provider_id";
    $params['provider_id'] = $providerFilter;
}
$sql .= " ORDER BY provider_documents.submitted_at DESC";
$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$documents = $stmt->fetchAll();

$providers = $pdo->query("SELECT id, name FROM providers ORDER BY name ASC")->fetchAll();
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Verification Documents</title>
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
          <a class="nav-pill" href="./provider-overview.php">Service Providers</a>
          <a class="nav-pill" href="./reports-management.php">Reports</a>
          <a class="nav-pill is-active" href="./provider-documents.php">Documents</a>
        </nav>
        <a class="logout-pill" href="./logout.php">Log Out <span>⎋</span></a>
      </header>

      <div class="container">
        <div class="card" style="margin-bottom: 16px;">
          <div class="section-title">Filter by Provider</div>
          <form method="get" style="display: flex; gap: 10px; align-items: center;">
            <select name="provider_id">
              <option value="0">All providers</option>
              <?php foreach ($providers as $provider) : ?>
              <option
                value="<?php echo (int) $provider['id']; ?>"
                <?php echo $providerFilter === (int) $provider['id'] ? 'selected' : ''; ?>
              >
                <?php echo htmlspecialchars($provider['name'], ENT_QUOTES); ?>
              </option>
              <?php endforeach; ?>
            </select>
            <button class="btn small" type="submit">Apply</button>
          </form>
        </div>

        <div class="card">
          <div class="section-title">Managing Documents for Verification</div>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Provider</th>
                <th>Document Type</th>
                <th>File</th>
                <th>Status</th>
                <th>Update</th>
              </tr>
            </thead>
            <tbody>
              <?php if (!$documents) : ?>
              <tr>
                <td colspan="6">No documents available.</td>
              </tr>
              <?php else : ?>
              <?php foreach ($documents as $doc) : ?>
              <tr>
                <td><?php echo (int) $doc['id']; ?></td>
                <td><?php echo htmlspecialchars($doc['provider_name'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($doc['doc_type'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($doc['file_name'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($doc['status'], ENT_QUOTES); ?></td>
                <td>
                  <form method="post" style="display: flex; gap: 8px; align-items: center;">
                    <input type="hidden" name="doc_id" value="<?php echo (int) $doc['id']; ?>" />
                    <select name="status">
                      <option <?php echo $doc['status'] === 'Pending' ? 'selected' : ''; ?>>Pending</option>
                      <option <?php echo $doc['status'] === 'Approved' ? 'selected' : ''; ?>>Approved</option>
                      <option <?php echo $doc['status'] === 'Rejected' ? 'selected' : ''; ?>>Rejected</option>
                    </select>
                    <button class="btn small" type="submit">Save</button>
                  </form>
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
