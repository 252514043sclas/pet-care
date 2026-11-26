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

echo $sysPrompt . "\n";
