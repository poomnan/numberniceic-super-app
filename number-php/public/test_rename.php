<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

$old = "uploads/temple/temple_c1b71d1acc439893.jpeg";
$new = "uploads/temple/test_img.txt";

if (rename($old, $new)) {
    echo "Renamed $old to $new";
} else {
    echo "Failed to rename";
    print_r(error_get_last());
}
?>