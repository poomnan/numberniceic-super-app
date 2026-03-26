<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

$config = include __DIR__ . '/configs/config.php';

try {
    $dsn = "mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $config['db']['user'], $config['db']['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "<h2>Updating Database Paths...</h2>";

    // 1. Buddha Pang
    $sql1 = "UPDATE buddha_pang_tb SET image_url = CONCAT('/public', image_url) WHERE image_url LIKE '/uploads/%'";
    $count1 = $pdo->exec($sql1);
    echo "Updated $count1 records in buddha_pang_tb<br>";

    // 2. Sacred Temple
    $sql2 = "UPDATE sacred_temple_tb SET image_url = CONCAT('/public', image_url) WHERE image_url LIKE '/uploads/%'";
    $count2 = $pdo->exec($sql2);
    echo "Updated $count2 records in sacred_temple_tb<br>";

    // 3. Spells Warnings
    $sql3 = "UPDATE spells_warnings SET photo = CONCAT('/public', photo) WHERE photo LIKE '/uploads/%'";
    $count3 = $pdo->exec($sql3);
    echo "Updated $count3 records in spells_warnings<br>";

    echo "<p style='color: green;'><b>All and ready!</b> All paths now include /public prefix.</p>";
    echo "<a href='/admin/buddha'>Back to Admin</a>";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
