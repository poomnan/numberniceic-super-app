<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$db = $config['db'];

try {
    $dsn = "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $db['user'], $db['pass']);

    // Check max date in auspicious_days
    $stmt = $pdo->query("SELECT MAX(date) FROM auspicious_days");
    $max_aus = $stmt->fetchColumn();

    // Check max date in dayspecialtb
    $stmt2 = $pdo->query("SELECT MAX(wan_date) FROM dayspecialtb");
    $max_spec = $stmt2->fetchColumn();

    echo "Max date in auspicious_days: $max_aus\n";
    echo "Max date in dayspecialtb: $max_spec\n";

    $today = date('Y-m-d');
    $target = date('Y-m-d', strtotime('+6 months'));
    echo "Today: $today\n";
    echo "Target (+6 months): $target\n";

    if ($max_aus < $target) {
        echo "!!! WARNING: AUSPICIOUS DAYS DATA IS INSUFFICIENT !!!\nIt ends before the target range.\n";
    } else {
        echo "Data coverage is sufficient.\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
