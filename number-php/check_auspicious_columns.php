<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$db = $config['db'];

try {
    $dsn = "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $db['user'], $db['pass']);
    $stmt = $pdo->query("SHOW COLUMNS FROM auspicious_days");
    echo "Columns in auspicious_days:\n";
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        echo " - " . $row['Field'] . "\n";
    }
} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
