<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

require __DIR__ . '/vendor/autoload.php';

use Slim\Factory\AppFactory;
use DI\Container;

$container = new Container();
AppFactory::setContainer($container);

// DB connection
$container->set('db', function () {
    $dbhost = 'localhost';
    $dbuser = 'zoqlszwh_ananyadb';
    $dbpass = 'IntelliP24.X';
    $dbname = 'zoqlszwh_ananyadb';
    $pdo = new PDO("mysql:host=$dbhost;dbname=$dbname;charset=utf8", $dbuser, $dbpass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    return $pdo;
});

require_once __DIR__ . '/app/Managers/Manager.php';
require_once __DIR__ . '/app/Managers/UserController.php';

$request = (new \Slim\Psr7\Factory\ServerRequestFactory())->createServerRequest('GET', '/member/lengyam');
$response = (new \Slim\Psr7\Factory\ResponseFactory())->createResponse();

try {
    $controller = new \App\Managers\UserController($container);
    $result = $controller->lengyamList($request, $response);
    $body = (string)$result->getBody();
    $data = json_decode($body, true);
    echo "wan_pras count: " . count($data['wan_pras'] ?? []) . "\n";
    echo "First 2 items:\n";
    print_r(array_slice($data['wan_pras'] ?? [], 0, 2));
} catch (\Throwable $e) {
    echo "Error: " . $e->getMessage() . "\n";
    echo $e->getTraceAsString();
}

