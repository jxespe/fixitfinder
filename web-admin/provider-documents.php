<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_once __DIR__ . "/includes/firebase.php";
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
$providerUidFilter = trim((string) ($_GET['provider_uid'] ?? ''));
$providerNameFilter = trim((string) ($_GET['provider_name'] ?? ''));

function fetch_firestore_documents(string $providerUidFilter = '', string $providerNameFilter = ''): array
{
    $token = firebase_access_token();
    $projectId = firebase_project_id();
    $url = "https://firestore.googleapis.com/v1/projects/" . $projectId
        . "/databases/(default)/documents:runQuery";

    $payload = [
        'structuredQuery' => [
            'from' => [
                ['collectionId' => 'provider_documents']
            ],
            'orderBy' => [
                ['field' => ['fieldPath' => 'submittedAt'], 'direction' => 'DESCENDING']
            ],
            'limit' => 100
        ],
    ];

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Authorization: Bearer ' . $token
    ]);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
    $response = curl_exec($ch);
    $error = curl_error($ch);
    curl_close($ch);

    if ($response === false) {
        throw new RuntimeException("Firestore request failed: " . $error);
    }

    $rows = json_decode($response, true);
    if (!is_array($rows)) {
        return [];
    }

    $docs = [];
    $normalizedNameFilter = strtolower($providerNameFilter);
    $normalizedUidFilter = strtolower($providerUidFilter);
    foreach ($rows as $row) {
        if (empty($row['document'])) {
            continue;
        }
        $doc = $row['document'];
        $fields = firestore_decode_fields($doc['fields'] ?? []);
        $providerId = (string) ($fields['providerId'] ?? '');
        if ($normalizedUidFilter !== '' && strtolower($providerId) !== $normalizedUidFilter) {
            continue;
        }
        $providerName = (string) ($fields['providerName'] ?? '');
        if ($normalizedNameFilter !== '' && strtolower($providerName) !== $normalizedNameFilter) {
            continue;
        }
        $name = $doc['name'] ?? '';
        $parts = explode('/', $name);
        $docId = $parts ? end($parts) : '';
        $docs[] = [
            'id' => $docId,
            'provider_id' => $providerId,
            'provider_name' => $providerName !== '' ? $providerName : ($fields['providerId'] ?? 'Provider'),
            'doc_type' => $fields['docType'] ?? 'Document',
            'file_name' => $fields['fileName'] ?? '',
            'file_url' => $fields['fileUrl'] ?? '',
            'status' => $fields['status'] ?? 'Pending',
            'submitted_at' => $fields['submittedAt'] ?? ''
        ];
    }
    return $docs;
}

$documents = [];
$usingFirestore = false;
try {
    $documents = fetch_firestore_documents($providerUidFilter, $providerNameFilter);
    if (!empty($documents)) {
        $usingFirestore = true;
    }
} catch (RuntimeException $e) {
    $usingFirestore = false;
}

if (!$usingFirestore) {
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
}

$providers = $pdo->query("SELECT id, name FROM providers ORDER BY name ASC")->fetchAll();

function is_image_url(string $url): bool
{
    $path = (string) parse_url($url, PHP_URL_PATH);
    $ext = strtolower(pathinfo($path, PATHINFO_EXTENSION));
    return in_array($ext, ['jpg', 'jpeg', 'png', 'webp'], true);
}
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
                <th>Preview</th>
                <th>Status</th>
                <th>Update</th>
              </tr>
            </thead>
            <tbody>
              <?php if (!$documents) : ?>
              <tr>
                <td colspan="7">No documents available.</td>
              </tr>
              <?php else : ?>
              <?php foreach ($documents as $doc) : ?>
              <tr>
                <td><?php echo htmlspecialchars((string) $doc['id'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($doc['provider_name'], ENT_QUOTES); ?></td>
                <td><?php echo htmlspecialchars($doc['doc_type'], ENT_QUOTES); ?></td>
                <td>
                  <?php if (!empty($doc['file_url'])) : ?>
                    <a href="<?php echo htmlspecialchars($doc['file_url'], ENT_QUOTES); ?>" target="_blank">
                      <?php echo htmlspecialchars($doc['file_name'], ENT_QUOTES); ?>
                    </a>
                  <?php else : ?>
                    <?php echo htmlspecialchars($doc['file_name'], ENT_QUOTES); ?>
                  <?php endif; ?>
                </td>
                <td>
                  <?php if (!empty($doc['file_url']) && is_image_url($doc['file_url'])) : ?>
                    <a href="<?php echo htmlspecialchars($doc['file_url'], ENT_QUOTES); ?>" target="_blank">
                      <img
                        src="<?php echo htmlspecialchars($doc['file_url'], ENT_QUOTES); ?>"
                        alt="Document"
                        style="width: 60px; height: 60px; object-fit: cover; border-radius: 8px; border: 1px solid #e2e2e2;"
                      />
                    </a>
                  <?php else : ?>
                    <span class="subtle">No preview</span>
                  <?php endif; ?>
                </td>
                <td><?php echo htmlspecialchars($doc['status'], ENT_QUOTES); ?></td>
                <td>
                  <?php if (!$usingFirestore) : ?>
                    <form method="post" style="display: flex; gap: 8px; align-items: center;">
                      <input type="hidden" name="doc_id" value="<?php echo (int) $doc['id']; ?>" />
                      <select name="status">
                        <option <?php echo $doc['status'] === 'Pending' ? 'selected' : ''; ?>>Pending</option>
                        <option <?php echo $doc['status'] === 'Approved' ? 'selected' : ''; ?>>Approved</option>
                        <option <?php echo $doc['status'] === 'Rejected' ? 'selected' : ''; ?>>Rejected</option>
                      </select>
                      <button class="btn small" type="submit">Save</button>
                    </form>
                  <?php else : ?>
                    <span class="subtle">Managed in app</span>
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
