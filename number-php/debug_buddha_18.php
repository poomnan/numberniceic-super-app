<?php
require 'configs/config.php';

$memberId = 18;
echo "Checking Buddha Assignment for Member ID: $memberId\n";

$sql = "SELECT b.pang_name, a.* FROM user_buddha_assign a 
        JOIN buddha_pang_tb b ON a.buddha_id = b.id 
        WHERE a.memberid = :mid";
$stmt = $db->prepare($sql);
$stmt->execute([':mid' => $memberId]);
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

if (count($rows) > 0) {
    print_r($rows);
} else {
    echo "No assignment found.\n";
}

// Check member birth day
$stmt = $db->prepare("SELECT * FROM membertb WHERE memberid = :mid");
$stmt->execute([':mid' => $memberId]);
$user = $stmt->fetch(PDO::FETCH_ASSOC);
print_r($user);
