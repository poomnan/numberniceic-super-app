<?php
$config = include 'configs/config.php';

try {
    $db = new PDO("mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'], $config['db']['user'], $config['db']['pass']);
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $sql = "CREATE TABLE IF NOT EXISTS user_buddha_assign (
        id INT AUTO_INCREMENT PRIMARY KEY,
        memberid VARCHAR(50) NOT NULL,
        buddha_id INT NOT NULL,
        assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY (memberid)
    )";

    $db->exec($sql);
    echo "Successfully created user_buddha_assign table." . PHP_EOL;

} catch (PDOException $e) {
    echo "Error creating table: " . $e->getMessage() . PHP_EOL;
}
