<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$db = $config['db'];

try {
    $dsn = "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $db['user'], $db['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Date for test: Tomorrow or Soon
    $date = date('Y-m-d', strtotime('+2 days')); // e.g. 12 Feb 2026

    // Clean up any existing test data for this date to avoid clutter
    $stmt = $pdo->prepare("DELETE FROM dayspecialtb WHERE wan_date = ? AND wan_desc = 'วันกระทิงทดสอบ'");
    $stmt->execute([$date]);

    // Insert new test record
    $sql = "INSERT INTO dayspecialtb (wan_date, wan_desc, wan_detail, wan_pra, wan_kating, wan_tongchai, wan_atipbadee) VALUES (?, ?, ?, ?, ?, ?, ?)";
    $stmt = $pdo->prepare($sql);
    $res = $stmt->execute([$date, 'วันกระทิงทดสอบ', 'รายละเอียดวันกระทิงทดสอบ', '0', '1', '0', '0']);

    if ($res) {
        echo "Successfully inserted test 'Wan Kating' for date: $date\n";
    } else {
        echo "Failed to insert test data.\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
