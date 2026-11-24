<?php

require_once __DIR__ . '/../src/Services/OllamaService.php';

$svc = new \App\Services\OllamaService();
$res = $svc->chat([
    ['role' => 'user', 'content' => 'Say hi in 3 words']
]);

echo "Result:\n";
print_r($res);
