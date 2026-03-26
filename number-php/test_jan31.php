<?php
require_once __DIR__ . '/app/Managers/ThaiCalendarHelper.php';
use App\Managers\ThaiCalendarHelper;
$date = '2026-01-31';
echo "Date: $date\n";
echo "  isWanPra: " . (ThaiCalendarHelper::isWanPra($date) ? 'Yes' : 'No') . "\n";
echo "  Tongchai: " . (ThaiCalendarHelper::getAuspiciousStatus($date)['is_tongchai'] ? 'Yes' : 'No') . "\n";
echo "  Atipbadee: " . (ThaiCalendarHelper::getAuspiciousStatus($date)['is_atipbadee'] ? 'Yes' : 'No') . "\n";
