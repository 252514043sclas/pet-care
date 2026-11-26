<?php
$ch = curl_init('http://127.0.0.1:11434/api/chat');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode([
    'model' => 'qwen3-vl:2b',
    'messages' => [
        ['role' => 'system', 'content' => 'You are a veterinary assistant. Answer directly and concisely in 2-3 sentences. Do not think aloud.'],
        ['role' => 'user', 'content' => 'What vegetables are safe for a rabbit?']
    ],
    'options' => [
        'num_gpu' => 0,
        'num_thread' => 6,
        'temperature' => 0.1,
        'num_predict' => 200
    ],
    'stream' => false
]));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
$t0 = microtime(true);
$res = curl_exec($ch);
$elapsed = microtime(true) - $t0;
echo "Time: " . round($elapsed, 2) . "s\n";
$d = json_decode($res, true);
echo "Message Content: " . ($d['message']['content'] ?? 'EMPTY') . "\n";
echo "Message Thinking: " . substr(($d['message']['thinking'] ?? 'NONE'), 0, 300) . "\n";
echo "Done Reason: " . ($d['done_reason'] ?? 'NONE') . "\n";
