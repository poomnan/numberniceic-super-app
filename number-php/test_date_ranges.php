<?php
date_default_timezone_set('Asia/Bangkok');
$presentDay = date('Y-m-d');
$endDate6M = date('Y-m-d', strtotime('+6 months'));
$endDate1Y = date('Y-m-d', strtotime('+1 year'));

$dbhost = 'localhost';
$dbuser = 'zoqlszwh_ananyadb';
$dbpass = 'IntelliP24.X';
$dbname = 'zoqlszwh_ananyadb';

$pdo = new PDO("mysql:host=$dbhost;dbname=$dbname;charset=utf8", $dbuser, $dbpass);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

// Test 6 months
$stmt = $pdo->prepare("SELECT COUNT(*) as cnt FROM auspicious_days WHERE date >= ? AND date <= ?");
$stmt->execute([$presentDay, $endDate6M]);
$count6M = $stmt->fetch(PDO::FETCH_ASSOC)['cnt'];

// Test 1 year
$stmt = $pdo->prepare("SELECT COUNT(*) as cnt FROM auspicious_days WHERE date >= ? AND date <= ?");
$stmt->execute([$presentDay, $endDate1Y]);
$count1Y = $stmt->fetch(PDO::FETCH_ASSOC)['cnt'];

echo "Date range:\n";
echo "  Today: $presentDay\n";
echo "  6 months: $endDate6M (count: $count6M)\n";
echo "  1 year: $endDate1Y (count: $count1Y)\n";
echo "\n";
echo "If API returns 60 items with 6 months setting, buffer limit is ~60 items\n";
echo "If API returns $count6M items, the fix works!\n";
?>
