<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

$src = "/tmp/temple_c1b71d1acc439893.jpeg";
$dest = "uploads/buddha/temple/temple_c1b71d1acc439893.jpeg";

if (copy($src, $dest)) {
    echo "Copied $src to $dest";
    chmod($dest, 0666);
} else {
    echo "Failed copy";
    print_r(error_get_last());
}

$src2 = "/home/tayap/ananya-php/public/uploads/temple/temple_d2cbad413cdbf039.jpg";
$dest2 = "uploads/buddha/temple/temple_d2cbad413cdbf039.jpg";

if (copy($src2, $dest2)) {
    echo "Copied $src2 to $dest2";
    chmod($dest2, 0666);
} else {
    echo "Failed to copy $src2";
    print_r(error_get_last());
}

?>