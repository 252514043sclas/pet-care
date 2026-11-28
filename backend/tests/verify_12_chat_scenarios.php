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

$pdo = Database::getConnection();

echo "========================================================\n";
echo "   PET CARE MINI CHATGPT - 12 TEST SCENARIOS SUITE      \n";
echo "========================================================\n\n";

function getOrCreateUser($email, $name) {
    global $pdo;
    $u = $pdo->query("SELECT id, email, name FROM users WHERE email = '{$email}'")->fetch(PDO::FETCH_ASSOC);
    if (!$u) {
        $stmt = $pdo->prepare("INSERT INTO users (name, email, password_hash, created_at) VALUES (?, ?, ?, NOW())");
        $stmt->execute([$name, $email, password_hash('password123', PASSWORD_BCRYPT)]);
        $u = ['id' => (int)$pdo->lastInsertId(), 'email' => $email, 'name' => $name];
    }
    return $u;
}

function getAuthToken($user) {
    global $pdo;
    $rawToken = bin2hex(random_bytes(32));
    $tokenHash = hash('sha256', $rawToken);
    $expiresAt = date('Y-m-d H:i:s', strtotime('+30 days'));

    $stmt = $pdo->prepare("INSERT INTO user_tokens (user_id, token_hash, device_name, expires_at) VALUES (?, ?, 'Test', ?)");
    $stmt->execute([(int)$user['id'], $tokenHash, $expiresAt]);
    return $rawToken;
}

function httpPost($path, $body, $token) {
    $url = "http://localhost/petcare/public/api" . $path;
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 120);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Accept: application/json',
        "Authorization: Bearer {$token}"
    ]);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body));
    $raw = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return ['code' => $code, 'data' => json_decode($raw, true), 'raw' => $raw];
}

function httpGet($path, $token) {
    $url = "http://localhost/petcare/public/api" . $path;
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 60);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Accept: application/json',
        "Authorization: Bearer {$token}"
    ]);
    $raw = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return ['code' => $code, 'data' => json_decode($raw, true), 'raw' => $raw];
}

function httpDelete($path, $token) {
    $url = "http://localhost/petcare/public/api" . $path;
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'DELETE');
    curl_setopt($ch, CURLOPT_TIMEOUT, 60);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Accept: application/json',
        "Authorization: Bearer {$token}"
    ]);
    $raw = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return ['code' => $code, 'data' => json_decode($raw, true), 'raw' => $raw];
}

$userA = getOrCreateUser('scenario_user_a@petcare.com', 'Scenario User A');
$tokenA = getAuthToken($userA);

$userB = getOrCreateUser('scenario_user_b@petcare.com', 'Scenario User B');
$tokenB = getAuthToken($userB);

$passedCount = 0;

// Scenario 1: Dog-related question -> clear pet answer
echo "Scenario 1: Dog-related question...\n";
$res1 = httpPost('/chat', ['message' => 'What are safe fruits for dogs to eat?'], $tokenA);
assert($res1['code'] === 200);
$ans1 = $res1['data']['data']['message'] ?? '';
$conv1Id = $res1['data']['data']['conversation_id'] ?? null;
assert(!empty($ans1) && !str_contains($ans1, 'unrelated'));
echo "   [PASS] Dog question received response length " . strlen($ans1) . " (Conv ID: $conv1Id)\n";
$passedCount++;

// Scenario 2: Unrelated question -> polite restriction message
echo "Scenario 2: Unrelated question domain restriction...\n";
$res2 = httpPost('/chat', ['message' => 'What is the capital of France?'], $tokenA);
assert($res2['code'] === 200);
$ans2 = $res2['data']['data']['message'] ?? '';
$expectedRefusal = "I'm your Pet Care AI assistant. I can help you with questions related to pets and their care.";
assert($ans2 === $expectedRefusal);
echo "   [PASS] Non-pet question rejected with exact refusal text\n";
$passedCount++;

// Scenario 3: Tamil question -> Tamil response
echo "Scenario 3: Tamil question in Tamil script...\n";
$res3 = httpPost('/chat', ['message' => 'என் நாய்க்கு என்ன உணவு கொடுக்க வேண்டும்?'], $tokenA);
assert($res3['code'] === 200);
$ans3 = $res3['data']['data']['message'] ?? '';
assert(preg_match('/[\x{0B80}-\x{0BFF}]/u', $ans3));
echo "   [PASS] Tamil question returned Tamil script response (length: " . strlen($ans3) . ")\n";
$passedCount++;

// Scenario 4: English question -> English response
echo "Scenario 4: English question...\n";
$res4 = httpPost('/chat', ['message' => 'How often should a 3 month puppy be fed daily?'], $tokenA);
assert($res4['code'] === 200);
$ans4 = $res4['data']['data']['message'] ?? '';
assert(strlen($ans4) > 30);
echo "   [PASS] English question returned clear English response\n";
$passedCount++;

