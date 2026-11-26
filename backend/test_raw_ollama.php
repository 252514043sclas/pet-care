<?php
require_once __DIR__ . '/src/Utils/Database.php';
require_once __DIR__ . '/src/Services/OllamaService.php';
require_once __DIR__ . '/src/Services/SearchService.php';
require_once __DIR__ . '/src/Controllers/ChatController.php';

$pdo = App\Utils\Database::getConnection();
$controller = new App\Controllers\ChatController();
$ref = new ReflectionClass($controller);

$resolvePet = $ref->getMethod('resolvePetContext');
$resolvePet->setAccessible(true);
$petContext = $resolvePet->invoke($controller, $pdo, 1, 1);

$buildPrompt = $ref->getMethod('buildSystemPrompt');
$buildPrompt->setAccessible(true);
$sysPrompt = $buildPrompt->invoke($controller, $petContext, '');

$getHistory = $ref->getMethod('getRecentConversationHistory');
$getHistory->setAccessible(true);
$history = $getHistory->invoke($controller, $pdo, 1, 1, 10);

echo "History count: " . count($history) . "\n";
foreach ($history as $i => $h) {
    echo "  [$i] {$h['sender']}: " . substr($h['message'], 0, 60) . "\n";
}

$messages = [
    ['role' => 'system', 'content' => $sysPrompt]
];
foreach ($history as $h) {
    $messages[] = [
        'role' => $h['sender'] === 'user' ? 'user' : 'assistant',
        'content' => $h['message']
    ];
}
$messages[] = ['role' => 'user', 'content' => 'What vegetables can Clover eat daily and what is toxic?'];

$ollama = new App\Services\OllamaService();
$res = $ollama->chat($messages);
echo "AI Raw Result:\n";
print_r($res);
