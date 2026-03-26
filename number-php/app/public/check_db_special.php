<?php
chdir(dirname(__DIR__));
require 'vendor/autoload.php';
require 'configs/constant.php';
require 'configs/config.php';

use App\Managers\ThaiCalendarHelper;

// Fix Timezone
date_default_timezone_set('Asia/Bangkok');
$date = date('Y-m-d'); // 2026-01-21

// Connect DB
$container = new \DI\Container();
$settings = require 'configs/config.php';
$dbConfig = $settings['db'];
$pdo = new PDO("mysql:host=" . $dbConfig['host'] . ";dbname=" . $dbConfig['dbname'] . ";charset=utf8", $dbConfig['user'], $dbConfig['pass']);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

echo "<h3>Check dayspecialtb for Date: $date</h3>";

$stmt = $pdo->query("SELECT * FROM dayspecialtb WHERE wan_date = '$date'");
$row = $stmt->fetch(PDO::FETCH_ASSOC);

if ($row) {
    echo "Found Record:<br>";
    print_r($row);
} else {
    echo "No Record Found in dayspecialtb";
}
