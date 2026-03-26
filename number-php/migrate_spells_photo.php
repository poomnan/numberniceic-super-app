<?php
require_once 'vendor/autoload.php';
require_once 'configs/config.php';

try {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'],
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->exec("set names utf8mb4");

    echo "Connected to DB.\n";
    echo "Adding 'photo' column to 'spells_warnings' table...\n";

    $sql = "ALTER TABLE spells_warnings ADD COLUMN photo VARCHAR(255) DEFAULT NULL";

    $pdo->exec($sql);

    echo "Success: Column 'photo' added.\n";

} catch (PDOException $e) {
    if (strpos($e->getMessage(), "Duplicate column name") !== false) {
        echo "Info: Column 'photo' already exists. Skipping.\n";
    } else {
        echo "Error: " . $e->getMessage() . "\n";
        exit(1);
    }
}
