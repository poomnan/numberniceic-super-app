<?php
date_default_timezone_set('Asia/Bangkok');

if (PHP_SAPI == 'cli-server') {
    $url = parse_url($_SERVER['REQUEST_URI']);
    $file = __DIR__ . '/public' . $url['path'];
    if (is_file($file)) {
        return false;
    }
}

// Session configuration
ini_set('session.gc_maxlifetime', 86400);
session_set_cookie_params([
    'lifetime' => 86400,
    'path' => '/',
    'domain' => '',
    'secure' => false,
    'httponly' => true,
    'samesite' => 'Lax'
]);

if (session_status() == PHP_SESSION_NONE) {
    session_start();
}

// Debug log for session persistence
file_put_contents(__DIR__ . '/session_debug.log', date('[Y-m-d H:i:s] ') . "[SESSION] URI: " . $_SERVER['REQUEST_URI'] . " | SID: " . session_id() . " | User SET: " . (isset($_SESSION['user']) ? (is_object($_SESSION['user']) ? $_SESSION['user']->username : 'ARRAY') : 'NO') . "\n", FILE_APPEND);

error_reporting(E_ALL & ~E_DEPRECATED & ~E_STRICT);
ini_set('display_errors', 'Off');

// Request Debugger - Log every hit to the server
$requestLog = __DIR__ . '/cache/requests.log';
$rawBody = file_get_contents('php://input');
$logEntry = date('[Y-m-d H:i:s] ') . $_SERVER['REQUEST_METHOD'] . " " . $_SERVER['REQUEST_URI'] . " from " . $_SERVER['REMOTE_ADDR'] . "\nBody: " . $rawBody . "\n";
@file_put_contents($requestLog, $logEntry, FILE_APPEND);

use DI\Container;
use Slim\Factory\AppFactory;
use Slim\Views\PhpRenderer;

require_once 'vendor/autoload.php';
require_once 'configs/constant.php';
require_once 'configs/config.php';

// Create Container using PHP-DI
$container = new Container();

// Configure Container
$container->set('db', function () use ($config) {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'],
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    $pdo->exec("set names utf8mb4");
    $pdo->exec("SET time_zone = '+07:00'");
    return $pdo;
});

// View
$container->set('view', function () {
    return new PhpRenderer('./views');
});

// Set container to AppFactory
AppFactory::setContainer($container);
// Create App
$app = AppFactory::create();

// Add Base Path Detection
$basePath = (function () {
    $scriptDir = str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME']));
    if ($scriptDir === '/')
        return '';
    return $scriptDir;
})();
$app->setBasePath($basePath);

// Add Body Parsing Middleware
$app->addBodyParsingMiddleware();

// Add Routing Middleware
$app->addRoutingMiddleware();

// Add Error Middleware
$errorMiddleware = $app->addErrorMiddleware(true, true, true);

// Custom 404 Error Handler
$errorMiddleware->setErrorHandler(Slim\Exception\HttpNotFoundException::class, function ($request, $exception, $displayErrorDetails, $logErrors, $logErrorDetails) use ($app) {
    $response = $app->getResponseFactory()->createResponse();
    $view = $app->getContainer()->get('view');
    return $view->render($response, '404.php')->withStatus(404);
});

// Register container itself for injection into legacy Managers
$container->set('container', function () use ($container) {
    return $container;
});

// Routes
require_once 'app/routes.php';

$app->run();

