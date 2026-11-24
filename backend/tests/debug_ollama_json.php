<?php

$ch = curl_init('http://127.0.0.1:11434/api/chat');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 60);
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode([
    'model' => 'qwen3-vl:2b',
    'messages' => [['role' => 'user', 'content' => 'Say hello!']],
    'options' => ['num_gpu' => 0, 'num_predict' => 50],
    'stream' => false
]));

$raw = curl_exec($ch);
curl_close($ch);

echo "RAW RESPONSE:\n" . $raw . "\n";
