<?php
$petName = "Clover";
$prompt = "<|im_start|>system\nYou are a pet care veterinary assistant. Give concise bullet points of safe vegetables and toxic vegetables to avoid.<|im_end|>\n<|im_start|>user\nWhat vegetables can Clover eat daily?<|im_end|>\n<|im_start|>assistant\n<think>\n</think>\nFor {$petName}:\n";

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
        'num_predict' => 250
    ],
    'stream' => false
]));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
$res = curl_exec($ch);
$d = json_decode($res, true);
$rawText = "For {$petName}:\n" . ($d['response'] ?? '');
echo $rawText . "\n";
