<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_once __DIR__ . "/includes/firebase.php";
require_login();

$providers = [];
$usingFirestore = false;
try {
    $firestoreProviders = firestore_list_collection('providers', 'createdAt', 'DESCENDING', 8);
    if (!empty($firestoreProviders)) {
        foreach ($firestoreProviders as $provider) {
            $providers[] = [
                'id' => $provider['id'] ?? '',
                'name' => $provider['fullName'] ?? ($provider['name'] ?? 'Provider'),
                'branch' => $provider['branch'] ?? ($provider['address'] ?? 'N/A'),
                'status' => $provider['status'] ?? 'Active',
                'verified' => $provider['verified'] ?? ($provider['phoneVerified'] ?? false),
            ];
        }
        $usingFirestore = true;
    }
} catch (RuntimeException $e) {
    $usingFirestore = false;
}

if (!$usingFirestore) {
    $providers = $pdo->query("SELECT * FROM providers ORDER BY id ASC LIMIT 8")->fetchAll();
}
$loginFlash = get_flash('login_success');
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - Dashboard</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      .quick-actions {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 16px;
        margin-bottom: 22px;
      }

      .quick-card {
        background: #f1f1f1;
        border-radius: 14px;
        padding: 16px;
        text-align: center;
        border: 1px solid #e1e1e1;
      }

      .quick-icon {
        width: 44px;
        height: 44px;
        border-radius: 10px;
        background: #dedede;
        display: grid;
        place-items: center;
        margin: 0 auto 10px;
      }

      .filters {
        display: grid;
        grid-template-columns: 1.1fr 1fr;
        gap: 20px;
        margin-top: 12px;
      }

      .filter-row {
        display: grid;
        grid-template-columns: 1fr 1fr auto;
        gap: 10px;
        align-items: center;
        margin-bottom: 10px;
      }

      .filter-row.two {
        grid-template-columns: 1fr 1fr auto;
      }

      .filter-row.single {
        grid-template-columns: 0.9fr 1.1fr auto;
      }

      .status-pill {
        background: #f8f8f8;
        border: 1px solid #d9d9d9;
        border-radius: 999px;
        padding: 4px 10px;
        font-size: 12px;
        display: inline-flex;
        align-items: center;
        gap: 8px;
      }

      .toggle {
        width: 28px;
        height: 14px;
        background: #f6f6f6;
        border-radius: 999px;
        position: relative;
        border: 1px solid #cfcfcf;
      }

      .toggle::after {
        content: "";
        width: 12px;
        height: 12px;
        background: #f7b233;
        border-radius: 50%;
        position: absolute;
        top: 0.5px;
        left: 14px;
      }

      .table-wrap {
        margin-top: 18px;
      }

      .pagination {
        display: flex;
        justify-content: space-between;
        margin: 10px 0 16px;
      }

      .export-card {
        display: flex;
        justify-content: space-between;
        align-items: center;
        background: #fff;
        border-radius: 14px;
        padding: 14px 18px;
        border: 1px solid var(--border);
      }

      @media (max-width: 980px) {
        .quick-actions {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .filters {
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
          <a class="nav-pill is-active" href="./dashboard.php">Dashboard</a>
          <a class="nav-pill" href="./users.php">Users</a>
          <a class="nav-pill" href="./service-providers.php">Service Providers</a>
          <a class="nav-pill" href="./technicians.php">Technicians</a>
          <a class="nav-pill" href="./reports.php">Report</a>
        </nav>
        <a class="logout-pill" href="./logout.php">Log Out <span>⎋</span></a>
      </header>

      <div class="container">
        <div class="card">
          <?php if (!empty($loginFlash)) : ?>
          <div class="message success" style="margin-bottom: 12px;">
            <?php echo htmlspecialchars($loginFlash, ENT_QUOTES); ?>
          </div>
          <?php endif; ?>
          <div class="section-title">Quick Actions:</div>
          <div class="quick-actions">
            <div class="quick-card">
              <div class="quick-icon">💵</div>
              <div>Fee Payment Status</div>
            </div>
            <div class="quick-card">
              <div class="quick-icon">📋</div>
              <div>Registration Status</div>
            </div>
            <div class="quick-card">
              <div class="quick-icon">🧾</div>
              <div>Verification Request</div>
            </div>
            <div class="quick-card">
              <div class="quick-icon">📤</div>
              <div>Upload Documents</div>
            </div>
          </div>

          <div class="section-title">Search for Service Providers and Users:</div>
          <div class="soft-divider"></div>
          <div class="filters">
            <div>
              <div class="filter-row">
                <select>
                  <option>Select Users Services</option>
                  <option>Plumbing</option>
                  <option>Carpentry</option>
                </select>
                <select>
                  <option>Select Branch</option>
                  <option>Manila</option>
                  <option>Caloocan</option>
                </select>
                <button class="btn small">Apply</button>
              </div>
              <div class="filter-row two">
                <select>
                  <option>Status</option>
                  <option>Active</option>
                  <option>Offline</option>
                </select>
                <select>
                  <option>Verification</option>
                  <option>Registered</option>
                  <option>Verified</option>
                </select>
                <button class="btn small">Apply</button>
              </div>
              <div class="filter-row single">
                <select>
                  <option>ID Number</option>
                  <option>Name</option>
                </select>
                <input class="input" type="text" placeholder="Name (Auto)" />
                <button class="btn small">Apply</button>
              </div>
            </div>
            <div style="display: flex; justify-content: flex-end;">
              <div class="status-pill">
                <span>Master List</span>
                <div class="toggle"></div>
              </div>
            </div>
          </div>

          <div class="section-title" style="margin-top: 20px;">Admin Functions</div>
          <div class="quick-actions">
            <a class="quick-card" href="./user-overview.php">
              <div class="quick-icon">👤</div>
              <div>User Overview</div>
            </a>
            <a class="quick-card" href="./provider-overview.php">
              <div class="quick-icon">🛠️</div>
              <div>Service Provider Overview</div>
            </a>
            <a class="quick-card" href="./reports-management.php">
              <div class="quick-icon">🐞</div>
              <div>Managing Bugs & Reports</div>
            </a>
            <a class="quick-card" href="./provider-documents.php">
              <div class="quick-icon">📄</div>
              <div>Verification Documents</div>
            </a>
          </div>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>ID Number</th>
                  <th>Users / Services</th>
                  <th>Branch</th>
                  <th>Year</th>
                  <th>Status</th>
                  <th>Registration</th>
                  <th>Verification</th>
                  <th>Details</th>
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
                  <td><?php echo htmlspecialchars($provider['branch'], ENT_QUOTES); ?></td>
                  <td>2023-24</td>
                  <td><?php echo htmlspecialchars($provider['status'], ENT_QUOTES); ?></td>
                  <td>Registered</td>
                  <td><?php echo !empty($provider['verified']) ? 'Verified' : 'Pending'; ?></td>
                  <td>
                    <a
                      class="status-open"
                      href="./provider-profile.php?<?php echo $usingFirestore ? 'uid=' . urlencode((string) $provider['id']) : 'id=' . (int) $provider['id']; ?>"
                    >
                      Open
                    </a>
                  </td>
                </tr>
                <?php endforeach; ?>
                <?php endif; ?>
              </tbody>
            </table>
          </div>

          <div class="pagination">
            <button class="btn small">Previous</button>
            <button class="btn small">Next</button>
          </div>

          <div class="export-card">
            <div>
              <div class="section-title" style="margin-bottom: 4px;">
                Export Result as Excel File
              </div>
            </div>
            <div style="display: flex; align-items: center; gap: 16px;">
              <span style="font-size: 12px; color: var(--text-muted);">
                Last updated on: 12 March | 23:00
              </span>
              <button class="btn small">Export</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
