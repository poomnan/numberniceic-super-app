<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Buddha Pang Diagnostics</h2>";

$config = include 'configs/config.php';

try {
    echo "1. Testing Database Connection... ";
    $db = new PDO("mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'], $config['db']['user'], $config['db']['pass']);
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "<span style='color:green'>OK</span><br>";

    echo "2. Checking 'buddha_pang_tb' table... ";
    $stmt = $db->query("SHOW TABLES LIKE 'buddha_pang_tb'");
    if ($stmt->rowCount() > 0) {
        echo "<span style='color:green'>Found</span><br>";
        $stmt = $db->query("DESCRIBE buddha_pang_tb");
        echo "<pre>";
        print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
        echo "</pre>";
    } else {
        echo "<span style='color:red'>NOT FOUND</span>. Please run migrate_buddha_full.php<br>";
    }

    echo "3. Checking 'user_buddha_assign' table... ";
    $stmt = $db->query("SHOW TABLES LIKE 'user_buddha_assign'");
    if ($stmt->rowCount() > 0) {
        echo "<span style='color:green'>Found</span><br>";
        $stmt = $db->query("DESCRIBE user_buddha_assign");
        echo "<pre>";
        print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
        echo "</pre>";
    } else {
        echo "<span style='color:red'>NOT FOUND</span>. Please run migrate_buddha_full.php<br>";
    }

    echo "4. Checking Upload Directory... ";
    $directory = dirname(__FILE__) . '/public/uploads/buddha';
    if (is_dir($directory)) {
        echo "<span style='color:green'>Exists</span> ($directory)<br>";
        if (is_writable($directory)) {
            echo "--- <span style='color:green'>Writable</span><br>";
        } else {
            echo "--- <span style='color:red'>NOT Writable</span> (chmod 755 or 777 required)<br>";
        }
    } else {
        echo "<span style='color:red'>NOT Found</span>. Creating it... ";
        if (@mkdir($directory, 0755, true)) {
            echo "<span style='color:green'>Created</span><br>";
        } else {
            echo "<span style='color:red'>Failed to create</span><br>";
        }
    }

} catch (Exception $e) {
    echo "<br><span style='color:red'>Error: " . $e->getMessage() . "</span>";
}
