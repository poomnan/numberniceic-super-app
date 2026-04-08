<?php

declare(strict_types=1);

use App\Managers\ThaiCalendarHelper;

require dirname(__DIR__) . '/vendor/autoload.php';

$sharedPath = dirname(__DIR__, 2) . '/shared/rengyam/myhora_verified_tag_overrides.json';

if (!is_file($sharedPath)) {
    fwrite(STDERR, "Shared override file not found: {$sharedPath}\n");
    exit(1);
}

$decoded = json_decode((string) file_get_contents($sharedPath), true);
if (!is_array($decoded)) {
    fwrite(STDERR, "Unable to parse shared override file: {$sharedPath}\n");
    exit(1);
}

$args = array_values(array_slice($argv, 1));
$dates = [];

if (count($args) === 0) {
    $dates = array_keys($decoded);
    sort($dates);
} elseif (count($args) === 2) {
    [$start, $end] = $args;
    $startDate = DateTimeImmutable::createFromFormat('Y-m-d', $start);
    $endDate = DateTimeImmutable::createFromFormat('Y-m-d', $end);
    if (!$startDate || !$endDate) {
        fwrite(STDERR, "Usage: php scripts/audit_myhora_overrides.php [YYYY-MM-DD YYYY-MM-DD]\n");
        exit(1);
    }
    if ($startDate > $endDate) {
        [$startDate, $endDate] = [$endDate, $startDate];
    }
    for ($cursor = $startDate; $cursor <= $endDate; $cursor = $cursor->modify('+1 day')) {
        $dates[] = $cursor->format('Y-m-d');
    }
} else {
    fwrite(STDERR, "Usage: php scripts/audit_myhora_overrides.php [YYYY-MM-DD YYYY-MM-DD]\n");
    exit(1);
}

$helper = new ReflectionClass(ThaiCalendarHelper::class);
$buildGeneric = $helper->getMethod('buildGenericMyHoraDisplayTags');
$buildGeneric->setAccessible(true);
$normalize = $helper->getMethod('normalizeMyHoraDisplayTags');
$normalize->setAccessible(true);

$rows = [];
foreach ($dates as $date) {
    $status = ThaiCalendarHelper::queryMahaChok($date);
    $status['is_wanpra'] = ThaiCalendarHelper::isWanPra($date);

    $genericRaw = $buildGeneric->invoke(null, $date, $status);
    $generic = $normalize->invoke(null, $genericRaw);
    $override = $decoded[$date] ?? null;
    $final = ThaiCalendarHelper::getAuspiciousStatus($date)['myhora_display_tags'] ?? [];

    $classification = 'no-override';
    if (is_array($override)) {
        $classification = ($override === $generic) ? 'override-redundant' : 'override-applied';
    }

    $rows[] = [
        'date' => $date,
        'classification' => $classification,
        'generic' => $generic,
        'override' => $override ?? [],
        'final' => $final,
    ];
}

foreach ($rows as $row) {
    echo $row['date'] . ' [' . $row['classification'] . ']' . PHP_EOL;
    echo '  generic : ' . implode(' | ', $row['generic']) . PHP_EOL;
    if ($row['classification'] !== 'no-override') {
        echo '  override: ' . implode(' | ', $row['override']) . PHP_EOL;
    }
    echo '  final   : ' . implode(' | ', $row['final']) . PHP_EOL;
    echo PHP_EOL;
}
