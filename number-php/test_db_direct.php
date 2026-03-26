<?php
require_once dirname(__DIR__) . '/configs/config.php';
try {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'],
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "Connection successful!\n";
    $stmt = $pdo->query("SELECT COUNT(*) FROM bagcolortb");
    echo "Rows in bagcolortb: " . $stmt->fetchColumn() . "\n";
} catch (\Exception $e) {
    echo "Connection failed: " . $e->getMessage() . "\n";
}
