<?php
/**
 * ตรวจสอบโครงสร้างตาราง membertb
 */

require_once __DIR__ . '/configs/config.php';

try {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4",
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "Checking table structure for 'membertb'...\n\n";

    $sql = "DESCRIBE membertb";
    $stmt = $pdo->query($sql);
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo "Columns in membertb:\n";
    echo str_repeat("-", 80) . "\n";
    printf("%-20s %-20s %-10s %-10s\n", "Field", "Type", "Null", "Key");
    echo str_repeat("-", 80) . "\n";

    foreach ($columns as $column) {
        printf(
            "%-20s %-20s %-10s %-10s\n",
            $column['Field'],
            $column['Type'],
            $column['Null'],
            $column['Key']
        );
    }

    echo "\n";

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
