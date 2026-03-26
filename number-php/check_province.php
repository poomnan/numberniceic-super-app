<?php
// DB Credentials
$host = "localhost";
$user = "zoqlszwh_ananyadb";
$pass = "IntelliP24.X";
$dbname = "zoqlszwh_ananyadb";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_OBJ);
} catch (PDOException $e) {
    die("Connection failed: " . $e->getMessage());
}

$username = isset($_GET['username']) ? $_GET['username'] : '';
if (empty($username)) {
    echo "Please provide ?username=...";
    exit;
}

$stmt = $pdo->prepare("SELECT memberid, realname, surname, birthday, sprovince, sgender, username FROM membertb WHERE username = :username");
$stmt->execute([':username' => $username]);
$user = $stmt->fetch();

if ($user) {
    echo "<h1>User Data for: " . htmlspecialchars($user->username) . "</h1>";
    echo "<pre>";
    print_r($user);
    echo "</pre>";

    // Check if sprovince column exists in table structure
    $stmtC = $pdo->query("SHOW COLUMNS FROM membertb LIKE 'sprovince'");
    $col = $stmtC->fetch();
    if ($col) {
        echo "<p>✅ Column 'sprovince' exists in 'membertb'.</p>";
    } else {
        echo "<p>❌ Column 'sprovince' DOES NOT EXIST in 'membertb'. You might need to add it.</p>";
    }
} else {
    echo "User not found.";
}
?>