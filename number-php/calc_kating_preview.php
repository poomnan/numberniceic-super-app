<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);
date_default_timezone_set('Asia/Bangkok');

require __DIR__ . '/vendor/autoload.php';

use App\Managers\ThaiCalendarHelper;

function getThaiDayOfWeek($dateStr)
{
    // PHP 'w': 0=Sun, 6=Sat
    // Thai: 1=Sun, 7=Sat
    $dt = new DateTime($dateStr);
    return (int) $dt->format('w') + 1;
}

function getTargetNumber($lunarMonth)
{
    // Formula: Months 1-7 -> 1-7
    // Months 8-12 -> Loop back (8->1, 9->2...)
    return ($lunarMonth - 1) % 7 + 1;
}

echo "--- Kating Day Diagnostic ---\n";

// 1. Analyze the existing DB date: 2026-05-15
$debugDate = '2026-05-15';
echo "\nAnalyzing existing DB entry: $debugDate\n";
try {
    $lunar = ThaiCalendarHelper::getThaiLunarDate($debugDate);
    $thaiDoW = getThaiDayOfWeek($debugDate);
    $month = $lunar['month'];
    $day = $lunar['day'];
    $phase = ($lunar['phase'] == 'waxing') ? "ขึ้น" : "แรม";
    $target = getTargetNumber($month);

    echo "  - Day of Week: $thaiDoW (Friday is 6)\n";
    echo "  - Lunar Month: $month (Target Number: $target)\n";
    echo "  - Lunar Day: $day ($phase)\n";

    // Check Match
    $matchDoW = ($thaiDoW == $target);
    $matchDay = ($day == $target);

    if ($matchDoW && $matchDay) {
        echo "  - Result: VALID MATCH!\n";
    } else {
        echo "  - Result: INVALID (Needs DoW=$target and Day=$target)\n";
    }

} catch (Exception $e) {
    echo "  - Error: " . $e->getMessage() . "\n";
}

// 2. Find Correct Dates for 2026-2030
echo "\n--- Finding Valid Kating Days (2026-2030) ---\n";
$start = new DateTime('2026-01-01');
$end = new DateTime('2030-12-31');
$count = 0;

while ($start <= $end) {
    $dStr = $start->format('Y-m-d');

    try {
        $lunar = ThaiCalendarHelper::getThaiLunarDate($dStr);
        $thaiDoW = getThaiDayOfWeek($dStr);
        $month = $lunar['month'];
        $day = $lunar['day'];
        $target = getTargetNumber($month);

        if ($thaiDoW == $target && $day == $target) {
            $phase = ($lunar['phase'] == 'waxing') ? "ขึ้น" : "แรม";
            echo "MATCH: $dStr | Dow:$thaiDoW | Month:$month | Lunar: $phase $day ค่ำ\n";
            $count++;
        }

    } catch (Exception $e) {
        // flutter on error
    }

    $start->modify('+1 day');
}

echo "\nTotal Found: $count\n";
