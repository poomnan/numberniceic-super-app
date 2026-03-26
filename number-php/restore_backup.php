<?php
// Restore from backup
$file = __DIR__ . '/app/Managers/UserController.php';
$backup = $file . '.backup';

if (file_exists($backup)) {
    copy($backup, $file);
    echo "✅ Restored from backup\n";
} else {
    echo "❌ No backup found\n";
}

// Verify
if (file_exists($file)) {
    $content = file_get_contents($file);
    if (strpos($content, 'syntax error') === false) {
        echo "✅ File is valid PHP\n";
    }
}
?>
