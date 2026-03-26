<?php
use Slim\Factory\AppFactory;
use DI\Container;

require_once 'vendor/autoload.php';
require_once 'configs/config.php';

$container = new Container();
$container->set('db', function () use ($config) {
    $db = $config['db'];
    return new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'],
        $db['user'],
        $db['pass']
    );
});

try {
    $db = $container->get('db');
    $sql = "CREATE TABLE IF NOT EXISTS buddha_pang_tb (
        id INT AUTO_INCREMENT PRIMARY KEY,
        pang_name VARCHAR(255) NOT NULL,
        buddha_day INT DEFAULT NULL,
        description TEXT,
        image_url VARCHAR(255),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

    $db->exec($sql);
    echo "Successfully updated database structure.";
} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
