<?php
namespace App\Managers;

use PDO;

class AdminTabianController extends Manager
{
    public function saveTabian($request, $response)
    {
        try {
            $data = $request->getParsedBody();

            $id = $data['id'] ?? null;
            $number = $data['tabian_number'] ?? '';
            $province = $data['tabian_province'] ?? 'กรุงเทพมหานคร';
            $price = $data['tabian_price'] ?? 0;
            $status = $data['tabian_status'] ?? 'available';
            $category = $data['tabian_category'] ?? '';
            $order = $data['order_no'] ?? 0;

            if (empty($number)) {
                $response->getBody()->write(json_encode(['message' => 'failed', 'error' => 'Number is required']));
                return $response->withHeader('Content-Type', 'application/json');
            }

            if (!empty($id)) {
                // Update
                $sql = "UPDATE tabian_sell SET 
                        tabian_number = :no, 
                        tabian_province = :prov, 
                        tabian_price = :price, 
                        tabian_status = :status,
                        tabian_category = :cat
                        WHERE tabian_id = :id";

                $stmt = $this->db->prepare($sql);
                $stmt->execute([
                    ':no' => $number,
                    ':prov' => $province,
                    ':price' => $price,
                    ':status' => $status,
                    ':cat' => $category,
                    ':id' => $id
                ]);
            } else {
                // Insert
                $sql = "INSERT INTO tabian_sell (tabian_number, tabian_province, tabian_price, tabian_status, tabian_category, order_no) 
                        VALUES (:no, :prov, :price, :status, :cat, :ord)";

                $stmt = $this->db->prepare($sql);
                $stmt->execute([
                    ':no' => $number,
                    ':prov' => $province,
                    ':price' => $price,
                    ':status' => $status,
                    ':cat' => $category,
                    ':ord' => $order
                ]);
            }

            $response->getBody()->write(json_encode(['message' => 'success']));
            return $response->withHeader('Content-Type', 'application/json');

        } catch (\Exception $e) {
            $response->getBody()->write(json_encode(['message' => 'failed', 'error' => $e->getMessage()]));
            return $response->withHeader('Content-Type', 'application/json');
        }
    }

    public function deleteTabian($request, $response)
    {
        try {
            $data = $request->getParsedBody();
            $id = $data['id'] ?? null;

            if ($id) {
                $stmt = $this->db->prepare("DELETE FROM tabian_sell WHERE tabian_id = :id");
                $stmt->execute([':id' => $id]);
            }

            $response->getBody()->write(json_encode(['message' => 'success']));
            return $response->withHeader('Content-Type', 'application/json');

        } catch (\Exception $e) {
            $response->getBody()->write(json_encode(['message' => 'failed', 'error' => $e->getMessage()]));
            return $response->withHeader('Content-Type', 'application/json');
        }
    }
}
