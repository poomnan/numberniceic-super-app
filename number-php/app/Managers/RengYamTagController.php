<?php

namespace App\Managers;

use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class RengYamTagController extends Manager
{
    /**
     * View the dashboard for managing tag meanings.
     */
    public function dashboard(Request $request, Response $response)
    {
        $stmt = $this->db->query("SELECT * FROM rengyam_tag_meanings ORDER BY source_label ASC, tag_name ASC");
        $tags = $stmt->fetchAll();

        return $this->view->render($response, 'admin_rengyam_tags.php', [
            'tags' => $tags
        ]);
    }

    /**
     * API: Get all tag meanings (for reference or frontend usage).
     */
    public function listTags(Request $request, Response $response)
    {
        $stmt = $this->db->query("SELECT tag_name, source_label, short_description FROM rengyam_tag_meanings WHERE is_active = 1");
        $tags = $stmt->fetchAll();
        
        $response->getBody()->write(json_encode([
            'status' => 'success',
            'data' => $tags
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    /**
     * Admin: Save/Update a tag meaning.
     */
    public function saveTag(Request $request, Response $response)
    {
        $data = $request->getParsedBody();
        $tagName = trim((string) ($data['tag_name'] ?? ''));
        $sourceLabel = trim((string) ($data['source_label'] ?? ''));
        $description = trim((string) ($data['short_description'] ?? ''));
        $isActive = isset($data['is_active']) ? (int)$data['is_active'] : 1;

        if (empty($tagName)) {
             $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Tag name is required']));
             return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }
        if (empty($sourceLabel)) {
             $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Source label is required']));
             return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }
        if (empty($description)) {
             $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Description is required']));
             return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        try {
            $stmt = $this->db->prepare("
                INSERT INTO rengyam_tag_meanings (tag_name, source_label, short_description, is_active)
                VALUES (:tag_name, :source, :desc, :active)
                ON DUPLICATE KEY UPDATE 
                    source_label = VALUES(source_label),
                    short_description = VALUES(short_description),
                    is_active = VALUES(is_active)
            ");

            $success = $stmt->execute([
                ':tag_name' => $tagName,
                ':source' => $sourceLabel,
                ':desc' => $description,
                ':active' => $isActive
            ]);
        } catch (\Throwable $e) {
            $response->getBody()->write(json_encode([
                'status' => 'error',
                'message' => 'Failed to save: ' . $e->getMessage()
            ]));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(500);
        }

        $response->getBody()->write(json_encode([
            'status' => $success ? 'success' : 'error',
            'message' => $success ? 'Saved successfully' : 'Failed to save'
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    /**
     * Admin: Delete a tag meaning record.
     */
    public function deleteTag(Request $request, Response $response)
    {
        $data = $request->getParsedBody();
        $tagName = trim((string) ($data['tag_name'] ?? ''));

        if (empty($tagName)) {
             $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Tag name is required']));
             return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        try {
            $stmt = $this->db->prepare("DELETE FROM rengyam_tag_meanings WHERE tag_name = :tag_name");
            $success = $stmt->execute([':tag_name' => $tagName]);
        } catch (\Throwable $e) {
            $response->getBody()->write(json_encode([
                'status' => 'error',
                'message' => 'Failed to delete: ' . $e->getMessage()
            ]));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(500);
        }

        $response->getBody()->write(json_encode([
            'status' => $success ? 'success' : 'error',
            'message' => $success ? 'Deleted successfully' : 'Failed to delete'
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }
}
