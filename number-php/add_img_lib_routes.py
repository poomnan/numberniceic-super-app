
filename = 'app/routes.php'
with open(filename, 'r') as f:
    lines = f.readlines()

# Find insertion point: After Image API Route
insert_index = -1
for i, line in enumerate(lines):
    if "/api/images" in line:
        # Find the closing brace/semicolon of this route block
        # Usually indented. Let's look for the next "});" or next route
        for j in range(i, len(lines)):
             if "});" in lines[j] and "api/images" not in lines[j]: # Simple heuristic
                 insert_index = j + 1
                 break
        break

if insert_index == -1:
    # Fallback: Insert before Article Routes
    for i, line in enumerate(lines):
        if "// Articles Management Routes" in line:
             insert_index = i
             break

new_code = """
        // ----------------------------------------------------
        // IMAGE LIBRARY ROUTES
        // ----------------------------------------------------
        
        // 1. View Library
        $adminGroup->get('/images', function ($request, $response) use ($container) {
            if (session_status() == PHP_SESSION_NONE) session_start();
            
            // Check Admin
            if (!isset($_SESSION['user']) || (!in_array(strtolower($_SESSION['user']->vipcode ?? ''), ['admin', 'administrator']))) {
                 return $response->withHeader('Location', '/web/login')->withStatus(302);
            }

            $dir = __DIR__ . '/../public/uploads';
            $images = [];
            
            if (is_dir($dir)) {
                $files = scandir($dir);
                foreach ($files as $f) {
                    if ($f !== '.' && $f !== '..' && !is_dir("$dir/$f")) {
                        if (preg_match('/\.(jpg|jpeg|png|gif|webp)$/i', $f)) {
                            $images[] = [
                                'name' => $f,
                                'url' => '/uploads/' . $f,
                                'size' => filesize("$dir/$f"),
                                'time' => filemtime("$dir/$f")
                            ];
                        }
                    }
                }
            }
            // Sort by Date (Newest first)
            usort($images, function($a, $b) { return $b['time'] - $a['time']; });

            $view = new \Slim\Views\PhpRenderer(__DIR__ . '/../views');
            return $view->render($response, 'web_admin_images.php', ['images' => $images]);
        });

        // 2. Upload Image
        $adminGroup->post('/images/upload', function ($request, $response) {
            $directory = __DIR__ . '/../public/uploads';
            $uploadedFiles = $request->getUploadedFiles();
            $uploadedFile = $uploadedFiles['image'] ?? null;

            if ($uploadedFile && $uploadedFile->getError() === UPLOAD_ERR_OK) {
                $extension = pathinfo($uploadedFile->getClientFilename(), PATHINFO_EXTENSION);
                // Random name to prevent conflicts and encoding issues
                $basename = bin2hex(random_bytes(8)); 
                $filename = sprintf('%s.%s', $basename, $extension);
                $uploadedFile->moveTo($directory . DIRECTORY_SEPARATOR . $filename);
            }
            return $response->withHeader('Location', '/web/admin/images')->withStatus(302);
        });

        // 3. Delete Image
        $adminGroup->get('/images/delete/{name}', function ($request, $response, $args) {
            // Check Admin logic again if strict security needed, but assuming admin group middleware handles basic auth context
            // Better to re-check inside sensitive op
             if (session_status() == PHP_SESSION_NONE) session_start();
             if (!isset($_SESSION['user']) || (!in_array(strtolower($_SESSION['user']->vipcode ?? ''), ['admin', 'administrator']))) {
                 die("Access Denied");
             }

            $name = $args['name'];
            // Prevent Path Traversal
            if (strpos($name, '/') !== false || strpos($name, '\\') !== false || strpos($name, '..') !== false) {
                die("Invalid filename");
            }

            $path = __DIR__ . '/../public/uploads/' . $name;
            if (file_exists($path)) {
                unlink($path);
            }
            return $response->withHeader('Location', '/web/admin/images')->withStatus(302);
        });
        
"""

if insert_index != -1:
    lines.insert(insert_index, new_code)
    with open(filename, 'w') as f:
        f.writelines(lines)
    print("Added Image Library Routes.")
else:
    print("Could not find insertion point.")
