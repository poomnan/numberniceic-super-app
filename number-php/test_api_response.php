<?php
header('Content-Type: application/json');
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
    
    $stmt = $pdo->prepare("SELECT date as wanpra_date, is_wanpra, is_tongchai, is_atipbadee FROM auspicious_days WHERE date >= ? AND date <= ? ORDER BY date ASC");
    $stmt->execute([$presentDay, $endDate]);
    $arrWanpras = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Convert to String like UserController does
    foreach ($arrWanpras as $key => $wp) {
        $arrWanpras[$key]['is_wanpra'] = ($wp['is_wanpra'] == 1 || $wp['is_wanpra'] == '1') ? "1" : "0";
        $arrWanpras[$key]['is_tongchai'] = ($wp['is_tongchai'] == 1 || $wp['is_tongchai'] == '1') ? "1" : "0";
        $arrWanpras[$key]['is_atipbadee'] = ($wp['is_atipbadee'] == 1 || $wp['is_atipbadee'] == '1') ? "1" : "0";
    }
    
    $response = [
        'leng_yam' => null,
        'next_wanpra' => '',
        'wan_pras' => $arrWanpras
    ];
    
    echo json_encode($response);
    
} catch (Exception $e) {
    echo json_encode(['error' => $e->getMessage()]);
}
?>
