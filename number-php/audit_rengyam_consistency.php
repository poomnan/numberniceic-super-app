<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);
date_default_timezone_set('Asia/Bangkok');

require __DIR__ . '/vendor/autoload.php';

$supportedRange = \App\Managers\ThaiCalendarHelper::getSupportedDateRange();
$startDate = $argv[1] ?? $supportedRange['start'];
$endDate = $argv[2] ?? $supportedRange['end'];

$config = require __DIR__ . '/configs/config.php';
$dbConf = $config['db'];

$flags = [
    'is_wanpra',
    'is_tongchai',
    'is_atipbadee',
    'is_kating',
    'is_ubath',
    'is_lokawinat',
    'is_riangmon',
    'is_ammarit',
    'is_mahasittichok',
    'is_sittichok',
    'is_rachachok',
    'is_chaichok'
];

$summary = array_fill_keys($flags, 0);
$samples = array_fill_keys($flags, []);
$liveByDate = [];

$cursor = new DateTime($startDate);
$end = new DateTime($endDate);

while ($cursor <= $end) {
    $dateStr = $cursor->format('Y-m-d');
    $status = \App\Managers\ThaiCalendarHelper::getAuspiciousStatus($dateStr);
    $liveByDate[$dateStr] = $status;

    foreach ($flags as $flag) {
        if (!empty($status[$flag])) {
            $summary[$flag]++;
            if (count($samples[$flag]) < 12) {
                $samples[$flag][] = $dateStr;
            }
        }
    }

    $cursor->modify('+1 day');
}

$report = [
    'range' => [
        'start' => $startDate,
        'end' => $endDate
    ],
    'helper_summary' => $summary,
    'helper_samples' => $samples,
    'db_audit' => [
        'available' => false
    ]
];

try {
    $dsn = "mysql:host=" . $dbConf['host'] . ";dbname=" . $dbConf['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $dbConf['user'], $dbConf['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $stmt = $pdo->prepare(
        "SELECT date, is_wanpra, is_tongchai, is_atipbadee
         FROM auspicious_days
         WHERE date >= ? AND date <= ?
         ORDER BY date ASC"
    );
    $stmt->execute([$startDate, $endDate]);

    $dbRows = [];
    foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
        $dbRows[$row['date']] = $row;
    }

    $mismatchCounts = [
        'is_wanpra' => 0,
        'is_tongchai' => 0,
        'is_atipbadee' => 0
    ];
    $mismatchDates = [
        'is_wanpra' => [],
        'is_tongchai' => [],
        'is_atipbadee' => []
    ];
    $missingDbDates = [];

    foreach ($liveByDate as $dateStr => $status) {
        if (!isset($dbRows[$dateStr])) {
            $missingDbDates[] = $dateStr;
            continue;
        }

        foreach (['is_wanpra', 'is_tongchai', 'is_atipbadee'] as $flag) {
            $liveValue = !empty($status[$flag]) ? 1 : 0;
            $dbValue = !empty($dbRows[$dateStr][$flag]) ? 1 : 0;

            if ($liveValue !== $dbValue) {
                $mismatchCounts[$flag]++;
                if (count($mismatchDates[$flag]) < 25) {
                    $mismatchDates[$flag][] = [
                        'date' => $dateStr,
                        'live' => $liveValue,
                        'db' => $dbValue
                    ];
                }
            }
        }
    }

    $stmtKating = $pdo->prepare(
        "SELECT wan_date
         FROM dayspecialtb
         WHERE wan_kating = '1' AND wan_date >= ? AND wan_date <= ?
         ORDER BY wan_date ASC"
    );
    $stmtKating->execute([$startDate, $endDate]);
    $katingDates = $stmtKating->fetchAll(PDO::FETCH_COLUMN);
    $katingMap = array_fill_keys($katingDates, true);

    $liveKatingDates = [];
    foreach ($liveByDate as $dateStr => $status) {
        if (!empty($status['is_kating'])) {
            $liveKatingDates[] = $dateStr;
        }
    }

    $missingKatingInDb = [];
    foreach ($liveKatingDates as $dateStr) {
        if (!isset($katingMap[$dateStr])) {
            $missingKatingInDb[] = $dateStr;
        }
    }

    $extraKatingInDb = [];
    foreach ($katingDates as $dateStr) {
        if (empty($liveByDate[$dateStr]['is_kating'])) {
            $extraKatingInDb[] = $dateStr;
        }
    }

    $report['db_audit'] = [
        'available' => true,
        'auspicious_days_row_count' => count($dbRows),
        'mismatch_counts' => $mismatchCounts,
        'mismatch_dates' => $mismatchDates,
        'missing_db_dates_count' => count($missingDbDates),
        'missing_db_dates' => array_slice($missingDbDates, 0, 25),
        'kating_count' => count($katingDates),
        'kating_dates' => $katingDates,
        'live_kating_count' => count($liveKatingDates),
        'missing_kating_in_db_count' => count($missingKatingInDb),
        'missing_kating_in_db' => array_slice($missingKatingInDb, 0, 25),
        'extra_kating_in_db_count' => count($extraKatingInDb),
        'extra_kating_in_db' => array_slice($extraKatingInDb, 0, 25)
    ];
} catch (Throwable $e) {
    $report['db_audit'] = [
        'available' => false,
        'error' => $e->getMessage()
    ];
}

echo json_encode($report, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT) . PHP_EOL;
