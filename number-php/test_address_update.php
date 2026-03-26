<?php
/**
 * ทดสอบการอัปเดตที่อยู่จัดส่งสินค้า
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

    echo "Testing address update functionality...\n\n";

    // ทดสอบข้อมูล
    $testMemberId = '832'; // ใช้ memberid ที่มีอยู่จริง
    $testAddress = "123/45 ถนนสุขุมวิท แขวงคลองเตย เขตคลองเตย กรุงเทพมหานคร 10110";

    // อัปเดตที่อยู่
    $sql = "UPDATE membertb SET address = :address WHERE memberid = :memberid";
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':address', $testAddress);
    $stmt->bindParam(':memberid', $testMemberId);
    
    if ($stmt->execute()) {
        echo "✅ Address updated successfully for member ID: $testMemberId\n";
        echo "Address: $testAddress\n\n";
        
        // ตรวจสอบผลลัพธ์
        $sql = "SELECT memberid, realname, address FROM membertb WHERE memberid = :memberid";
        $stmt = $pdo->prepare($sql);
        $stmt->bindParam(':memberid', $testMemberId);
        $stmt->execute();
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($result) {
            echo "📋 Current data:\n";
            echo "Member ID: " . $result['memberid'] . "\n";
            echo "Name: " . $result['realname'] . "\n";
            echo "Address: " . ($result['address'] ?? 'NULL') . "\n";
        }
    } else {
        echo "❌ Failed to update address\n";
    }

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
