<?php
require_once 'vendor/autoload.php';
$config = require 'configs/config.php';

try {
    $db = new PDO(
        "mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'],
        $config['db']['user'],
        $config['db']['pass']
    );

    echo "Changing fcm_token column to TEXT...\n";
    $db->exec("ALTER TABLE membertb MODIFY fcm_token TEXT");
    echo "Success!\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
