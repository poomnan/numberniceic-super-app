<?php

namespace App\Managers;

use PDO;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class AdminImageController extends Manager
{
    // Upload Directory (Shared with Go backend for Nginx alias)
    protected $uploadDir = '/home/tayap/go-naming/uploads/library';

    public function __construct(\Psr\Container\ContainerInterface $container)
    {
        parent::__construct($container);

        // Ensure directory exists
        if (!file_exists($this->uploadDir)) {
            mkdir($this->uploadDir, 0777, true);
        }
    }

    // List Images
    public function listImages(Request $request, Response $response)
    {
        $files = glob($this->uploadDir . '/*.{jpg,jpeg,png,gif,webp}', GLOB_BRACE);
        $images = [];

        foreach ($files as $file) {
            $images[] = [
                'name' => basename($file),
                'url' => '/uploads/library/' . basename($file),
                'size' => filesize($file),
                'time' => filemtime($file)
            ];
        }

        // Sort by newest first
        usort($images, function ($a, $b) {
            return $b['time'] - $a['time'];
        });

        $response->getBody()->write(json_encode($images));
        return $response->withHeader('Content-Type', 'application/json');
    }

    // Image Upload
    public function uploadImage(Request $request, Response $response)
    {
        $uploadedFiles = $request->getUploadedFiles();
        if (empty($uploadedFiles['file'])) {
            $response->getBody()->write(json_encode(['error' => 'No file uploaded']));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        $uploadedFile = $uploadedFiles['file'];
        if ($uploadedFile->getError() === UPLOAD_ERR_OK) {
            $filename = $this->moveUploadedFile($uploadedFile);
            $response->getBody()->write(json_encode([
                'status' => 'success',
                'url' => '/uploads/library/' . $filename
            ]));
            return $response->withHeader('Content-Type', 'application/json');
        }

        $response->getBody()->write(json_encode(['error' => 'Upload failed']));
        return $response->withStatus(500)->withHeader('Content-Type', 'application/json');
    }

    // Delete Image
    public function deleteImage(Request $request, Response $response)
    {
        $data = $request->getParsedBody();
        $name = $data['name'] ?? '';

        if (!$name) {
            $response->getBody()->write(json_encode(['error' => 'Filename required']));
            return $response->withStatus(400)->withHeader('Content-Type', 'application/json');
        }

        $filename = basename($name); // Security
        $filePath = $this->uploadDir . '/' . $filename;

        if (file_exists($filePath)) {
            unlink($filePath);
            $response->getBody()->write(json_encode(['status' => 'success']));
        } else {
            $response->getBody()->write(json_encode(['error' => 'File not found']));
            return $response->withStatus(404)->withHeader('Content-Type', 'application/json');
        }

        return $response->withHeader('Content-Type', 'application/json');
    }

    private function moveUploadedFile($uploadedFile)
    {
        $extension = pathinfo($uploadedFile->getClientFilename(), PATHINFO_EXTENSION);
        $basename = bin2hex(random_bytes(8));
        $filename = sprintf('%s_%d.%s', $basename, time(), $extension);

        $uploadedFile->moveTo($this->uploadDir . DIRECTORY_SEPARATOR . $filename);

        return $filename;
    }
}
