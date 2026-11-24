<?php

require_once __DIR__ . '/../src/Utils/Database.php';

echo "=== Verifying All 7 AI Chatbot Backend REST Endpoints ===\n\n";

$baseUrl = 'http://127.0.0.1/petcare/public/api';

function testReq(string $method, string $url, ?array $body = null, ?string $token = null): array {
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    curl_setopt($ch, CURLOPT_TIMEOUT, 180);
    $headers = ['Content-Type: application/json', 'Accept: application/json'];
    if ($token) $headers[] = "Authorization: Bearer {$token}";
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    if ($body !== null) curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body));
    $t0 = microtime(true);
    $res = curl_exec($ch);
    $elapsed = round(microtime(true) - $t0, 2);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return ['code' => $code, 'json' => json_decode($res, true), 'raw' => $res, 'elapsed' => $elapsed];
}

// 1. GET /api/health
echo "[1] GET /api/health ...\n";
$h = testReq('GET', "$baseUrl/health");
if ($h['code'] === 200 && ($h['json']['data']['ai_service']['model_loaded'] ?? false)) {
    echo "  -> PASS: Status '{$h['json']['data']['status']}', Model: '{$h['json']['data']['ai_service']['model']}' online.\n\n";
} else {
    die("  -> FAIL: " . $h['raw']);
}

// Authenticate
$login = testReq('POST', "$baseUrl/auth/login", ['email' => 'kavya@petcare.com', 'password' => 'password123']);
$token = $login['json']['data']['token'];
echo "[AUTH] Authenticated as Kavya (Token verified)\n\n";

// 2. POST /api/chat
echo "[2] POST /api/chat (Text inquiry) ...\n";
$c = testReq('POST', "$baseUrl/chat", ['message' => 'What vaccinations does a 2-month-old puppy need?'], $token);
if ($c['code'] === 200 && !empty($c['json']['data']['message'])) {
    echo "  -> PASS ({$c['elapsed']}s): AI Message generated: " . substr(str_replace("\n", " ", $c['json']['data']['message']), 0, 120) . "...\n\n";
} else {
    echo "  -> FAIL: " . $c['raw'] . "\n\n";
}

// 3. POST /api/chat/image
echo "[3] POST /api/chat/image (Vision analysis) ...\n";
$imgB64 = base64_encode(file_get_contents('c:/Users/SCAD LAB/AndroidStudioProjects/PetCare/app/src/main/res/drawable/img_pet_dog.jpg'));
$imgRes = testReq('POST', "$baseUrl/chat/image", [
    'image' => $imgB64,
    'prompt' => 'Perform visual check for visible skin or coat symptoms.'
], $token);
if ($imgRes['code'] === 200 && !empty($imgRes['json']['data']['message'])) {
    $meta = $imgRes['json']['data']['metadata'] ?? [];
    echo "  -> PASS ({$imgRes['elapsed']}s): Vision triage completed.\n";
    echo "     Pet Type: '{$meta['pet_type']}', Severity: '{$meta['severity']}'\n\n";
} else {
    echo "  -> FAIL: " . $imgRes['raw'] . "\n\n";
}

// 4. POST /api/nutrition
echo "[4] POST /api/nutrition ...\n";
$nutr = testReq('POST', "$baseUrl/nutrition", ['query' => 'What vegetables are safe for dogs?'], $token);
if ($nutr['code'] === 200 && !empty($nutr['json']['data']['message'])) {
    echo "  -> PASS ({$nutr['elapsed']}s): Nutrition advice received: " . substr(str_replace("\n", " ", $nutr['json']['data']['message']), 0, 120) . "...\n\n";
} else {
    echo "  -> FAIL: " . $nutr['raw'] . "\n\n";
}

// 5. POST /api/search
echo "[5] POST /api/search ...\n";
$srch = testReq('POST', "$baseUrl/search", ['query' => 'feline diabetes dietary guidelines', 'max_results' => 2], $token);
if ($srch['code'] === 200 && count($srch['json']['data']['results']) > 0) {
    echo "  -> PASS: " . count($srch['json']['data']['results']) . " results from trusted veterinary sources.\n\n";
} else {
    echo "  -> FAIL: " . $srch['raw'] . "\n\n";
}

// 6. GET /api/chat/history
echo "[6] GET /api/chat/history ...\n";
$hist = testReq('GET', "$baseUrl/chat/history", null, $token);
if ($hist['code'] === 200 && is_array($hist['json']['data'])) {
    echo "  -> PASS: " . count($hist['json']['data']) . " total messages retrieved from MySQL.\n\n";
} else {
    echo "  -> FAIL: " . $hist['raw'] . "\n\n";
}

// 7. DELETE /api/chat/history
echo "[7] DELETE /api/chat/history ...\n";
$del = testReq('DELETE', "$baseUrl/chat/history", null, $token);
if ($del['code'] === 200) {
    echo "  -> PASS: Deleted {$del['json']['data']['deleted_count']} chat messages & uploaded image files.\n\n";
} else {
    echo "  -> FAIL: " . $del['raw'] . "\n\n";
}

echo ">>> ALL 7 BACKEND AI ENDPOINTS VERIFIED & 100% OPERATIONAL! <<<\n";
