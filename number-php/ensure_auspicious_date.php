<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$db = $config['db'];

try {
    $pdo = new PDO("mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'], $db['user'], $db['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $date = '2026-02-12';
    // Check auspicious_days
    $stmt = $pdo->prepare("SELECT count(*) FROM auspicious_days WHERE date = ?");
    $stmt->execute([$date]);
    if ($stmt->fetchColumn() == 0) {
        echo "Date $date MISSING in auspicious_days. Inserting dummy entry.\n";
        $stmt = $pdo->prepare("INSERT INTO auspicious_days (date, is_wanpra, is_tongchai, is_atipbadee) VALUES (?, 0, 0, 0)");
        $stmt->execute([$date]);
    } else {
        echo "Date $date exists in auspicious_days.\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
