<?php

namespace App\Managers;

use PDO;
use Psr\Container\ContainerInterface;

class AdminPrivilegeController extends Manager
{
    public function __construct(ContainerInterface $container)
    {
        parent::__construct($container);
    }

    /**
     * List all privileges for management view
     */
    public function index($request, $response)
    {
        // Require admin login
        if (!isset($_SESSION['user'])) {
            return $response->withHeader('Location', '/web/login')->withStatus(302);
        }

        $this->db->exec("SET NAMES utf8mb4");
        $sql = "SELECT * FROM application_privileges ORDER BY id ASC";
        $stmt = $this->db->query($sql);
        $privileges = $stmt->fetchAll(PDO::FETCH_ASSOC);

        return $this->view->render($response, 'web_admin_privileges.php', [
            'privileges' => $privileges,
            'user' => $_SESSION['user']
        ]);
    }

    /**
     * Update privilege data
     */
    public function update($request, $response)
    {
        // Require admin login
        if (!isset($_SESSION['user'])) {
            return $response->withHeader('Location', '/web/login')->withStatus(302);
        }

        $body = $request->getParsedBody();
        $id = $body['id'] ?? null;
        $name = $body['name_th'] ?? '';
        $detail = $body['detail_th'] ?? '';
        $benefits = $body['benefits_th'] ?? '';
        $price = $body['price_th'] ?? '';

        if (!$id) {
            return $response->withHeader('Location', '/web/admin/privileges')->withStatus(302);
        }

        $this->db->exec("SET NAMES utf8mb4");
        $sql = "UPDATE application_privileges SET 
                privilege_name_th = :name,
                privilege_detail_th = :detail,
                privilege_benefits_th = :benefits,
                upgrade_price_th = :price,
                updated_at = CURRENT_TIMESTAMP
                WHERE id = :id";

        $stmt = $this->db->prepare($sql);
        $stmt->execute([
            ':name' => $name,
            ':detail' => $detail,
            ':benefits' => $benefits,
            ':price' => $price,
            ':id' => $id
        ]);

        return $response->withHeader('Location', '/web/admin/privileges')->withStatus(302);
    }
}
