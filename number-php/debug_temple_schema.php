<?php
require_once 'vendor/autoload.php';
require_once 'configs/config.php';

use DI\Container;

echo "<pre>";
echo "<h3>Debug Temple Schema & Data</h3>";

$dbConfig = $config['db'];
$pdo = new PDO(
    "mysql:host=" . $dbConfig['host'] . ";dbname=" . $dbConfig['dbname'],
    $dbConfig['user'],
    $dbConfig['pass']
);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->exec("set names utf8mb4");

// 1. Check Table Definition
echo "<h4>1. Table Structure: user_temple_assign</h4>";
try {
    $stmt = $pdo->query("DESCRIBE user_temple_assign");
    $cols = $stmt->fetchAll(PDO::FETCH_ASSOC);
    print_r($cols);
} catch (Exception $e) {
    echo "Error describing table: " . $e->getMessage();
}

// 2. Check Table Keys
echo "<h4>2. Table Keys: user_temple_assign</h4>";
try {
    $stmt = $pdo->query("SHOW KEYS FROM user_temple_assign");
    $keys = $stmt->fetchAll(PDO::FETCH_ASSOC);
    print_r($keys);
} catch (Exception $e) {
    echo "Error showing keys: " . $e->getMessage();
}

// 3. Check Data Count
echo "<h4>3. Data Count</h4>";
try {
    $stmt = $pdo->query("SELECT COUNT(*) as c FROM user_temple_assign");
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "Total assignments: " . $row['c'] . "\n";

    $stmt = $pdo->query("SELECT * FROM user_temple_assign ORDER BY assigned_at DESC LIMIT 5");
    print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
} catch (Exception $e) {
    echo "Error selecting data: " . $e->getMessage();
}

// 4. Check Temple Data
echo "<h4>4. Sacred Temples Count</h4>";
try {
    $stmt = $pdo->query("SELECT COUNT(*) as c FROM sacred_temple_tb");
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "Total Temples: " . $row['c'] . "\n";

    $stmt = $pdo->query("SELECT id, temple_name FROM sacred_temple_tb LIMIT 5");
    print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
} catch (Exception $e) {
    echo "Error selecting temples: " . $e->getMessage();
}

echo "</pre>";
