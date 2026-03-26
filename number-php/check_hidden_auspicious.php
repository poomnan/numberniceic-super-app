<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$db = $config['db'];

try {
    $dsn = "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $db['user'], $db['pass']);

    $start = '2026-04-11'; // Since screenshot ends at April 10
    $end = date('Y-m-d', strtotime('+6 months'));

    echo "Checking auspicious days from $start to $end:\n";
    $sql = "SELECT count(*) FROM auspicious_days WHERE date >= '$start' AND date <= '$end' AND (is_wanpra = 1 OR is_tongchai = 1 OR is_atipbadee = 1)";
    $count = $pdo->query($sql)->fetchColumn();

    echo "Found $count auspicious days in this range.\n";

    if ($count == 0) {
        echo "Reason: No auspicious days in this period.\n";
    } else {
        echo "Reason: Maybe API limit or output buffering limit.\n";
        // Let's list some
        $sql = "SELECT * FROM auspicious_days WHERE date >= '$start' AND date <= '$end' AND (is_wanpra = 1 OR is_tongchai = 1 OR is_atipbadee = 1) LIMIT 5";
        $stmt = $pdo->query($sql);
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            echo " - " . $row['date'] . "\n";
        }
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
