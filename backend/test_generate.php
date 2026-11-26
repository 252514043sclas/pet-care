<?php
$ch = curl_init('http://127.0.0.1:11434/api/generate');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode([
    'model' => 'qwen3-vl:2b',
    'prompt' => "<|im_start|>system\nYou are a veterinary assistant. Provide safe vegetables for rabbits and portion guidance.<|im_end|>\n<|im_start|>user\nWhat vegetables can my rabbit Clover eat?<|im_end|>\n<|im_start|>assistant\n<think>\n</think>\nHere is the dietary advice for Clover:\n",
    'raw' => true,
    'options' => [
        'num_gpu' => 0,
        'num_thread' => 6,
        'temperature' => 0.2,
        'num_predict' => 250
    ],
    'stream' => false
]));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
$t0 = microtime(true);
$res = curl_exec($ch);
$elapsed = microtime(true) - $t0;
echo "Time: " . round($elapsed, 2) . "s\n";
$d = json_decode($res, true);
echo "Response:\nHere is the dietary advice for Clover:\n" . ($d['response'] ?? 'EMPTY') . "\n";
