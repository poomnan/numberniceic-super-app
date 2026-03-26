<?php
require __DIR__ . '/vendor/autoload.php';
$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$db = $config['db'];

try {
    $pdo = new PDO("mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'], $db['user'], $db['pass']);
    $pdo->exec("set names utf8mb4");

    $stmt = $pdo->query("SELECT wan_date, wan_desc FROM dayspecialtb WHERE wan_kating = '1' AND wan_date >= '2025-01-01' ORDER BY wan_date ASC LIMIT 100");
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo "Kating Days Found (2025-Now): " . count($rows) . "\n";
    foreach ($rows as $r) {
        echo "- " . $r['wan_date'] . ": " . $r['wan_desc'] . "\n";
    }

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage();
}
