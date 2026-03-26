<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

require __DIR__ . '/vendor/autoload.php';
$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$db = $config['db'];

try {
    $pdo = new PDO("mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'], $db['user'], $db['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Simulate the logic in UserController::lengyamList
    $presentDay = date('Y-m-d');
    $endDate = date('Y-m-d', strtotime('+7 days'));

    // 1. Fetch Kating Map
    $stmtKating = $pdo->prepare("SELECT wan_date FROM dayspecialtb WHERE wan_kating = '1' AND wan_date >= ? AND wan_date <= ?");
    $stmtKating->execute([$presentDay, $endDate]);
    $katingDays = $stmtKating->fetchAll(PDO::FETCH_COLUMN);
    $katingMap = array_flip($katingDays);

    echo "Kating Map for next 7 days:\n";
    print_r($katingMap);

    // 2. Fetch Wanpra List (for 12 Feb)
    $stmt = $pdo->prepare("SELECT date as wanpra_date, is_wanpra, is_tongchai, is_atipbadee FROM auspicious_days WHERE date = '2026-02-12'");
    $stmt->execute();
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    foreach ($rows as $wp) {
        $isKating = isset($katingMap[$wp['wanpra_date']]);
        echo "Date: {$wp['wanpra_date']} | is_kating: " . ($isKating ? 'YES' : 'NO') . "\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
