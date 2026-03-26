<?php
/**
 * Notification Debug & Utility Tool
 * ---------------------------------
 * Use this script to:
 * 1. Verify database connection
 * 2. Migrate (Create) notifications table automatically
 * 3. View/Dump notification data via browser
 * 
 * Usage:
 * - View Table: /notification_debug.php
 * - View by Member: /notification_debug.php?memberid=USER123
 * - Run Migration: /notification_debug.php?action=migrate
 */

header('Content-Type: text/html; charset=utf-8');

// --- Configuration ---
$host = "localhost";
$user = "zoqlszwh_ananyadb";
$pass = "IntelliP24.X";
$dbname = "zoqlszwh_ananyadb";

// --- Database Connection ---
try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_OBJ);
} catch (PDOException $e) {
    die("<div style='color:red; font-weight:bold;'>❌ Connection failed: " . $e->getMessage() . "</div>");
}

$action = $_GET['action'] ?? '';
$memberId = $_GET['memberid'] ?? '';

echo "<html><head><title>Notification Debug Tool</title>";
echo "<style>
    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f4f7f6; padding: 20px; }
    .container { max-width: 1200px; margin: auto; background: white; padding: 25px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
    h1 { color: #C59D5F; border-bottom: 2px solid #C59D5F; padding-bottom: 10px; }
    .status { padding: 10px; margin-bottom: 20px; border-radius: 4px; border: 1px solid #ccc; }
    .status.ok { background: #e8f5e9; color: #2e7d32; border-color: #c8e6c9; }
    .status.error { background: #ffebee; color: #c62828; border-color: #ffcdd2; }
    table { width: 100%; border-collapse: collapse; margin-top: 20px; font-size: 14px; }
    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #eee; }
    th { background: #f8f9fa; color: #333; font-weight: 600; }
    tr:hover { background: #f1f1f1; }
    .badge { padding: 4px 8px; border-radius: 12px; font-size: 11px; font-weight: bold; text-transform: uppercase; }
    .badge-read { background: #e8f5e9; color: #2e7d32; }
    .badge-unread { background: #fff3e0; color: #ef6c00; }
    .type-tag { background: #eceff1; color: #455a64; padding: 2px 6px; border-radius: 4px; font-family: monospace; }
    .btn { display: inline-block; background: #C59D5F; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; font-weight: bold; }
    .btn:hover { background: #a8854d; }
    pre { background: #333; color: #eee; padding: 15px; border-radius: 4px; overflow-x: auto; }
</style></head><body><div class='container'>";

echo "<h1>🔔 Notification System Debug Tool</h1>";

// --- Action: Migrate ---
if ($action === 'migrate') {
    echo "<h3>🛠️ Running Migration...</h3>";
    $sql = "CREATE TABLE IF NOT EXISTS notifications (
        id INT AUTO_INCREMENT PRIMARY KEY,
        member_id VARCHAR(50) NOT NULL,
        type VARCHAR(50) NOT NULL,
        title VARCHAR(255) NOT NULL,
        body TEXT,
        url TEXT,
        note TEXT,
        is_read BOOLEAN DEFAULT FALSE,
        read_at TIMESTAMP NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_notifications_member_type (member_id, type),
        INDEX idx_notifications_created (created_at DESC),
        INDEX idx_notifications_member_created (member_id, created_at DESC),
        INDEX idx_notifications_unread (member_id, is_read)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

    try {
        $pdo->exec($sql);
        echo "<div class='status ok'>✅ TABLE 'notifications' created or already exists.</div>";
    } catch (PDOException $e) {
        echo "<div class='status error'>❌ Migration failed: " . $e->getMessage() . "</div>";
    }
    echo "<br><a href='notification_debug.php' class='btn'>Back to Data View</a>";
}
// --- Action: View Data ---
else {
    // Check if table exists
    try {
        $stmtCheck = $pdo->prepare("SHOW TABLES LIKE 'notifications'");
        $stmtCheck->execute();
        $tableExists = $stmtCheck->fetch();

        if (!$tableExists) {
            echo "<div class='status error'>❌ Table 'notifications' does not exist in database '$dbname'.</div>";
            echo "<p>Please click the button below to migrate the database automatically.</p>";
            echo "<a href='notification_debug.php?action=migrate' class='btn'>🚀 Run Database Migration</a>";
        } else {
            echo "<div class='status ok'>✅ Database Connected & Table Ready</div>";

            // Build Query
            $sql = "SELECT * FROM notifications";
            $params = [];
            if ($memberId) {
                $sql .= " WHERE member_id = :mid";
                $params['mid'] = $memberId;
            }
            $sql .= " ORDER BY created_at DESC LIMIT 50";

            $stmt = $pdo->prepare($sql);
            $stmt->execute($params);
            $rows = $stmt->fetchAll();

            echo "<h3>📊 Latest 50 Notifications " . ($memberId ? "for User: <b>$memberId</b>" : "(Global View)") . "</h3>";

            if (count($rows) > 0) {
                echo "<table>";
                echo "<tr>
                        <th>ID</th>
                        <th>Member</th>
                        <th>Type</th>
                        <th>Title & Body</th>
                        <th>Status</th>
                        <th>Created At</th>
                      </tr>";
                foreach ($rows as $row) {
                    $statusBadge = $row->is_read
                        ? "<span class='badge badge-read'>Read</span>"
                        : "<span class='badge badge-unread'>Unread</span>";

                    echo "<tr>";
                    echo "<td>{$row->id}</td>";
                    echo "<td><a href='notification_debug.php?memberid={$row->member_id}'>{$row->member_id}</a></td>";
                    echo "<td><span class='type-tag'>{$row->type}</span></td>";
                    echo "<td><b>" . htmlspecialchars($row->title) . "</b><br><small style='color:#666;'>" . htmlspecialchars($row->body) . "</small></td>";
                    echo "<td>$statusBadge</td>";
                    echo "<td>{$row->created_at}</td>";
                    echo "</tr>";
                }
                echo "</table>";
            } else {
                echo "<p>No notifications found in the database yet.</p>";
            }

            echo "<br><hr><br>";
            echo "<h4>🛠️ Admin Utilities</h4>";
            echo "<a href='notification_debug.php?action=migrate' style='color:#666; font-size:12px;'>Force Run Migration Again</a>";
            if ($memberId) {
                echo " | <a href='notification_debug.php' style='color:#666; font-size:12px;'>Show All Users</a>";
            }
        }
    } catch (PDOException $e) {
        echo "<div class='status error'>❌ Database Error: " . $e->getMessage() . "</div>";
    }
}

echo "</div></body></html>";
