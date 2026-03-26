<?php
require __DIR__ . '/vendor/autoload.php';

use Slim\Factory\AppFactory;
use DI\Container;

// MOCK THE APP & CONTAINER
$container = new Container();
AppFactory::setContainer($container);
$app = AppFactory::create();

// DB connection
$container->set('db', function () {
    $dbhost = 'localhost';
    $dbuser = 'zoqlszwh_ananyadb';
    $dbpass = 'IntelliP24.X';
    $dbname = 'zoqlszwh_ananyadb';
    $pdo = new PDO("mysql:host=$dbhost;dbname=$dbname;charset=utf8", $dbuser, $dbpass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    $pdo->exec("SET time_zone = '+07:00'");
    return $pdo;
});

// INSTANTIATE CONTROLLER MANUALLY
require_once __DIR__ . '/app/Managers/Manager.php';
require_once __DIR__ . '/app/Managers/UserController.php';

// MOCK REQUEST/RESPONSE
$request = (new \Slim\Psr7\Factory\ServerRequestFactory())->createServerRequest('GET', '/member/lengyam');
$response = (new \Slim\Psr7\Factory\ResponseFactory())->createResponse();

// CALL METHOD
try {
    $controller = new \App\Managers\UserController($container);
    $result = $controller->lengyamList($request, $response);
    echo (string)$result->getBody();
} catch (\Throwable $e) {
    echo json_encode(['error' => $e->getMessage()]);
}

