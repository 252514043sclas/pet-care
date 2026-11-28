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
echo "   DYNAMIC CURRENT PET CONTEXT & LANGUAGE TEST SUITE   \n";
echo "========================================================\n\n";

// 1. Get or create test user
$userEmail = 'mugil_owner@petcare.com';
$u = $pdo->query("SELECT id, email, name FROM users WHERE email = '{$userEmail}'")->fetch(PDO::FETCH_ASSOC);
if (!$u) {
    $stmt = $pdo->prepare("INSERT INTO users (name, email, password_hash, created_at) VALUES (?, ?, ?, NOW())");
    $stmt->execute(['Mugil Owner', $userEmail, password_hash('password123', PASSWORD_BCRYPT)]);
    $u = ['id' => (int)$pdo->lastInsertId(), 'email' => $userEmail, 'name' => 'Mugil Owner'];
}
$userId = (int)$u['id'];

// 2. Create Rabbit named Mugil
$pdo->prepare("DELETE FROM pets WHERE user_id = ?")->execute([$userId]);

$pStmt = $pdo->prepare("INSERT INTO pets (user_id, name, species, breed, age_years, weight_kg, gender, is_active, created_at) VALUES (?, 'Mugil', 'Rabbit', 'Holland Lop', '3 years', 6.9, 'Male', 1, NOW())");
$pStmt->execute([$userId]);
$mugilId = (int)$pdo->lastInsertId();

// Create Dog named Rocky for this same user
$pStmt2 = $pdo->prepare("INSERT INTO pets (user_id, name, species, breed, age_years, weight_kg, gender, is_active, created_at) VALUES (?, 'Rocky', 'Dog', 'German Shepherd', '2 years', 28.5, 'Male', 0, NOW())");
$pStmt2->execute([$userId]);
$rockyId = (int)$pdo->lastInsertId();

echo "Created User ID: {$userId}\n";
echo "Pet 1: Mugil (ID: {$mugilId}, Species: Rabbit, Breed: Holland Lop, Age: 3 years, Weight: 6.9 kg)\n";
echo "Pet 2: Rocky (ID: {$rockyId}, Species: Dog, Breed: German Shepherd, Age: 2 years, Weight: 28.5 kg)\n\n";

// Generate Auth Token
$rawToken = bin2hex(random_bytes(32));
$tokenHash = hash('sha256', $rawToken);
$expiresAt = date('Y-m-d H:i:s', strtotime('+30 days'));
$tStmt = $pdo->prepare("INSERT INTO user_tokens (user_id, token_hash, device_name, expires_at) VALUES (?, ?, 'Test', ?)");
$tStmt->execute([$userId, $tokenHash, $expiresAt]);

function apiChat($message, $petId, $token) {
    $url = "http://localhost/petcare/public/api/chat";
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 120);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Accept: application/json',
        "Authorization: Bearer {$token}"
    ]);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode([
        'message' => $message,
        'pet_id' => $petId,
        'enable_search' => false
    ]));
    $raw = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return ['code' => $code, 'data' => json_decode($raw, true), 'raw' => $raw];
}

// TEST 1: Rabbit Mugil not eating properly
echo "TEST 1: Asking 'Why is my pet not eating properly?' for Mugil (Rabbit)...\n";
$res1 = apiChat('Why is my pet not eating properly?', $mugilId, $rawToken);
assert($res1['code'] === 200);
$ans1 = $res1['data']['data']['message'];
echo "Answer preview:\n" . substr($ans1, 0, 250) . "...\n\n";

// Must mention Mugil, must mention rabbit/GI stasis/hay/dental, must NOT treat as dog/cat
assert(stripos($ans1, 'Mugil') !== false, "Response must mention Mugil");
assert(stripos($ans1, 'rabbit') !== false, "Response must identify Mugil as a rabbit");
assert(stripos($ans1, 'stasis') !== false || stripos($ans1, 'hay') !== false || stripos($ans1, 'dental') !== false || stripos($ans1, 'gut') !== false, "Response must reference rabbit physiology");
echo "[PASS] Mugil (Rabbit) dynamic context verified!\n\n";

// TEST 2: Rabbit Mugil food question
echo "TEST 2: Asking 'What food can I give to my pet?' for Mugil (Rabbit)...\n";
$res2 = apiChat('What food can I give to my pet?', $mugilId, $rawToken);
assert($res2['code'] === 200);
$ans2 = $res2['data']['data']['message'];
echo "Answer preview:\n" . substr($ans2, 0, 250) . "...\n\n";
assert(stripos($ans2, 'hay') !== false || stripos($ans2, 'timothy') !== false || stripos($ans2, 'pellets') !== false || stripos($ans2, 'greens') !== false, "Response must include rabbit diet items like hay/greens");
echo "[PASS] Mugil (Rabbit) species-specific food advice verified!\n\n";

// TEST 3: Switch context to Rocky (Dog)
echo "TEST 3: Asking 'What food should I feed my pet?' for Rocky (Dog)...\n";
$res3 = apiChat('What food should I feed my pet?', $rockyId, $rawToken);
assert($res3['code'] === 200);
$ans3 = $res3['data']['data']['message'];
echo "Answer preview:\n" . substr($ans3, 0, 250) . "...\n\n";
assert(stripos($ans3, 'Rocky') !== false, "Response must mention Rocky");
assert(stripos($ans3, 'dog') !== false || stripos($ans3, 'canine') !== false, "Response must identify Rocky as a dog");
echo "[PASS] Context successfully switched to Rocky (Dog)!\n\n";

// TEST 4: Tamil meaning request
echo "TEST 4: Asking 'What is the Tamil meaning of vaccination?'...\n";
$res4 = apiChat('What is the Tamil meaning of vaccination?', $mugilId, $rawToken);
assert($res4['code'] === 200);
$ans4 = $res4['data']['data']['message'];
echo "Answer preview:\n" . substr($ans4, 0, 250) . "...\n\n";
assert(preg_match('/(தடுப்பூசி|thaduppoosi)/iu', $ans4), "Response must provide Tamil meaning for vaccination");
echo "[PASS] Tamil meaning request answered with Tamil definition!\n\n";

// TEST 5: Normal pet question default language: Simple English
echo "TEST 5: Asking normal question, verifying Simple English default...\n";
$res5 = apiChat('How can I groom my pet properly at home?', $mugilId, $rawToken);
assert($res5['code'] === 200);
$ans5 = $res5['data']['data']['message'];
assert(strlen($ans5) > 50);
// Ensure it's English, not Tamil script
assert(!preg_match('/[\x{0B80}-\x{0BFF}]/u', $ans5), "Normal pet response must be in English, not Tamil script");
echo "[PASS] Normal question answered in simple English!\n\n";

// TEST 6: Unrelated question
echo "TEST 6: Unrelated question restriction...\n";
$res6 = apiChat('What is the capital of Spain?', $mugilId, $rawToken);
assert($res6['code'] === 200);
$ans6 = $res6['data']['data']['message'];
assert($ans6 === "I'm your Pet Care AI assistant. I can help you with questions related to pets and their care.");
echo "[PASS] Unrelated question domain restriction verified!\n\n";

echo "========================================================\n";
echo "   ALL DYNAMIC PET CONTEXT & LANGUAGE TESTS PASSED!     \n";
echo "========================================================\n";
