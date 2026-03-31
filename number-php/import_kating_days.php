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

    $supportedRange = ThaiCalendarHelper::getSupportedDateRange();
    $startDate = $argv[1] ?? $supportedRange['start'];
    $endDate = $argv[2] ?? $supportedRange['end'];
    echo "Synchronizing 'วันกระทิงวัน' entries for {$startDate} to {$endDate}...\n";

    $start = new DateTime($startDate);
    $end = new DateTime($endDate);
    $countInserted = 0;
    $countUpdated = 0;
    $countCleared = 0;

    $clearStmt = $pdo->prepare(
        "UPDATE dayspecialtb
         SET wan_kating = '0',
             wan_desc = CASE WHEN wan_desc = 'วันกระทิงวัน' THEN '' ELSE wan_desc END,
             wan_detail = CASE WHEN wan_detail = 'วันกระทิงวันตามสูตรฤกษ์ยาม' THEN '' ELSE wan_detail END
         WHERE wan_date >= ? AND wan_date <= ?"
    );
    $clearStmt->execute([$startDate, $endDate]);

    $selectStmt = $pdo->prepare("SELECT dayid, wan_desc, wan_detail FROM dayspecialtb WHERE wan_date = ? ORDER BY dayid ASC");
    $updateStmt = $pdo->prepare(
        "UPDATE dayspecialtb
         SET wan_desc = ?, wan_detail = ?, wan_pra = ?, wan_kating = '1', wan_tongchai = ?, wan_atipbadee = ?
         WHERE dayid = ?"
    );
    $insertStmt = $pdo->prepare(
        "INSERT INTO dayspecialtb (wan_date, wan_desc, wan_detail, wan_pra, wan_kating, wan_tongchai, wan_atipbadee)
         VALUES (?, ?, ?, ?, '1', ?, ?)"
    );

    while ($start <= $end) {
        $dStr = $start->format('Y-m-d');

        try {
            $status = ThaiCalendarHelper::getAuspiciousStatus($dStr);
            if (!empty($status['is_kating'])) {
                $selectStmt->execute([$dStr]);
                $existingRows = $selectStmt->fetchAll(PDO::FETCH_ASSOC);

                $desc = "วันกระทิงวัน";
                $detail = "วันกระทิงวันตามสูตรฤกษ์ยาม";
                $wanPra = !empty($status['is_wanpra']) ? '1' : '0';
                $tongchai = !empty($status['is_tongchai']) ? '1' : '0';
                $atipbadee = !empty($status['is_atipbadee']) ? '1' : '0';

                if (!empty($existingRows)) {
                    $row = $existingRows[0];
                    $finalDesc = !empty($row['wan_desc']) ? $row['wan_desc'] : $desc;
                    $finalDetail = !empty($row['wan_detail']) ? $row['wan_detail'] : $detail;
                    $updateStmt->execute([$finalDesc, $finalDetail, $wanPra, $tongchai, $atipbadee, $row['dayid']]);
                    $countUpdated++;
                } else {
                    $insertStmt->execute([$dStr, $desc, $detail, $wanPra, $tongchai, $atipbadee]);
                    $countInserted++;
                }
            } else {
                $countCleared++;
            }
        } catch (Exception $e) {
            // Skip errors
        }
        $start->modify('+1 day');
    }

    echo "\nKating sync complete. Inserted: $countInserted Updated: $countUpdated Checked non-kating days: $countCleared\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
