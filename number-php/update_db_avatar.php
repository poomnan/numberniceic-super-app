<?php
// DB Credentials
$host = "localhost";
$user = "zoqlszwh_ananyadb";
$pass = "IntelliP24.X";
$dbname = "zoqlszwh_ananyadb";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Check if column exists
    $stmt = $pdo->query("SHOW COLUMNS FROM membertb LIKE 'avatar'");
    $col = $stmt->fetch();

    if (!$col) {
        echo "Column 'avatar' not found. Adding it...\n";
        $sql = "ALTER TABLE membertb ADD COLUMN avatar VARCHAR(50) DEFAULT '1'";
        $pdo->exec($sql);
        echo "Column 'avatar' added successfully.\n";
    } else {
        echo "Column 'avatar' already exists.\n";
    }

} catch (PDOException $e) {
    die("Error: " . $e->getMessage());
}
?>