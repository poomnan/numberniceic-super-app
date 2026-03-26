<?php
$ch = curl_init('https://ananya.in.th/member/lengyam');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 30);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$error = curl_error($ch);
curl_close($ch);

echo "HTTP Code: $httpCode\n";
echo "Response Size: " . strlen($response) . " bytes\n";
echo "Error: $error\n";

if ($response) {
    $data = json_decode($response, true);
    echo "wan_pras count: " . count($data['wan_pras'] ?? []) . "\n";
    echo "First item: " . json_encode($data['wan_pras'][0] ?? null) . "\n";
    echo "Last item: " . json_encode(end($data['wan_pras']) ?? null) . "\n";
}
?>
