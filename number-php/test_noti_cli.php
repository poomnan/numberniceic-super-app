<?php
use Slim\Psr7\Request;
use Slim\Psr7\Response;
use Slim\Psr7\Uri;
use Slim\Psr7\Headers;
use Slim\Psr7\Stream;

require_once 'vendor/autoload.php';
$config = require 'configs/config.php';

// Mock Container
$container = new DI\Container();
$container->set('db', function () use ($config) {
    $db = $config['db'];
    $pdo = new PDO("mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'], $db['user'], $db['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    return $pdo;
});

// Mock Request
$uri = new Uri('http', 'localhost', 80, '/admin/notifications/send-bag-colors', 'memberid=10');
$headers = new Headers();
$body = new Stream(fopen('php://temp', 'r+'));
$request = new Request('GET', $uri, $headers, [], [], $body);

// Mock Response
$response = new Response();

$controller = new \App\Managers\NotificationController($container);

echo "--- RUNNING sendBagColors(memberid=10) ---\n";
try {
    $res = $controller->sendBagColors($request, $response);
    echo "Response Body: " . (string) $res->getBody() . "\n";
} catch (\Throwable $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
    echo "TRACE:\n" . $e->getTraceAsString() . "\n";
}
