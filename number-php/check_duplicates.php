<?php
$dbhost = 'localhost';
$dbuser = 'zoqlszwh_ananyadb';
$dbpass = 'IntelliP24.X';
$dbname = 'zoqlszwh_ananyadb';

try {
    $pdo = new PDO("mysql:host=$dbhost;dbname=$dbname;charset=utf8", $dbuser, $dbpass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Check for duplicates
    $sql = "SELECT date, COUNT(*) c FROM auspicious_days WHERE date >= '2026-04-01' GROUP BY date HAVING c > 1";
    $stmt = $pdo->query($sql);
    $dups = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo 'Duplicates found: ' . count($dups) . "<br>";
    foreach ($dups as $d) {
        echo $d['date'] . ' : ' . $d['c'] . "<br>";
    }
    
    // Check specific 2026-04-10
    $stmt2 = $pdo->query("SELECT * FROM auspicious_days WHERE date = '2026-04-10'");
    $rows = $stmt2->fetchAll(PDO::FETCH_ASSOC);
    echo '<br>Details for 2026-04-10:<br>';
    foreach ($rows as $r) {
        print_r($r);
        echo '<br>';
    }

} catch (PDOException $e) {
    echo 'Error: ' . $e->getMessage();
}
?>
