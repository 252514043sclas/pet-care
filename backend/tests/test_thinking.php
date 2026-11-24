<?php

$tests = [
    'direct_system' => [
        'model' => 'qwen3-vl:2b',
        'messages' => [
            ['role' => 'system', 'content' => '/no_think\nAnswer directly without thinking.'],
            ['role' => 'user', 'content' => 'Say hello in 3 words!']
        ],
        'options' => ['num_gpu' => 0, 'num_predict' => 200],
        'stream' => false
    ],
    'higher_tokens' => [
        'model' => 'qwen3-vl:2b',
        'messages' => [
            ['role' => 'user', 'content' => 'Say hello in 3 words!']
        ],
        'options' => ['num_gpu' => 0, 'num_predict' => 600],
        'stream' => false
    ]
];

foreach ($tests as $name => $payload) {
    echo "=== Running {$name} ===\n";
    $ch = curl_init('http://127.0.0.1:11434/api/chat');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 60);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
    $raw = curl_exec($ch);
    curl_close($ch);
    $json = json_decode($raw, true);
    echo "Content: '" . ($json['message']['content'] ?? '') . "'\n";
    echo "Thinking length: " . strlen($json['message']['thinking'] ?? '') . "\n";
    echo "Done reason: " . ($json['done_reason'] ?? '') . "\n\n";
}
