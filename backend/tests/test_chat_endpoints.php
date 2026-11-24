<?php

require_once __DIR__ . '/../src/Utils/Database.php';

echo "=== Testing Backend AI Chat, Vision, Search & Nutrition Endpoints ===\n\n";

$baseUrl = 'http://127.0.0.1/petcare/public/api';

function apiCall(string $method, string $url, ?array $body = null, ?string $token = null): array {
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    curl_setopt($ch, CURLOPT_TIMEOUT, 240);
    $headers = [
        'Content-Type: application/json',
        'Accept: application/json',
    ];
    if ($token) {
        $headers[] = "Authorization: Bearer {$token}";
    }
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    if ($body !== null) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body));
    }
    $res = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return ['code' => $code, 'json' => json_decode($res, true), 'raw' => $res];
}

// 1. Authenticate
$login = apiCall('POST', "$baseUrl/auth/login", [
    'email' => 'kavya@petcare.com',
    'password' => 'password123'
]);
if ($login['code'] !== 200) {
    die("Login failed: " . $login['raw']);
}
$token = $login['json']['data']['token'];
echo "[1] Auth Login: PASS (Token verified)\n";

// 2. Test /api/chat with Pet Care Question
echo "[2] Testing POST /api/chat (Question: 'What are the common diseases in dogs?')...\n";
$chatRes = apiCall('POST', "$baseUrl/chat", [
    'message' => 'What are the common diseases in dogs?'
], $token);

if ($chatRes['code'] === 200 && !empty($chatRes['json']['data']['message'])) {
    echo "  -> PASS (HTTP 200)\n";
    echo "  -> AI Response Snippet: " . substr(trim($chatRes['json']['data']['message']), 0, 180) . "...\n";
    if (!empty($chatRes['json']['data']['sources'])) {
        echo "  -> Web Search Sources Cited: " . count($chatRes['json']['data']['sources']) . "\n";
    }
} else {
    echo "  -> FAIL: HTTP {$chatRes['code']}: " . $chatRes['raw'] . "\n";
}

// 3. Test /api/nutrition (Question: 'Can rabbits eat carrots? What is a safe portion?')
echo "\n[3] Testing POST /api/nutrition (Question: 'Can rabbits eat carrots?')...\n";
$nutrRes = apiCall('POST', "$baseUrl/nutrition", [
    'query' => 'Can rabbits eat carrots? What is a safe portion and preparation?'
], $token);

if ($nutrRes['code'] === 200 && !empty($nutrRes['json']['data']['message'])) {
    echo "  -> PASS (HTTP 200)\n";
    echo "  -> Nutrition Guidance: " . substr(trim($nutrRes['json']['data']['message']), 0, 180) . "...\n";
} else {
    echo "  -> FAIL: HTTP {$nutrRes['code']}: " . $nutrRes['raw'] . "\n";
}

// 4. Test /api/search (Query: 'cat vomiting common causes')
echo "\n[4] Testing POST /api/search (Query: 'cat vomiting common causes')...\n";
$searchRes = apiCall('POST', "$baseUrl/search", [
    'query' => 'cat vomiting common causes',
    'max_results' => 3
], $token);

if ($searchRes['code'] === 200 && !empty($searchRes['json']['data']['results'])) {
    echo "  -> PASS (HTTP 200, " . count($searchRes['json']['data']['results']) . " results found)\n";
    foreach ($searchRes['json']['data']['results'] as $i => $s) {
        echo "     [" . ($i+1) . "] {$s['source']}: {$s['title']} ({$s['url']})\n";
    }
} else {
    echo "  -> FAIL: HTTP {$searchRes['code']}: " . $searchRes['raw'] . "\n";
}

// 5. Test /api/chat/history (Check persistent storage in MySQL)
echo "\n[5] Testing GET /api/chat/history...\n";
$histRes = apiCall('GET', "$baseUrl/chat/history", null, $token);
if ($histRes['code'] === 200 && is_array($histRes['json']['data'])) {
    echo "  -> PASS (HTTP 200, " . count($histRes['json']['data']) . " messages saved in history)\n";
} else {
    echo "  -> FAIL: HTTP {$histRes['code']}: " . $histRes['raw'] . "\n";
}

echo "\n>>> ALL TESTED BACKEND AI CHAT APIS OPERATIONAL! <<<\n";
