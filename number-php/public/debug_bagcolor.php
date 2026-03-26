<?php
$host = "localhost";
$user = "zoqlszwh_ananyadb";
$pass = "IntelliP24.X";
$dbname = "zoqlszwh_ananyadb";

header('Content-Type: text/plain; charset=utf-8');

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "Check bagcolortb table:\n";
    $stmt = $pdo->query("SHOW TABLES LIKE 'bagcolortb'");
    if ($stmt->rowCount() == 0) {
        echo "Table 'bagcolortb' does NOT exist.\n";
    } else {
        echo "Table 'bagcolortb' exists.\n";
        $stmt = $pdo->query("SELECT * FROM bagcolortb LIMIT 5");
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
        echo "Data count: " . count($rows) . "\n";
        print_r($rows);
    }
} catch (PDOException $e) {
    die("Error: " . $e->getMessage());
}
?>