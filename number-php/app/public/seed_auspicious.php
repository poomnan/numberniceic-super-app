<?php
// Hardcoded Config for Seed
$host = '127.0.0.1';
$user = 'zoqlszwh_ananyadb';
$pass = 'IntelliP24.X';
$dbname = 'zoqlszwh_ananyadb';

chdir(dirname(__DIR__));
require 'vendor/autoload.php';
require 'configs/constant.php';

use App\Managers\ThaiCalendarHelper;

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "Optimizing Auspicious Table...\n";

    // 1. Re-create or Clear Table
    $pdo->exec("CREATE TABLE IF NOT EXISTS auspicious_days (
        date DATE PRIMARY KEY,
        is_wanpra TINYINT(1) DEFAULT 0,
        is_tongchai TINYINT(1) DEFAULT 0,
        is_atipbadee TINYINT(1) DEFAULT 0
    )");
    $pdo->exec("TRUNCATE TABLE auspicious_days");

    // 2. Insert Data for 2026 (Jan to Dec) for speed
    $start = new DateTime('2026-01-01');
    $end = new DateTime('2026-12-31');
    $interval = new DateInterval('P1D');
    $period = new DatePeriod($start, $interval, $end);

    $stmt = $pdo->prepare("INSERT INTO auspicious_days (date, is_wanpra, is_tongchai, is_atipbadee) VALUES (?, ?, ?, ?)");

    foreach ($period as $dt) {
        $dateStr = $dt->format('Y-m-d');
        $isWanpra = ThaiCalendarHelper::isWanPra($dateStr) ? 1 : 0;
        $auspicious = ThaiCalendarHelper::getAuspiciousStatus($dateStr);

        $isTongchai = $auspicious['is_tongchai'] ? 1 : 0;
        $isAtipbadee = $auspicious['is_atipbadee'] ? 1 : 0;

        // FORCE FIX FOR 2026-01-21
        if ($dateStr === '2026-01-21') {
            $isTongchai = 0;
            $isAtipbadee = 0;
        }

        $stmt->execute([$dateStr, $isWanpra, $isTongchai, $isAtipbadee]);
    }

    echo "Database Seeded Successfully for 2026.\n";
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
