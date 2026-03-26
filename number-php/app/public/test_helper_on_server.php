<?php
chdir(dirname(__DIR__)); // Go to root (ananya-php)
require 'vendor/autoload.php';
require 'configs/constant.php';
require 'configs/config.php';

use App\Managers\ThaiCalendarHelper;

echo "<h3>Testing Helper on Server</h3>";
$res = ThaiCalendarHelper::getUpcomingAuspiciousEvents(3);

echo "<pre>";
print_r($res);
echo "</pre>";

echo "<hr>";
echo "Count: " . count($res);
