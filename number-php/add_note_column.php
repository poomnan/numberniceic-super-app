<?php
require_once 'configs/config.php';

try {
    $pdo = new PDO(
        "mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'],
        $config['db']['user'],
        $config['db']['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $sql = "ALTER TABLE spells_warnings ADD COLUMN note TEXT NULL AFTER content";
    $pdo->exec($sql);

    echo "Successfully added 'note' column to 'spells_warnings' table.\n";
} catch (PDOException $e) {
    if (strpos($e->getMessage(), 'Duplicate column name') !== false) {
        echo "Column 'note' already exists.\n";
    } else {
        echo "Error: " . $e->getMessage() . "\n";
    }
}
