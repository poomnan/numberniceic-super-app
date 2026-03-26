<?php
// Debug script to check colortb
$host = "localhost";
$user = "zoqlszwh_ananyadb";
$pass = "IntelliP24.X";
$dbname = "zoqlszwh_ananyadb";

header('Content-Type: text/plain; charset=utf-8');

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    die("DB Connection failed: " . $e->getMessage());
}

$stmt = $pdo->query("SELECT * FROM colortb LIMIT 5");
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

echo "Color Table Count: " . count($rows) . "\n";
print_r($rows);

$stmt2 = $pdo->query("SELECT * FROM membertb LIMIT 1");
$mem = $stmt2->fetch(PDO::FETCH_ASSOC);
echo "\nMember Table Check:\n";
print_r($mem);
?>