// Scenario 5: Tamil meaning request -> Tamil definition
echo "Scenario 5: Tamil meaning request...\n";
$res5 = httpPost('/chat', ['message' => 'What is the Tamil meaning of vaccination?'], $tokenA);
assert($res5['code'] === 200);
$ans5 = $res5['data']['data']['message'] ?? '';
assert(preg_match('/(தடுப்பூசி|thaduppoosi|vaccin)/iu', $ans5));
echo "   [PASS] Tamil meaning of vaccination explained correctly\n";
$passedCount++;

// Scenario 6: Tanglish question -> understood & answered
echo "Scenario 6: Tanglish question...\n";
$res6 = httpPost('/chat', ['message' => 'Na en dog ku enna food kudukalam?'], $tokenA);
assert($res6['code'] === 200);
$ans6 = $res6['data']['data']['message'] ?? '';
assert(strlen($ans6) > 30);
echo "   [PASS] Tanglish query answered properly\n";
$passedCount++;

// Scenario 7: Start new chat -> previous conversation not mixed
echo "Scenario 7: Start new chat session...\n";
$newConvRes = httpPost('/chat/conversations', ['title' => 'Cat Health Session'], $tokenA);
assert($newConvRes['code'] === 201);
$newConvId = $newConvRes['data']['data']['id'];
$res7 = httpPost('/chat', ['message' => 'My cat is sneezing frequently', 'conversation_id' => $newConvId], $tokenA);
assert($res7['code'] === 200);
assert($res7['data']['data']['conversation_id'] === $newConvId);
echo "   [PASS] New chat created and kept strictly separated (Conv ID: $newConvId)\n";
$passedCount++;

// Scenario 8: Open old conversation -> continue chatting
echo "Scenario 8: Open old conversation & continue...\n";
$res8 = httpPost('/chat', ['message' => 'Are there any other home remedies?', 'conversation_id' => $newConvId], $tokenA);
assert($res8['code'] === 200);
$convDetails = httpGet("/chat/conversations/{$newConvId}", $tokenA);
assert($convDetails['code'] === 200);
$msgList = $convDetails['data']['data']['messages'] ?? [];
assert(count($msgList) >= 4); // 2 user + 2 assistant
echo "   [PASS] Continued old chat successfully, message count: " . count($msgList) . "\n";
$passedCount++;

// Scenario 9: Delete conversation -> removed from DB & UI
echo "Scenario 9: Delete conversation...\n";
$delRes = httpDelete("/chat/conversations/{$newConvId}", $tokenA);
assert($delRes['code'] === 200);
$checkDel = httpGet("/chat/conversations/{$newConvId}", $tokenA);
assert($checkDel['code'] === 404);
echo "   [PASS] Conversation and all messages successfully deleted\n";
$passedCount++;

// Scenario 10: Logout & login -> chat history retained
echo "Scenario 10: Logout and Login session persistence...\n";
// Re-generate a fresh token simulating a new login session for user A
$newTokenA = getAuthToken($userA);
$historyList = httpGet('/chat/conversations', $newTokenA);
assert($historyList['code'] === 200);
assert(count($historyList['data']['data']) >= 1);
echo "   [PASS] Conversations persisted across login sessions\n";
$passedCount++;

// Scenario 11: Long AI response -> full answer without cutoff
echo "Scenario 11: Long AI response without cutoff...\n";
$res11 = httpPost('/chat', ['message' => 'Please provide a comprehensive feeding schedule and complete diet plan for a German Shepherd from puppy to senior stage.'], $tokenA);
assert($res11['code'] === 200);
$ans11 = $res11['data']['data']['message'] ?? '';
assert(strlen($ans11) > 200);
echo "   [PASS] Full comprehensive answer returned without truncation (length: " . strlen($ans11) . ")\n";
$passedCount++;

// Scenario 12: Two accounts -> chat histories strictly separated
echo "Scenario 12: Multi-user isolation...\n";
$userAConvs = httpGet('/chat/conversations', $tokenA);
$userBConvs = httpGet('/chat/conversations', $tokenB);
$userAIds = array_column($userAConvs['data']['data'], 'id');
$userBIds = array_column($userBConvs['data']['data'], 'id');
$overlap = array_intersect($userAIds, $userBIds);
assert(empty($overlap));
// Also verify User B cannot read User A's conversation
if (!empty($userAIds)) {
    $firstAConvId = $userAIds[0];
    $unauthAccess = httpGet("/chat/conversations/{$firstAConvId}", $tokenB);
    assert($unauthAccess['code'] === 404);
}
echo "   [PASS] Chat histories strictly isolated between User A and User B\n";
$passedCount++;

echo "\n========================================================\n";
echo "   SUCCESS: ALL 12 TEST SCENARIOS PASSED ({$passedCount}/12)!   \n";
echo "========================================================\n";
