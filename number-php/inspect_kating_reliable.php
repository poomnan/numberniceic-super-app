<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

$configFile = __DIR__ . '/configs/config.php';
if (!file_exists($configFile)) {
    die("Config file not found.\n");
}
$config = require $configFile;
$db = $config['db'];

try {
    $dsn = "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $db['user'], $db['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "Connected via PDO.\n";

    // Check count of kating days
    $stmt = $pdo->query("SELECT count(*) FROM dayspecialtb WHERE wan_kating = '1'");
    $count = $stmt->fetchColumn();
    echo "Total Wan Kating days: " . $count . "\n";

    if ($count > 0) {
        $stmt = $pdo->query("SELECT wan_date FROM dayspecialtb WHERE wan_kating = '1' ORDER BY wan_date DESC LIMIT 5");
        echo "Example dates:\n";
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            echo " - " . $row['wan_date'] . "\n";
        }
    }

    // Check range for next 6 months
    $start = date('Y-m-d');
    $end = date('Y-m-d', strtotime('+6 months'));
    $stmt = $pdo->prepare("SELECT wan_date FROM dayspecialtb WHERE wan_kating = '1' AND wan_date >= ? AND wan_date <= ?");
    $stmt->execute([$start, $end]);
    $future = $stmt->fetchAll(PDO::FETCH_COLUMN);
    echo "Future Kating days (" . count($future) . "):\n";
    foreach ($future as $date) {
        echo " - " . $date . "\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
