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

    $date = '2026-02-12';

    // Remove from auspicious_days (if it was the dummy entry we added)
    // We strictly delete only if it matches our dummy criteria (all 0s) to be safe
    $stmt = $pdo->prepare("DELETE FROM auspicious_days WHERE date = ? AND is_wanpra = 0 AND is_tongchai = 0 AND is_atipbadee = 0");
    $stmt->execute([$date]);
    if ($stmt->rowCount() > 0) {
        echo "Removed dummy entry from auspicious_days for $date.\n";
    }

    // Remove from dayspecialtb
    $stmt2 = $pdo->prepare("DELETE FROM dayspecialtb WHERE wan_date = ? AND wan_desc = 'วันกระทิงทดสอบ'");
    $stmt2->execute([$date]);
    if ($stmt2->rowCount() > 0) {
        echo "Removed test entry from dayspecialtb for $date.\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
