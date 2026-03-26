<?php

declare(strict_types=1);

if ($argc < 2) {
    fwrite(STDERR, "Usage: php scripts/rengyam_restore_member.php <memberid> [rengyam_yearly|rengyam_vip] [codename]\n");
    exit(1);
}

require __DIR__ . '/../configs/config.php';

$memberId = trim((string) $argv[1]);
$viptype = trim((string) ($argv[2] ?? 'rengyam_yearly'));
$codename = trim((string) ($argv[3] ?? ('manual_restore_' . $memberId)));

if ($memberId === '') {
    fwrite(STDERR, "memberid is required\n");
    exit(1);
}

if (!in_array($viptype, ['rengyam_yearly', 'rengyam_vip'], true)) {
    fwrite(STDERR, "viptype must be rengyam_yearly or rengyam_vip\n");
    exit(1);
}

$db = $config['db'];
$pdo = new PDO(
    "mysql:host={$db['host']};dbname={$db['dbname']}",
    $db['user'],
    $db['pass']
);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);

$checkMember = $pdo->prepare("SELECT memberid, username, realname FROM membertb WHERE memberid = :mid LIMIT 1");
$checkMember->execute([':mid' => $memberId]);
$member = $checkMember->fetch();

if (!$member) {
    fwrite(STDERR, "member not found: {$memberId}\n");
    exit(1);
}

$checkUse = $pdo->prepare("
    SELECT memuseid, viptype, codename, dateadd
    FROM memberuse
    WHERE memberid = :mid
      AND viptype IN ('rengyam_yearly', 'rengyam_vip')
    ORDER BY memuseid DESC
    LIMIT 1
");
$checkUse->execute([':mid' => $memberId]);
$existing = $checkUse->fetch();

$today = date('Y-m-d');

if ($existing) {
    $update = $pdo->prepare("
        UPDATE memberuse
        SET viptype = :viptype,
            codename = :codename,
            dateadd = :dateadd
        WHERE memuseid = :memuseid
    ");
    $update->execute([
        ':viptype' => $viptype,
        ':codename' => $codename,
        ':dateadd' => $today,
        ':memuseid' => $existing['memuseid'],
    ]);
    $action = 'updated';
} else {
    $insert = $pdo->prepare("
        INSERT INTO memberuse (viptype, codename, memberid, dateadd)
        VALUES (:viptype, :codename, :memberid, :dateadd)
    ");
    $insert->execute([
        ':viptype' => $viptype,
        ':codename' => $codename,
        ':memberid' => $memberId,
        ':dateadd' => $today,
    ]);
    $action = 'inserted';
}

$result = $pdo->prepare("
    SELECT memuseid, viptype, codename, memberid, dateadd
    FROM memberuse
    WHERE memberid = :mid
      AND viptype IN ('rengyam_yearly', 'rengyam_vip')
    ORDER BY memuseid DESC
    LIMIT 1
");
$result->execute([':mid' => $memberId]);
$row = $result->fetch();

echo json_encode([
    'status' => 'success',
    'action' => $action,
    'member' => $member,
    'rengyam_access_row' => $row,
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT) . PHP_EOL;
