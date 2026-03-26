<?php
require_once 'vendor/autoload.php';
require_once 'configs/config.php';

$dbConfig = $config['db'];
$pdo = new PDO(
    "mysql:host=" . $dbConfig['host'] . ";dbname=" . $dbConfig['dbname'],
    $dbConfig['user'],
    $dbConfig['pass']
);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->exec("set names utf8mb4");

echo "<pre>";
echo "<h3>Cleaning Duplicates for User 832</h3>";

// 1. Deduplicate Sacred Temples
echo "Deduplicating user_temple_assign...\n";
$sqlTempl = "DELETE t1 FROM user_temple_assign t1 
            INNER JOIN user_temple_assign t2 
            WHERE t1.id < t2.id 
            AND t1.memberid = t2.memberid 
            AND t1.temple_id = t2.temple_id";
$count = $pdo->exec($sqlTempl);
echo "Removed $count duplicate temple assignments.\n";

// 2. Deduplicate Merit Assigns
echo "Deduplicating user_merit_assign...\n";
$sqlMerit = "DELETE t1 FROM user_merit_assign t1 
            INNER JOIN user_merit_assign t2 
            WHERE t1.id < t2.id 
            AND t1.memberid = t2.memberid 
            AND t1.merit_type = t2.merit_type 
            AND t1.title = t2.title 
            AND t1.body = t2.body";
$count2 = $pdo->exec($sqlMerit);
echo "Removed $count2 duplicate merit assignments.\n";


// Verify Temple 832
$stmt = $pdo->query("SELECT * FROM user_temple_assign WHERE memberid = '832'");
echo "<h4>Remaining Temples for 832:</h4>";
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));

echo "</pre>";
