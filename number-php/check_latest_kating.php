<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$db = $config['db'];

try {
    $dsn = "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $db['user'], $db['pass']);

    // Get the absolute latest date
    $stmt = $pdo->query("SELECT MAX(wan_date) as last_date FROM dayspecialtb WHERE wan_kating = '1'");
    $date = $stmt->fetchColumn();

    // Get the count just in case
    $count = $pdo->query("SELECT COUNT(*) FROM dayspecialtb WHERE wan_kating = '1'")->fetchColumn();

    echo "Latest Wan Kating date in database: " . ($date ? $date : "None found") . "\n";
    echo "Total Wan Kating records: $count\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
