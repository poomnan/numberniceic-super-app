<?php
chdir(__DIR__);
require 'vendor/autoload.php';
use App\Managers\ThaiCalendarHelper;

$date = '2026-01-20';
$lunar = ThaiCalendarHelper::getThaiLunarDate($date);
$status = ThaiCalendarHelper::getAuspiciousStatus($date);

echo "Check Date: $date\n";
echo "Day Of Week: " . date('l', strtotime($date)) . " (" . date('w', strtotime($date)) . ")\n";
echo "Thai Month: " . $lunar['month'] . "\n";
echo "Thai Year BE: " . $lunar['year_be'] . "\n";
echo "Phase: " . $lunar['phase'] . " " . $lunar['day'] . " kham\n";
echo "Calculated Status:\n";
echo " - Tongchai: " . ($status['is_tongchai'] ? 'YES' : 'NO') . "\n";
echo " - Atipbadee: " . ($status['is_atipbadee'] ? 'YES' : 'NO') . "\n";

// Debug Logic in Helper for Month 3,4 (User expect Tuesday to be Tongchai/Atipbadee?)
if ($lunar['month'] == 3 || $lunar['month'] == 4) {
    echo "Logic: In Month 3/4, Tuesday (2) should be Tongchai & Atipbadee.\n";
} else {
    echo "Logic: Month is NOT 3 or 4, so Tuesday rule might not apply.\n";
}
