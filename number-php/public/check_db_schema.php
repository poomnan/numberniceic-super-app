<?php
require __DIR__ . '/../vendor/autoload.php';
$config = require __DIR__ . '/../configs/config.php';

try {
    $dbConfig = $config['db'];
    $dsn = "mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']}";
    $pdo = new PDO($dsn, $dbConfig['user'], $dbConfig['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $stmt = $pdo->query("DESCRIBE membertb");
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo "Table: membertb\n";
    foreach ($columns as $column) {
        echo "{$column['Field']} - {$column['Type']}\n";
    }
} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
