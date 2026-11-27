<?php
require_once __DIR__ . '/src/Utils/Database.php';

$pdo = App\Utils\Database::getConnection();
$pdo->exec('DELETE FROM chat_messages WHERE user_id = 1');

$rawToken = bin2hex(random_bytes(32));
$hash = hash('sha256', $rawToken);
$pdo->prepare("INSERT INTO user_tokens (user_id, token_hash, expires_at) VALUES (1, ?, DATE_ADD(NOW(), INTERVAL 30 DAY))")->execute([$hash]);

function callChatApi(string $token, string $message, int $petId): array {
    $ch = curl_init('http://localhost/petcare/public/api/chat');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 180);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Authorization: Bearer ' . $token
    ]);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode([
        'message' => $message,
        'pet_id' => $petId,
        'enable_search' => false
    ]));
    $t0 = microtime(true);
    $res = curl_exec($ch);
    $elapsed = round(microtime(true) - $t0, 2);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return [
        'http_code' => $httpCode,
        'elapsed' => $elapsed,
        'raw' => $res,
        'data' => json_decode($res, true)
    ];
}

echo "=== TEST 1: Non-Pet Question (Domain Guard) ===\n";
$res1 = callChatApi($rawToken, "What is the capital of India?", 1);
echo "HTTP: {$res1['http_code']} | Elapsed: {$res1['elapsed']}s\n";
echo "Response Message:\n" . ($res1['data']['data']['message'] ?? $res1['raw']) . "\n\n";

echo "=== TEST 2: Pet Health Question (Dynamic Qwen3-VL Inference) ===\n";
$res2 = callChatApi($rawToken, "Why is Clover refusing to eat hay and acting dull?", 1);
echo "HTTP: {$res2['http_code']} | Elapsed: {$res2['elapsed']}s\n";
echo "Response Message:\n" . ($res2['data']['data']['message'] ?? $res2['raw']) . "\n\n";

echo "=== TEST 3: Pet Nutrition Question (Dynamic Qwen3-VL Inference) ===\n";
$res3 = callChatApi($rawToken, "What vegetables are safe for Clover to eat daily and what is toxic?", 1);
echo "HTTP: {$res3['http_code']} | Elapsed: {$res3['elapsed']}s\n";
echo "Response Message:\n" . ($res3['data']['data']['message'] ?? $res3['raw']) . "\n";
