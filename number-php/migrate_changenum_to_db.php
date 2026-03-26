<?php
require_once __DIR__ . '/configs/config.php';

try {
    $db = new PDO(
        "mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'],
        $config['db']['user'],
        $config['db']['pass']
    );
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $db->exec("set names utf8mb4");

    // Create table for change procedures
    $db->exec("CREATE TABLE IF NOT EXISTS changenum_contents (
        file_name VARCHAR(50) PRIMARY KEY,
        title VARCHAR(255),
        content LONGTEXT,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    // Read and Process files from views/changenum/
    $files = glob(__DIR__ . '/views/changenum/*.phtml');
    foreach ($files as $file) {
        $basename = basename($file, '.phtml');

        $html = file_get_contents($file);

        // Extract title from <h3 class="... header">...</h3>
        $title = $basename;
        $titleStart = strpos($html, '<h3');
        if ($titleStart !== false) {
            $titleStart = strpos($html, '>', $titleStart) + 1;
            $titleEnd = strpos($html, '</h3>', $titleStart);
            if ($titleEnd !== false) {
                $title = trim(strip_tags(substr($html, $titleStart, $titleEnd - $titleStart)));
            }
        }

        // Extract content from <div class="ui attached segment">...</div>
        $content = '';
        $contentStart = strpos($html, '<div class="ui attached segment">');
        if ($contentStart !== false) {
            $contentStart += strlen('<div class="ui attached segment">');
            // Find the last </div> before </body> or end of file
            $lastDiv = strrpos($html, '</div>');
            if ($lastDiv !== false && $lastDiv > $contentStart) {
                $content = trim(substr($html, $contentStart, $lastDiv - $contentStart));
            }
        }

        // Insert or Update DB
        if ($basename && $content) {
            $stmt = $db->prepare("INSERT INTO changenum_contents (file_name, title, content) VALUES (:fname, :title, :content) 
                ON DUPLICATE KEY UPDATE title=VALUES(title), content=VALUES(content)");
            $stmt->execute([
                ':fname' => $basename,
                ':title' => $title,
                ':content' => $content
            ]);
            echo "Migrated: $basename \n";
        } else {
            echo "Failed to parse content for: $basename \n";
        }
    }
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
