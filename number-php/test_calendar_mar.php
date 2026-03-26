<?php
require_once __DIR__ . '/app/Managers/ThaiCalendarHelper.php';

use App\Managers\ThaiCalendarHelper;

$dates = [
    '2026-03-10',
    '2026-03-11',
];

foreach ($dates as $date) {
    echo "Date: $date\n";
    $lunar = ThaiCalendarHelper::getThaiLunarDate($date);
    echo "  Lunar: {$lunar['day']} {$lunar['phase']} Month {$lunar['month']} Year BE {$lunar['year_be']} is_8-2: " . ($lunar['is_second_8'] ? 'Yes' : 'No') . "\n";
    echo "  isWanPra: " . (ThaiCalendarHelper::isWanPra($date) ? 'Yes' : 'No') . "\n";
    $status = ThaiCalendarHelper::getAuspiciousStatus($date);
    echo "  Tongchai: " . ($status['is_tongchai'] ? 'Yes' : 'No') . "\n";
    echo "  Atipbadee: " . ($status['is_atipbadee'] ? 'Yes' : 'No') . "\n";
    echo "--------------------\n";
}
