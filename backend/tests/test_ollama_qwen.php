<?php

echo "=== Testing Ollama Qwen3-VL 2B Multimodal Inference (CPU Mode) ===\n\n";

$url = 'http://127.0.0.1:11434/api/chat';

// 1. Test Text-Only Prompt
echo "[1] Testing Text-Only Generation...\n";
$textPayload = [
    'model' => 'qwen3-vl:2b',
    'messages' => [
        [
            'role' => 'system',
            'content' => 'You are a compassionate veterinary pet-care AI assistant named PetCare AI.'
        ],
        [
            'role' => 'user',
            'content' => 'In 2 sentences, what are the primary nutritional needs of an adult domestic cat?'
        ]
    ],
    'options' => [
        'num_gpu' => 0, // Force CPU inference for GT 1030 stability
        'temperature' => 0.7,
        'num_thread' => 6 // Intel Core i5-11400F 6 physical cores
    ],
    'stream' => false
];

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 60);
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($textPayload));

$t0 = microtime(true);
$response = curl_exec($ch);
$elapsed = round(microtime(true) - $t0, 2);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode === 200) {
    $resJson = json_decode($response, true);
    $reply = $resJson['message']['content'] ?? '';
    echo "  -> SUCCESS (HTTP 200 in {$elapsed}s):\n";
    echo "  -> AI Reply: " . trim($reply) . "\n\n";
} else {
    die("  -> FAIL! HTTP {$httpCode}: {$response}\n");
}

// 2. Test Multimodal Image Analysis with real animal image
echo "[2] Testing Multimodal Vision with 'img_pet_dog.jpg'...\n";
$imagePath = 'c:/Users/SCAD LAB/AndroidStudioProjects/PetCare/app/src/main/res/drawable/img_pet_dog.jpg';
if (!file_exists($imagePath)) {
    die("  -> Image file not found: {$imagePath}\n");
}

$imageBase64 = base64_encode(file_get_contents($imagePath));

$visionPayload = [
    'model' => 'qwen3-vl:2b',
    'messages' => [
        [
            'role' => 'system',
            'content' => 'You are a veterinary AI assistant. Analyze the pet photograph and identify the animal and observe its visible appearance.'
        ],
        [
            'role' => 'user',
            'content' => 'What animal is shown in this picture, and what are its visible characteristics?',
            'images' => [$imageBase64]
        ]
    ],
    'options' => [
        'num_gpu' => 0,
        'temperature' => 0.5,
        'num_thread' => 6
    ],
    'stream' => false
];

$ch2 = curl_init($url);
curl_setopt($ch2, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch2, CURLOPT_POST, true);
curl_setopt($ch2, CURLOPT_TIMEOUT, 90);
curl_setopt($ch2, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
curl_setopt($ch2, CURLOPT_POSTFIELDS, json_encode($visionPayload));

$t1 = microtime(true);
$visResponse = curl_exec($ch2);
$elapsed2 = round(microtime(true) - $t1, 2);
$httpCode2 = curl_getinfo($ch2, CURLINFO_HTTP_CODE);
curl_close($ch2);

if ($httpCode2 === 200) {
    $visJson = json_decode($visResponse, true);
    $visReply = $visJson['message']['content'] ?? '';
    echo "  -> SUCCESS (HTTP 200 in {$elapsed2}s):\n";
    echo "  -> AI Vision Output:\n" . trim($visReply) . "\n\n";
    echo ">>> QWEN3-VL:2B TEXT AND VISION INFERENCE ON CPU 100% OPERATIONAL! <<<\n";
} else {
    die("  -> VISION FAIL! HTTP {$httpCode2}: {$visResponse}\n");
}
