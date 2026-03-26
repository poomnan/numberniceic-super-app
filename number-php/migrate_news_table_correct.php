<?php
$config = require __DIR__ . '/configs/config.php';
$dbConfig = $config['db'];

try {
    $pdo = new PDO("mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8", $dbConfig['user'], $dbConfig['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // 1. ตรวจสอบว่ามีตาราง 'news' อยู่จริงหรือไม่ (น่าจะมีอยู่แล้ว)
    // แต่เราจะเพิ่มคอลัมน์ 'category_id' และ 'category_name' เผื่อไว้สำหรับการจัดการแบบใหม่

    // ตรวจสอบคอลัมน์ category_name (เพื่อเก็บชื่อหมวดหมู่แบบ text ง่ายๆ ตามที่ต้องการ)
    $colCheck = $pdo->query("SHOW COLUMNS FROM news LIKE 'category_name'");
    if ($colCheck->rowCount() == 0) {
        $pdo->exec("ALTER TABLE news ADD COLUMN category_name VARCHAR(100) DEFAULT 'ทั่วไป' AFTER news_detail");
        echo "Added 'category_name' to 'news' table.\n";
    }

    // สร้างตาราง news_categories (สำหรับเก็บรายการหมวดหมู่ให้ Admin เลือก)
    $sqlCategories = "CREATE TABLE IF NOT EXISTS news_categories (
        category_id INT AUTO_INCREMENT PRIMARY KEY,
        category_name VARCHAR(100) NOT NULL UNIQUE,
        category_color VARCHAR(20) DEFAULT '#59C514',
        sort_order INT DEFAULT 0
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;";
    $pdo->exec($sqlCategories);
    echo "Table 'news_categories' checked/created.\n";

    // ใส่ข้อมูลหมวดหมู่เริ่มต้น (Default Data)
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
