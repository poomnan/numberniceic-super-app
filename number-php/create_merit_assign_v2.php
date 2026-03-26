<?php
// DB Credentials (Production)
$host = "localhost";
$user = "zoqlszwh_ananyadb";
$pass = "IntelliP24.X";
$dbname = "zoqlszwh_ananyadb";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Create user_merit_assign table (without merit_id NOT NULL constraint)
    $sql = "CREATE TABLE IF NOT EXISTS user_merit_assign (
        id INT AUTO_INCREMENT PRIMARY KEY,
        memberid VARCHAR(50) NOT NULL,
        merit_type VARCHAR(50) DEFAULT 'webview_merit',
        title VARCHAR(255),
        body TEXT,
        url TEXT,
        assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        KEY idx_memberid (memberid),
        KEY idx_type (merit_type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

    $pdo->exec($sql);
    echo json_encode(['status' => 'ok', 'message' => 'Table user_merit_assign created/verified successfully.']);

    // Verify table exists
    $check = $pdo->query("SELECT COUNT(*) as cnt FROM user_merit_assign");
    $cnt = $check->fetch(PDO::FETCH_OBJ)->cnt;
    echo "\nCurrent rows: " . $cnt;

} catch (PDOException $e) {
    echo json_encode(['status' => 'error', 'message' => $e->getMessage()]);
}
?>
