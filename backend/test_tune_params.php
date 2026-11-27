<?php
$prompt = "<|im_start|>system\nYou are 'PetCare AI', a specialized veterinary assistant.
Patient: Clover, a 1.2 yrs Female Rabbit (Holland Lop), weight 2.50 kg. Overall Health Score: 88/100. Vaccination Status: OVERDUE.
Provide direct veterinary advice. Structure your response clearly:
- Nutritional Assessment:
- Safe Daily Foods:
- Toxic / Dangerous Foods to Avoid:
- Portion Guidance:
Do not include internal planning, conversational monologue, or third-person narration.<|im_end|>
<|im_start|>user
What vegetables can Clover eat daily and what is toxic?<|im_end|>
<|im_start|>assistant
<think>
</think>
";

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
        'temperature' => 0.1,
        'repeat_penalty' => 1.15,
        'num_predict' => 600
    ],
    'stream' => false
]));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
$t0 = microtime(true);
$res = curl_exec($ch);
$elapsed = microtime(true) - $t0;
$d = json_decode($res, true);
$rawText = $d['response'] ?? '';
echo "Elapsed: " . round($elapsed, 2) . "s\n";
echo "Eval count: " . ($d['eval_count'] ?? 0) . "\n";
echo "Done reason: " . ($d['done_reason'] ?? 'unknown') . "\n";
echo "Raw Response:\n" . $rawText . "\n";
