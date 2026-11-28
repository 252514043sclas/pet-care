<?php

spl_autoload_register(function ($class) {
    $prefix = 'App\\';
    $baseDir = __DIR__ . '/../src/';
    $len = strlen($prefix);
    if (strncmp($prefix, $class, $len) !== 0) return;
    $relativeClass = substr($class, $len);
    $file = $baseDir . str_replace('\\', '/', $relativeClass) . '.php';
    if (file_exists($file)) require_once $file;
});

use App\Utils\Database;
use App\Middleware\AuthMiddleware;

$pdo = Database::getConnection();
$user = $pdo->query("SELECT id, email, name FROM users WHERE email = 'test_chat_a@example.com'")->fetch(PDO::FETCH_ASSOC);
$rawToken = bin2hex(random_bytes(32));
$tokenHash = hash('sha256', $rawToken);
$expiresAt = date('Y-m-d H:i:s', strtotime('+30 days'));

$tokenStmt = $pdo->prepare(
    'INSERT INTO user_tokens (user_id, token_hash, device_name, expires_at) VALUES (:user_id, :token_hash, :device_name, :expires_at)'
);
$tokenStmt->execute([
    ':user_id' => (int)$user['id'],
    ':token_hash' => $tokenHash,
    ':device_name' => 'Test Runner',
    ':expires_at' => $expiresAt,
]);
$token = $rawToken;

echo "Testing HTTP API with Auth Token for User {$user['id']}...\n";

function apiCall($method, $path, $body = null, $token = null) {
    $url = "http://localhost/petcare/public/api" . $path;
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    $headers = ['Content-Type: application/json', 'Accept: application/json'];
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
    return ['code' => $code, 'data' => json_decode($res, true), 'raw' => $res];
}

// 1. Create a conversation
$createRes = apiCall('POST', '/chat/conversations', ['title' => 'Puppy Diet Guide', 'language' => 'en'], $token);
echo "Create Conversation Status: {$createRes['code']}\n";
assert($createRes['code'] === 201);
$convId = $createRes['data']['data']['id'];
echo "Created conversation ID: $convId\n";

// 2. List conversations
$listRes = apiCall('GET', '/chat/conversations', null, $token);
echo "List Conversations Status: {$listRes['code']}\n";
assert($listRes['code'] === 200);
$convs = $listRes['data']['data'];
echo "Conversations count: " . count($convs) . "\n";
assert(count($convs) >= 1);

// 3. Test non-pet query refusal
$refuseRes = apiCall('POST', '/chat', ['message' => 'What is the capital of Australia?', 'conversation_id' => $convId], $token);
echo "Refusal response status: {$refuseRes['code']}\n";
assert($refuseRes['code'] === 200);
echo "Refusal response message: '{$refuseRes['data']['data']['message']}'\n";
assert($refuseRes['data']['data']['message'] === "I'm your Pet Care AI assistant. I can help you with questions related to pets and their care.");
echo "[PASS] Refusal via HTTP API verified\n";

// 4. Get conversation messages
$getRes = apiCall('GET', "/chat/conversations/{$convId}", null, $token);
echo "Get Conversation Status: {$getRes['code']}\n";
assert($getRes['code'] === 200);
$messages = $getRes['data']['data']['messages'];
echo "Messages count in conversation: " . count($messages) . "\n";
assert(count($messages) === 2); // user msg + refusal msg
echo "[PASS] Messages preserved in session\n";

// 5. Delete conversation
$delRes = apiCall('DELETE', "/chat/conversations/{$convId}", null, $token);
echo "Delete Conversation Status: {$delRes['code']}\n";
assert($delRes['code'] === 200);

// Verify conversation is gone
$getAfterRes = apiCall('GET', "/chat/conversations/{$convId}", null, $token);
assert($getAfterRes['code'] === 404);
echo "[PASS] Conversation deleted via HTTP API\n";

echo "ALL HTTP API TESTS PASSED!\n";
