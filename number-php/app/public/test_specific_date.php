<?php
chdir(dirname(__DIR__));
require 'vendor/autoload.php';
require 'configs/constant.php';
require 'configs/config.php';

use App\Managers\ThaiCalendarHelper;

$date = '2026-01-21';
echo "<h3>Check Date: $date</h3>";

$status = ThaiCalendarHelper::getAuspiciousStatus($date);
echo "<pre>";
print_r($status);
echo "</pre>";

$isTongchai = $status['is_tongchai'] ? 'YES' : 'NO';
echo "Is Tongchai? $isTongchai";
