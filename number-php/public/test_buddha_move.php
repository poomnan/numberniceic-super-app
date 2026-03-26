<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

$src = "uploads/temple/test_img.txt"; // I renamed it earlier
if (!file_exists($src))
    $src = "uploads/temple/temple_c1b71d1acc439893.jpeg";

$dest = "uploads/buddha/test_temple.txt";

if (copy($src, $dest)) {
    echo "Copied to $dest";
    chmod($dest, 0666);
} else {
    echo "Failed copy";
    print_r(error_get_last());
}
?>