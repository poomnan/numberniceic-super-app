
filename = 'app/routes.php'
# Read all lines
with open(filename, 'r') as f:
    lines = f.readlines()

# Find the line with "// EMERGENCY ROUTE OUTSIDE GROUPS" (Line 368 approx)
cutoff_index = -1
for i, line in enumerate(lines):
    if "// EMERGENCY ROUTE OUTSIDE GROUPS" in line:
        cutoff_index = i
        break

if cutoff_index != -1:
    # Keep lines before the debug route
    new_lines = lines[:cutoff_index]
else:
    # If not found, append to end (assuming clean file)
    new_lines = lines

# Add the production route code
production_code = """
// Production Delete Route (Outside groups to prevent conflicts)
$app->get('/web/admin/exec-delete/{id}', function ($request, $response, $args) use ($container) {
    if (session_status() == PHP_SESSION_NONE) session_start();
    $id = $args['id'];
    
    // 1. Check Admin Permission
    if (!isset($_SESSION['user']) || (strtolower($_SESSION['user']->vipcode) !== 'admin' && strtolower($_SESSION['user']->vipcode) !== 'administrator')) {
         return $response->withHeader('Location', '/web/login')->withStatus(302);
    }

    // 2. Execute Delete
    try {
        $stmt = $container->get('db')->prepare("DELETE FROM articles WHERE art_id = :id");
        $stmt->execute([':id' => $id]);
    } catch (Exception $e) {
        // Log error if needed, but for now just redirect
    }
    
    // 3. Redirect back to list
    return $response->withHeader('Location', '/web/admin/articles')->withStatus(302);
});
"""

# Write back
with open(filename, 'w') as f:
    f.writelines(new_lines)
    f.write(production_code)

print("Updated routes.php")
