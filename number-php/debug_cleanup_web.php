<?php
use Slim\Factory\AppFactory;
use DI\Container;

require_once 'vendor/autoload.php';
require_once 'configs/config.php';

$container = new Container();
$dbConfig = $config['db'];
$pdo = new PDO(
    "mysql:host=" . $dbConfig['host'] . ";dbname=" . $dbConfig['dbname'],
    $dbConfig['user'],
    $dbConfig['pass']
);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->exec("SET time_zone = '+07:00'");

echo "<h3>Cleanup Debugger</h3>";
echo "Server Time (PHP): " . date('Y-m-d H:i:s') . "<br>";
echo "DB Time (MySQL NOW()): " . $pdo->query("SELECT NOW()")->fetchColumn() . "<br>";
echo "Threshold (NOW - 1 MIN): " . $pdo->query("SELECT NOW() - INTERVAL 1 MINUTE")->fetchColumn() . "<br>";

echo "<h4>All entries in user_buddha_assign:</h4>";
$stmt = $pdo->query("SELECT *, UNIX_TIMESTAMP(assigned_at) as ts, UNIX_TIMESTAMP(NOW()) as now_ts FROM user_buddha_assign");
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
foreach ($rows as $row) {
    $diff = $row['now_ts'] - $row['ts'];
    echo "ID: {$row['buddha_id']} | User: {$row['memberid']} | Assigned: {$row['assigned_at']} | Now: " . date('Y-m-d H:i:s', $row['now_ts']) . " | Diff (sec): $diff <br>";
}

echo "<h4>Expired Buddha assignments (rows that SHOULD be deleted):</h4>";
$stmt = $pdo->query("SELECT id, memberid, assigned_at FROM user_buddha_assign WHERE assigned_at < NOW() - INTERVAL 1 MINUTE");
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
echo "<pre>";
print_r($rows);
echo "</pre>";

echo "<h4>Checking FCM requirements:</h4>";
$file = __DIR__ . '/configs/service-account.json';
echo "Service Account exists: " . (file_exists($file) ? "YES" : "NO") . "<br>";
echo "Service Account readable: " . (is_readable($file) ? "YES" : "NO") . "<br>";

?>