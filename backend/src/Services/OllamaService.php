<?php

declare(strict_types=1);

namespace App\Services;

class OllamaService {
    private string $baseUrl;
    private string $model;
    private int $numThreads;
    private int $numGpu;

    public function __construct(
        string $baseUrl = 'http://127.0.0.1:11434',
        string $model = 'qwen3-vl:2b',
        int $numThreads = 6, // 6 physical cores of i5-11400F for peak memory bandwidth
        int $numGpu = 0 // Enforce CPU mode to protect 2GB GT 1030
    ) {
        $this->baseUrl = rtrim(getenv('OLLAMA_BASE_URL') ?: $baseUrl, '/');
        $this->model = getenv('OLLAMA_MODEL') ?: $model;
        $this->numThreads = (int)(getenv('OLLAMA_NUM_THREADS') ?: $numThreads);
        $this->numGpu = (int)(getenv('OLLAMA_NUM_GPU') ?: $numGpu);
    }

    public function getBaseUrl(): string {
        return $this->baseUrl;
    }

    public function getModel(): string {
        return $this->model;
    }

    /**
     * Check if Ollama is running and model is available
     */
    public function getHealth(): array {
        $ch = curl_init("{$this->baseUrl}/api/tags");
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 4);
        $res = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($httpCode !== 200 || empty($res)) {
            return [
                'status' => 'offline',
                'message' => 'Ollama service is not reachable at ' . $this->baseUrl,
                'model_loaded' => false,
                'model' => $this->model
            ];
        }

        $data = json_decode($res, true);
        $models = $data['models'] ?? [];
        $modelFound = false;

        foreach ($models as $m) {
            $name = $m['name'] ?? '';
            if ($name === $this->model || str_starts_with($name, $this->model)) {
                $modelFound = true;
                break;
            }
        }

