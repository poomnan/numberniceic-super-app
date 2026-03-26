<?php
require 'vendor/autoload.php';
require_once 'configs/config.php';

try {
    $dbSettings = $config['db'];
    $dsn = "mysql:host={$dbSettings['host']};dbname={$dbSettings['dbname']};charset=utf8mb4";
    $pdo = new PDO($dsn, $dbSettings['user'], $dbSettings['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $sql = "CREATE TABLE IF NOT EXISTS spells_warnings (
        id INT AUTO_INCREMENT PRIMARY KEY,
        type ENUM('spell', 'warning') NOT NULL,
        title VARCHAR(255) NOT NULL,
        content TEXT NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

    $pdo->exec($sql);
    echo "Table 'spells_warnings' created successfully.";
} catch (PDOException $e) {
    echo "Error: " . $e->getMessage();
}
