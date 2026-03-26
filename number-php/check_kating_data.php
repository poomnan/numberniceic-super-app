<?php
require __DIR__ . '/vendor/autoload.php';
$settings = require __DIR__ . '/app/settings.php';
$app = new \Slim\App($settings);
$container = $app->getContainer();
$db = $container['db'];

echo "Checking for Wan Kating days in dayspecialtb:\n";
// Check count first
$count = $db->query("SELECT COUNT(*) FROM dayspecialtb WHERE wan_kating = '1'")->fetchColumn();
echo "Total Wan Kating days found: $count\n";

if ($count > 0) {
    echo "First 10 Wan Kating days:\n";
    $q = $db->query("SELECT wan_date, wan_kating FROM dayspecialtb WHERE wan_kating = '1' ORDER BY wan_date DESC LIMIT 10");
    while ($row = $q->fetch(PDO::FETCH_ASSOC)) {
        echo "Date: " . $row['wan_date'] . " | Kating: " . $row['wan_kating'] . "\n";
    }
}

echo "\nChecking today (" . date('Y-m-d') . ") to +6 months:\n";
$start = date('Y-m-d');
$end = date('Y-m-d', strtotime('+6 months'));
$q2 = $db->prepare("SELECT wan_date FROM dayspecialtb WHERE wan_kating = '1' AND wan_date >= ? AND wan_date <= ?");
$q2->execute([$start, $end]);
$future = $q2->fetchAll(PDO::FETCH_ASSOC);
echo "Future Kating days found: " . count($future) . "\n";
foreach ($future as $f) {
    echo " - " . $f['wan_date'] . "\n";
}
