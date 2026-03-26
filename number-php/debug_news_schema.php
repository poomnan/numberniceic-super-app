<?php
$config = require __DIR__ . '/configs/config.php';
$dbConfig = $config['db'];

try {
    $pdo = new PDO("mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8", $dbConfig['user'], $dbConfig['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "--- COLUMNS IN news TABLE ---\n";
    $stmt = $pdo->query("SHOW COLUMNS FROM news");
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    foreach ($columns as $col) {
        echo $col['Field'] . "\n";
    }

    echo "\n--- FIRST ROW DATA ---\n";
    $stmt = $pdo->query("SELECT * FROM news LIMIT 1");
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    print_r($row);

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage();
}
