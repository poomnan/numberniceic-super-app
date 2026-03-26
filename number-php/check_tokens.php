<?php
require_once 'vendor/autoload.php';
$config = require 'configs/config.php';

try {
    $db = new PDO(
        "mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'],
        $config['db']['user'],
        $config['db']['pass']
    );

    echo "--- USERS WITH TOKEN ---\n";
    $stmt = $db->query("SELECT memberid, username, realname, fcm_token FROM membertb WHERE fcm_token IS NOT NULL AND fcm_token != '' LIMIT 10");
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        echo "ID: {$row['memberid']} | User: {$row['username']} | Name: {$row['realname']} | Token: " . substr($row['fcm_token'], 0, 15) . "...\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
