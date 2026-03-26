<?php
date_default_timezone_set('Asia/Bangkok');
$dbhost = 'localhost';
$dbuser = 'zoqlszwh_ananyadb';
$dbpass = 'IntelliP24.X';
$dbname = 'zoqlszwh_ananyadb';

try {
    $pdo = new PDO("mysql:host=$dbhost;dbname=$dbname;charset=utf8", $dbuser, $dbpass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Check auspicious days for April
    $stmt = $pdo->query("SELECT * FROM auspicious_days WHERE date BETWEEN '2026-04-01' AND '2026-04-30'");
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo 'Count April 2026: ' . count($rows) . "<br>";
    
    // Force update a specific date to ensure it is flagged
    $stmtUpdate = $pdo->prepare("UPDATE auspicious_days SET is_tongchai = 1 WHERE date = '2026-04-10'");
    $stmtUpdate->execute();
    echo 'Updated 2026-04-10 to use INT 1 for is_tongchai<br>';
    
    // Fetch and show again
    $stmt2 = $pdo->query("SELECT * FROM auspicious_days WHERE date = '2026-04-10'");
    $r = $stmt2->fetch(PDO::FETCH_ASSOC);
    echo 'After Update: ';
    print_r($r);

} catch (PDOException $e) {
    echo 'Error: ' . $e->getMessage();
}
?>
