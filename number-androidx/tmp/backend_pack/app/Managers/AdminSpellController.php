<?php
namespace App\Managers;

use PDO;

class AdminSpellController extends Manager
{
    public function index($request, $response)
    {
        $sql = "SELECT * FROM spells_warnings ORDER BY id DESC";
        $stmt = $this->db->query($sql);
        $items = $stmt->fetchAll(PDO::FETCH_OBJ);

        return $this->view->render($response, 'web_admin_spell_list.php', [
            'items' => $items,
            'user' => $_SESSION['user'] ?? null
        ]);
    }

    public function create($request, $response)
    {
        return $this->view->render($response, 'web_admin_spell_form.php', [
            'user' => $_SESSION['user'] ?? null
        ]);
    }

    public function store($request, $response)
    {
        $body = $request->getParsedBody();
        $type = $body['type'] ?? 'spell';
        $title = $body['title'] ?? '';
        $content = $body['content'] ?? '';

        $photoPath = $this->handleUpload($request);

        $sql = "INSERT INTO spells_warnings (type, title, content, photo) VALUES (:type, :title, :content, :photo)";
        $stmt = $this->db->prepare($sql);
        $stmt->execute([
            ':type' => $type,
            ':title' => $title,
            ':content' => $content,
            ':photo' => $photoPath
        ]);

        return $response->withHeader('Location', '/web/admin/spells')->withStatus(302);
    }

    public function edit($request, $response, $args)
    {
        $id = $args['id'];
        $stmt = $this->db->prepare("SELECT * FROM spells_warnings WHERE id = :id");
        $stmt->execute([':id' => $id]);
        $item = $stmt->fetch(PDO::FETCH_OBJ);

        if (!$item) {
            return $response->withHeader('Location', '/web/admin/spells')->withStatus(302);
        }

        return $this->view->render($response, 'web_admin_spell_form.php', [
            'item' => $item,
            'user' => $_SESSION['user'] ?? null
        ]);
    }

    public function update($request, $response, $args)
    {
        $id = $args['id'];
        $body = $request->getParsedBody();
        $type = $body['type'] ?? 'spell';
        $title = $body['title'] ?? '';
        $content = $body['content'] ?? '';

        $photoPath = $this->handleUpload($request, $id);

        $sql = "UPDATE spells_warnings SET type = :type, title = :title, content = :content";
        $params = [
            ':type' => $type,
            ':title' => $title,
            ':content' => $content,
            ':id' => $id
        ];

        if ($photoPath) {
            $sql .= ", photo = :photo";
            $params[':photo'] = $photoPath;
        }

        $sql .= " WHERE id = :id";

        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);

        return $response->withHeader('Location', '/web/admin/spells')->withStatus(302);
    }

    public function delete($request, $response, $args)
    {
        $id = $args['id'];
        $stmt = $this->db->prepare("DELETE FROM spells_warnings WHERE id = :id");
        $stmt->execute([':id' => $id]);

        return $response->withHeader('Location', '/web/admin/spells')->withStatus(302);
    }

    private function handleUpload($request, $existingId = null)
    {
        // Check for standard $_FILES
        if (isset($_FILES['photo']) && $_FILES['photo']['error'] === UPLOAD_ERR_OK) {
            // Absolute path to public/uploads/spells
            $uploadDir = __DIR__ . '/../../public/uploads/spells';

            // Check if directory exists, if not try to create it
            if (!file_exists($uploadDir)) {
                @mkdir($uploadDir, 0755, true);
            }

            $tmpName = $_FILES['photo']['tmp_name'];
            $name = basename($_FILES['photo']['name']);
            $extension = strtolower(pathinfo($name, PATHINFO_EXTENSION));

            // Allow only images
            if (!in_array($extension, ['jpg', 'jpeg', 'png', 'gif', 'webp'])) {
                return null;
            }

            $newName = uniqid('spell_') . '.' . $extension;
            $targetPath = $uploadDir . '/' . $newName;

            if (move_uploaded_file($tmpName, $targetPath)) {
                @chmod($targetPath, 0644); // Ensure web server can read it
                return '/public/uploads/spells/' . $newName;
            }
        }

        return null;
    }
}