        return [
            'status' => 'online',
            'message' => 'Ollama is operational',
            'model_loaded' => $modelFound,
            'model' => $this->model,
            'available_models' => array_column($models, 'name')
        ];
    }

    /**
     * General chat completion
     */
    public function chat(array $messages, array $extraOptions = []): array {
        // Build ChatML prompt with prefilled <think>\n</think>\n to bypass prolonged thinking loops on CPU
        $prompt = '';
        foreach ($messages as $msg) {
            $role = $msg['role'];
            $content = $msg['content'];
            $prompt .= "<|im_start|>{$role}\n{$content}<|im_end|>\n";
        }
        $prompt .= "<|im_start|>assistant\n<think>\n</think>\n";

        $options = array_merge([
            'num_gpu' => $this->numGpu,
            'num_thread' => $this->numThreads,
            'num_ctx' => 2048,
            'num_predict' => 650,
            'temperature' => 0.1,
            'repeat_penalty' => 1.15,
            'top_p' => 0.9,
            'top_k' => 40,
        ], $extraOptions);

        $payload = [
            'model' => $this->model,
            'prompt' => $prompt,
            'raw' => true,
            'options' => $options,
            'stream' => false
        ];

        return $this->sendGenerateRequest($payload);
    }

    private function sendGenerateRequest(array $payload): array {
        @set_time_limit(300);
        @ini_set('max_execution_time', '300');

        $ch = curl_init("{$this->baseUrl}/api/generate");
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 240);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Content-Type: application/json',
            'Accept: application/json'
        ]);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));

        $t0 = microtime(true);
        $raw = curl_exec($ch);
        $elapsed = round(microtime(true) - $t0, 2);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);

        if ($httpCode !== 200 || empty($raw)) {
            $errMsg = $error ?: "Ollama returned HTTP {$httpCode}";
            return [
                'success' => false,
                'error' => $errMsg,
                'raw' => $raw,
                'elapsed' => $elapsed
            ];
        }

        $data = json_decode($raw, true);
        $content = trim($data['response'] ?? '');

        // Remove raw thinking blocks if emitted inside content
        if (!empty($content)) {
            $content = preg_replace('/<think>.*?<\/think>/is', '', $content);
            $content = preg_replace('/^(?:(?:The user|First,?\s*(?:I need|let\'s)|Now,?\s*let\'s|Wait|From the dossier|Let me|Okay|As a|I need to|I will|I should)[^\n]*\n*)+/is', '', trim($content));
            $content = trim($content);
        }

        return [
            'success' => true,
            'content' => $content,
            'model' => $data['model'] ?? $this->model,
            'total_duration' => $data['total_duration'] ?? 0,
            'eval_count' => $data['eval_count'] ?? 0,
            'elapsed' => $elapsed
        ];
    }

    /**
     * Multimodal image analysis
     */
    public function analyzeImage(string $imageBase64, string $prompt, array $petContext = []): array {
        $petSpecies = $petContext['species'] ?? 'pet';
        $petName = $petContext['name'] ?? 'the pet';
        $petAge = $petContext['age_years'] ?? ($petContext['age'] ?? 'unknown age');
        $petBreed = $petContext['breed'] ?? 'unknown breed';
        $petWeight = !empty($petContext['weight_kg']) ? "{$petContext['weight_kg']} kg" : 'unknown weight';

        $systemPrompt = "You are a professional, compassionate veterinary AI assistant specializing in clinical triage and pet health education.
Pet Context: Name: {$petName}, Species: {$petSpecies}, Breed: {$petBreed}, Age: {$petAge}, Weight: {$petWeight}.

CRITICAL VETERINARY DIRECTIVES:
1. Examine the image carefully for visible anatomical features, skin abnormalities, wounds, lesions, swelling, parasites, posture, or inflammation.
2. Consider {$petName}'s species ({$petSpecies}) and breed ({$petBreed}) in your visual assessment.
3. If the image DOES NOT show a pet or animal (e.g. human face, scenery, vehicle, document, or food for humans), reply with EXACTLY:
\"I'm a pet care assistant, so I can only analyze pet-related images.\"
4. NEVER claim that an image alone confirms a medical diagnosis.
5. NEVER state confidence scores as clinically validated probabilities.
6. If the photograph is blurry, dark, or unclear, politely advise the user to provide a clearer, well-lit photo.
7. If severe signs are detected (such as deep active bleeding, respiratory distress, pale gums, profound trauma, severe burns, or neurological symptoms), immediately mark Severity as 'Emergency' and instruct the user to seek immediate emergency veterinary hospital attention.

You MUST structure your response strictly in the following format:
- Pet type: [Identified animal and observed breed characteristics, e.g. {$petSpecies} / {$petBreed}]
- Visible observations: [Objective description of visible skin, coat, wound, swelling, eyes, or condition]
- Possible conditions: [Potential differential diagnoses to discuss with a vet, e.g., superficial dermatitis, hot spot, laceration, etc.]
- Severity: [Choose one: Low, Moderate, High, or Emergency]
- Confidence: [Qualitative assessment: Preliminary / Low / Moderate / High visual indication]
- Recommended care: [Immediate home comfort steps, hygiene, safe cleaning if superficial, what NOT to do]
- When to visit a veterinarian: [Specific red-flag triggers requiring in-person clinical examination]";

        $userContent = $prompt;
        if (empty(trim($userContent))) {
            $userContent = "Please examine this image of {$petName} ({$petSpecies}) and provide a thorough clinical visual assessment using the required veterinary format.";
        }

        $messages = [
            [
                'role' => 'system',
                'content' => $systemPrompt
            ],
            [
                'role' => 'user',
                'content' => $userContent,
                'images' => [$imageBase64]
            ]
        ];

        $payload = [
            'model' => $this->model,
            'messages' => $messages,
            'options' => [
                'num_gpu' => $this->numGpu,
                'num_thread' => $this->numThreads,
                'num_ctx' => 2048,
                'num_predict' => 850,
                'temperature' => 0.2, // Fast, deterministic visual triage
                'top_p' => 0.9,
                'top_k' => 40
            ],
            'stream' => false
        ];

        return $this->sendChatRequest($payload);
    }

    private function sendChatRequest(array $payload): array {
        @set_time_limit(300);
        @ini_set('max_execution_time', '300');

        $ch = curl_init("{$this->baseUrl}/api/chat");
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 240);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Content-Type: application/json',
            'Accept: application/json'
        ]);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));

        $t0 = microtime(true);
        $raw = curl_exec($ch);
        $elapsed = round(microtime(true) - $t0, 2);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);

        if ($httpCode !== 200 || empty($raw)) {
            $errMsg = $error ?: "Ollama returned HTTP {$httpCode}";
            return [
                'success' => false,
                'error' => $errMsg,
                'raw' => $raw,
                'elapsed' => $elapsed
            ];
        }

        $data = json_decode($raw, true);
        $content = trim($data['message']['content'] ?? '');

        // Remove raw thinking blocks if emitted inside content
        if (!empty($content)) {
            $content = preg_replace('/<think>.*?<\/think>/is', '', $content);
            // Remove leading third-person narrator internal monologue if emitted
            $content = preg_replace('/^(?:(?:The user|First,?\s*I need|Wait|From the dossier|Let me|Okay|As a|I need to|I will|I should)[^\n]*\n*)+/is', '', trim($content));
            $content = trim($content);
        }

        // Fallback: if model's reasoning filled thinking before final content tag
        if (empty($content) && !empty($data['message']['thinking'])) {
            $thinking = trim($data['message']['thinking']);
            
            // Extract drafted final answer if marked inside thinking
            if (preg_match('/(?:Final answer|Response|Here is the response|In summary|Summary|Here are the|Answer|Advice)[:\s*\n]+(.*)$/is', $thinking, $match)) {
                $content = trim($match[1]);
            } elseif (preg_match('/((?:#{1,3}|\*|\-)\s+[^\n]+.*)$/is', $thinking, $match)) {
                $content = trim($match[1]);
            } elseif (preg_match('/((?:For |If |Common |Rabbits |Dogs |Cats |Safe |To care |When to ).*)$/is', $thinking, $match)) {
                $content = trim($match[1]);
            } else {
                // Strip all third-person monologue sentences
                $cleaned = preg_replace('/^(?:(?:The user|First,? I|Wait,?|From the dossier|Let me|Okay,?|Hmm,?)[^\n]*\n*)+/is', '', $thinking);
                $content = !empty(trim($cleaned)) ? trim($cleaned) : $thinking;
            }
        }

        return [
            'success' => true,
            'content' => $content,
            'thinking' => $data['message']['thinking'] ?? null,
            'model' => $data['model'] ?? $this->model,
            'total_duration' => $data['total_duration'] ?? 0,
            'eval_count' => $data['eval_count'] ?? 0,
            'elapsed' => $elapsed
        ];
    }
}
