<?php
$file = __DIR__ . '/app/Managers/UserController.php';
$content = file_get_contents($file);

// Replace the filtering logic
$search = <<<'EOD'
        // Map types to match expected helper format if necessary
        // OPTIMIZATION: Convert to compact format to reduce response size
        foreach ( as  => ) {
            [] = [
                'wanpra_date' => ['wanpra_date'],
                'is_wanpra' => (['is_wanpra'] == 1 || ['is_wanpra'] == '1') ? "1" : "0",
                'is_tongchai' => (['is_tongchai'] == 1 || ['is_tongchai'] == '1') ? "1" : "0",
                'is_atipbadee' => (['is_atipbadee'] == 1 || ['is_atipbadee'] == '1') ? "1" : "0"
            ];
        }
EOD;

$replace = <<<'EOD'
        // Map types to match expected helper format if necessary
        // OPTIMIZATION: Convert to compact format to reduce response size
        $filteredWanpras = [];
        foreach ($arrWanpras as $key => $wp) {
            $isWanpra = ($wp['is_wanpra'] == 1 || $wp['is_wanpra'] == '1');
            $isTongchai = ($wp['is_tongchai'] == 1 || $wp['is_tongchai'] == '1');
            $isAtipbadee = ($wp['is_atipbadee'] == 1 || $wp['is_atipbadee'] == '1');
            
            // CRITICAL FIX: Only send items with at least one flag = true
            // This reduces 332 items to ~113 items to bypass web server buffer limit
            if ($isWanpra || $isTongchai || $isAtipbadee) {
                $filteredWanpras[] = [
                    'wanpra_date' => $wp['wanpra_date'],
                    'is_wanpra' => $isWanpra ? "1" : "0",
                    'is_tongchai' => $isTongchai ? "1" : "0",
                    'is_atipbadee' => $isAtipbadee ? "1" : "0"
                ];
            }
        }
        
        $arrWanpras = $filteredWanpras;
EOD;

$newContent = str_replace($search, $replace, $content);

if ($newContent !== $content) {
    file_put_contents($file, $newContent);
    touch($file);
    echo "✅ Applied server-side filtering\n";
    echo "Now API will send only items with flags = true (~113 items)\n";
} else {
    echo "⚠️ Pattern not found, applying manual update...\n";
    // Manual update if pattern doesn't match
    $lines = file($file);
    $output = [];
    $inBlock = false;
    $blockStart = 0;
    
    foreach ($lines as $i => $line) {
        if (strpos($line, '// Map types to match expected helper format') !== false) {
            $inBlock = true;
            $blockStart = $i;
        }
        
        if ($inBlock && strpos($line, '}') !== false && $i > $blockStart + 5) {
            // End of block, insert new code
            $output[] = $replace . "\n";
            $inBlock = false;
            continue;
        }
        
        if (!$inBlock || $i == $blockStart) {
            $output[] = $line;
        }
    }
    
    file_put_contents($file, implode('', $output));
    touch($file);
    echo "✅ Manual update applied\n";
}
?>
