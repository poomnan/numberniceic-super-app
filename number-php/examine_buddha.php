<?php
require_once 'vendor/autoload.php';
require_once 'configs/constant.php';
require_once 'configs/config.php';

$db_config = $config['db'];
$pdo = new PDO(
    "mysql:host=" . $db_config['host'] . ";dbname=" . $db_config['dbname'],
    $db_config['user'],
    $db_config['pass']
);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);

$stmt = $pdo->query("SELECT * FROM buddha_pang_tb");
$rows = $stmt->fetchAll();

echo json_encode($rows, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
