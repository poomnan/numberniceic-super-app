<?php
chdir(__DIR__);
require 'vendor/autoload.php';
require 'configs/constant.php';
require 'configs/config.php';

// Setup DB Connection manually
try {
    $dbConf = $config['db'];
    $db = new PDO(
        "mysql:host=" . $dbConf['host'] . ";dbname=" . $dbConf['dbname'],
        $dbConf['user'],
        $dbConf['pass']
    );
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $db->exec("set names utf8mb4");
} catch (PDOException $e) {
    die("Database Connection Failed: " . $e->getMessage());
}

// Logic Code
$presentDay = date('Y-m-d');
use App\Managers\ThaiCalendarHelper;

$auspicious = ThaiCalendarHelper::getAuspiciousStatus($presentDay);
$isWanPraToday = ThaiCalendarHelper::isWanPra($presentDay);

echo "DEBUG DATE: $presentDay\n";
echo "IS TONGCHAI (Helper): " . ($auspicious['is_tongchai'] ? '1' : '0') . "\n";
echo "IS ATIPBADEE (Helper): " . ($auspicious['is_atipbadee'] ? '1' : '0') . "\n";

// Query DB
$sql = "SELECT * FROM dayspecialtb WHERE wan_date = '$presentDay'";
$stmt = $db->prepare($sql);
$stmt->execute();
$dbSpecial = $stmt->fetch(\PDO::FETCH_OBJ);

$wanTongchai = $auspicious['is_tongchai'] ? "1" : "0";
$wanAtipbadee = $auspicious['is_atipbadee'] ? "1" : "0";
$wanPraStr = $isWanPraToday ? "1" : "0";
$wanKating = "0";
$wanDesc = $isWanPraToday ? "วันนี้วันพระ" : "";
$wanDetail = "";
$dayId = "1";

if (is_object($dbSpecial)) {
    echo "FOUND DB SPECIAL!\n";
    $dayId = $dbSpecial->dayid ?? "1";
    if (!empty($dbSpecial->wan_desc))
        $wanDesc = $dbSpecial->wan_desc;
    if (!empty($dbSpecial->wan_detail))
        $wanDetail = $dbSpecial->wan_detail;
    if (!empty($dbSpecial->wan_kating))
        $wanKating = $dbSpecial->wan_kating;
}

$WanSpecial = [
    'dayid' => $dayId,
    'wan_date' => $presentDay,
    'wan_desc' => $wanDesc,
    'wan_detail' => $wanDetail,
    'wan_pra' => $wanPraStr,
    'wan_kating' => $wanKating,
    'wan_tongchai' => $wanTongchai,
    'wan_atipbadee' => $wanAtipbadee
];

echo "Final JSON 'leng_yam':\n";
echo json_encode($WanSpecial, JSON_PRETTY_PRINT);
