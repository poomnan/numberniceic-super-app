<?php
$config = require __DIR__ . '/configs/config.php';
$dbConfig = $config['db'];

try {
    $pdo = new PDO("mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8", $dbConfig['user'], $dbConfig['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // 1. Add 'category_name' to 'news' table (allow NULL initially to avoid strict mode default errors)
    $colCheck = $pdo->query("SHOW COLUMNS FROM news LIKE 'category_name'");
    if ($colCheck->rowCount() == 0) {
        $pdo->exec("ALTER TABLE news ADD COLUMN category_name VARCHAR(100) DEFAULT NULL AFTER news_detail");
        echo "Added 'category_name' to 'news' table (Nullable).\n";

        // Update existing rows to have a default value if needed 
        $pdo->exec("UPDATE news SET category_name = 'ทั่วไป' WHERE category_name IS NULL");
    }

    // 2. Create 'news_categories' table
    $sqlCategories = "CREATE TABLE IF NOT EXISTS news_categories (
        category_id INT AUTO_INCREMENT PRIMARY KEY,
        category_name VARCHAR(100) NOT NULL UNIQUE,
        category_color VARCHAR(20) DEFAULT '#59C514',
        sort_order INT DEFAULT 0
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;";
    $pdo->exec($sqlCategories);
    echo "Table 'news_categories' checked/created.\n";

    // 3. Insert Default Categories
    $defaults = [
        ['ข่าวและบทความที่น่าสนใจ', '#FFD600', 0],
        ['Reviews และ Feedback จากประสบการณ์ลูกค้า', '#59C514', 1],
        ['ความรู้และบทความเกี่ยวกับเบอร์โทรศัพท์', '#59C514', 2],
        ['ชื่อและนามสกุลเสริมดวงชะตา', '#59C514', 3],
        ['ศาสตร์ตัวเลขและทะเบียนรถมงคล', '#59C514', 4],
        ['ทำนายดวงชะตาจากบ้านเลขที่', '#59C514', 5],
        ['หลักการใช้และการเลือกเลขมงคลที่ถูกต้อง', '#59C514', 6]
    ];

    $stmt = $pdo->prepare("INSERT IGNORE INTO news_categories (category_name, category_color, sort_order) VALUES (?, ?, ?)");
    foreach ($defaults as $cat) {
        $stmt->execute($cat);
    }
    echo "Default categories inserted into 'news_categories'.\n";

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage();
}
