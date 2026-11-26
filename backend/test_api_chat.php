<?php
require_once __DIR__ . '/src/Utils/Database.php';

$pdo = App\Utils\Database::getConnection();
$pdo->exec('DELETE FROM chat_messages WHERE user_id = 1');

$rawToken = bin2hex(random_bytes(32));
$hash = hash('sha256', $rawToken);
$pdo->prepare("INSERT INTO user_tokens (user_id, token_hash, expires_at) VALUES (1, ?, DATE_ADD(NOW(), INTERVAL 30 DAY))")->execute([$hash]);

$ch = curl_init('http://localhost/petcare/public/api/chat');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Authorization: Bearer ' . $rawToken
]);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode([
    'message' => 'Why is Clover refusing to eat hay and acting dull?',
    'pet_id' => 1,
    'enable_search' => false
]));
$t0 = microtime(true);
$res = curl_exec($ch);
$elapsed = microtime(true) - $t0;
echo "Elapsed: " . round($elapsed, 2) . "s\n";
echo "Response:\n" . $res . "\n";
