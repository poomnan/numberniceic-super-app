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

// Show tables
echo "Tables in database:\n";
$stmt = $pdo->query("SHOW TABLES");
$tables = $stmt->fetchAll(PDO::FETCH_COLUMN);
// print_r($tables);

foreach ($tables as $table) {
    if (strpos($table, 'tb') !== false || strpos($table, 'assign') !== false || strpos($table, 'user') !== false) {
        echo "- $table\n";
    }
}

echo "\nColumns in membertb:\n";
$stmt = $pdo->query("DESCRIBE membertb");
$columns = $stmt->fetchAll(PDO::FETCH_COLUMN);
print_r($columns);

// Check user 832
echo "\nChecking user with id = 832:\n";
$stmt = $pdo->prepare("SELECT * FROM membertb WHERE id = 832");
$stmt->execute();
$user = $stmt->fetch();

if ($user) {
    echo "Found user id 832: " . $user->realname . " (MemberID: " . $user->memberid . ")\n";
} else {
    echo "User id 832 not found.\n";
    // Check memberid just in case
    $stmt = $pdo->prepare("SELECT * FROM membertb WHERE memberid = '832'");
    $stmt->execute();
    $userMemberId = $stmt->fetch();
    if ($userMemberId) {
        echo "Found user memberid 832: " . $userMemberId->realname . "\n";
    }
}
?>