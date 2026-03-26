<?php
// Debug script to check AdminController logic standalone
// require __DIR__ . '/../vendor/autoload.php';

// Mock DB
$host = "localhost";
$user = "zoqlszwh_ananyadb";
$pass = "IntelliP24.X";
$dbname = "zoqlszwh_ananyadb";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_OBJ);
} catch (PDOException $e) {
    die("DB Connection failed: " . $e->getMessage());
}

// Emulate Controller Logic
$username = isset($_GET['username']) ? $_GET['username'] : ''; // Simulate getAttribute or QueryParam

echo "Testing with username: '$username'<br>";

try {
    if (empty($username)) {
        // Return latest 50 users
        $sql = "SELECT memberid, username, realname, surname, birthday, fcm_token 
                FROM membertb 
                ORDER BY memberid DESC 
                LIMIT 20";
        $stmt = $pdo->prepare($sql);
        $stmt->execute();
    } else {
        // Search
        $sql = "SELECT memberid, username, realname, surname, birthday, fcm_token
                FROM membertb 
                WHERE username LIKE :query 
                   OR realname LIKE :query 
                ORDER BY memberid DESC 
                LIMIT 20";
        $stmt = $pdo->prepare($sql);
        $stmt->execute([':query' => "%$username%"]);
    }

    $data = $stmt->fetchAll();
    echo "Found " . count($data) . " users.<br>";

    // Test JSON Encode
    $json = json_encode(['result_userz' => $data], JSON_UNESCAPED_UNICODE | JSON_PARTIAL_OUTPUT_ON_ERROR);

    if ($json === false) {
        echo "JSON Encode Failed: " . json_last_error_msg();
    } else {
        echo "JSON Encode Success. Length: " . strlen($json);
        // echo "<br>Sample: " . substr($json, 0, 200);
    }

} catch (Exception $e) {
    echo "Logic Error: " . $e->getMessage();
}
?>