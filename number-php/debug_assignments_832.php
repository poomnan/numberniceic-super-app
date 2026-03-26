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

$mid = 832;

echo "--- User 832 Buddha Assignments ---\n";
$stmt = $pdo->prepare("SELECT * FROM user_buddha_assign WHERE memberid = :mid");
$stmt->execute([':mid' => $mid]);
$rows = $stmt->fetchAll();
print_r($rows);

echo "\n--- User 832 Merit Assignments ---\n";
$stmt = $pdo->prepare("SELECT * FROM user_merit_assign WHERE memberid = :mid");
$stmt->execute([':mid' => $mid]);
$rows = $stmt->fetchAll();
print_r($rows);

echo "\n--- Current Database Time ---\n";
$stmt = $pdo->query("SELECT NOW() as now");
echo $stmt->fetch()->now . "\n";
?>