<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
$config = require __DIR__ . '/configs/config.php';
$dbConfig = $config['db'];

try {
    $pdo = new PDO("mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8", $dbConfig['user'], $dbConfig['pass']);

    $output = "--- COLUMNS ---\n";
    $stmt = $pdo->query("SHOW COLUMNS FROM news");
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    foreach ($columns as $col) {
        $output .= $col['Field'] . "\n";
    }

    $output .= "\n--- 1st ROW ---\n";
    $stmt = $pdo->query("SELECT * FROM news LIMIT 1");
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    $output .= print_r($row, true);

    file_put_contents('debug_output.txt', $output);

} catch (Exception $e) {
    file_put_contents('debug_output.txt', "Error: " . $e->getMessage());
}
