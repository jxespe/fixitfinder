<?php
declare(strict_types=1);
require_once __DIR__ . "/includes/auth.php";
require_once __DIR__ . "/includes/firebase.php";
require_login();

$userUid = trim((string) ($_GET['uid'] ?? ''));
$userId = (int) ($_GET['id'] ?? 0);
$usingFirestore = false;
$user = null;

if ($userUid !== '') {
    try {
        $doc = firestore_get_document('users', $userUid);
        if (is_array($doc)) {
            $user = [
                'id' => $doc['id'] ?? $userUid,
                'full_name' => $doc['fullName'] ?? ($doc['name'] ?? 'User'),
                'email' => $doc['email'] ?? '',
                'phone' => $doc['phone'] ?? '',
                'verified' => $doc['verified'] ?? ($doc['phoneVerified'] ?? false),
            ];
            $usingFirestore = true;
        }
    } catch (RuntimeException $e) {
        $usingFirestore = false;
    }
}

if (!$usingFirestore) {
    if ($userId <= 0) {
        $userId = (int) $pdo->query("SELECT id FROM users ORDER BY id ASC LIMIT 1")->fetchColumn();
    }
    if ($userId > 0) {
        $stmt = $pdo->prepare("SELECT * FROM users WHERE id = :id");
        $stmt->execute(['id' => $userId]);
        $user = $stmt->fetch();
    }
}

if (!$user) {
    header("Location: users.php");
    exit;
}

$pendingItems = [];
$transactions = [];
if (!$usingFirestore) {
    $pendingItems = $pdo->prepare("SELECT * FROM user_pending_items WHERE user_id = :id");
    $pendingItems->execute(['id' => $userId]);
    $pendingItems = $pendingItems->fetchAll();

    $transactions = $pdo->prepare("SELECT * FROM user_transactions WHERE user_id = :id ORDER BY date DESC");
    $transactions->execute(['id' => $userId]);
    $transactions = $transactions->fetchAll();
}

$totalPending = 0.0;
foreach ($pendingItems as $item) {
    $totalPending += (float) ($item['amount'] ?? 0);
}
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Fix It Finder Admin - User Detail</title>
    <link rel="stylesheet" href="./styles.css" />
    <style>
      .header-card {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 16px;
        padding: 14px 18px;
      }

      .name-chip {
        display: flex;
        align-items: center;
        gap: 12px;
        font-weight: 600;
      }

      .name-chip img {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        background: #eee;
      }

      .pending-grid {
        display: grid;
        grid-template-columns: 1.3fr 0.6fr;
        gap: 16px;
        margin-top: 12px;
      }

      .pending-card table {
        width: 100%;
        border-collapse: collapse;
        font-size: 12px;
      }

      .pending-card td,
      .pending-card th {
        border: 1px solid #4b4b4b;
        padding: 6px 8px;
      }

      .status-box {
        display: grid;
        place-items: center;
        background: #f1b502;
        border-radius: 12px;
        color: #2c2200;
        font-weight: 600;
        font-size: 14px;
      }

      .history-filters {
        display: grid;
        grid-template-columns: 1fr 1fr auto;
        gap: 12px;
        align-items: center;
        margin: 12px 0;
      }

      .history-table .table td {
        background: #dce9f7;
      }

      @media (max-width: 980px) {
        .pending-grid {
          grid-template-columns: 1fr;
        }

        .history-filters {
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
          <a class="nav-pill is-active" href="./users.php">Users</a>
          <a class="nav-pill" href="./service-providers.php">Service Providers</a>
          <a class="nav-pill" href="./technicians.php">Technicians</a>
          <a class="nav-pill" href="./reports.php">Report</a>
        </nav>
        <a class="logout-pill" href="./logout.php">Log Out <span>⎋</span></a>
      </header>

      <div class="container">
        <div class="card header-card">
          <div class="name-chip">
            <span>Name: <?php echo htmlspecialchars($user['full_name'], ENT_QUOTES); ?></span>
            <img src="./assets/ic_banner.png" alt="User" />
            <span style="color: #e01b1b; font-size: 11px;">
              <?php echo htmlspecialchars((string) $user['id'], ENT_QUOTES); ?>
            </span>
          </div>
          <?php if (!empty($user['verified'])) : ?>
          <div class="badge success">Verified ✅</div>
          <?php else : ?>
          <div class="badge warning">Pending</div>
          <?php endif; ?>
        </div>

        <div class="card" style="margin-top: 16px;">
          <div class="section-title">Pendings</div>
          <div class="pending-grid">
            <div class="pending-card">
              <div style="display: flex; gap: 12px; margin-bottom: 10px;">
                <div style="flex: 1;">
                  <div class="subtle">Select Services:</div>
                  <select>
                    <option>Plumbing</option>
                    <option>Carpentry</option>
                  </select>
                </div>
                <div style="flex: 1;">
                  <div class="subtle">Search</div>
                  <input class="input" type="text" />
                </div>
              </div>
              <table>
                <thead>
                  <tr>
                    <th>Service Fee</th>
                    <th></th>
                    <th></th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <?php if (!$pendingItems) : ?>
                  <tr>
                    <td colspan="4">No pending items.</td>
                  </tr>
                  <?php else : ?>
                  <?php foreach ($pendingItems as $index => $item) : ?>
                  <tr>
                    <td><?php echo $index + 1; ?>.</td>
                    <td><?php echo htmlspecialchars($item['description'], ENT_QUOTES); ?></td>
                    <td>
                      <?php echo $item['amount'] === null ? 'N/A' : 'Php ' . number_format((float) $item['amount']); ?>
                    </td>
                    <td></td>
                  </tr>
                  <?php endforeach; ?>
                  <tr>
                    <td></td>
                    <td>Total</td>
                    <td>Php <?php echo number_format($totalPending); ?></td>
                    <td></td>
                  </tr>
                  <?php endif; ?>
                </tbody>
              </table>
            </div>
            <div class="status-box">Pending (Cash)</div>
          </div>
        </div>

        <div class="card" style="margin-top: 16px;">
          <div class="section-title">Transaction History</div>
          <div class="history-filters">
            <div>
              <div class="subtle">Select Date:</div>
              <select>
                <option>2022-23</option>
                <option>2023-24</option>
              </select>
            </div>
            <div>
              <div class="subtle">Add Filter</div>
              <select>
                <option>All Payments</option>
                <option>Verified</option>
              </select>
            </div>
            <button class="btn small">Search</button>
          </div>
          <div class="history-table">
            <table class="table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Amount</th>
                  <th>Type</th>
                  <th>Transaction ID</th>
                  <th>Verification Status</th>
                  <th>Receipt</th>
                </tr>
              </thead>
              <tbody>
                <?php if (!$transactions) : ?>
                <tr>
                  <td colspan="6">No transactions available.</td>
                </tr>
                <?php else : ?>
                <?php foreach ($transactions as $txn) : ?>
                <tr>
                  <td><?php echo htmlspecialchars($txn['date'], ENT_QUOTES); ?></td>
                  <td>Php <?php echo number_format((float) $txn['amount']); ?></td>
                  <td><?php echo htmlspecialchars($txn['type'], ENT_QUOTES); ?></td>
                  <td><?php echo htmlspecialchars($txn['transaction_id'], ENT_QUOTES); ?></td>
                  <td><?php echo htmlspecialchars($txn['verification_status'], ENT_QUOTES); ?></td>
                  <td><?php echo htmlspecialchars((string) $txn['receipt'], ENT_QUOTES); ?></td>
                </tr>
                <?php endforeach; ?>
                <?php endif; ?>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
