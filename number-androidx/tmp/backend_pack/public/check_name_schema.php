<?php
require __DIR__ . '/../vendor/autoload.php';
$settings = require __DIR__ . '/../configs/config.php';
try {
    $dbConfig = $settings['db'];
    $pdo = new PDO("mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8", $dbConfig['user'], $dbConfig['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $tables = ['nickname', 'realname'];
    $results = [];
    foreach ($tables as $table) {
        try {
            $stmt = $pdo->query("DESCRIBE $table");
            $results[$table] = $stmt->fetchAll(PDO::FETCH_ASSOC);
        } catch (Exception $e) {
            $results[$table] = "Error: " . $e->getMessage();
        }
    }
    header('Content-Type: application/json');
    echo json_encode($results, JSON_PRETTY_PRINT);
} catch (Exception $e) {
    echo "Connection error: " . $e->getMessage();
}
