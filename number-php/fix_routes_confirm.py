
filename = 'app/routes.php'
with open(filename, 'r') as f:
    lines = f.readlines()

# Find start of last route section (The one we added)
# Look for "// Production Delete Route"
cutoff_index = -1
for i, line in enumerate(lines):
    if "// Production Delete Route" in line:
        cutoff_index = i
        break

if cutoff_index != -1:
    new_lines = lines[:cutoff_index]
else:
    new_lines = lines # Fallback

# New Code Block
new_block = """
// ----------------------------------------------------
// DELETE CONFIRMATION & EXECUTION ROUTES (Root Level)
// ----------------------------------------------------

// 1. Confirm Page
$app->get('/web/admin/confirm-delete/{id}', function ($request, $response, $args) use ($container) {
    if (session_status() == PHP_SESSION_NONE) session_start();
    $id = $args['id'];
    
    // Check Admin
    if (!isset($_SESSION['user']) || (!in_array(strtolower($_SESSION['user']->vipcode ?? ''), ['admin', 'administrator']))) {
         return $response->withHeader('Location', '/web/login')->withStatus(302);
    }

    // Get Article Info for better confirm message
    $stmt = $container->get('db')->prepare("SELECT title FROM articles WHERE art_id = :id");
    $stmt->execute([':id' => $id]);
    $art = $stmt->fetch(PDO::FETCH_OBJ);
    $title = $art ? $art->title : "Unknown Article";

    echo "<div style='font-family:sans-serif; text-align:center; margin-top:50px; padding:20px;'>";
    echo "<div style='border:1px solid #ccc; max-width:600px; margin:0 auto; padding:40px; border-radius:10px; box-shadow:0 0 10px rgba(0,0,0,0.1);'>";
    echo "<h1 style='color:#dc3545; margin-bottom:20px;'>ยืนยันการลบ (Confirm Deletion)</h1>";
    echo "<h3 style='margin-bottom:10px;'>บทความ ID: $id</h3>";
    echo "<h2 style='color:#333; margin-bottom:30px;'>Title: " . htmlspecialchars($title) . "</h2>";
    echo "<p style='color:#666; margin-bottom:40px;'>คุณแน่ใจหรือไม่ที่จะลบ? การกระทำนี้ไม่สามารถย้อนกลับได้</p>";
    
    echo "<a href='/web/admin/exec-delete/$id' style='background:#dc3545; color:white; padding:12px 30px; text-decoration:none; font-size:18px; border-radius:5px; margin-right:20px;'>ยืนยันลบ (Confirm Delete)</a>";
    echo "<a href='/web/admin/articles' style='background:#6c757d; color:white; padding:12px 30px; text-decoration:none; font-size:18px; border-radius:5px;'>ยกเลิก (Cancel)</a>";
    echo "</div></div>";
    
    return $response;
});

// 2. Execute Page
$app->get('/web/admin/exec-delete/{id}', function ($request, $response, $args) use ($container) {
    if (session_status() == PHP_SESSION_NONE) session_start();
    $id = $args['id'];
    
    // Check Admin
    if (!isset($_SESSION['user']) || (!in_array(strtolower($_SESSION['user']->vipcode ?? ''), ['admin', 'administrator']))) {
         die("Access Denied");
    }

    try {
        $stmt = $container->get('db')->prepare("DELETE FROM articles WHERE art_id = :id");
        $stmt->execute([':id' => $id]);
        $count = $stmt->rowCount();
        
        if ($count > 0) {
            // Success - Redirect immediately
            return $response->withHeader('Location', '/web/admin/articles')->withStatus(302);
        } else {
            echo "<h1>Error</h1>";
            echo "Could not delete ID $id. It may not exist.<br>";
            echo "<a href='/web/admin/articles'>Back</a>";
        }
    } catch (Exception $e) {
        echo "Database Error: " . $e->getMessage();
    }
    return $response;
});
"""

with open(filename, 'w') as f:
    f.writelines(new_lines)
    f.write(new_block)

print("Routes updated with confirmation page.")
