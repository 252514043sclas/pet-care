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

require_once __DIR__ . '/../src/Utils/Database.php';
require_once __DIR__ . '/../src/Controllers/ChatController.php';

use App\Utils\Database;
use App\Controllers\ChatController;

echo "--- Testing Chat Mini ChatGPT Upgrades ---\n";

$pdo = Database::getConnection();

// Create two test users for isolation testing if they don't exist
$u1 = $pdo->query("SELECT id, email, name FROM users WHERE email = 'test_chat_a@example.com'")->fetch(PDO::FETCH_ASSOC);
if (!$u1) {
    $pdo->prepare("INSERT INTO users (name, email, password_hash, created_at) VALUES ('User A', 'test_chat_a@example.com', 'hash', NOW())")->execute();
    $u1 = ['id' => (int)$pdo->lastInsertId(), 'email' => 'test_chat_a@example.com', 'name' => 'User A'];
}
$u2 = $pdo->query("SELECT id, email, name FROM users WHERE email = 'test_chat_b@example.com'")->fetch(PDO::FETCH_ASSOC);
if (!$u2) {
    $pdo->prepare("INSERT INTO users (name, email, password_hash, created_at) VALUES ('User B', 'test_chat_b@example.com', 'hash', NOW())")->execute();
    $u2 = ['id' => (int)$pdo->lastInsertId(), 'email' => 'test_chat_b@example.com', 'name' => 'User B'];
}

echo "User A ID: {$u1['id']}, User B ID: {$u2['id']}\n";

// 1. Test Refusal String
$refusal = ChatController::NON_PET_RESPONSE;
echo "Expected Refusal: '$refusal'\n";
assert($refusal === "I'm your Pet Care AI assistant. I can help you with questions related to pets and their care.");
echo "[PASS] Refusal constant exact match\n";

// 2. Test detectLanguage helper via reflection
$chatController = new ChatController();
$refMethod = new ReflectionMethod(ChatController::class, 'detectLanguage');
$refMethod->setAccessible(true);

$l1 = $refMethod->invoke($chatController, 'என் நாய்க்கு என்ன உணவு கொடுக்க வேண்டும்?');
echo "Tamil script detected: $l1\n";
assert($l1 === 'ta');

$l2 = $refMethod->invoke($chatController, 'Na en dog ku enna food kudukalam?');
echo "Tanglish detected: $l2\n";
assert($l2 === 'tanglish');

$l3 = $refMethod->invoke($chatController, 'What is the Tamil meaning of vaccination?');
echo "Tamil meaning detected: $l3\n";
assert($l3 === 'ta_meaning');

$l4 = $refMethod->invoke($chatController, 'How often should I feed my golden retriever puppy?');
echo "English detected: $l4\n";
assert($l4 === 'en');
echo "[PASS] Language detection working accurately\n";

// 3. Test isUnrelatedToPets helper via reflection
$refUnrelated = new ReflectionMethod(ChatController::class, 'isUnrelatedToPets');
$refUnrelated->setAccessible(true);

$uCheck1 = $refUnrelated->invoke($chatController, 'What is the capital of France?', [], []);
echo "Capital of France is unrelated: " . ($uCheck1 ? 'YES' : 'NO') . "\n";
assert($uCheck1 === true);

$uCheck2 = $refUnrelated->invoke($chatController, 'Write python code to invert a binary tree', [], []);
echo "Python code is unrelated: " . ($uCheck2 ? 'YES' : 'NO') . "\n";
assert($uCheck2 === true);

$uCheck3 = $refUnrelated->invoke($chatController, 'My dog is vomiting yellow foam, what should I do?', [], []);
echo "Dog vomiting is unrelated: " . ($uCheck3 ? 'YES' : 'NO') . "\n";
assert($uCheck3 === false);

$uCheck4 = $refUnrelated->invoke($chatController, 'என் பூனைக்கு காய்ச்சல் இருக்கு', [], []);
echo "Tamil cat fever is unrelated: " . ($uCheck4 ? 'YES' : 'NO') . "\n";
assert($uCheck4 === false);

$uCheck5 = $refUnrelated->invoke($chatController, 'What is the Tamil meaning of vaccination?', [], []);
echo "Tamil meaning of vaccination is unrelated: " . ($uCheck5 ? 'YES' : 'NO') . "\n";
assert($uCheck5 === false);
echo "[PASS] Domain filter working accurately\n";

// 4. Test Conversation Session Database Creation & User Isolation
$pdo->prepare("DELETE FROM chat_conversations WHERE user_id IN (?, ?)")->execute([$u1['id'], $u2['id']]);

$c1Stmt = $pdo->prepare("INSERT INTO chat_conversations (user_id, title, language) VALUES (?, 'User A Dog Care', 'en')");
$c1Stmt->execute([$u1['id']]);
$c1Id = (int)$pdo->lastInsertId();

$c2Stmt = $pdo->prepare("INSERT INTO chat_conversations (user_id, title, language) VALUES (?, 'User B Cat Diet', 'en')");
$c2Stmt->execute([$u2['id']]);
$c2Id = (int)$pdo->lastInsertId();

// Verify User A only sees their own conversations
$userAConvs = $pdo->query("SELECT id, title FROM chat_conversations WHERE user_id = {$u1['id']}")->fetchAll(PDO::FETCH_ASSOC);
echo "User A conversations count: " . count($userAConvs) . "\n";
assert(count($userAConvs) === 1);
assert($userAConvs[0]['id'] == $c1Id);

$userBConvs = $pdo->query("SELECT id, title FROM chat_conversations WHERE user_id = {$u2['id']}")->fetchAll(PDO::FETCH_ASSOC);
echo "User B conversations count: " . count($userBConvs) . "\n";
assert(count($userBConvs) === 1);
assert($userBConvs[0]['id'] == $c2Id);
echo "[PASS] Multi-user conversation isolation verified\n";

// 5. Test Cascading Delete
$pdo->prepare("INSERT INTO chat_messages (conversation_id, user_id, sender, message) VALUES (?, ?, 'user', 'Hello')")->execute([$c1Id, $u1['id']]);
$msgCount = $pdo->query("SELECT COUNT(*) FROM chat_messages WHERE conversation_id = $c1Id")->fetchColumn();
assert($msgCount == 1);

// Delete User A conversation
$pdo->prepare("DELETE FROM chat_conversations WHERE id = ? AND user_id = ?")->execute([$c1Id, $u1['id']]);
$convCount = $pdo->query("SELECT COUNT(*) FROM chat_conversations WHERE id = $c1Id")->fetchColumn();
$msgCountAfter = $pdo->query("SELECT COUNT(*) FROM chat_messages WHERE conversation_id = $c1Id")->fetchColumn();
assert($convCount == 0);
assert($msgCountAfter == 0);
echo "[PASS] Cascading conversation deletion verified\n";

echo "ALL BACKEND UNIT TESTS PASSED!\n";
