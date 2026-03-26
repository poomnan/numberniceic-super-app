<?php
require_once 'vendor/autoload.php';
use DI\Container;
$container = new Container();
try {
    $controller = $container->get(\App\Managers\AdminController::class);
    echo "Success: " . get_class($controller);
} catch (\Exception $e) {
    echo "Error: " . $e->getMessage();
}
