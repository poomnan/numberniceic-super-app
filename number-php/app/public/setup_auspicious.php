<?php
// Fix paths since we are in public/ directory
$baseDir = __DIR__ . '/..';
chdir($baseDir);

require 'vendor/autoload.php';
require 'configs/constant.php';
$config = require 'configs/config.php';

echo "<h3>Setting up Auspicious Days Database...</h3>";

// 1. Setup DB Connection
try {
    $dbConf = $config['db'];
    $db = new PDO(
        "mysql:host=" . $dbConf['host'] . ";dbname=" . $dbConf['dbname'] . ";charset=utf8",
        $dbConf['user'],
        $dbConf['pass']
    );
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $db->exec("set names utf8mb4");
    echo "Database Connected.<br>";
} catch (PDOException $e) {
    die("Database Connection Failed: " . $e->getMessage());
}

// 2. Create Table
try {
    $sql = "CREATE TABLE IF NOT EXISTS auspicious_days (
        id INT AUTO_INCREMENT PRIMARY KEY,
        date DATE NOT NULL UNIQUE,
        is_wanpra TINYINT(1) DEFAULT 0,
        is_tongchai TINYINT(1) DEFAULT 0,
        is_atipbadee TINYINT(1) DEFAULT 0,
        description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

    $db->exec($sql);
    echo "Table 'auspicious_days' checked/created.<br>";
} catch (Exception $e) {
    die("Error creating table: " . $e->getMessage());
}

// 3. Generate Data
use App\Managers\ThaiCalendarHelper;

try {
    // Generate for next 12 months (starts from yesterday/today)
    echo "Calculatingauspicious days...<br>";
    $events = ThaiCalendarHelper::getUpcomingAuspiciousEvents(13);
    echo "Found " . count($events) . " auspicious days.<br>";

    $count = 0;
    $stmt = $db->prepare("INSERT INTO auspicious_days (date, is_wanpra, is_tongchai, is_atipbadee, description) 
                          VALUES (:date, :pra, :tong, :atip, :desc)
                          ON DUPLICATE KEY UPDATE 
                          is_wanpra=:pra, is_tongchai=:tong, is_atipbadee=:atip, description=:desc");

    foreach ($events as $event) {
        $date = $event['wanpra_date'];
        $isPra = $event['is_wanpra'] ? 1 : 0;
        $isTong = $event['is_tongchai'] ? 1 : 0;
        $isAtip = $event['is_atipbadee'] ? 1 : 0;

        // Generate Description
        $descParts = [];
        if ($isPra)
            $descParts[] = "วันพระ: ควรทำบุญ ตักบาตร ฟังธรรม ถือศีล ปฏิบัติธรรม ละเว้นอบายมุข";
        if ($isTong)
            $descParts[] = "วันธงชัย: ฤกษ์งามยามดี เหมาะแก่การเริ่มต้นกิจการงานใหม่ ขึ้นบ้านใหม่ ออกรถ หรือทำการมงคลต่างๆ";
        if ($isAtip)
            $descParts[] = "วันอธิบดี: เหมาะแก่การเข้าหาผู้ใหญ่ เจรจาต่อรอง ขอความช่วยเหลือ หรือเริ่มโครงการสำคัญที่ต้องการอำนาจบารมี";

        $description = implode("\n", $descParts);

        $stmt->execute([
            ':date' => $date,
            ':pra' => $isPra,
            ':tong' => $isTong,
            ':atip' => $isAtip,
            ':desc' => $description
        ]);
        $count++;
    }
    echo "Successfully inserted/updated $count days.<br>";

} catch (Exception $e) {
    die("Error generating data: " . $e->getMessage());
}

echo "Done.";
