<?php
$files = glob(__DIR__ . '/views/tambon/*.phtml');
foreach ($files as $file) {
    $basename = basename($file, '.phtml');
    if ($basename === 'template' || $basename === 'dynamic') continue;
    
    $html = file_get_contents($file);
    
    // Find title
    $titleStart = strpos($html, '<h3');
    if ($titleStart !== false) {
        $titleStart = strpos($html, '>', $titleStart) + 1;
        $titleEnd = strpos($html, '</h3>', $titleStart);
        $title = trim(substr($html, $titleStart, $titleEnd - $titleStart));
    } else {
        $title = $basename;
    }

    // Find content
    $contentStart = strpos($html, '<div class="ui attached segment">');
    if ($contentStart !== false) {
        $contentStart += strlen('<div class="ui attached segment">');
        // Find last </div>
        $divEnd = strrpos($html, '</div>');
        $content = trim(substr($html, $contentStart, $divEnd - $contentStart));
    } else {
        $content = '';
    }
    
    echo "File: $basename | Title: $title | Content Length: " . strlen($content) . "\n";
}
