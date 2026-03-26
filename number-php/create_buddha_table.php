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

    $sql = "CREATE TABLE IF NOT EXISTS buddha_pang_tb (
        id INT AUTO_INCREMENT PRIMARY KEY,
        pang_name VARCHAR(255) NOT NULL,
        buddha_day INT DEFAULT NULL COMMENT '1:Sun, 2:Mon, 3:Tue, 4:Wed_Day, 5:Thu, 6:Fri, 7:Sat, 8:Wed_Night',
        description TEXT,
        image_url VARCHAR(255),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

    $pdo->exec($sql);
    echo "Table buddha_pang_tb created successfully.\n";

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
