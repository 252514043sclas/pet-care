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
        $options = array_merge([
            'num_gpu' => $this->numGpu,
            'num_thread' => $this->numThreads,
            'num_ctx' => 2048,   // Optimal context size for CPU cache efficiency
            'num_predict' => 600, // Focused, high-speed response
            'temperature' => 0.5,
            'top_p' => 0.9,
            'top_k' => 40,
        ], $extraOptions);

        $payload = [
            'model' => $this->model,
            'messages' => $messages,
            'options' => $options,
            'stream' => false
        ];

        return $this->sendChatRequest($payload);
    }

    /**
     * Multimodal image analysis
     */
    public function analyzeImage(string $imageBase64, string $prompt, array $petContext = []): array {
        $petSpecies = $petContext['species'] ?? 'pet';
        $petName = $petContext['name'] ?? 'the pet';
        $petAge = $petContext['age'] ?? 'unknown age';
        $petBreed = $petContext['breed'] ?? 'unknown breed';

        $systemPrompt = "You are a professional, compassionate veterinary AI assistant specializing in clinical triage and pet health education.
Pet Context: Name: {$petName}, Species: {$petSpecies}, Breed: {$petBreed}, Age: {$petAge}.

CRITICAL VETERINARY DIRECTIVES:
1. Examine the image carefully for visible anatomical features, skin abnormalities, wounds, lesions, swelling, parasites, or inflammation.
2. NEVER claim that an image alone confirms a medical diagnosis.
3. NEVER state confidence scores as clinically validated probabilities.
4. If the photograph is blurry, dark, or unclear, politely advise the user to provide a clearer, well-lit photo.
5. If severe signs are detected (such as deep active bleeding, respiratory distress, pale gums, profound trauma, severe burns, or neurological symptoms), immediately mark Severity as 'Emergency' and instruct the user to seek immediate emergency veterinary hospital attention.

You MUST structure your response strictly in the following format:
- Pet type: [Identified animal and observed breed characteristics]
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
                'num_predict' => 650,
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

        // Fallback: if model's reasoning filled thinking before final content tag
        if (empty($content) && !empty($data['message']['thinking'])) {
            $thinking = trim($data['message']['thinking']);
            
            // Extract drafted final answer if marked inside thinking
            if (preg_match('/(?:Final answer|Response|Here is the response|In summary|Summary|Here are the|Answer)[:\s*\n]+(.*)$/is', $thinking, $match)) {
                $content = trim($match[1]);
            } elseif (preg_match('/(#{1,3}\s+[^\n]+.*)$/is', $thinking, $match)) {
                $content = trim($match[1]);
            } elseif (preg_match('/(- (?:Pet type|Common diseases|First aid|Ingredients|Diet|Visible).*)$/is', $thinking, $match)) {
                $content = trim($match[1]);
            } else {
                // Clean leading internal reasoning monologue
                $cleaned = preg_replace('/^(?:Okay|Let\'s|Hmm|Alright|So)[^.\n]*[.\n]+\s*/i', '', $thinking);
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
