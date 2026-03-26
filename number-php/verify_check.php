<?php
require_once 'vendor/autoload.php';
require_once 'configs/config.php';

$dbConfig = $config['db'];
$pdo = new PDO(
    "mysql:host=" . $dbConfig['host'] . ";dbname=" . $dbConfig['dbname'],
    $dbConfig['user'],
    $dbConfig['pass']
);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->exec("set names utf8mb4");

echo "<pre>";
echo "<h3>Verifying user_merit_assign for User 832</h3>";

$stmt = $pdo->prepare("SELECT * FROM user_merit_assign WHERE memberid = '832' ORDER BY id DESC");
$stmt->execute();
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

if (count($rows) > 0) {
    echo "Found " . count($rows) . " rows.<br>";
    print_r($rows);
} else {
    echo "<h2 style='color:red'>No rows found for user 832 in user_merit_assign!</h2>";
}

echo "<h3>Check types in DB</h3>";
$stmt = $pdo->query("SELECT DISTINCT merit_type FROM user_merit_assign");
print_r($stmt->fetchAll(PDO::FETCH_COLUMN));

echo "</pre>";
