<?php
require_once 'configs/config.php';

try {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'],
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Check if table is empty
    $stmt = $pdo->query("SELECT COUNT(*) FROM sacred_temple_tb");
    $count = $stmt->fetchColumn();

    if ($count == 0) {
        $sql = "INSERT INTO sacred_temple_tb (temple_name, description, address, image_url, created_at) VALUES 
        ('วัดพระแก้ว', 'วัดพระศรีรัตนศาสดาราม หรือที่เรียกกันทั่วไปว่า วัดพระแก้ว เป็นวัดที่พระบาทสมเด็จพระพุทธยอดฟ้าจุฬาโลกมหาราชโปรดเกล้าฯ ให้สร้างขึ้นในพร้อมกับการสถาปนากรุงรัตนโกสินทร์', 'กรุงเทพมหานคร', '/uploads/temple/wat_pra_kaew.jpg', NOW()),
        ('วัดอรุณราชวราราม', 'วัดอรุณราชวรารามราชวรมหาวิหาร หรือที่นิยมเรียกกันในภาษาพูดว่า วัดแจ้ง หรือที่เรียกสั้น ๆ ว่า วัดอรุณ เป็นวัดโบราณ สร้างในสมัยอยุธยา', 'กรุงเทพมหานคร', '/uploads/temple/wat_arun.jpg', NOW())";

        $pdo->exec($sql);
        echo "✅ Added 2 sample temples successfully.\n";
    } else {
        echo "ℹ️ Table already has data ($count rows). No changes made.\n";
    }

} catch (PDOException $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
