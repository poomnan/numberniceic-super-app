<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

$targetDir = __DIR__ . '/public/uploads/buddha/';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_FILES['test_image'])) {
    echo "<h3>Upload Results:</h3>";
    $file = $_FILES['test_image'];

    if ($file['error'] === UPLOAD_ERR_OK) {
        $ext = pathinfo($file['name'], PATHINFO_EXTENSION);
        $newName = 'test_' . time() . '.' . $ext;
        $targetFile = $targetDir . $newName;

        if (move_uploaded_file($file['tmp_name'], $targetFile)) {
            @chmod($targetFile, 0644);
            echo "<p style='color: green;'>✅ Success! File uploaded to: $newName</p>";
            echo "<img src='/uploads/buddha/$newName' style='max-width: 300px; border: 5px solid green;'>";
        } else {
            echo "<p style='color: red;'>❌ Failed to move file. Destination: $targetFile</p>";
            echo "Current Dir: " . __DIR__ . "<br>";
            echo "Is Target Dir Writable? " . (is_writable($targetDir) ? "Yes" : "No") . "<br>";
        }
    } else {
        echo "<p style='color: red;'>❌ PHP Upload Error Code: " . $file['error'] . "</p>";
        $error_msgs = [
            1 => 'File exceeds upload_max_filesize',
            2 => 'File exceeds MAX_FILE_SIZE in form',
            3 => 'File only partially uploaded',
            4 => 'No file uploaded',
            6 => 'Missing temporary folder',
            7 => 'Failed to write to disk',
            8 => 'A PHP extension stopped the upload'
        ];
        echo "Reason: " . ($error_msgs[$file['error']] ?? 'Unknown');
    }
}

?>
<hr>
<h2>Test Direct Upload</h2>
<form method="POST" enctype="multipart/form-data">
    <input type="file" name="test_image" required>
    <button type="submit">Test Upload Now</button>
</form>
<p>This script bypasses the main app logic to see if the server environment allows writing files.</p>