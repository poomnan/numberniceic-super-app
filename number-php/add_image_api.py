
filename = 'app/routes.php'
with open(filename, 'r') as f:
    lines = f.readlines()

# Find insertion point: Before "// Articles Management Routes"
insert_index = -1
for i, line in enumerate(lines):
    if "// Articles Management Routes" in line:
        insert_index = i
        break

if insert_index == -1:
    # Fallback: insert after "$adminGroup->post('/users/{id}', ..." block end
    # Trying to find a safe spot inside Admin Group
    for i, line in enumerate(lines):
        if "$adminGroup->get('/articles'," in line:
             insert_index = i
             break

new_code = """
        // API List Images
        $adminGroup->get('/api/images', function ($request, $response) {
             $dir = __DIR__ . '/../public/uploads';
             $images = [];
             if (is_dir($dir)) {
                 $files = scandir($dir);
                 foreach ($files as $f) {
                     if ($f !== '.' && $f !== '..' && !is_dir("$dir/$f")) {
                         if (preg_match('/\.(jpg|jpeg|png|gif|webp)$/i', $f)) {
                            $images[] = $f;
                         }
                     }
                 }
             }
             $response->getBody()->write(json_encode($images));
             return $response->withHeader('Content-Type', 'application/json');
        });
        
"""

if insert_index != -1:
    lines.insert(insert_index, new_code)
    
    with open(filename, 'w') as f:
        f.writelines(lines)
    print("Added Image API Route.")
else:
    print("Could not find insertion point.")
