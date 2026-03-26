<?php
chdir(dirname(__DIR__));
require 'vendor/autoload.php';
require 'configs/constant.php';
require 'configs/config.php';

use App\Managers\ThaiCalendarHelper;

// Fix Timezone
date_default_timezone_set('Asia/Bangkok');

$date = date('Y-m-d');
echo "<h3>Current Date (Asia/Bangkok): $date</h3>";

$status = ThaiCalendarHelper::getAuspiciousStatus($date);
echo "<pre>";
print_r($status);
echo "</pre>";
