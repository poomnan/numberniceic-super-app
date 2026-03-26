<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

require __DIR__ . '/vendor/autoload.php';
$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$db = $config['db'];

try {
    $dsn = "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $db['user'], $db['pass']);

    // Check auspicious_days range
    $min = $pdo->query("SELECT MIN(date) FROM auspicious_days")->fetchColumn();
    $max = $pdo->query("SELECT MAX(date) FROM auspicious_days")->fetchColumn();
    echo "Server DB auspicious_days: $min to $max\n";

    // Check count for next 6 months
    $start = date('Y-m-d');
    $end = date('Y-m-d', strtotime('+6 months'));
    $sql = "SELECT count(*) FROM auspicious_days WHERE date >= '$start' AND date <= '$end' AND (is_wanpra = 1 OR is_tongchai = 1 OR is_atipbadee = 1)";
    $count = $pdo->query($sql)->fetchColumn();
    echo "Auspicious days count ($start to $end): $count\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
