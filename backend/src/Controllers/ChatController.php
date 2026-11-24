<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Services\OllamaService;
use App\Services\SearchService;
use App\Utils\Database;
use App\Utils\Response;
use App\Utils\Validator;
use PDO;

class ChatController {
    private OllamaService $ollama;
    private SearchService $searchService;

    public function __construct() {
        $this->ollama = new OllamaService();
        $this->searchService = new SearchService();
    }

    /**
     * GET /api/health
     * Check system, database, and AI model health
     */
    public function health(): void {
        $dbStatus = 'unknown';
        try {
            $pdo = Database::getConnection();
            $pdo->query('SELECT 1');
            $dbStatus = 'connected';
        } catch (\Exception $e) {
            $dbStatus = 'error: ' . $e->getMessage();
        }

        $ollamaHealth = $this->ollama->getHealth();

        Response::success([
            'status' => ($dbStatus === 'connected' && $ollamaHealth['status'] === 'online') ? 'healthy' : 'degraded',
            'database' => $dbStatus,
            'ai_service' => $ollamaHealth,
            'server_time' => date('Y-m-d H:i:s'),
            'search_provider' => getenv('TAVILY_API_KEY') ? 'tavily' : (getenv('BRAVE_API_KEY') ? 'brave' : 'trusted_live_fallback')
        ], 'System health check completed');
    }

    /**
     * POST /api/chat
     * Conversational pet-care chat with optional web-search augmentation
     */
    public function chat(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'message' => 'required|min:1',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $userMessage = trim((string)$input['message']);
        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : null;
        $enableSearch = isset($input['enable_search']) ? (bool)$input['enable_search'] : true;

        $pdo = Database::getConnection();

        // 1. Fetch active pet context if available
        $petContext = $this->resolvePetContext($pdo, (int)$currentUser['id'], $petId);

        // 2. Perform live internet search if required or requested
        $searchResults = [];
        $searchContext = '';
        if ($enableSearch && $this->searchService->needsSearch($userMessage)) {
            try {
                $searchQuery = ($petContext['species'] ?? 'pet') . ' ' . $userMessage;
                $searchResults = $this->searchService->search($searchQuery, 4);
                $searchContext = $this->searchService->formatSearchContext($searchResults);
            } catch (\Exception $e) {
                // Non-blocking search fallback
                $searchResults = [];
            }
        }

        // 3. Build system prompt & message history
        $systemPrompt = $this->buildSystemPrompt($petContext, $searchContext);

        // Fetch recent conversation history from DB (up to last 6 messages)
        $history = $this->getRecentConversationHistory($pdo, (int)$currentUser['id'], $petId, 6);

        $messages = [
            ['role' => 'system', 'content' => $systemPrompt]
        ];

        foreach ($history as $h) {
            $messages[] = [
                'role' => $h['sender'] === 'user' ? 'user' : 'assistant',
                'content' => $h['message']
            ];
        }

        // Add current user message
        $messages[] = ['role' => 'user', 'content' => $userMessage];

        // 4. Save User Message to Database
        $userMsgId = $this->saveMessageToDb($pdo, [
            'user_id' => (int)$currentUser['id'],
            'pet_id' => $petId,
            'sender' => 'user',
            'message' => $userMessage,
            'message_type' => 'text'
        ]);

        // 5. Generate AI Response using Ollama (CPU mode)
        $aiResult = $this->ollama->chat($messages);

        if (!$aiResult['success']) {
            $fallbackMessage = "I apologize, but the local AI service is currently unavailable (" . ($aiResult['error'] ?? 'timeout') . "). Please verify that Ollama is running and try again.";
            
            // Still save error notification to conversation
            $this->saveMessageToDb($pdo, [
                'user_id' => (int)$currentUser['id'],
                'pet_id' => $petId,
                'sender' => 'assistant',
                'message' => $fallbackMessage,
                'message_type' => 'error'
            ]);

            Response::error($fallbackMessage, 503, [
                'ollama_error' => $aiResult['error'] ?? 'Service Unavailable'
            ]);
        }

        $aiContent = $aiResult['content'];

        // 6. Save AI Response to Database
        $aiMsgId = $this->saveMessageToDb($pdo, [
            'user_id' => (int)$currentUser['id'],
            'pet_id' => $petId,
            'sender' => 'assistant',
            'message' => $aiContent,
            'message_type' => !empty($searchResults) ? 'web_search' : 'text',
            'sources' => !empty($searchResults) ? json_encode($searchResults) : null
        ]);

        Response::success([
            'message' => $aiContent,
            'message_id' => $aiMsgId,
            'user_message_id' => $userMsgId,
            'sender' => 'assistant',
            'sources' => $searchResults,
            'pet' => $petContext,
            'model' => $aiResult['model'] ?? 'qwen3-vl:2b',
            'elapsed_seconds' => $aiResult['elapsed'] ?? 0,
            'timestamp' => date('Y-m-d H:i:s')
        ], 'Response generated');
    }

