<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

$srcDir = "uploads/temple";
$destDir = "uploads/buddha/temple";

if (!is_dir($destDir))
    mkdir($destDir, 0777, true);

$files = scandir($srcDir);
foreach ($files as $file) {
    if ($file != "." && $file != "..") {
        $src = "$srcDir/$file";
        $dest = "$destDir/$file";
        if (copy($src, $dest)) {
            echo "Copied $file to $destDir<br>";
        } else {
            echo "Failed to copy $file<br>";
        }
    }
}
?>