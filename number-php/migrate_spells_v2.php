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

    // 1. Create the new member_spell_notes table
    $sql1 = "CREATE TABLE IF NOT EXISTS member_spell_notes (
        id INT AUTO_INCREMENT PRIMARY KEY,
        memberid VARCHAR(50) NOT NULL,
        spell_id INT NOT NULL,
        note TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY member_spell (memberid, spell_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    $pdo->exec($sql1);
    echo "Table member_spell_notes created successfully.\n";

    // 2. Remove the note column from spells_warnings if it exists
    // We check if it exists first to avoid errors
    $stmt = $pdo->prepare("SHOW COLUMNS FROM spells_warnings LIKE 'note'");
    $stmt->execute();
    if ($stmt->fetch()) {
        $sql2 = "ALTER TABLE spells_warnings DROP COLUMN note;";
        $pdo->exec($sql2);
        echo "Column 'note' dropped from spells_warnings.\n";
    } else {
        echo "Column 'note' does not exist in spells_warnings, skipping drop.\n";
    }

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
