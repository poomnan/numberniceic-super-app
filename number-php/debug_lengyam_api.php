<?php
date_default_timezone_set('Asia/Bangkok');
$dbhost = 'localhost';
$dbuser = 'zoqlszwh_ananyadb';
$dbpass = 'IntelliP24.X';
$dbname = 'zoqlszwh_ananyadb';

try {
    $pdo = new PDO("mysql:host=$dbhost;dbname=$dbname;charset=utf8", $dbuser, $dbpass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    $presentDay = date('Y-m-d');
    $endDate = date('Y-m-d', strtotime('+1 year'));
    
    echo "Date Range: $presentDay to $endDate <br>";
    
    $stmt = $pdo->prepare("SELECT date as wanpra_date, is_wanpra as is_wanpra, is_tongchai, is_atipbadee FROM auspicious_days WHERE date >= ? AND date <= ? ORDER BY date ASC");
    $stmt->execute([$presentDay, $endDate]);
    $arrWanpras = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo "Count: " . count($arrWanpras) . "<br>";
    
    echo "Checking for 2026-04-10:<br>";
    $found = false;
    foreach ($arrWanpras as $wp) {
        if ($wp['wanpra_date'] == '2026-04-10') {
            echo "FOUND: " . json_encode($wp) . "<br>";
            $found = true;
        }
    }
    if (!$found) echo "NOT FOUND 2026-04-10<br>";

    // Dump last 5
    echo "Last 5 items:<br>";
    $last5 = array_slice($arrWanpras, -5);
    foreach ($last5 as $l) {
        echo json_encode($l) . "<br>";
    }

} catch (Exception $e) {
    echo 'Error: ' . $e->getMessage();
}
?>
