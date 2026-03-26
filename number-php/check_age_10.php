<?php
require_once 'vendor/autoload.php';
$config = require 'configs/config.php';

try {
    $db = new PDO(
        "mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'],
        $config['db']['user'],
        $config['db']['pass']
    );

    $stmt = $db->prepare("SELECT birthday FROM membertb WHERE memberid = ?");
    $stmt->execute([10]);
    $u = $stmt->fetch(PDO::FETCH_OBJ);
    echo "ID 10 Birthday: " . ($u->birthday ?? 'NULL') . "\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
