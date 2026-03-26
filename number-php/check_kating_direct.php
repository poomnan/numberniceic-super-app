<?php
date_default_timezone_set('Asia/Bangkok');
$config = require __DIR__ . '/configs/config.php';
$db = $config['db'];

try {
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'],
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    $pdo->exec("set names utf8mb4");

    echo "Checking Kating in dayspecialtb:\n";
    $stmt = $pdo->query("SELECT wan_date, wan_kating FROM dayspecialtb WHERE wan_kating = '1' ORDER BY wan_date DESC LIMIT 10");
    $rows = $stmt->fetchAll();
    if (count($rows) == 0) {
        echo "NO Kating days found in the entire table!\n";
    } else {
        foreach ($rows as $row) {
            echo "Date: {$row['wan_date']} Kating: {$row['wan_kating']}\n";
        }
    }

    $start = date('Y-m-d');
    $end = date('Y-m-d', strtotime('+6 months'));
    echo "\nChecking future Kating days from $start to $end:\n";
    $stmt2 = $pdo->prepare("SELECT wan_date FROM dayspecialtb WHERE wan_kating = '1' AND wan_date >= ? AND wan_date <= ?");
    $stmt2->execute([$start, $end]);
    $future = $stmt2->fetchAll();
    echo "Count: " . count($future) . "\n";
    foreach ($future as $f) {
        echo " - {$f['wan_date']}\n";
    }

} catch (PDOException $e) {
    echo "Connection failed: " . $e->getMessage();
}
