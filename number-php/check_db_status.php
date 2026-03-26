<?php
require_once 'vendor/autoload.php';
$config = require 'configs/config.php';

try {
    $db = new PDO(
        "mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'],
        $config['db']['user'],
        $config['db']['pass']
    );
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "--- LATEST 5 USERS ---\n";
    $stmt = $db->query("SELECT memberid, username, realname, fcm_token, birthday FROM membertb ORDER BY memberid DESC LIMIT 5");
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        echo "ID: {$row['memberid']} | User: {$row['username']} | Name: {$row['realname']} | Birthday: {$row['birthday']} | Token: " . (empty($row['fcm_token']) ? "NULL" : "EXISTS") . "\n";
    }

    echo "\n--- RECENT BAG COLORS ---\n";
    $stmt = $db->query("SELECT memberid, age, date_color_updated FROM bagcolortb ORDER BY date_color_updated DESC LIMIT 5");
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        echo "MID: {$row['memberid']} | Age: {$row['age']} | Updated: {$row['date_color_updated']}\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
