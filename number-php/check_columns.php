<?php
require __DIR__ . '/vendor/autoload.php';
$settings = require __DIR__ . '/app/settings.php';
$app = new \Slim\App($settings);
$container = $app->getContainer();
$db = $container['db'];

echo "Checking auspicious_days columns:\n";
$q = $db->query("SHOW COLUMNS FROM auspicious_days");
while ($row = $q->fetch(PDO::FETCH_ASSOC)) {
    echo $row['Field'] . "\n";
}

echo "\nChecking dayspecialtb columns:\n";
$q = $db->query("SHOW COLUMNS FROM dayspecialtb");
while ($row = $q->fetch(PDO::FETCH_ASSOC)) {
    echo $row['Field'] . "\n";
}
