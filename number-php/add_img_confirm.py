
filename = 'app/routes.php'
with open(filename, 'r') as f:
    lines = f.readlines()

# Find insertion point: After Image Library Routes
# Look for "// 3. Delete Image" block end
insert_index = -1
found_delete = False
for i, line in enumerate(lines):
    if "// 3. Delete Image" in line:
        found_delete = True
    
    if found_delete and "});" in line:
        insert_index = i + 1
        break

new_code = """
        // 4. Image Delete Confirmation
        $adminGroup->get('/images/confirm-delete/{name}', function ($request, $response, $args) use ($container) {
             if (session_status() == PHP_SESSION_NONE) session_start();
             if (!isset($_SESSION['user']) || (!in_array(strtolower($_SESSION['user']->vipcode ?? ''), ['admin', 'administrator']))) {
                 return $response->withHeader('Location', '/web/login')->withStatus(302);
             }
             
             $name = $args['name'];
             $imageUrl = '/uploads/' . $name;
             
             echo "<div style='font-family:sans-serif; text-align:center; margin-top:50px;'>";
             echo "<h2 style='color:red;'>ยืนยันการลบรูปภาพ (Confirm Delete Image)</h2>";
             echo "<div style='text-align:center;'><img src='$imageUrl' style='max-height:300px; max-width:90%; border:1px solid #ddd; margin:20px; box-shadow:0 0 10px rgba(0,0,0,0.1); border-radius:8px;'></div>";
             echo "<h3 style='color:#555;'>$name</h3>";
             echo "<div style='margin-top:30px;'>";
             echo "<a href='/web/admin/images/exec-delete/$name' style='background:#dc3545; color:white; padding:15px 30px; text-decoration:none; border-radius:5px; font-weight:bold;'>ยืนยัน ลบรูปนี้ (Yes, Delete)</a>";
             echo "&nbsp;&nbsp;&nbsp;";
             echo "<a href='/web/admin/images' style='background:#6c757d; color:white; padding:15px 30px; text-decoration:none; border-radius:5px; font-weight:bold;'>ยกเลิก (Cancel)</a>";
             echo "</div></div>";
             return $response;
        });

        // 5. Image Delete Execution
        $adminGroup->get('/images/exec-delete/{name}', function ($request, $response, $args) {
            if (session_status() == PHP_SESSION_NONE) session_start();
             if (!isset($_SESSION['user']) || (!in_array(strtolower($_SESSION['user']->vipcode ?? ''), ['admin', 'administrator']))) {
                 die("Access Denied");
             }

            $name = $args['name'];
            if (strpos($name, '/') !== false || strpos($name, '\\\\') !== false || strpos($name, '..') !== false) {
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
    print("Added Image Delete Confirmation Routes.")
else:
    print("Could not find insertion point.")
