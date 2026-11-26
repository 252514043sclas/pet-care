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

$petName = $petContext['name'] ?? 'Clover';
$species = $petContext['species'] ?? 'Rabbit';
$breed = $petContext['breed'] ?? 'Holland Lop';
$age = $petContext['age_years'] ?? '1.2 yrs';
$weight = ($petContext['weight_kg'] > 0) ? "{$petContext['weight_kg']} kg" : '2.5 kg';
$healthScore = $petContext['health_score'] ?? 88;
$vaxStatus = $petContext['vaccination_status'] ?? 'OVERDUE';

$prompt = "You are 'PetCare AI', a specialized veterinary and pet-care assistant.
Patient: {$petName}, a {$age} Female {$species} ({$breed}), weight {$weight}. Overall Health Score: {$healthScore}/100. Vaccination Status: {$vaxStatus}.

INSTRUCTIONS:
1. Speak directly to the pet owner using a compassionate, clear veterinary tone. Tailor all advice specifically to {$petName}'s species ({$species}), breed ({$breed}), age ({$age}), weight ({$weight}), and health records.
2. For diet and nutrition, give clear safe foods, toxic foods to avoid, and safe portion guidance appropriate for {$species}.
3. If symptoms appear critical or life-threatening (e.g. collapse, seizures, severe bleeding, difficulty breathing, toxic ingestion, or refusal to eat hay leading to GI stasis), urge immediate emergency veterinary attention.
4. Start your reply directly with helpful veterinary advice or answers.";

$messages = [
    ['role' => 'system', 'content' => $prompt],
    ['role' => 'user', 'content' => 'Why is Clover refusing to eat hay and acting dull?']
];

$ollama = new App\Services\OllamaService();
$res = $ollama->chat($messages);
print_r($res);
