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
echo "<h3>Fixing 'merit_id' Error & Populating for User 832</h3>";

// 1. Check if merit_id exists (it does based on error)
// We will modify the column to allow NULL or Default 0 if possible, 
// OR simpler: Just insert with merit_id = 0

echo "<h3>Re-running Population for User 832 (with merit_id = 0)...</h3>";

$mid = 832;
// Clear existing
$pdo->exec("DELETE FROM user_merit_assign WHERE memberid = $mid");

// Data
$data = [
    ['mid' => $mid, 'type' => 'merit', 'title' => 'วิธีการทำบุญ', 'body' => 'คุณนินแนะนำ : ปางวันเสาร์', 'url' => '', 'at' => '2026-02-02 02:34:10', 'merit_id' => 0],
    ['mid' => $mid, 'type' => 'merit', 'title' => 'วิธีการทำบุญ', 'body' => 'คุณนินแนะนำ : ปางวันจันทร์', 'url' => '', 'at' => '2026-02-02 02:33:56', 'merit_id' => 0],
    ['mid' => $mid, 'type' => 'changenum', 'title' => 'ขั้นตอนการเปลี่ยนแปลง', 'body' => 'คุณนินแนะนำ : ขั้นตอนการเปลี่ยนแปลงเบอร์โทรศัพท์', 'url' => '', 'at' => '2026-02-02 02:34:48', 'merit_id' => 0],
    ['mid' => $mid, 'type' => 'changenum', 'title' => 'ขั้นตอนการเปลี่ยนแปลง', 'body' => 'คุณนินแนะนำ : ขั้นตอนการเปลี่ยนแปลงชื่อจริง นามสกุล', 'url' => '', 'at' => '2026-02-02 02:34:31', 'merit_id' => 0],
    ['mid' => $mid, 'type' => 'spell', 'title' => 'คาถาและคำเตือนพิเศษ', 'body' => 'คุณนินแนะนำ : คาถาชินบัญชร โดยสมเด็จพระพุฒาจารย์', 'url' => '', 'at' => '2026-02-02 02:15:07', 'merit_id' => 0]
];

// Updated SQL to include merit_id
$sql = "INSERT INTO user_merit_assign (memberid, merit_type, title, body, url, assigned_at, merit_id) 
        VALUES (:mid, :type, :title, :body, :url, :at, :merit_id)";
$stmt = $pdo->prepare($sql);

$count = 0;
foreach ($data as $row) {
    try {
        $stmt->execute([
            ':mid' => $row['mid'],
            ':type' => $row['type'],
            ':title' => $row['title'],
            ':body' => $row['body'],
            ':url' => $row['url'],
            ':at' => $row['at'],
            ':merit_id' => $row['merit_id']
        ]);
        $count++;
    } catch (Exception $e) {
        echo "Error inserting row: " . $e->getMessage() . "\n";
    }
}

echo "Successfully re-populated $count records.\n";
echo "</pre>";
