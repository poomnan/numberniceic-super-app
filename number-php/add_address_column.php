<?php
/**
 * เพิ่มคอลัมน์ address ในตาราง membertb สำหรับเก็บที่อยู่จัดส่งสินค้า
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

    echo "Checking if 'address' column exists in membertb...\n";

    // ตรวจสอบว่ามีคอลัมน์ address อยู่แล้วหรือไม่
    $stmt = $pdo->prepare("SHOW COLUMNS FROM membertb LIKE 'address'");
    $stmt->execute();
    $columnExists = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($columnExists) {
        echo "✅ Column 'address' already exists in 'membertb'.\n";
        echo "Column details:\n";
        print_r($columnExists);
    } else {
        echo "❌ Column 'address' does not exist. Adding it...\n";
        
        // เพิ่มคอลัมน์ address
        $sql = "ALTER TABLE membertb ADD COLUMN address TEXT DEFAULT NULL AFTER avatar";
        $pdo->exec($sql);
        
        echo "✅ Column 'address' added successfully to 'membertb'.\n";
    }

    echo "\nUpdated membertb structure:\n";
    echo str_repeat("-", 80) . "\n";
    
    $stmt = $pdo->query("DESCRIBE membertb");
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    printf("%-20s %-25s %-10s %-10s\n", "Field", "Type", "Null", "Key");
    echo str_repeat("-", 80) . "\n";
    
    foreach ($columns as $column) {
        printf(
            "%-20s %-25s %-10s %-10s\n",
            $column['Field'],
            $column['Type'],
            $column['Null'],
            $column['Key']
        );
    }

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
