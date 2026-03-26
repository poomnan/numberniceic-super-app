<?php
// install_notification_db.php
// Visit this file in browser to create the notifications table

require_once 'vendor/autoload.php';
require_once 'configs/constant.php';
require_once 'configs/config.php';

use App\Managers\NotificationManager; // Just to autoload if needed
use DI\Container;

// 1. Connect DB
try {
    $dbConfig = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $dbConfig['host'] . ";dbname=" . $dbConfig['dbname'],
        $dbConfig['user'],
        $dbConfig['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->exec("set names utf8mb4");

    echo "<h3>Database Connected Successfully</h3>";

    // 2. SQL to Create Table
    $sql = "CREATE TABLE IF NOT EXISTS notifications (
        id INT AUTO_INCREMENT PRIMARY KEY,
        member_id VARCHAR(50) NOT NULL,
        type VARCHAR(50) NOT NULL,  -- 'webview_merit', 'webview_changenum', 'webview_spell', 'bag_color', 'lucky_number', etc.
        title VARCHAR(255) NOT NULL,
        body TEXT,
        url TEXT,
        note TEXT,
        is_read BOOLEAN DEFAULT FALSE,
        read_at TIMESTAMP NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_notifications_member_type (member_id, type),
        INDEX idx_notifications_created (created_at DESC),
        INDEX idx_notifications_member_created (member_id, created_at DESC),
        INDEX idx_notifications_unread (member_id, is_read)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

    $pdo->exec($sql);
    echo "<h3>Table 'notifications' checked/created successfully.</h3>";
    echo "<p>You can now delete this file or secure it.</p>";

} catch (PDOException $e) {
    echo "<h3 style='color:red;'>Error: " . $e->getMessage() . "</h3>";
}