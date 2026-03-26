<?php
$config = require __DIR__ . '/configs/config.php';
$dbConfig = $config['db'];

try {
    $pdo = new PDO("mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8", $dbConfig['user'], $dbConfig['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // 1. Create news_categories table
    $sqlCategories = "CREATE TABLE IF NOT EXISTS news_categories (
        category_id INT AUTO_INCREMENT PRIMARY KEY,
        category_name VARCHAR(100) NOT NULL,
        category_color VARCHAR(20) DEFAULT '#59C514',
        sort_order INT DEFAULT 0
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;";
    $pdo->exec($sqlCategories);
    echo "Table news_categories created.\n";

    // 2. Insert Default Categories
    $defaults = [
        ['Reviews และ Feedback จากประสบการณ์ลูกค้า', '#59C514', 1],
        ['ความรู้และบทความเกี่ยวกับเบอร์โทรศัพท์', '#59C514', 2],
        ['ชื่อและนามสกุลเสริมดวงชะตา', '#59C514', 3],
        ['ศาสตร์ตัวเลขและทะเบียนรถมงคล', '#59C514', 4],
        ['ทำนายดวงชะตาจากบ้านเลขที่', '#59C514', 5],
        ['หลักการใช้และการเลือกเลขมงคลที่ถูกต้อง', '#59C514', 6],
        ['ข่าวและบทความที่น่าสนใจ', '#FFD600', 0] // Hot News
    ];

    $stmt = $pdo->prepare("INSERT IGNORE INTO news_categories (category_name, category_color, sort_order) VALUES (?, ?, ?)");
    foreach ($defaults as $cat) {
        // Check duplication by name to avoid bloat (simple check)
        $check = $pdo->prepare("SELECT COUNT(*) FROM news_categories WHERE category_name = ?");
        $check->execute([$cat[0]]);
        if ($check->fetchColumn() == 0) {
            $stmt->execute($cat);
        }
    }
    echo "Default categories inserted.\n";

    // 3. Update Articles Table to include foreign key (optional, loose coupling fine for now) or just index
    // We already have 'category' column in articles (string). We might want to migrate it to ID later, 
    // but requested task implies ability to "Set Category" for article.
    // Let's ensure 'category_id' column exists in articles for relation.

    // Check if column exists first
    $colCheck = $pdo->query("SHOW COLUMNS FROM articles LIKE 'category_id'");
    if ($colCheck->rowCount() == 0) {
        $pdo->exec("ALTER TABLE articles ADD COLUMN category_id INT DEFAULT NULL");
        echo "Column category_id added to articles.\n";
    }

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage();
}
