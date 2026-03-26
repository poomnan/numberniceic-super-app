<?php
/**
 * ทดสอบการอัปเดตที่อยู่จัดส่งสินค้าบนเซิร์ฟเวอร์
 */

require_once __DIR__ . '/configs/config.php';

try {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4",
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "🔍 ตรวจสอบข้อมูลสมาชิกก่อนอัปเดต...\n\n";

    // ตรวจสอบข้อมูลปัจจุบัน
    $testMemberId = '832';
    $sql = "SELECT memberid, realname, surname, address FROM membertb WHERE memberid = :memberid";
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':memberid', $testMemberId);
    $stmt->execute();
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($result) {
        echo "📋 ข้อมูลปัจจุบัน:\n";
        echo "Member ID: " . $result['memberid'] . "\n";
        echo "Name: " . $result['realname'] . " " . ($result['surname'] ?? '') . "\n";
        echo "Address: " . ($result['address'] ?? 'NULL') . "\n\n";
    } else {
        echo "❌ ไม่พบข้อมูลสมาชิก ID: $testMemberId\n";
        exit;
    }

    echo "🧪 ทดสอบการอัปเดตที่อยู่...\n\n";

    // ทดสอบการอัปเดต
    $testAddress = "123/45 ถนนสุขุมวิท แขวงคลองเตย เขตคลองเตย กรุงเทพมหานคร 10110";
    
    $sql = "UPDATE membertb SET address = :address WHERE memberid = :memberid";
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':address', $testAddress);
    $stmt->bindParam(':memberid', $testMemberId);
    
    if ($stmt->execute()) {
        echo "✅ อัปเดตที่อยู่สำเร็จ\n";
        echo "Address: $testAddress\n\n";
        
        // ตรวจสอบผลลัพธ์
        $sql = "SELECT memberid, realname, surname, address FROM membertb WHERE memberid = :memberid";
        $stmt = $pdo->prepare($sql);
        $stmt->bindParam(':memberid', $testMemberId);
        $stmt->execute();
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        
        echo "📋 ข้อมูลหลังอัปเดต:\n";
        echo "Member ID: " . $result['memberid'] . "\n";
        echo "Name: " . $result['realname'] . " " . ($result['surname'] ?? '') . "\n";
        echo "Address: " . ($result['address'] ?? 'NULL') . "\n\n";
        
        // ทดสอบ API endpoint
        echo "🌐 ทดสอบ API endpoint...\n";
        
        // สร้าง HTTP request ไปยัง API
        $apiUrl = 'http://43.228.85.200/member/update';
        $postData = [
            'memberid' => $testMemberId,
            'realname' => $result['realname'],
            'surname' => $result['surname'] ?? '',
            'address' => 'ที่อยู่ใหม่จาก API: 456/78 ถนนพระราม 4 เขตบางรัก กรุงเทพมหานคร 10120'
        ];
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $apiUrl);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($postData));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Content-Type: application/x-www-form-urlencoded'
        ]);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        echo "HTTP Status: $httpCode\n";
        echo "Response: $response\n";
        
    } else {
        echo "❌ อัปเดตที่อยู่ไม่สำเร็จ\n";
    }

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
