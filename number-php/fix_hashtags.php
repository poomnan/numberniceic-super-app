<?php
// fix_hashtags.php
require 'vendor/autoload.php';

// Database settings
$host = '127.0.0.1';
$db = 'zoqlszwh_ananyadb';
$user = 'root';
$pass = 'root'; // Default for many local envs, adjust if needed
$charset = 'utf8mb4';

$dsn = "mysql:host=$host;dbname=$db;charset=$charset";
$options = [
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES => false,
];

try {
    $pdo = new PDO($dsn, $user, $pass, $options);
} catch (\PDOException $e) {
    throw new \PDOException($e->getMessage(), (int) $e->getCode());
}

$pdo->exec("SET sql_mode = ''");

echo "Connected to DB\n";

// Get all news IDs
$stmt = $pdo->query("SELECT newsid FROM news");
$ids = $stmt->fetchAll(PDO::FETCH_COLUMN);

foreach ($ids as $id) {
    // Randomly assign a hashtag 1-6
    $tag = rand(1, 6);
    $col = "hashtag" . $tag;

    // Also ensure 'fix' is set to something useful if we want
    // But user logic is separate.

    // Update
    $sql = "UPDATE news SET $col = 1 WHERE newsid = ?";
    $pdo->prepare($sql)->execute([$id]);
    echo "Updated news $id with $col = 1\n";
}

echo "Done fixing hashtags.\n";
