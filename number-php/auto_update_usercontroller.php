<?php
$file = __DIR__ . '/app/Managers/UserController.php';
$content = file_get_contents($file);

// Backup original
file_put_contents($file . '.backup', $content);

// Replace +1 year with +6 months
$newContent = str_replace(
    "strtotime('+1 year')",
    "strtotime('+6 months')",
    $content
);

// Write back
if (file_put_contents($file, $newContent)) {
    echo "✅ Successfully updated UserController.php\n";
    echo "Changed: +1 year → +6 months\n";
    echo "Backup saved to: UserController.php.backup\n";
    
    // Verify
    $verify = file_get_contents($file);
    if (strpos($verify, '+6 months') !== false) {
        echo "✅ Verification: Change confirmed\n";
    } else {
        echo "❌ Verification: Failed to apply change\n";
    }
} else {
    echo "❌ Failed to write file\n";
}
?>
