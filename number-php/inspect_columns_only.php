<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
$config = require __DIR__ . '/configs/config.php';
$dbConfig = $config['db'];

try {
    $pdo = new PDO("mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8", $dbConfig['user'], $dbConfig['pass']);

    // ONLY SHOW COLUMNS - NO ROW DATA to avoid truncation
    $stmt = $pdo->query("SHOW COLUMNS FROM news");
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo "--- COLUMNS --- \n";
    foreach ($columns as $col) {
        echo $col['Field'] . "\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
