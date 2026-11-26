<?php
$imgPath = __DIR__ . '/public/uploads/chat/chat_1795507919_7dee1d5563d8.jpg';
$base64 = base64_encode(file_get_contents($imgPath));

$ch = curl_init('http://127.0.0.1:11434/api/chat');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode([
    'model' => 'qwen3-vl:2b',
    'messages' => [
        [
            'role' => 'system',
            'content' => "You are a veterinary assistant. If the image is not an animal, reply with EXACTLY: \"I'm a pet care assistant, so I can only analyze pet-related images.\""
        ],
        [
            'role' => 'user',
            'content' => 'What animal is this and describe its condition briefly.',
            'images' => [$base64]
        ]
    ],
    'options' => [
        'num_gpu' => 0,
        'num_thread' => 6,
        'temperature' => 0.2,
        'num_predict' => 300
    ],
    'stream' => false
]));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
$t0 = microtime(true);
$res = curl_exec($ch);
$elapsed = microtime(true) - $t0;
$code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
echo "HTTP code: $code\n";
echo "Time: " . round($elapsed, 2) . "s\n";
$d = json_decode($res, true);
echo "Content: " . ($d['message']['content'] ?? 'EMPTY') . "\n";
echo "Thinking: " . substr(($d['message']['thinking'] ?? 'NONE'), 0, 300) . "\n";
