<?php
require __DIR__ . '/configs/config.php';
// Re-read config cleanly
$config = include __DIR__ . '/configs/config.php';
$dbConfig = $config['db'];

echo "--- CONNECTING ---\n";
try {
    $pdo = new PDO("mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8", $dbConfig['user'], $dbConfig['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "--- COLUMNS OF 'news' ---\n";
    $q = $pdo->query("SHOW COLUMNS FROM news");
    while ($col = $q->fetch(PDO::FETCH_ASSOC)) {
        echo $col['Field'] . " | " . $col['Type'] . "\n";
    }

    echo "\n--- 1 ROW OF DATA ---\n";
    $q = $pdo->query("SELECT * FROM news ORDER BY newsid DESC LIMIT 1");
    $row = $q->fetch(PDO::FETCH_ASSOC);
    print_r($row);

} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage();
}