    /**
     * POST /api/chat/image
     * Multimodal pet condition & wound analysis
     */
    public function analyzeImage(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'image' => 'required',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed: image data required', 422, $errors);
        }

        $rawImage = (string)$input['image'];
        $userPrompt = trim((string)($input['prompt'] ?? ''));
        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : null;

        // Clean Base64 format (strip data:image/...;base64, prefix if present)
        $cleanBase64 = $rawImage;
        if (preg_match('/^data:image\/(\w+);base64,/', $rawImage, $matches)) {
            $cleanBase64 = substr($rawImage, strpos($rawImage, ',') + 1);
        }

        // Validate image size (max 8MB decoded)
        $decoded = base64_decode($cleanBase64, true);
        if ($decoded === false) {
            Response::error('Invalid image base64 data', 422);
        }
        $fileSize = strlen($decoded);
        if ($fileSize > 8 * 1024 * 1024) {
            Response::error('Image file exceeds the 8 MB maximum limit', 413);
        }

        // Optionally save uploaded image to disk for user's persistent chat reference
        $imageFileName = 'chat_' . time() . '_' . bin2hex(random_bytes(6)) . '.jpg';
        $uploadDir = __DIR__ . '/../../public/uploads/chat/';
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0777, true);
        }
        file_put_contents($uploadDir . $imageFileName, $decoded);
        $imageUrl = '/uploads/chat/' . $imageFileName;

        $pdo = Database::getConnection();
        $petContext = $this->resolvePetContext($pdo, (int)$currentUser['id'], $petId);

        // Save User Message with Image Reference
        $userMsgId = $this->saveMessageToDb($pdo, [
            'user_id' => (int)$currentUser['id'],
            'pet_id' => $petId,
            'sender' => 'user',
            'message' => !empty($userPrompt) ? $userPrompt : 'Uploaded pet image for visual health assessment.',
            'image_url' => $imageUrl,
            'message_type' => 'image_upload'
        ]);

        // Call Ollama Qwen3-VL Multimodal Vision
        $aiResult = $this->ollama->analyzeImage($cleanBase64, $userPrompt, $petContext);

        if (!$aiResult['success']) {
            Response::error("AI Vision analysis failed: " . ($aiResult['error'] ?? 'Service error'), 503);
        }

        $aiContent = $aiResult['content'];

        // Extract structured metadata from response
        $parsedMeta = $this->parseVisionResponse($aiContent);

        // Save AI analysis response
        $aiMsgId = $this->saveMessageToDb($pdo, [
            'user_id' => (int)$currentUser['id'],
            'pet_id' => $petId,
            'sender' => 'assistant',
            'message' => $aiContent,
            'image_url' => $imageUrl,
            'message_type' => 'image_analysis',
            'analysis_metadata' => json_encode($parsedMeta)
        ]);

        Response::success([
            'message' => $aiContent,
            'message_id' => $aiMsgId,
            'image_url' => $imageUrl,
            'metadata' => $parsedMeta,
            'pet' => $petContext,
            'model' => $aiResult['model'] ?? 'qwen3-vl:2b',
            'elapsed_seconds' => $aiResult['elapsed'] ?? 0,
            'timestamp' => date('Y-m-d H:i:s')
        ], 'Image analysis completed');
    }

    /**
     * POST /api/nutrition
     * Comprehensive pet food, recipe, and nutrition planning
     */
    public function nutrition(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'query' => 'required',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $userQuery = trim((string)$input['query']);
        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : null;

        $pdo = Database::getConnection();
        $petContext = $this->resolvePetContext($pdo, (int)$currentUser['id'], $petId);

        $prompt = "The pet owner is asking about pet food, nutrition, or meal planning: \"{$userQuery}\"\n";
        $prompt .= "Please provide a detailed, well-structured nutrition guide including:\n";
        $prompt .= "1. Meal / Recipe suggestions\n";
        $prompt .= "2. Wholesome, safe ingredient list\n";
        $prompt .= "3. Preparation & cooking instructions (ensuring unseasoned, plain preparation)\n";
        $prompt .= "4. Recommended daily feeding frequency & portion guidance based on species and weight\n";
        $prompt .= "5. Critical toxic ingredients to avoid for this species\n";
        $prompt .= "6. Clear veterinary disclaimer advising consultation before permanently changing diet.";

        $systemPrompt = $this->buildSystemPrompt($petContext, '');

        $messages = [
            ['role' => 'system', 'content' => $systemPrompt],
            ['role' => 'user', 'content' => $prompt]
        ];

        // Save User Message
        $userMsgId = $this->saveMessageToDb($pdo, [
            'user_id' => (int)$currentUser['id'],
            'pet_id' => $petId,
            'sender' => 'user',
            'message' => $userQuery,
            'message_type' => 'nutrition'
        ]);

        $aiResult = $this->ollama->chat($messages);
        if (!$aiResult['success']) {
            Response::error("AI Nutrition generation failed: " . ($aiResult['error'] ?? 'Service error'), 503);
        }

        $aiContent = $aiResult['content'];

        $aiMsgId = $this->saveMessageToDb($pdo, [
            'user_id' => (int)$currentUser['id'],
            'pet_id' => $petId,
            'sender' => 'assistant',
            'message' => $aiContent,
            'message_type' => 'nutrition'
        ]);

        Response::success([
            'message' => $aiContent,
            'message_id' => $aiMsgId,
            'user_message_id' => $userMsgId,
            'pet' => $petContext,
            'timestamp' => date('Y-m-d H:i:s')
        ], 'Nutrition plan generated');
    }

    /**
     * POST /api/search
     * Standalone veterinary search endpoint
     */
    public function search(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'query' => 'required',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed: query required', 422, $errors);
        }

        $query = trim((string)$input['query']);
        $max = isset($input['max_results']) ? (int)$input['max_results'] : 4;

        $results = $this->searchService->search($query, $max);

        Response::success([
            'query' => $query,
            'total_results' => count($results),
            'results' => $results,
            'trusted_sources_included' => true
        ], 'Search completed');
    }

    /**
     * GET /api/chat/history
     * Retrieve persistent chat history for the user and pet
     */
    public function history(array $currentUser, array $input): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];
        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : null;
        $limit = isset($input['limit']) ? min((int)$input['limit'], 100) : 50;

        $sql = 'SELECT id, user_id, pet_id, sender, message, image_url, message_type, sources, analysis_metadata, created_at 
                FROM chat_messages 
                WHERE user_id = :uid';
        $params = [':uid' => $userId];

        if ($petId !== null) {
            $sql .= ' AND (pet_id = :pid OR pet_id IS NULL)';
            $params[':pid'] = $petId;
        }

        $sql .= ' ORDER BY id ASC LIMIT ' . $limit;

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Decode JSON fields
        foreach ($rows as &$r) {
            $r['sources'] = !empty($r['sources']) ? json_decode($r['sources'], true) : [];
            $r['analysis_metadata'] = !empty($r['analysis_metadata']) ? json_decode($r['analysis_metadata'], true) : null;
        }

        Response::success($rows, 'Chat history retrieved');
    }

    /**
     * DELETE /api/chat/history
     * Delete persistent conversation history and associated image files
     */
    public function clearHistory(array $currentUser, array $input): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];
        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : null;

        // Select and delete uploaded image files
        $imgSql = 'SELECT image_url FROM chat_messages WHERE user_id = :uid AND image_url IS NOT NULL';
        $params = [':uid' => $userId];
        if ($petId !== null) {
            $imgSql .= ' AND pet_id = :pid';
            $params[':pid'] = $petId;
        }
        $stmt = $pdo->prepare($imgSql);
        $stmt->execute($params);
        $images = $stmt->fetchAll(PDO::FETCH_COLUMN);

        foreach ($images as $imgRel) {
            $fullPath = __DIR__ . '/../../public' . $imgRel;
            if (file_exists($fullPath)) {
                @unlink($fullPath);
            }
        }

        // Delete from database
        $delSql = 'DELETE FROM chat_messages WHERE user_id = :uid';
        $delParams = [':uid' => $userId];
        if ($petId !== null) {
            $delSql .= ' AND pet_id = :pid';
            $delParams[':pid'] = $petId;
        }
        $delStmt = $pdo->prepare($delSql);
        $delStmt->execute($delParams);

        Response::success([
            'deleted_count' => $delStmt->rowCount()
        ], 'Chat history and images successfully cleared');
    }

    // ------------------------------------------------------------------------
    // Helper Methods
    // ------------------------------------------------------------------------

    private function resolvePetContext(PDO $pdo, int $userId, ?int $petId): array {
        if ($petId) {
            $stmt = $pdo->prepare('SELECT id, name, species, breed, age_years, weight_kg, gender FROM pets WHERE id = :id AND user_id = :uid LIMIT 1');
            $stmt->execute([':id' => $petId, ':uid' => $userId]);
            $pet = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($pet) {
                return $pet;
            }
        }

        // Fallback to active pet
        $stmt = $pdo->prepare('SELECT id, name, species, breed, age_years, weight_kg, gender FROM pets WHERE user_id = :uid ORDER BY is_active DESC LIMIT 1');
        $stmt->execute([':uid' => $userId]);
        $pet = $stmt->fetch(PDO::FETCH_ASSOC);

        return $pet ?: [
            'id' => null,
            'name' => 'Pet',
            'species' => 'Domestic Animal',
            'breed' => 'General',
            'age_years' => 'Adult',
            'weight_kg' => 0,
            'gender' => 'Unknown'
        ];
    }

    private function buildSystemPrompt(array $pet, string $searchContext): string {
        $prompt = "You are 'PetCare AI', a knowledgeable, compassionate, and responsible veterinary educational assistant designed for pet owners.
Active Pet Details:
- Name: {$pet['name']}
- Species: {$pet['species']}
- Breed: {$pet['breed']}
- Age: {$pet['age_years']}
- Weight: " . ($pet['weight_kg'] > 0 ? "{$pet['weight_kg']} kg" : "Unspecified") . "

CORE MEDICAL & SAFETY DIRECTIVES:
1. You provide educational guidance and preliminary triage; you are NOT a substitute for an in-person licensed veterinarian.
2. If the user describes emergency symptoms (e.g., severe bleeding, choking, difficulty breathing, seizures, sudden paralysis, heat stroke, collapse, or ingestion of toxic poisons/antifreeze), immediately instruct the user to seek emergency veterinary hospitalization.
3. Tailor all advice specifically to the pet's species ({$pet['species']}). What is safe for dogs may be lethal to cats or rabbits.
4. Never recommend human OTC drugs (e.g., Acetaminophen/Paracetamol, Ibuprofen, Aspirin) as they are frequently toxic to pets.
5. Structure answers cleanly using concise headings, bullet points, and practical advice.
\n";

        if (!empty($searchContext)) {
            $prompt .= "\n" . $searchContext . "\n";
        }

        return $prompt;
    }

    private function getRecentConversationHistory(PDO $pdo, int $userId, ?int $petId, int $limit): array {
        $sql = 'SELECT sender, message FROM chat_messages WHERE user_id = :uid';
        $params = [':uid' => $userId];
        if ($petId !== null) {
            $sql .= ' AND (pet_id = :pid OR pet_id IS NULL)';
            $params[':pid'] = $petId;
        }
        $sql .= ' ORDER BY id DESC LIMIT ' . $limit;
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
        return array_reverse($rows);
    }

    private function saveMessageToDb(PDO $pdo, array $data): int {
        $stmt = $pdo->prepare('
            INSERT INTO chat_messages (user_id, pet_id, sender, message, image_url, message_type, sources, analysis_metadata, created_at)
            VALUES (:uid, :pid, :sender, :msg, :img, :mtype, :sources, :meta, NOW())
        ');
        $stmt->execute([
            ':uid' => $data['user_id'],
            ':pid' => $data['pet_id'] ?? null,
            ':sender' => $data['sender'],
            ':msg' => $data['message'],
            ':img' => $data['image_url'] ?? null,
            ':mtype' => $data['message_type'] ?? 'text',
            ':sources' => $data['sources'] ?? null,
            ':meta' => $data['analysis_metadata'] ?? null
        ]);
        return (int)$pdo->lastInsertId();
    }

    private function parseVisionResponse(string $aiText): array {
        $meta = [
            'pet_type' => '',
            'observations' => '',
            'possible_conditions' => '',
            'severity' => 'Moderate',
            'confidence' => 'Moderate',
            'recommended_care' => '',
            'vet_recommendation' => ''
        ];

        // Flexible multi-format regex matching
        if (preg_match('/(?:Pet type|Animal identified|Animal type|Species)[:\s*-]+([^\n\r]+)/i', $aiText, $m)) {
            $meta['pet_type'] = trim($m[1], " *-\t");
        }
        if (preg_match('/(?:Visible observations?|Observations?|Physical signs?|Visible appearance)[:\s*-]+([^\n\r]+)/i', $aiText, $m)) {
            $meta['observations'] = trim($m[1], " *-\t");
        }
        if (preg_match('/(?:Possible conditions?|Potential conditions?|Differential diagnoses?|Conditions?)[:\s*-]+([^\n\r]+)/i', $aiText, $m)) {
            $meta['possible_conditions'] = trim($m[1], " *-\t");
        }
        if (preg_match('/(?:Severity(?: level)?)[:\s*-]+([^\n\r]+)/i', $aiText, $m)) {
            $meta['severity'] = trim($m[1], " *-\t");
        }
        if (preg_match('/(?:Confidence(?: level)?)[:\s*-]+([^\n\r]+)/i', $aiText, $m)) {
            $meta['confidence'] = trim($m[1], " *-\t");
        }
        if (preg_match('/(?:Recommended care|Care recommendations?|First aid|Home care)[:\s*-]+([^\n\r]+)/i', $aiText, $m)) {
            $meta['recommended_care'] = trim($m[1], " *-\t");
        }
        if (preg_match('/(?:When to visit.*|Veterinary recommendation|Seek vet care|Visit a vet)[:\s*-]+([^\n\r]+)/i', $aiText, $m)) {
            $meta['vet_recommendation'] = trim($m[1], " *-\t");
        }

        // If pet_type is still empty, infer from animal keywords
        if (empty($meta['pet_type'])) {
            if (preg_match('/\b(dog|canine|puppy|beagle|retriever|shepherd)\b/i', $aiText)) {
                $meta['pet_type'] = 'Dog / Canine';
            } elseif (preg_match('/\b(cat|feline|kitten)\b/i', $aiText)) {
                $meta['pet_type'] = 'Cat / Feline';
            } elseif (preg_match('/\b(rabbit|bunny)\b/i', $aiText)) {
                $meta['pet_type'] = 'Rabbit';
            } elseif (preg_match('/\b(bird|parrot)\b/i', $aiText)) {
                $meta['pet_type'] = 'Bird';
            }
        }

        return $meta;
    }
}
