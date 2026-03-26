<?php
require_once 'vendor/autoload.php';
require_once 'configs/config.php';

$dbConfig = $config['db'];
$pdo = new PDO(
    "mysql:host=" . $dbConfig['host'] . ";dbname=" . $dbConfig['dbname'],
    $dbConfig['user'],
    $dbConfig['pass']
);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->exec("set names utf8mb4");

echo "<pre>";
echo "<h3>Fixing Schema for user_merit_assign</h3>";

// Helper function to check column existence
function columnExists($pdo, $table, $column)
{
    $stmt = $pdo->prepare("SHOW COLUMNS FROM $table LIKE :col");
    $stmt->execute([':col' => $column]);
    return $stmt->fetch() !== false;
}

$table = 'user_merit_assign';
$columnsNeeded = [
    'merit_type' => "VARCHAR(50) NOT NULL DEFAULT 'merit' COMMENT 'merit, changenum, spell'",
    'title' => "VARCHAR(255)",
    'body' => "TEXT",
    'url' => "TEXT",
    'assigned_at' => "DATETIME DEFAULT CURRENT_TIMESTAMP"
];

foreach ($columnsNeeded as $col => $def) {
    if (!columnExists($pdo, $table, $col)) {
        echo "Adding column '$col'...\n";
        try {
            $pdo->exec("ALTER TABLE $table ADD COLUMN $col $def");
            echo "Success.\n";
        } catch (Exception $e) {
            echo "Error adding $col: " . $e->getMessage() . "\n";
        }
    } else {
        echo "Column '$col' already exists.\n";
    }
}

echo "<h3>Re-running Population for User 832...</h3>";

$mid = 832;
// Clear existing
$pdo->exec("DELETE FROM user_merit_assign WHERE memberid = $mid");

// Data
$data = [
    ['mid' => $mid, 'type' => 'merit', 'title' => 'วิธีการทำบุญ', 'body' => 'คุณนินแนะนำ : ปางวันเสาร์', 'url' => '', 'at' => '2026-02-02 02:34:10'],
    ['mid' => $mid, 'type' => 'merit', 'title' => 'วิธีการทำบุญ', 'body' => 'คุณนินแนะนำ : ปางวันจันทร์', 'url' => '', 'at' => '2026-02-02 02:33:56'],
    ['mid' => $mid, 'type' => 'changenum', 'title' => 'ขั้นตอนการเปลี่ยนแปลง', 'body' => 'คุณนินแนะนำ : ขั้นตอนการเปลี่ยนแปลงเบอร์โทรศัพท์', 'url' => '', 'at' => '2026-02-02 02:34:48'],
    ['mid' => $mid, 'type' => 'changenum', 'title' => 'ขั้นตอนการเปลี่ยนแปลง', 'body' => 'คุณนินแนะนำ : ขั้นตอนการเปลี่ยนแปลงชื่อจริง นามสกุล', 'url' => '', 'at' => '2026-02-02 02:34:31'],
    ['mid' => $mid, 'type' => 'spell', 'title' => 'คาถาและคำเตือนพิเศษ', 'body' => 'คุณนินแนะนำ : คาถาชินบัญชร โดยสมเด็จพระพุฒาจารย์', 'url' => '', 'at' => '2026-02-02 02:15:07']
];

$sql = "INSERT INTO user_merit_assign (memberid, merit_type, title, body, url, assigned_at) VALUES (:mid, :type, :title, :body, :url, :at)";
$stmt = $pdo->prepare($sql);

$count = 0;
foreach ($data as $row) {
    try {
        $stmt->execute([':mid' => $row['mid'], ':type' => $row['type'], ':title' => $row['title'], ':body' => $row['body'], ':url' => $row['url'], ':at' => $row['at']]);
        $count++;
    } catch (Exception $e) {
        echo "Error inserting row: " . $e->getMessage() . "\n";
    }
}

echo "Successfully re-populated $count records.\n";
echo "</pre>";
