<?php
// DB Credentials
$host = "localhost";
$user = "zoqlszwh_ananyadb";
$pass = "IntelliP24.X";
$dbname = "zoqlszwh_ananyadb";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Create user_merit_assign table
    $sql = "CREATE TABLE IF NOT EXISTS user_merit_assign (
        id INT AUTO_INCREMENT PRIMARY KEY,
        memberid VARCHAR(50) NOT NULL,
        merit_id INT NOT NULL,
        merit_type VARCHAR(50) DEFAULT 'webview_merit',
        title VARCHAR(255),
        body TEXT,
        url TEXT,
        assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        KEY (memberid)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

    $pdo->exec($sql);
    echo "Table user_merit_assign created successfully.\n";

} catch (PDOException $e) {
    die("Error: " . $e->getMessage());
}
?>