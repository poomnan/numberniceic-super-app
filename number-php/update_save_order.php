<?php
// Script to update saveGuestOrder on server
// Run: scp this file to server, then php /tmp/update_save_order.php

$file = '/var/www/html/app/Managers/GuestController.php';
$content = file_get_contents($file);

// Find and replace the SQL insert part only
$old = 'INSERT INTO guest_orders (guest_id, order_id, order_data, created_at) 
                    VALUES (:guest_id, :order_id, :order_data, NOW())';
$new = 'INSERT INTO guest_orders (guest_id, member_id, order_id, order_data, shipping_address, created_at) 
                    VALUES (:guest_id, :member_id, :order_id, :order_data, :shipping_address, NOW())';

$content = str_replace($old, $new, $content);

// Find and replace the execute params
$old2 = "':guest_id' => \$guestId,
                ':order_id' => \$orderId,
                ':order_data' => \$orderData";
$new2 = "':guest_id' => \$guestId,
                ':member_id' => \$memberId,
                ':order_id' => \$orderId,
                ':order_data' => \$orderData,
                ':shipping_address' => \$shippingAddress";

$content = str_replace($old2, $new2, $content);

// Add member_id and shipping_address parsing after orderData line
$old3 = "\$orderData = \$body['order_data'] ?? '';";
$new3 = "\$orderData = \$body['order_data'] ?? '';
        \$memberId = \$body['member_id'] ?? null;
        \$shippingAddress = \$body['shipping_address'] ?? null;";

$content = str_replace($old3, $new3, $content);

// Update validation to accept either guest_id or member_id
$old4 = "if (empty(\$guestId) || empty(\$orderId))";
$new4 = "if ((empty(\$guestId) && empty(\$memberId)) || empty(\$orderId))";

$content = str_replace($old4, $new4, $content);

file_put_contents($file, $content);
echo "Updated successfully\n";
