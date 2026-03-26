<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);
date_default_timezone_set('Asia/Bangkok');

require __DIR__ . '/vendor/autoload.php';

use App\Managers\ThaiCalendarHelper;

$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$dbConf = $config['db'];

try {
    $dsn = "mysql:host=" . $dbConf['host'] . ";dbname=" . $dbConf['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $dbConf['user'], $dbConf['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "Populating 'auspicious_days' table until end of 2027...\n";

    $start = new DateTime('2026-01-01');
    $end = new DateTime('2027-12-31');

    $stmt = $pdo->prepare("INSERT IGNORE INTO auspicious_days (date, is_wanpra, is_tongchai, is_atipbadee) VALUES (?, ?, ?, ?)");

    $count = 0;
    $current = clone $start;
    while ($current <= $end) {
        $dateStr = $current->format('Y-m-d');

        $isWanPra = ThaiCalendarHelper::isWanPra($dateStr) ? 1 : 0;
        $status = ThaiCalendarHelper::getAuspiciousStatus($dateStr);
        $isTongchai = $status['is_tongchai'] ? 1 : 0;
        $isAtipbadee = $status['is_atipbadee'] ? 1 : 0;

        $stmt->execute([$dateStr, $isWanPra, $isTongchai, $isAtipbadee]);
        $count++;

        $current->modify('+1 day');
    }

    echo "Processed $count days. Database updated.\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
