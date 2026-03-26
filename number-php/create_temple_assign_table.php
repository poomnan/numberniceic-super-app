<?php
require_once 'configs/config.php';

try {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'],
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Table: Sacred Temple (Main Data) - Already created but ensuring consistence
    $sql = "CREATE TABLE IF NOT EXISTS sacred_temple_tb (
        id INT AUTO_INCREMENT PRIMARY KEY,
        temple_name VARCHAR(255) NOT NULL,
        description TEXT,
        address TEXT,
        image_url VARCHAR(255),
        latitude DECIMAL(10, 8) DEFAULT NULL,
        longitude DECIMAL(11, 8) DEFAULT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    $pdo->exec($sql);

    // Table: User Temple Assignment
    $sql = "CREATE TABLE IF NOT EXISTS user_temple_assign (
        id INT AUTO_INCREMENT PRIMARY KEY,
        memberid INT NOT NULL,
        temple_id INT NOT NULL,
        custom_description TEXT,
        assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY unique_user_temple (memberid),
        FOREIGN KEY (temple_id) REFERENCES sacred_temple_tb(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    $pdo->exec($sql);

    echo "Tables sacred_temple_tb and user_temple_assign created/checked successfully.\n";

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
