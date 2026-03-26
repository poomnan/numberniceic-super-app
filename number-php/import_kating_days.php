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

    echo "Cleaning up existing 'วันกระทิงวัน' entries to avoid duplicates and fix errors...\n";
    $pdo->exec("DELETE FROM dayspecialtb WHERE wan_kating = '1'");

    echo "Calculating and Importing Kating Days (2025-2027) based on formula...\n";

    $start = new DateTime('2025-01-01');
    $end = new DateTime('2027-12-31');
    $count = 0;

    $stmt = $pdo->prepare("INSERT INTO dayspecialtb (wan_date, wan_desc, wan_detail, wan_pra, wan_kating, wan_tongchai, wan_atipbadee) VALUES (?, ?, ?, ?, ?, ?, ?)");

    while ($start <= $end) {
        $dStr = $start->format('Y-m-d');

        try {
            $lunar = ThaiCalendarHelper::getThaiLunarDate($dStr);
            $w = (int) $start->format('w'); // 0=Sun, 6=Sat
            $thaiDoW = $w + 1; // 1=Sun, 7=Sat

            $month = $lunar['month'];
            $day = $lunar['day'];
            $target = ($month - 1) % 7 + 1; // Loop back after 7

            if ($thaiDoW == $target && $day == $target) {
                $phase = ($lunar['phase'] == 'waxing') ? "ขึ้น" : "แรม";
                $desc = "วันกระทิงวัน";
                $detail = "วันแรงฤกษ์มงคลตามตำรา (เลขวัน $thaiDoW | เดือน $month | $phase $day ค่ำ)";

                $stmt->execute([$dStr, $desc, $detail, '0', '1', '0', '0']);
                echo "INSERTED: $dStr ($desc)\n";
                $count++;
            }
        } catch (Exception $e) {
            // Skip errors
        }
        $start->modify('+1 day');
    }

    echo "\nSuccessfully imported $count Kating days.\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
