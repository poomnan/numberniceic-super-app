<?php
/**
 * สคริปต์สำหรับสร้างบัญชีทดสอบสำหรับ Google Play Review
 * Test Account Creator for Google Play Console
 */

date_default_timezone_set('Asia/Bangkok');

require_once __DIR__ . '/configs/config.php';

// ข้อมูลบัญชีทดสอบ
$testAccount = [
    'username' => 'test@numberniceic.com',
    'password' => 'Test2026',
    'realname' => 'Google',
    'surname' => 'Reviewer',
    'birthday' => '1990-01-01',  // วันเกิด
    'shour' => '12',             // ชั่วโมงเกิด
    'sminute' => '00',           // นาทีเกิด
    'sprovince' => 'กรุงเทพมหานคร',
    'sgender' => 'm',
    'ageyear' => 36,
    'agemonth' => 0,
    'ageweek' => 0,
    'ageday' => 23,
    'vipcode' => '',
    'status' => 'active'
];

try {
    // เชื่อมต่อฐานข้อมูล
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4",
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "✓ เชื่อมต่อฐานข้อมูลสำเร็จ\n\n";

    // ตรวจสอบว่ามีบัญชีนี้อยู่แล้วหรือไม่
    $checkSql = "SELECT memberid, username FROM membertb WHERE username = :username";
    $checkStmt = $pdo->prepare($checkSql);
    $checkStmt->execute(['username' => $testAccount['username']]);
    $existing = $checkStmt->fetch(PDO::FETCH_ASSOC);

    if ($existing) {
        echo "⚠️  บัญชีนี้มีอยู่ในระบบแล้ว (ID: {$existing['memberid']})\n";
        echo "   Username: {$existing['username']}\n\n";

        // อัปเดตรหัสผ่าน
        $updateSql = "UPDATE membertb SET password = :password WHERE username = :username";
        $updateStmt = $pdo->prepare($updateSql);
        $updateStmt->execute([
            'password' => $testAccount['password'],
            'username' => $testAccount['username']
        ]);

        echo "✓ อัปเดตรหัสผ่านเรียบร้อย\n";
        $userId = $existing['memberid'];

    } else {
        // สร้างบัญชีใหม่
        $insertSql = "INSERT INTO membertb (
            realname, surname, birthday, shour, sminute, 
            ageyear, agemonth, ageweek, ageday, 
            sprovince, sgender, username, password, vipcode, status
        ) VALUES (
            :realname, :surname, :birthday, :shour, :sminute,
            :ageyear, :agemonth, :ageweek, :ageday,
            :sprovince, :sgender, :username, :password, :vipcode, :status
        )";

        $insertStmt = $pdo->prepare($insertSql);
        $insertStmt->execute($testAccount);

        $userId = $pdo->lastInsertId();

        echo "✓ สร้างบัญชีใหม่สำเร็จ (ID: {$userId})\n";
    }

    // เปิดสิทธิ์ VIP ให้บัญชีนี้
    echo "\n--- เปิดสิทธิ์ VIP ---\n";

    // ตรวจสอบว่ามี VIP อยู่แล้วหรือไม่
    $checkVipSql = "SELECT * FROM vip WHERE member_id = :member_id";
    $checkVipStmt = $pdo->prepare($checkVipSql);
    $checkVipStmt->execute(['member_id' => $userId]);
    $existingVip = $checkVipStmt->fetch(PDO::FETCH_ASSOC);

    $expireDate = date('Y-m-d', strtotime('+1 year'));

    if ($existingVip) {
        echo "✓ บัญชีนี้มีสิทธิ์ VIP อยู่แล้ว\n";

        // อัปเดตวันหมดอายุให้เป็น 1 ปีข้างหน้า
        $updateVipSql = "UPDATE vip SET expire_date = :expire_date WHERE member_id = :member_id";
        $updateVipStmt = $pdo->prepare($updateVipSql);
        $updateVipStmt->execute([
            'expire_date' => $expireDate,
            'member_id' => $userId
        ]);

        echo "✓ อัปเดตวันหมดอายุ VIP เป็น: {$expireDate}\n";

    } else {
        // สร้าง VIP ใหม่
        $insertVipSql = "INSERT INTO vip (member_id, vip_type, start_date, expire_date) 
                        VALUES (:member_id, 'premium', NOW(), :expire_date)";
        $insertVipStmt = $pdo->prepare($insertVipSql);
        $insertVipStmt->execute([
            'member_id' => $userId,
            'expire_date' => $expireDate
        ]);

        echo "✓ เปิดสิทธิ์ VIP สำเร็จ (หมดอายุ: {$expireDate})\n";
    }

    echo "\n========================================\n";
    echo "✅ สร้างบัญชีทดสอบสำเร็จ!\n";
    echo "========================================\n\n";

    echo "ข้อมูลบัญชีสำหรับ Google Play Console:\n\n";
    echo "Username: {$testAccount['username']}\n";
    echo "Password: {$testAccount['password']}\n";
    echo "User ID: {$userId}\n";
    echo "VIP Status: Active (หมดอายุ: {$expireDate})\n\n";

    echo "========================================\n";
    echo "กรอกข้อมูลนี้ใน Google Play Console:\n";
    echo "========================================\n\n";

    echo "Instruction name:\n";
    echo "Test Account for Google Play Review\n\n";

    echo "Username:\n";
    echo "{$testAccount['username']}\n\n";

    echo "Password:\n";
    echo "{$testAccount['password']}\n\n";

    echo "Additional instructions:\n";
    echo "This test account has full VIP access to all features including:\n";
    echo "- Name analysis and nickname suggestions\n";
    echo "- Article reading\n";
    echo "- Firebase notifications\n";
    echo "- All premium features\n\n";

    echo "Note: The app can be used immediately after login without any additional steps.\n";
    echo "========================================\n";

} catch (PDOException $e) {
    echo "❌ เกิดข้อผิดพลาด: " . $e->getMessage() . "\n";
    exit(1);
}
