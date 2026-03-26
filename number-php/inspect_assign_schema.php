<?php
// DB Credentials
$host = "localhost";
$user = "zoqlszwh_ananyadb";
$pass = "IntelliP24.X";
$dbname = "zoqlszwh_ananyadb";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $tables = ['user_buddha_assign', 'user_temple_assign', 'user_merit_assign'];
    foreach ($tables as $t) {
        echo "Table: $t\n";
        try {
            $stmt = $pdo->query("SHOW CREATE TABLE $t");
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            echo $row['Create Table'] . "\n\n";
        } catch (Exception $e) {
            echo "Error checking $t: " . $e->getMessage() . "\n\n";
        }
    }

} catch (PDOException $e) {
    die("Connection failed: " . $e->getMessage());
}
?>