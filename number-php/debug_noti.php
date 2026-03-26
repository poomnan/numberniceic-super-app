<?php
use Dotenv\Dotenv;
use DI\Container;
use App\Managers\NotificationManager;
use App\Managers\Manager;
use Psr\Container\ContainerInterface;

require_once 'vendor/autoload.php';
require_once 'configs/constant.php';
require_once 'configs/config.php';

// Mock Exception Handler
set_exception_handler(function ($e) {
    echo "Uncaught Exception: " . $e->getMessage() . "\n";
    echo "Trace: " . $e->getTraceAsString() . "\n";
});

set_error_handler(function ($errno, $errstr, $errfile, $errline) {
    echo "Error [$errno]: $errstr in $errfile on line $errline\n";
});

echo "1. Testing Container Creation...\n";
$container = new Container();
$container->set('db', function () use ($config) {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'],
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    return $pdo;
});
echo "Container created.\n";

echo "2. Testing NotificationManager Instantiation...\n";
try {
    $nm = new NotificationManager($container);
    echo "NotificationManager instantiated successfully.\n";
} catch (Throwable $e) {
    echo "FAIL: Could not instantiate NotificationManager. " . $e->getMessage() . "\n";
    exit;
}

echo "3. Testing DB Connection via Manager magic get...\n";
try {
    // Hack to access db check via reflection or just try to use it
    // NotificationManager::saveNotification uses $this->db
    // We'll try to insert a fake notification
    $res = $nm->saveNotification('test_user', 'debug', 'Debug Title', 'Debug Body');
    if ($res) {
        echo "SUCCESS: Notification saved. ID: " . $res . "\n";
    } else {
        echo "FAIL: saveNotification returned false (Check error logs or table existence)\n";
        // Check if table exists
        $pdo = $container->get('db');
        $stmt = $pdo->query("SHOW TABLES LIKE 'notifications'");
        if ($stmt->rowCount() == 0) {
            echo "CRITICAL: Table 'notifications' DOES NOT EXIST!\n";
            echo "Attempting to create table...\n";
            $sql = file_get_contents('create_notifications_table.sql');
            if ($sql) {
                $pdo->exec($sql);
                echo "Table created.\n";
            } else {
                echo "Could not find create_notifications_table.sql\n";
            }
        }
    }
} catch (Throwable $e) {
    echo "FAIL: DB Operation failed. " . $e->getMessage() . "\n";
}
