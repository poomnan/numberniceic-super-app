<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

require __DIR__ . '/vendor/autoload.php';
$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$db = $config['db'];

try {
    $pdo = new PDO("mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4", $db['user'], $db['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Simulation of UserController::lengyamList
    date_default_timezone_set('Asia/Bangkok');
    $presentDay = date('Y-m-d');
    $endDate = date('Y-m-d', strtotime('+6 months'));

    echo "Range: $presentDay to $endDate\n";

    // 1. Fetch Kating Map
    $stmtKating = $pdo->prepare("SELECT wan_date FROM dayspecialtb WHERE wan_kating = '1' AND wan_date >= ? AND wan_date <= ?");
    $stmtKating->execute([$presentDay, $endDate]);
    $katingDays = $stmtKating->fetchAll(PDO::FETCH_COLUMN);
    $katingMap = array_flip($katingDays);

    // 2. Query auspicious_days
    $sql = "SELECT date as wanpra_date, is_wanpra, is_tongchai, is_atipbadee FROM auspicious_days WHERE date >= ? AND date <= ? ORDER BY date ASC";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$presentDay, $endDate]);
    $arrWanpras = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo "Raw items fetched: " . count($arrWanpras) . "\n";

    // 3. Filter
    $filteredWanpras = [];
    foreach ($arrWanpras as $key => $wp) {
        $isWanpra = ($wp['is_wanpra'] == 1 || $wp['is_wanpra'] == '1');
        $isTongchai = ($wp['is_tongchai'] == 1 || $wp['is_tongchai'] == '1');
        $isAtipbadee = ($wp['is_atipbadee'] == 1 || $wp['is_atipbadee'] == '1');
        $isKating = isset($katingMap[$wp['wanpra_date']]);

        if ($isWanpra || $isTongchai || $isAtipbadee || $isKating) {
            $filteredWanpras[] = [
                'wanpra_date' => $wp['wanpra_date'],
                'is_wanpra' => $isWanpra ? "1" : "0",
                'is_tongchai' => $isTongchai ? "1" : "0",
                'is_atipbadee' => $isAtipbadee ? "1" : "0",
                'is_kating' => $isKating ? "1" : "0"
            ];
        }
    }

    echo "Filtered items: " . count($filteredWanpras) . "\n";
    if (count($filteredWanpras) > 0) {
        $last = end($filteredWanpras);
        echo "Last item date: " . $last['wanpra_date'] . "\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
