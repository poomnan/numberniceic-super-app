<?php
require __DIR__ . '/vendor/autoload.php';
$settings = require __DIR__ . '/app/settings.php';
$app = new \Slim\App($settings);
$container = $app->getContainer();
$db = $container['db'];

echo "Checking max dates in tables:\n";

// auspicious_days
$max_aus = $db->query("SELECT MAX(date) FROM auspicious_days")->fetchColumn();
echo "Max date in auspicious_days: $max_aus\n";

// dayspecialtb
$max_spec = $db->query("SELECT MAX(wan_date) FROM dayspecialtb")->fetchColumn();
echo "Max date in dayspecialtb: $max_spec\n";

// Check if we have data for the next 6 months
$target = date('Y-m-d', strtotime('+6 months'));
echo "Target date (+6 months): $target\n";

if ($max_aus < $target) {
    echo "WARNING: AUSPICIOUS DAYS DATA IS INSUFFICIENT!\n";
} else {
    echo "Auspicious days data covers the target range.\n";
}
