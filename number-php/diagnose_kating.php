<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

echo "Starting diagnosis...\n";

$configFile = __DIR__ . '/configs/config.php';
if (!file_exists($configFile)) {
    die("Config file not found: $configFile\n");
}
echo "Config file found.\n";

$config = require $configFile;
if (!isset($config['db'])) {
    die("DB config missing.\n");
}
echo "DB config loaded.\n";

$db = $config['db'];
$dsn = "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'];
echo "Connecting to $dsn with user " . $db['user'] . "...\n";

try {
    $pdo = new PDO($dsn, $db['user'], $db['pass'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);
    echo "Connected successfully.\n";
    $pdo->exec("set names utf8mb4");

    // Check table existence
    $tables = $pdo->query("SHOW TABLES LIKE 'dayspecialtb'")->fetchAll();
    if (count($tables) == 0) {
        die("Table 'dayspecialtb' does not exist!\n");
    }
    echo "Table 'dayspecialtb' exists.\n";

    // Check wan_kating column
    $cols = $pdo->query("SHOW COLUMNS FROM dayspecialtb LIKE 'wan_kating'")->fetchAll();
    if (count($cols) == 0) {
        die("Column 'wan_kating' does not exist in 'dayspecialtb'!\n");
    }
    echo "Column 'wan_kating' exists.\n";

    // Check data
    $count = $pdo->query("SELECT COUNT(*) FROM dayspecialtb WHERE wan_kating = '1'")->fetchColumn();
    echo "Found $count records with wan_kating='1'.\n";

    if ($count > 0) {
        echo "Sample data:\n";
        $stmt = $pdo->query("SELECT wan_date FROM dayspecialtb WHERE wan_kating = '1' ORDER BY wan_date DESC LIMIT 5");
        while ($row = $stmt->fetch()) {
            echo " - " . $row['wan_date'] . "\n";
        }
    }

} catch (PDOException $e) {
    die("DB Error: " . $e->getMessage() . "\n");
}
