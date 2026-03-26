<?php

namespace App\Controllers;

use PDO;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class GuestAddressController
{
    private $pdo;
    
    public function __construct($container)
    {
        $this->pdo = $container->get('pdo');
    }
    
    /**
     * Get all guest addresses
     */
    public function getAllGuestAddresses(Request $request, Response $response)
    {
        try {
            $sql = "
                SELECT 
                    g.guest_id,
                    g.address,
                    g.android_id,
                    g.device_info,
                    g.server_created_at,
                    DATE_FORMAT(g.server_created_at, '%d-%m-%Y %H:%i') as address_created_at_formatted
                FROM guest_addresses g
                ORDER BY g.server_created_at DESC
            ";
            
            $stmt = $this->pdo->prepare($sql);
            $stmt->execute();
            $addresses = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            $response->getBody()->write(json_encode([
                'success' => true,
                'data' => $addresses,
                'count' => count($addresses)
            ]));
            
            return $response->withHeader('Content-Type', 'application/json');
            
        } catch (Exception $e) {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Database error: ' . $e->getMessage()
            ]));
            
            return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
        }
    }
    
    /**
     * Get guest address by ID
     */
    public function getGuestAddressById(Request $request, Response $response, $guestId)
    {
        try {
            $sql = "
                SELECT 
                    g.guest_id,
                    g.address,
                    g.android_id,
                    g.device_info,
                    g.server_created_at,
                    DATE_FORMAT(g.server_created_at, '%d-%m-%Y %H:%i') as address_created_at_formatted
                FROM guest_addresses g
                WHERE g.guest_id = :guest_id
                ORDER BY g.server_created_at DESC
            ";
            
            $stmt = $this->pdo->prepare($sql);
            $stmt->bindParam(':guest_id', $guestId);
            $stmt->execute();
            $addresses = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            $response->getBody()->write(json_encode([
                'success' => true,
                'data' => $addresses,
                'count' => count($addresses)
            ]));
            
            return $response->withHeader('Content-Type', 'application/json');
            
        } catch (Exception $e) {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Database error: ' . $e->getMessage()
            ]));
            
            return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
        }
    }
    
    /**
     * Search guest addresses
     */
    public function searchGuestAddresses(Request $request, Response $response, $searchTerm)
    {
        try {
            $searchTerm = '%' . $searchTerm . '%';
            
            $sql = "
                SELECT 
                    g.guest_id,
                    g.address,
                    g.android_id,
                    g.device_info,
                    g.server_created_at,
                    DATE_FORMAT(g.server_created_at, '%d-%m-%Y %H:%i') as address_created_at_formatted
                FROM guest_addresses g
                WHERE g.guest_id LIKE :search_term
                   OR g.address LIKE :search_term
                   OR g.android_id LIKE :search_term
                ORDER BY g.server_created_at DESC
            ";
            
            $stmt = $this->pdo->prepare($sql);
            $stmt->bindParam(':search_term', $searchTerm);
            $stmt->execute();
            $addresses = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            $response->getBody()->write(json_encode([
                'success' => true,
                'data' => $addresses,
                'count' => count($addresses),
                'search_term' => $searchTerm
            ]));
            
            return $response->withHeader('Content-Type', 'application/json');
            
        } catch (Exception $e) {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Database error: ' . $e->getMessage()
            ]));
            
            return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
        }
    }
    
    /**
     * Get guest address statistics
     */
    public function getGuestAddressStats(Request $request, Response $response)
    {
        try {
            // Total addresses
            $totalSql = "SELECT COUNT(*) as total_addresses FROM guest_addresses";
            $stmt = $this->pdo->prepare($totalSql);
            $stmt->execute();
            $totalResult = $stmt->fetch(PDO::FETCH_ASSOC);
            
            // Unique guests
            $uniqueSql = "SELECT COUNT(DISTINCT guest_id) as unique_guests FROM guest_addresses";
            $stmt = $this->pdo->prepare($uniqueSql);
            $stmt->execute();
            $uniqueResult = $stmt->fetch(PDO::FETCH_ASSOC);
            
            // Recent addresses (last 7 days)
            $recentSql = "
                SELECT COUNT(*) as recent_addresses 
                FROM guest_addresses 
                WHERE server_created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            ";
            $stmt = $this->pdo->prepare($recentSql);
            $stmt->execute();
            $recentResult = $stmt->fetch(PDO::FETCH_ASSOC);
            
            $response->getBody()->write(json_encode([
                'success' => true,
                'stats' => [
                    'total_addresses' => (int)$totalResult['total_addresses'],
                    'unique_guests' => (int)$uniqueResult['unique_guests'],
                    'recent_addresses_7_days' => (int)$recentResult['recent_addresses']
                ]
            ]));
            
            return $response->withHeader('Content-Type', 'application/json');
            
        } catch (Exception $e) {
            $response->getBody()->write(json_encode([
                'success' => false,
                'message' => 'Database error: ' . $e->getMessage()
            ]));
            
            return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
        }
    }
}
