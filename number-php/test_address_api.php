<?php
/**
 * ทดสอบ API การอัปเดตข้อมูลสมาชิกพร้อมที่อยู่จัดส่ง
 */

require_once __DIR__ . '/configs/config.php';

// สร้าง Slim app
$app = new \Slim\App();

// เพิ่ม middleware สำหรับ JSON
$app->add(function ($request, $response, $next) {
    $response = $next($request, $response);
    return $response->withHeader('Content-Type', 'application/json');
});

// เส้นทางทดสอบการอัปเดตข้อมูลสมาชิก
$app->post('/test/member/update', function ($request, $response) {
    $body = $request->getParsedBody();
    
    // จำลองข้อมูลที่ส่งมาจากแอป
    $memberid = $body['memberid'] ?? '832';
    $realname = $body['realname'] ?? 'ทดสอบ ชื่อ';
    $surname = $body['surname'] ?? 'นามสกุลทดสอบ';
    $address = $body['address'] ?? '123/45 ถนนสุขุมวิท แขวงคลองเตย เขตคลองเตย กรุงเทพมหานคร 10110';
    
    try {
        $db = $this->get('db');
        
        $sql = "UPDATE membertb SET realname = :realname, surname = :surname, address = :address WHERE memberid = :memberid";
        $stmt = $db->prepare($sql);
        $stmt->bindParam(':realname', $realname);
        $stmt->bindParam(':surname', $surname);
        $stmt->bindParam(':address', $address);
        $stmt->bindParam(':memberid', $memberid);
        
        if ($stmt->execute()) {
            // ดึงข้อมูลที่อัปเดตแล้ว
            $sql = "SELECT * FROM membertb WHERE memberid = :memberid";
            $stmt = $db->prepare($sql);
            $stmt->bindParam(':memberid', $memberid);
            $stmt->execute();
            $updatedUser = $stmt->fetch(PDO::FETCH_ASSOC);
            
            $response->getBody()->write(json_encode([
                'success' => true,
                'message' => 'Member updated successfully',
                'data' => $updatedUser
            ]));
        } else {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Failed to update member'
            ]));
        }
    } catch (PDOException $e) {
        $response->getBody()->write(json_encode([
            'success' => false,
            'message' => 'Database error: ' . $e->getMessage()
        ]));
    }
    
    return $response;
});

// เส้นทางทดสอบการดึงข้อมูลสมาชิก
$app->get('/test/member/{id}', function ($request, $response, $args) {
    $memberid = $args['id'];
    
    try {
        $db = $this->get('db');
        
        $sql = "SELECT memberid, realname, surname, address FROM membertb WHERE memberid = :memberid";
        $stmt = $db->prepare($sql);
        $stmt->bindParam(':memberid', $memberid);
        $stmt->execute();
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($user) {
            $response->getBody()->write(json_encode([
                'success' => true,
                'data' => $user
            ]));
        } else {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Member not found'
            ]));
        }
    } catch (PDOException $e) {
        $response->getBody()->write(json_encode([
            'success' => false,
            'message' => 'Database error: ' . $e->getMessage()
        ]));
    }
    
    return $response;
});

// เพิ่ม dependency container
$container = new \Slim\Container();
$container['db'] = function ($c) {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8mb4",
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    return $pdo;
};

$app->run();
