<?php
date_default_timezone_set('Asia/Bangkok');
$dbhost = 'localhost';
$dbuser = 'zoqlszwh_ananyadb';
$dbpass = 'IntelliP24.X';
$dbname = 'zoqlszwh_ananyadb';

try {
    $pdo = new PDO("mysql:host=$dbhost;dbname=$dbname;charset=utf8", $dbuser, $dbpass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Simulate what getLengYam() does
    $presentDay = date('Y-m-d');
    $endDate = date('Y-m-d', strtotime('+1 year'));

    $stmt = $pdo->prepare("SELECT date as wanpra_date, is_wanpra as is_wanpra, is_tongchai, is_atipbadee FROM auspicious_days WHERE date >= ? AND date <= ? ORDER BY date ASC");
    $stmt->execute([$presentDay, $endDate]);
    $arrWanpras = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo "Total Raw: " . count($arrWanpras) . "<br>";
    echo "Date Range: $presentDay to $endDate <br>";

    // Simulate filtering in loop
    $result = [];
    foreach ($arrWanpras as $wp) {
        $isP = ($wp['is_wanpra'] == 1);
        $isT = ($wp['is_tongchai'] == 1);
        $isA = ($wp['is_atipbadee'] == 1);

        if ($isP || $isT || $isA) {
            $result[] = $wp;
        }
    }
    
    echo "Filtered Count (PHP Side Simulation): " . count($result) . "<br>";
    echo "Last 5 Filtered Items: <br>";
    $last5 = array_slice($result, -5);
    foreach ($last5 as $l) {
        echo json_encode($l) . "<br>";
    }

} catch (Exception $e) {
    echo 'Error: ' . $e->getMessage();
}
?>
