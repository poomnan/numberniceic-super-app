<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

require __DIR__ . '/vendor/autoload.php';

use Slim\Factory\AppFactory;
use DI\Container;

$container = new Container();
AppFactory::setContainer($container);

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
    
    // Capture output
    ob_start();
    $result = $controller->lengyamList($request, $response);
    $output = ob_get_clean();
    
    // If output was echoed directly
    if (!empty($output)) {
        $data = json_decode($output, true);
        echo "[DIRECT OUTPUT]\n";
        echo "wan_pras count: " . count($data['wan_pras'] ?? []) . "\n";
        echo "Last item: " . json_encode(end($data['wan_pras']) ?? null) . "\n";
    } else {
        // If returned via Slim Response
        $body = (string)$result->getBody();
        $data = json_decode($body, true);
        echo "[SLIM RESPONSE]\n";
        echo "wan_pras count: " . count($data['wan_pras'] ?? []) . "\n";
        echo "Last item: " . json_encode(end($data['wan_pras']) ?? null) . "\n";
    }
} catch (\Throwable $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

