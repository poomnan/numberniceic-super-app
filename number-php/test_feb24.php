<?php
require_once __DIR__ . '/app/Managers/ThaiCalendarHelper.php';
use App\Managers\ThaiCalendarHelper;
$date = '2026-02-24';
echo "Date: $date\n";
$lunar = ThaiCalendarHelper::getThaiLunarDate($date);
echo "  Lunar: {$lunar['day']} {$lunar['phase']} Month {$lunar['month']} Year BE {$lunar['year_be']}\n";
echo "  isWanPra: " . (ThaiCalendarHelper::isWanPra($date) ? 'Yes' : 'No') . "\n";
