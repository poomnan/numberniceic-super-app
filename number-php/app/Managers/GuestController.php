<?php

namespace App\Managers;

class GuestController
{
    private $db;
    
    public function __construct($container = null) {
        if ($container && $container->has('db')) {
            $this->db = $container->get('db');
        }
    }
    
    /**
     * บันทึกข้อมูล guest ใหม่
     */
    public function registerGuest($request, $response)
    {
        $body = $request->getParsedBody();
        $guestId = $body['guest_id'] ?? '';
        $androidId = $body['android_id'] ?? '';
        $deviceInfo = $body['device_info'] ?? '';
        $appVersion = $body['app_version'] ?? '';
        $createdAt = $body['created_at'] ?? time();
        
        if (empty($guestId)) {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Guest ID is required'
            ]));
            return $response->withHeader('Content-Type', 'application/json');
        }
        
        try {
            // ตรวจสอบว่ามีอยู่แล้วหรือไม่
            $checkSql = "SELECT id FROM guest_users WHERE guest_id = :guest_id";
            $stmt = $this->db->prepare($checkSql);
            $stmt->execute([':guest_id' => $guestId]);
            
            if (!$stmt->fetch()) {
                // สร้างตาราง guest_users ถ้ายังไม่มี
                $this->createGuestTableIfNotExists();
                
                // สร้าง guest ใหม่
                $sql = "INSERT INTO guest_users (guest_id, android_id, device_info, app_version, created_at) 
                        VALUES (:guest_id, :android_id, :device_info, :app_version, :created_at)";
                $stmt = $this->db->prepare($sql);
                $stmt->execute([
                    ':guest_id' => $guestId,
                    ':android_id' => $androidId,
                    ':device_info' => $deviceInfo,
                    ':app_version' => $appVersion,
                    ':created_at' => $createdAt
                ]);
            }
            
            $response->getBody()->write(json_encode([
                'success' => true,
                'guest_id' => $guestId,
                'server_guest_id' => $guestId
            ]));
            
        } catch (PDOException $e) {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Database error: ' . $e->getMessage()
            ]));
        }
        
        return $response->withHeader('Content-Type', 'application/json');
    }
    
    /**
     * บันทึกที่อยู่ของ guest
     */
    public function saveGuestAddress($request, $response)
    {
        $body = $request->getParsedBody();
        $guestId = $body['guest_id'] ?? '';
        $address = $body['address'] ?? '';
        
        if (empty($guestId) || empty($address)) {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Guest ID and address are required'
            ]));
            return $response->withHeader('Content-Type', 'application/json');
        }
        
        try {
            // สร้างตาราง guest_addresses ถ้ายังไม่มี
            $this->createGuestAddressTableIfNotExists();
            
            // บันทึกที่อยู่
            $sql = "INSERT INTO guest_addresses (guest_id, address, created_at) 
                    VALUES (:guest_id, :address, :created_at)";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([
                ':guest_id' => $guestId,
                ':address' => $address,
                ':created_at' => time()
            ]);
            
            $response->getBody()->write(json_encode([
                'success' => true,
                'message' => 'Address saved successfully'
            ]));
            
        } catch (PDOException $e) {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Database error: ' . $e->getMessage()
            ]));
        }
        
        return $response->withHeader('Content-Type', 'application/json');
    }
    
    /**
     * บันทึก order ของ guest
     */
    public function saveGuestOrder($request, $response)
    {
        $body = $request->getParsedBody();
        $guestId = $body['guest_id'] ?? '';
        $orderId = $body['order_id'] ?? '';
        $orderData = $body['order_data'] ?? '';
        
        if (empty($guestId) || empty($orderId)) {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Guest ID and order ID are required'
            ]));
            return $response->withHeader('Content-Type', 'application/json');
        }
        
        try {
            // สร้างตาราง guest_orders ถ้ายังไม่มี
            $this->createGuestOrderTableIfNotExists();
            
            // บันทึก order
            $sql = "INSERT INTO guest_orders (guest_id, order_id, order_data, created_at) 
                    VALUES (:guest_id, :order_id, :order_data, NOW())";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([
                ':guest_id' => $guestId,
                ':order_id' => $orderId,
                ':order_data' => $orderData
            ]);
            
            $response->getBody()->write(json_encode([
                'success' => true,
                'message' => 'Order saved successfully'
            ]));
            
        } catch (PDOException $e) {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Database error: ' . $e->getMessage()
            ]));
        }
        
        return $response->withHeader('Content-Type', 'application/json');
    }
    
    /**
     * สร้างตาราง guest_users
     */
    private function createGuestTableIfNotExists()
    {
        $sql = "CREATE TABLE IF NOT EXISTS guest_users (
            id INT AUTO_INCREMENT PRIMARY KEY,
            guest_id VARCHAR(255) UNIQUE NOT NULL,
            android_id VARCHAR(255),
            device_info TEXT,
            app_version VARCHAR(50),
            created_at BIGINT,
            server_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        $this->db->exec($sql);
        
        // สร้างตาราง guest_addresses
        $sql = "CREATE TABLE IF NOT EXISTS guest_addresses (
            id INT AUTO_INCREMENT PRIMARY KEY,
            guest_id VARCHAR(255) NOT NULL,
            address TEXT NOT NULL,
            created_at BIGINT,
            server_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_guest_id (guest_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        $this->db->exec($sql);
        
        // สร้างตาราง guest_orders
        $sql = "CREATE TABLE IF NOT EXISTS guest_orders (
            id INT AUTO_INCREMENT PRIMARY KEY,
            guest_id VARCHAR(255) NOT NULL,
            order_id VARCHAR(255) NOT NULL,
            order_data TEXT,
            created_at BIGINT,
            server_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_guest_id (guest_id),
            INDEX idx_order_id (order_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        $this->db->exec($sql);
    }
    
    /**
     * สร้างตาราง guest_addresses
     */
    private function createGuestAddressTableIfNotExists()
    {
        $sql = "CREATE TABLE IF NOT EXISTS guest_addresses (
            id INT AUTO_INCREMENT PRIMARY KEY,
            guest_id VARCHAR(255) NOT NULL,
            address TEXT NOT NULL,
            created_at BIGINT,
            server_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY unique_guest (guest_id),
            FOREIGN KEY (guest_id) REFERENCES guest_users(guest_id) ON DELETE CASCADE,
            INDEX idx_guest_id (guest_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        $this->db->exec($sql);
    }
    
    /**
     * สร้างตาราง guest_orders
     */
    private function createGuestOrderTableIfNotExists()
    {
        $sql = "CREATE TABLE IF NOT EXISTS guest_orders (
            id INT AUTO_INCREMENT PRIMARY KEY,
            guest_id VARCHAR(100) NOT NULL,
            order_id VARCHAR(100) NOT NULL,
            order_data TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (guest_id) REFERENCES guest_users(guest_id) ON DELETE CASCADE,
            INDEX idx_guest_id (guest_id),
            INDEX idx_order_id (order_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        $this->db->exec($sql);
    }
}
