<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

function moveFile($filename) {
    if (!file_exists($filename)) {
        echo "Source file $filename not found<br>";
        return;
    }
    $dest = "uploads/temple/" . basename($filename);
    
    // Check destination directory
    if (!is_dir("uploads/temple")) {
        if (!mkdir("uploads/temple", 0755, true)) {
            echo "Failed to create directory uploads/temple<br>";
            return;
        }
    }
    
    // Copy
    if (copy($filename, $dest)) {
        echo "Successfully copied $filename to $dest<br>";
        unlink($filename); // Delete source
        echo "Deleted source file $filename<br>";
    } else {
        echo "Failed to copy $filename to $dest<br>";
        $error = error_get_last();
        echo "Error: " . $error['message'] . "<br>";
    }
}

moveFile("temple_c1b71d1acc439893.jpeg");
moveFile("temple_d2cbad413cdbf039.jpg");
?>
