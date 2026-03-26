<?php
require_once __DIR__ . '/configs/config.php';

try {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4",
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "Checking for 'luckynumber_v2' table...\n";

    $checkSql = "SHOW TABLES LIKE 'luckynumber_v2'";
    $stmt = $pdo->query($checkSql);

    if ($stmt->rowCount() == 0) {
        echo "Table 'luckynumber_v2' NOT found. Creating it...\n";

        $createSql = "CREATE TABLE `luckynumber_v2` (
            `lucky_id` int(11) NOT NULL AUTO_INCREMENT,
            `lucky_date` date NOT NULL,
            `num1` varchar(10) DEFAULT NULL,
            `num2` varchar(10) DEFAULT NULL,
            `num3` varchar(10) DEFAULT NULL,
            `num4` varchar(10) DEFAULT NULL,
            `num5` varchar(10) DEFAULT NULL,
            `num6` varchar(10) DEFAULT NULL,
            PRIMARY KEY (`lucky_id`),
            UNIQUE KEY `lucky_date` (`lucky_date`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        $pdo->exec($createSql);
        echo "Table 'luckynumber_v2' created successfully.\n";
    } else {
        echo "Table 'luckynumber_v2' already exists.\n";
    }
} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
