<?php
if (file_exists(__DIR__ . '/../configs/config.php')) {
    require __DIR__ . '/../configs/config.php';
} else {
    die("config.php not found");
}

try {
    $db = $config['db'];
    $pdo = new PDO("mysql:host={$db['host']};dbname={$db['dbname']}", $db['user'], $db['pass']);

    $stmt = $pdo->query("DESCRIBE news");
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo "<h1>Table: news</h1>";
    echo "<table border='1'><tr><th>Field</th><th>Type</th></tr>";
    foreach ($columns as $col) {
        echo "<tr>";
        echo "<td>{$col['Field']}</td>";
        echo "<td>{$col['Type']}</td>";
        echo "</tr>";
    }
    echo "</table>";

} catch (Exception $e) {
    echo $e->getMessage();
}
?>