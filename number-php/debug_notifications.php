<?php
require_once 'vendor/autoload.php';
require_once 'configs/config.php';

use DI\Container;

echo "<pre>";
echo "<h3>Debug Notifications for User 832</h3>";

$dbConfig = $config['db'];
$pdo = new PDO(
    "mysql:host=" . $dbConfig['host'] . ";dbname=" . $dbConfig['dbname'],
    $dbConfig['user'],
    $dbConfig['pass']
);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->exec("set names utf8mb4");

$memberId = '832';
echo "Test Member ID: $memberId\n";

// 1. Check notifications table
echo "<h4>1. Notifications Table</h4>";
$stmt = $pdo->prepare("SELECT id, type, title, body, created_at FROM notifications WHERE member_id = :mid ORDER BY created_at DESC");
$stmt->execute([':mid' => $memberId]);
$notis = $stmt->fetchAll(PDO::FETCH_ASSOC);

if (count($notis) > 0) {
    echo "Found " . count($notis) . " notifications.\n";
    print_r($notis);
} else {
    echo "<h2 style='color:red'>No notifications found!</h2>";
}

// 2. Check if we have Merit/Change specific tables (Did we create them? I think not, we rely on notifications)
// But wait, the user previously asked to "Refactor Merit, Change, Temple... mirroring the pattern established for Spells and Buddha"
// If we moved to a table-based approach for Merit/Change, then looking at notifications is WRONG.

// Let's check if `user_merit_assign` or similar exists.
echo "<h4>2. Check for Table 'user_merit_assign'</h4>";
try {
    $stmt = $pdo->query("DESCRIBE user_merit_assign");
    echo "Table 'user_merit_assign' EXISTS.\n";

    $stmt = $pdo->prepare("SELECT * FROM user_merit_assign WHERE memberid = :mid");
    $stmt->execute([':mid' => $memberId]);
    $merits = $stmt->fetchAll(PDO::FETCH_ASSOC);
    print_r($merits);

} catch (Exception $e) {
    echo "Table 'user_merit_assign' DOES NOT EXIST.\n";
}

echo "</pre>";
