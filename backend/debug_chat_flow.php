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

$petName = $petContext['name'] ?? 'your pet';
$species = $petContext['species'] ?? 'pet';

$systemPrompt = "You are 'PetCare AI', a specialized veterinary assistant.
Patient: {$petName}, a {$petContext['age_years']} Female {$species} ({$petContext['breed']}), weight {$petContext['weight_kg']} kg. Overall Health Score: {$petContext['health_score']}/100.
Instructions:
1. Speak directly to the pet owner in clear veterinary terms.
2. If symptoms are dangerous (e.g. refusing hay, lethargy, GI stasis), urge veterinary evaluation.
3. Provide direct clinical explanations and practical care steps.";

$userQuestion = "Why is Clover refusing to eat hay and acting dull?";

$prompt = "<|im_start|>system\n{$systemPrompt}<|im_end|>\n<|im_start|>user\n{$userQuestion}<|im_end|>\n<|im_start|>assistant\n<think>\n</think>\nFor {$petName}:\n";

$ch = curl_init('http://127.0.0.1:11434/api/generate');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode([
    'model' => 'qwen3-vl:2b',
    'prompt' => $prompt,
    'raw' => true,
    'options' => [
        'num_gpu' => 0,
        'num_thread' => 6,
        'temperature' => 0.2,
        'num_predict' => 350
    ],
    'stream' => false
]));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
$t0 = microtime(true);
$res = curl_exec($ch);
$elapsed = microtime(true) - $t0;
$d = json_decode($res, true);
$response = "For {$petName}:\n" . trim($d['response'] ?? '');

echo "Elapsed: " . round($elapsed, 2) . "s\n";
echo "Response:\n" . $response . "\n";
