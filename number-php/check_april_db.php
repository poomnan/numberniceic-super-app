<?php
$dbhost = 'localhost';
$dbuser = 'zoqlszwh_ananyadb';
$dbpass = 'IntelliP24.X';
$dbname = 'zoqlszwh_ananyadb';

try {
    $pdo = new PDO("mysql:host=$dbhost;dbname=$dbname;charset=utf8", $dbuser, $dbpass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Check specific dates in April 2026
    $stmt = $pdo->query("SELECT * FROM auspicious_days WHERE date BETWEEN '2026-04-01' AND '2026-04-30'");
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo 'Count April 2026: ' . count($rows) . "<br>";
    foreach ($rows as $r) {
        echo $r['id'] . ': ' . $r['date'] . ' | P:' . $r['is_wanpra'] . "<br>";
    }

} catch (PDOException $e) {
    echo 'Error: ' . $e->getMessage();
}
?>
