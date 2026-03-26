<?php
// Script to add 'avatar' column to 'membertb' if it doesn't exist
// Place this in the root or appropriate folder on the server
// Usage: php update_avatar_column.php

$host = 'localhost';
$db = 'zoqlszwh_ananyadb';
$user = 'zoqlszwh_ananyadb';
$pass = 'IntelliP24.X';
$charset = 'utf8mb4';

$dsn = "mysql:host=$host;dbname=$db;charset=$charset";
$options = [
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES => false,
];

try {
    $pdo = new PDO($dsn, $user, $pass, $options);
    echo "Connected successfully to database.\n";

    // Check if column exists
    $stmt = $pdo->prepare("SHOW COLUMNS FROM membertb LIKE 'avatar'");
    $stmt->execute();
    $result = $stmt->fetch();

    if ($result) {
        echo "Column 'avatar' already exists.\n";
    } else {
        echo "Column 'avatar' not found. Adding it...\n";
        $sql = "ALTER TABLE membertb ADD avatar VARCHAR(50) DEFAULT '10'"; // Changed default to '10' for new system profile icon
        $pdo->exec($sql);
        echo "Column 'avatar' added successfully.\n";
    }

} catch (\PDOException $e) {
    echo "Connection failed: " . $e->getMessage() . "\n";
    // Usually on connection failure, username/pass might be issue.
    exit(1);
}
?>