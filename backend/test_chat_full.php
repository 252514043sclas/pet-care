<?php
spl_autoload_register(function ($class) {
    $prefix = 'App\\';
    $baseDir = __DIR__ . '/src/';
    $len = strlen($prefix);
    if (strncmp($prefix, $class, $len) !== 0) return;
    $relativeClass = substr($class, $len);
    $file = $baseDir . str_replace('\\', '/', $relativeClass) . '.php';
    if (file_exists($file)) require_once $file;
});

$messages = [
    [
        'role' => 'system',
        'content' => "You are 'PetCare AI', a specialized veterinary assistant. Patient: Clover, a 1.2 yrs Female Rabbit (Holland Lop), weight 2.5 kg.\nGive direct, concise advice on safe vegetables and portion sizes for Clover."
    ],
    [
        'role' => 'user',
        'content' => "What vegetables can Clover eat daily and how much?"
    ]
];

$t0 = microtime(true);
$prompt = '';
foreach ($messages as $msg) {
    $role = $msg['role'];
    $content = $msg['content'];
    $prompt .= "<|im_start|>{$role}\n{$content}<|im_end|>\n";
}
$prompt .= "<|im_start|>assistant\n<think>\n</think>\n";

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
$res = curl_exec($ch);
$elapsed = microtime(true) - $t0;
$d = json_decode($res, true);
$rawText = $d['response'] ?? '';

// Clean preamble
$cleaned = preg_replace('/^(?:(?:As a (?:veterinary|pet care|specialized)|The user (?:is asking|wants)|First,?\s*I need|Wait,?\s*(?:the user|I need)|Let me (?:check|recall)|Okay,?\s*the user|I should (?:focus|provide))[^\n]*\n*)+/is', '', trim($rawText));

echo "Elapsed: " . round($elapsed, 2) . "s\n";
echo "Cleaned Response:\n" . $cleaned . "\n";
