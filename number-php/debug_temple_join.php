<?php
require_once 'vendor/autoload.php';
require_once 'configs/config.php';

use DI\Container;

echo "<pre>";
echo "<h3>Debug Temple JOIN Query</h3>";

$dbConfig = $config['db'];
$pdo = new PDO(
    "mysql:host=" . $dbConfig['host'] . ";dbname=" . $dbConfig['dbname'],
    $dbConfig['user'],
    $dbConfig['pass']
);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->exec("set names utf8mb4");

$memberId = '832';
echo "Test Member ID: $memberId\n";

// 1. Check raw assignments
echo "<h4>1. Raw Assignments for $memberId</h4>";
$stmt = $pdo->prepare("SELECT * FROM user_temple_assign WHERE memberid = :mid");
$stmt->execute([':mid' => $memberId]);
$rawObj = $stmt->fetchAll(PDO::FETCH_ASSOC);
print_r($rawObj);

if (count($rawObj) > 0) {
    echo "Found " . count($rawObj) . " assignments.\n";
    $firstTempleId = $rawObj[0]['temple_id'];
    echo "First Temple ID: $firstTempleId\n";

    // 2. Check Temple Existence
    echo "<h4>2. Check Temple ID $firstTempleId in sacred_temple_tb</h4>";
    $stmt = $pdo->prepare("SELECT * FROM sacred_temple_tb WHERE id = :tid");
    $stmt->execute([':tid' => $firstTempleId]);
    $temple = $stmt->fetchAll(PDO::FETCH_ASSOC);
    print_r($temple);

    // 3. Test Full JOIN
    echo "<h4>3. Test Full JOIN Query (Simulate Controller)</h4>";
    $sql = "SELECT t.*, a.custom_description as assign_desc, a.assigned_at FROM sacred_temple_tb t 
            JOIN user_temple_assign a ON t.id = a.temple_id 
            WHERE a.memberid = :mid";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([':mid' => $memberId]);
    $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
    print_r($result);

    if (count($result) == 0) {
        echo "<h2 style='color:red'>JOIN FAILED!</h2>";
        // potential mismatched types or something
    } else {
        echo "<h2 style='color:green'>JOIN SUCCESS! Found " . count($result) . " items.</h2>";
    }
} else {
    echo "<h2 style='color:red'>No assignments found for raw query!</h2>";
}

echo "</pre>";
