<?php
require_once __DIR__ . '/configs/config.php';

try {
    $dbConfig = clone (object) []; // Using global $config var from config.php
    $db = new PDO(
        "mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'],
        $config['db']['user'],
        $config['db']['pass']
    );
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $db->exec("set names utf8mb4");

    // Create table
    $db->exec("CREATE TABLE IF NOT EXISTS merit_contents (
        file_name VARCHAR(50) PRIMARY KEY,
        title VARCHAR(255),
        content LONGTEXT,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    // Read and Process files
    $files = glob(__DIR__ . '/views/tambon/*.phtml');
    foreach ($files as $file) {
        $basename = basename($file, '.phtml');
        if ($basename === 'template' || $basename === 'dynamic')
            continue;

        $html = file_get_contents($file);

        // Extract title
        $titleStart = strpos($html, '<h3');
        if ($titleStart !== false) {
            $titleStart = strpos($html, '>', $titleStart) + 1;
            $titleEnd = strpos($html, '</h3>', $titleStart);
            $title = trim(substr($html, $titleStart, $titleEnd - $titleStart));
        } else {
            $title = $basename;
        }

        // Extract content
        $contentStart = strpos($html, '<div class="ui attached segment">');
        if ($contentStart !== false) {
            $contentStart += strlen('<div class="ui attached segment">');
            $divEnd = strrpos($html, '</div>');
            $content = trim(substr($html, $contentStart, $divEnd - $contentStart));
        } else {
            $content = '';
        }

        // Insert or Update DB
        if ($basename && $title && $content) {
            $stmt = $db->prepare("INSERT INTO merit_contents (file_name, title, content) VALUES (:fname, :title, :content) 
                ON DUPLICATE KEY UPDATE title=VALUES(title), content=VALUES(content)");
            $stmt->execute([
                ':fname' => $basename,
                ':title' => $title,
                ':content' => $content
            ]);
            echo "Migrated: $basename \n";
        } else {
            echo "Failed to parse: $basename \n";
        }
    }
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
