<?php
$config = require __DIR__ . '/configs/config.php';
$dbConfig = $config['db'];

try {
    $pdo = new PDO("mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8", $dbConfig['user'], $dbConfig['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $sql = "CREATE TABLE IF NOT EXISTS tabian_sell (
        tabian_id INT AUTO_INCREMENT PRIMARY KEY,
        tabian_number VARCHAR(20) NOT NULL,
        tabian_province VARCHAR(100) DEFAULT NULL,
        tabian_price INT DEFAULT 0,
        tabian_status VARCHAR(20) DEFAULT 'available',
        tabian_category VARCHAR(50) DEFAULT NULL,
        tabian_tag VARCHAR(50) DEFAULT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;";

    $pdo->exec($sql);
    echo "Table tabian_sell created successfully or already exists.";
} catch (PDOException $e) {
    echo "Error: " . $e->getMessage();
}
