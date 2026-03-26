<?php
require __DIR__ . '/vendor/autoload.php';

use Slim\Factory\AppFactory;
use DI\Container;

// Create Container using PHP-DI
$container = new Container();
AppFactory::setContainer($container);
$app = AppFactory::create();

// Database connection
$container->set('db', function () {
    $dbhost = "127.0.0.1";
    $dbuser = "tayap";
    $dbpass = "tayap";
    $dbname = "ananyadb_pg";
    $pdo = new PDO("mysql:host=$dbhost;dbname=$dbname;charset=utf8", $dbuser, $dbpass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    return $pdo;
});


$db = $container->get('db');

echo "Checking auspicious_days...\n";

try {
    $stmt = $db->query("SELECT MIN(date) as min_date, MAX(date) as max_date, COUNT(*) as count FROM auspicious_days");
    $row = $stmt->fetch();
    echo "Min Date: " . $row['min_date'] . "\n";
    echo "Max Date: " . $row['max_date'] . "\n";
    echo "Total Rows: " . $row['count'] . "\n";

    echo "\nLast 10 entries:\n";
    $stmt = $db->query("SELECT * FROM auspicious_days ORDER BY date DESC LIMIT 10");
    $rows = $stmt->fetchAll();
    foreach ($rows as $r) {
        echo $r['date'] . " | WanPra: " . $r['is_wanpra'] . "\n";
    }

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
