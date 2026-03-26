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
$pdo->exec("SET time_zone = '+07:00'");

$mid = 832;

echo "<pre>";
echo "<h3>Refreshing Assignments for User 832 (Set to NOW)</h3>";

// Update assigned_at to current time for all assignments of user 832
$pdo->exec("UPDATE user_buddha_assign SET assigned_at = NOW() WHERE memberid = '$mid'");
$pdo->exec("UPDATE user_merit_assign SET assigned_at = NOW() WHERE memberid = '$mid'");
$pdo->exec("UPDATE user_temple_assign SET assigned_at = NOW() WHERE memberid = '$mid'");

echo "Done. The items will be visible in the app for the next 1 minute.\n";
echo "Current Server Time: " . $pdo->query("SELECT NOW()")->fetchColumn() . "\n";

echo "</pre>";
?>