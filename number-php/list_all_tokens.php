<?php
require_once 'vendor/autoload.php';
$config = require 'configs/config.php';

try {
    $db = new PDO(
        "mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'],
        $config['db']['user'],
        $config['db']['pass']
    );

    $stmt = $db->query("SELECT memberid, realname, fcm_token FROM membertb WHERE fcm_token IS NOT NULL AND fcm_token != ''");
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo "Count: " . count($rows) . "\n";
    foreach ($rows as $row) {
        echo "ID: {$row['memberid']} | Name: {$row['realname']} | Token: " . substr($row['fcm_token'], 0, 15) . "...\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
