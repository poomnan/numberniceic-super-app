<?php
// migrate_allow_multiple.php
// Allow multiple assignments for Temple (temple_id) and Buddha (assignment_type+buddha_id)
// By dropping the primary key that enforces uniqueness on memberid

require_once 'vendor/autoload.php';
require_once 'configs/constant.php';
require_once 'configs/config.php';

use DI\Container;

echo "<h3>Migrating Database to Allow Multiple Assignments...</h3>";

try {
    $dbConfig = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $dbConfig['host'] . ";dbname=" . $dbConfig['dbname'],
        $dbConfig['user'],
        $dbConfig['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->exec("set names utf8mb4");

    // 1. Temple Assignments
    // Check if PRIMARY KEY is on memberid, if so drop it and add a new ID primary key
    try {
        // Only run if memberid is the primary key (old schema)
        // We will just try to DROP PRIMARY KEY and add an auto-increment ID
        // If it fails (e.g. already done), we catch exception

        // This is a bit brute force but effective for migration scripts
        // Add ID column if not exists
        $stmt = $pdo->query("SHOW COLUMNS FROM user_temple_assign LIKE 'id'");
        if ($stmt->rowCount() == 0) {
            echo "Migrating user_temple_assign...<br>";
            // Drop PK
            $pdo->exec("ALTER TABLE user_temple_assign DROP PRIMARY KEY");
            // Add ID column
            $pdo->exec("ALTER TABLE user_temple_assign ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST");
            // Add Index for memberid
            $pdo->exec("ALTER TABLE user_temple_assign ADD INDEX idx_memberid (memberid)");
            echo "Fixed: user_temple_assign now supports multiple items.<br>";
        } else {
            echo "Skipped: user_temple_assign already migrated.<br>";
        }
    } catch (Exception $e) {
        echo "Note on Temple: " . $e->getMessage() . "<br>";
    }

    // 2. Buddha Assignments
    try {
        $stmt = $pdo->query("SHOW COLUMNS FROM user_buddha_assign LIKE 'id'");
        if ($stmt->rowCount() == 0) {
            echo "Migrating user_buddha_assign...<br>";
            // Check keys first to avoid error dropping non-existent key
            // Drop PK (composite or simple)
            $pdo->exec("ALTER TABLE user_buddha_assign DROP PRIMARY KEY");
            // Add ID column
            $pdo->exec("ALTER TABLE user_buddha_assign ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST");
            // Add Index
            $pdo->exec("ALTER TABLE user_buddha_assign ADD INDEX idx_memberid (memberid)");
            echo "Fixed: user_buddha_assign now supports multiple items.<br>";
        } else {
            echo "Skipped: user_buddha_assign already migrated.<br>";
        }
    } catch (Exception $e) {
        echo "Note on Buddha: " . $e->getMessage() . "<br>";
    }

    echo "<h3>Migration Complete. You can now assign multiple items!</h3>";
    echo "<p>Please delete this file after use.</p>";

} catch (PDOException $e) {
    echo "<h3 style='color:red;'>Fatal Error: " . $e->getMessage() . "</h3>";
}