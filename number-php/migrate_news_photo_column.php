<?php

$config = require __DIR__ . '/configs/config.php';
$dbConfig = $config['db'];

try {
    $pdo = new PDO(
        "mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8",
        $dbConfig['user'],
        $dbConfig['pass'],
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
    );

    $check = $pdo->query("SHOW COLUMNS FROM news LIKE 'photo'");
    if ($check->rowCount() === 0) {
        $pdo->exec("SET SESSION sql_mode = ''");
        $pdo->exec("ALTER TABLE news ADD COLUMN photo VARCHAR(255) NULL");
        echo "Added column news.photo\n";
    } else {
        echo "Column news.photo already exists\n";
    }
} catch (Throwable $e) {
    fwrite(STDERR, "Migration failed: " . $e->getMessage() . "\n");
    exit(1);
}
