<?php
$file = __DIR__ . '/app/Managers/UserController.php';

// Try to restore from backup first
$backup = $file . '.backup';
if (file_exists($backup)) {
    copy($backup, $file);
    echo "✅ Restored from backup\n";
    echo "File should be working now.\n";
    echo "Test: https://ananya.in.th/test_curl_api.php\n";
} else {
    echo "❌ No backup file found at: $backup\n";
    echo "Please manually restore the file or contact hosting support.\n";
}
?>
