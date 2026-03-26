<?php
require_once 'vendor/autoload.php';
$config = require 'configs/config.php';

try {
    $db = new PDO(
        "mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'],
        $config['db']['user'],
        $config['db']['pass']
    );

    echo "--- MEMBERTB STRUCTURE ---\n";
    $stmt = $db->query("DESCRIBE membertb");
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        if ($row['Field'] == 'fcm_token') {
            print_r($row);
        }
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
