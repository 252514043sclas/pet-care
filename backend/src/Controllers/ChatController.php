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
    public const NON_PET_RESPONSE = "I'm your Pet Care AI assistant. I can help you with questions related to pets and their care.";
    public const NON_PET_IMAGE_RESPONSE = "I'm a pet care assistant, so I can only analyze pet-related images.";

    private OllamaService $ollama;
    private SearchService $searchService;

    public function __construct() {
        $this->ollama = new OllamaService();
        $this->searchService = new SearchService();
    }

    /**
     * GET /api/chat/conversations
     * List all chat conversations for the current user
     */
    public function conversations(array $currentUser): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $stmt = $pdo->prepare('
            SELECT c.id, c.user_id, c.pet_id, c.title, c.language, c.created_at, c.updated_at,
                   COUNT(m.id) as message_count,
                   (SELECT message FROM chat_messages WHERE conversation_id = c.id ORDER BY id DESC LIMIT 1) as last_message,
                   (SELECT created_at FROM chat_messages WHERE conversation_id = c.id ORDER BY id DESC LIMIT 1) as last_message_at
            FROM chat_conversations c
            LEFT JOIN chat_messages m ON m.conversation_id = c.id
            WHERE c.user_id = :uid
            GROUP BY c.id
            ORDER BY c.updated_at DESC, c.id DESC
        ');
        $stmt->execute([':uid' => $userId]);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($rows, 'Conversations retrieved');
    }

    /**
     * POST /api/chat/conversations
     * Create a new chat conversation session
     */
    public function createConversation(array $currentUser, array $input): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];
        $title = trim((string)($input['title'] ?? '')) ?: 'New Chat';
        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : null;
        $language = trim((string)($input['language'] ?? 'en'));

        $stmt = $pdo->prepare('
            INSERT INTO chat_conversations (user_id, pet_id, title, language, created_at, updated_at)
            VALUES (:uid, :pid, :title, :lang, NOW(), NOW())
        ');
        $stmt->execute([
            ':uid' => $userId,
            ':pid' => $petId,
            ':title' => $title,
            ':lang' => $language
        ]);

        $id = (int)$pdo->lastInsertId();

        Response::success([
            'id' => $id,
            'user_id' => $userId,
            'pet_id' => $petId,
            'title' => $title,
            'language' => $language,
            'message_count' => 0,
            'created_at' => date('Y-m-d H:i:s'),
            'updated_at' => date('Y-m-d H:i:s')
        ], 'Conversation created', 201);
    }

    /**
     * GET /api/chat/conversations/{id}
     * Get specific conversation details and all its messages
     */
    public function getConversation(array $currentUser, int $id): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $convStmt = $pdo->prepare('SELECT * FROM chat_conversations WHERE id = :id AND user_id = :uid');
        $convStmt->execute([':id' => $id, ':uid' => $userId]);
        $conv = $convStmt->fetch(PDO::FETCH_ASSOC);

        if (!$conv) {
            Response::notFound('Conversation not found');
            return;
        }

        $msgStmt = $pdo->prepare('
            SELECT id, conversation_id, user_id, pet_id, sender, message, image_url, message_type, sources, analysis_metadata, created_at
            FROM chat_messages
            WHERE conversation_id = :cid AND user_id = :uid
            ORDER BY id ASC
        ');
        $msgStmt->execute([':cid' => $id, ':uid' => $userId]);
        $messages = $msgStmt->fetchAll(PDO::FETCH_ASSOC);

        foreach ($messages as &$m) {
            $m['sources'] = !empty($m['sources']) ? json_decode($m['sources'], true) : [];
            $m['analysis_metadata'] = !empty($m['analysis_metadata']) ? json_decode($m['analysis_metadata'], true) : null;
        }

        $conv['messages'] = $messages;
        Response::success($conv, 'Conversation retrieved');
    }

    /**
     * DELETE /api/chat/conversations/{id}
     * Delete a conversation and all its messages
     */
    public function deleteConversation(array $currentUser, int $id): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $checkStmt = $pdo->prepare('SELECT id FROM chat_conversations WHERE id = :id AND user_id = :uid');
        $checkStmt->execute([':id' => $id, ':uid' => $userId]);
        if (!$checkStmt->fetch()) {
            Response::notFound('Conversation not found');
            return;
        }

        // Clean up any images associated with this conversation
        $imgStmt = $pdo->prepare('SELECT image_url FROM chat_messages WHERE conversation_id = :cid AND user_id = :uid AND image_url IS NOT NULL');
        $imgStmt->execute([':cid' => $id, ':uid' => $userId]);
        $images = $imgStmt->fetchAll(PDO::FETCH_COLUMN);
        foreach ($images as $imgRel) {
            $fullPath = __DIR__ . '/../../public' . $imgRel;
            if (file_exists($fullPath)) {
                @unlink($fullPath);
            }
        }

        // Delete conversation (foreign key cascade deletes chat_messages)
        $delStmt = $pdo->prepare('DELETE FROM chat_conversations WHERE id = :id AND user_id = :uid');
        $delStmt->execute([':id' => $id, ':uid' => $userId]);

        Response::success([
            'id' => $id,
            'deleted' => true
        ], 'Conversation deleted successfully');
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
        $conversationId = !empty($input['conversation_id']) ? (int)$input['conversation_id'] : null;

        $pdo = Database::getConnection();

        // Detect language
        $detectedLang = $this->detectLanguage($userMessage);

        // Resolve active pet context if available
        $petContext = $this->resolvePetContext($pdo, (int)$currentUser['id'], $petId);
        $actualPetId = $petContext['id'] ?? $petId;

        // Resolve or create conversation session
        $convTitle = 'Pet Consultation';
        if ($conversationId !== null) {
            $checkStmt = $pdo->prepare('SELECT id, title, language FROM chat_conversations WHERE id = :cid AND user_id = :uid');
            $checkStmt->execute([':cid' => $conversationId, ':uid' => (int)$currentUser['id']]);
            $conv = $checkStmt->fetch(PDO::FETCH_ASSOC);
            if (!$conv) {
                $conversationId = null;
            } else {
                $convTitle = $conv['title'];
            }
        }

        if ($conversationId === null) {
            $convTitle = $this->generateConversationTitle($userMessage);
            $createConvStmt = $pdo->prepare('
                INSERT INTO chat_conversations (user_id, pet_id, title, language, created_at, updated_at)
                VALUES (:uid, :pid, :title, :lang, NOW(), NOW())
            ');
            $createConvStmt->execute([
                ':uid' => (int)$currentUser['id'],
                ':pid' => $actualPetId,
                ':title' => $convTitle,
                ':lang' => $detectedLang
            ]);
            $conversationId = (int)$pdo->lastInsertId();
        } else {
            $updConv = $pdo->prepare('UPDATE chat_conversations SET updated_at = NOW() WHERE id = :cid');
            $updConv->execute([':cid' => $conversationId]);
        }

        // Fetch recent conversation history for this conversation
        $history = $this->getRecentConversationHistory($pdo, (int)$currentUser['id'], $actualPetId, 10, $conversationId);

        // Strict Domain Check: Restrict AI responses to pet-related topics only
        if ($this->isUnrelatedToPets($userMessage, $petContext, $history)) {
            $refusal = self::NON_PET_RESPONSE;
            $userMsgId = $this->saveMessageToDb($pdo, [
                'conversation_id' => $conversationId,
                'user_id' => (int)$currentUser['id'],
                'pet_id' => $actualPetId,
                'sender' => 'user',
                'message' => $userMessage,
                'message_type' => 'text'
            ]);
            $aiMsgId = $this->saveMessageToDb($pdo, [
                'conversation_id' => $conversationId,
                'user_id' => (int)$currentUser['id'],
                'pet_id' => $actualPetId,
                'sender' => 'assistant',
                'message' => $refusal,
                'message_type' => 'text'
            ]);

            Response::success([
                'message' => $refusal,
                'message_id' => $aiMsgId,
                'user_message_id' => $userMsgId,
                'conversation_id' => $conversationId,
                'conversation_title' => $convTitle,
                'sender' => 'assistant',
                'sources' => [],
                'pet' => $petContext,
                'model' => 'pet-domain-guard',
                'elapsed_seconds' => 0.01,
                'timestamp' => date('Y-m-d H:i:s')
            ], 'Response generated');
            return;
        }

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
        $systemPrompt = $this->buildSystemPrompt($petContext, $searchContext, $detectedLang);

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
            'conversation_id' => $conversationId,
            'user_id' => (int)$currentUser['id'],
            'pet_id' => $actualPetId,
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
                'conversation_id' => $conversationId,
                'user_id' => (int)$currentUser['id'],
                'pet_id' => $actualPetId,
                'sender' => 'assistant',
                'message' => $fallbackMessage,
                'message_type' => 'error'
            ]);

            Response::error($fallbackMessage, 503, [
                'ollama_error' => $aiResult['error'] ?? 'Service Unavailable'
            ]);
        }

        $aiContent = $aiResult['content'];

        // Normalize if the model refused due to domain restrictions
        if ($this->isAiRefusalResponse($aiContent)) {
            $aiContent = self::NON_PET_RESPONSE;
        } else {
            // Strip any accidental leading disclaimer sentence from an actual pet answer
            $aiContent = preg_replace('/^I\'m a pet care assistant[^\n.]*[.\n]*/i', '', $aiContent);
            $aiContent = trim($aiContent);
        }

        // 6. Save AI Response to Database
        $aiMsgId = $this->saveMessageToDb($pdo, [
            'conversation_id' => $conversationId,
            'user_id' => (int)$currentUser['id'],
            'pet_id' => $actualPetId,
            'sender' => 'assistant',
            'message' => $aiContent,
            'message_type' => !empty($searchResults) ? 'web_search' : 'text',
            'sources' => !empty($searchResults) ? json_encode($searchResults) : null
        ]);

        Response::success([
            'message' => $aiContent,
            'message_id' => $aiMsgId,
            'user_message_id' => $userMsgId,
            'conversation_id' => $conversationId,
            'conversation_title' => $convTitle,
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

        if ($this->isNonPetImageResponse($aiContent)) {
            $aiContent = self::NON_PET_IMAGE_RESPONSE;
            $parsedMeta = null;
        } else {
            // Extract structured metadata from response
            $parsedMeta = $this->parseVisionResponse($aiContent);
        }

        // Save AI analysis response
        $aiMsgId = $this->saveMessageToDb($pdo, [
            'user_id' => (int)$currentUser['id'],
            'pet_id' => $petContext['id'] ?? $petId,
            'sender' => 'assistant',
            'message' => $aiContent,
            'image_url' => $imageUrl,
            'message_type' => 'image_analysis',
            'analysis_metadata' => $parsedMeta ? json_encode($parsedMeta) : null
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
        $pet = null;
        if ($petId && $petId > 0) {
            $stmt = $pdo->prepare('SELECT id, name, species, breed, age_years, weight_kg, gender, health_score, vaccination_status, microchip_id FROM pets WHERE id = :id AND user_id = :uid LIMIT 1');
            $stmt->execute([':id' => $petId, ':uid' => $userId]);
            $pet = $stmt->fetch(PDO::FETCH_ASSOC);
        }

        if (!$pet) {
            // Fallback to active pet of this user (enforcing user isolation)
            $stmt = $pdo->prepare('SELECT id, name, species, breed, age_years, weight_kg, gender, health_score, vaccination_status, microchip_id FROM pets WHERE user_id = :uid AND is_active = 1 ORDER BY updated_at DESC LIMIT 1');
            $stmt->execute([':uid' => $userId]);
            $pet = $stmt->fetch(PDO::FETCH_ASSOC);
        }

        if (!$pet) {
            // Any pet belonging to this user
            $stmt = $pdo->prepare('SELECT id, name, species, breed, age_years, weight_kg, gender, health_score, vaccination_status, microchip_id FROM pets WHERE user_id = :uid ORDER BY id DESC LIMIT 1');
            $stmt->execute([':uid' => $userId]);
            $pet = $stmt->fetch(PDO::FETCH_ASSOC);
        }

        if ($pet) {
            $pid = (int)$pet['id'];
            // Fetch health records
            $hStmt = $pdo->prepare('SELECT record_type, title, description, severity, notes, recorded_date FROM pet_health_records WHERE pet_id = :pid AND user_id = :uid ORDER BY id DESC LIMIT 5');
            $hStmt->execute([':pid' => $pid, ':uid' => $userId]);
            $pet['health_records'] = $hStmt->fetchAll(PDO::FETCH_ASSOC) ?: [];

            // Fetch vaccinations
            $vStmt = $pdo->prepare('SELECT vaccine_name, vaccination_date, next_due_date FROM pet_vaccinations WHERE pet_id = :pid ORDER BY id DESC LIMIT 5');
            $vStmt->execute([':pid' => $pid]);
            $pet['vaccinations'] = $vStmt->fetchAll(PDO::FETCH_ASSOC) ?: [];

            return $pet;
        }

        return [
            'id' => null,
            'name' => 'your pet',
            'species' => 'Domestic Animal',
            'breed' => 'Unspecified',
            'age_years' => 'Unspecified',
            'weight_kg' => 0,
            'gender' => 'Unspecified',
            'health_score' => 100,
            'vaccination_status' => 'Unknown',
            'health_records' => [],
            'vaccinations' => []
        ];
    }

    private function buildSystemPrompt(array $pet, string $searchContext, string $lang = 'en'): string {
        $petName = !empty($pet['name']) ? $pet['name'] : 'the pet';
        $species = !empty($pet['species']) ? $pet['species'] : 'Domestic Animal';
        $breed = !empty($pet['breed']) ? $pet['breed'] : 'Mixed Breed';
        $age = !empty($pet['age_years']) ? $pet['age_years'] : 'Unspecified age';
        $weight = (!empty($pet['weight_kg']) && (float)$pet['weight_kg'] > 0) ? "{$pet['weight_kg']} kg" : 'Unspecified weight';
        $gender = !empty($pet['gender']) ? $pet['gender'] : 'Unspecified';
        $healthScore = $pet['health_score'] ?? 'N/A';
        $vaxStatus = $pet['vaccination_status'] ?? 'Unknown';

        $healthSummary = "None recorded";
        if (!empty($pet['health_records'])) {
            $records = [];
            foreach ($pet['health_records'] as $hr) {
                $records[] = "{$hr['record_type']} - {$hr['title']}: {$hr['description']} (Severity: {$hr['severity']})";
            }
            $healthSummary = implode('; ', $records);
        }

        $vaxSummary = "None recorded";
        if (!empty($pet['vaccinations'])) {
            $vaxes = [];
            foreach ($pet['vaccinations'] as $vx) {
                $vaxes[] = "{$vx['vaccine_name']} (Date: {$vx['vaccination_date']}, Next Due: {$vx['next_due_date']})";
            }
            $vaxSummary = implode('; ', $vaxes);
        }

        $prompt = "You are 'PetCare AI', a specialized pet care and veterinary assistant.
CURRENT SELECTED PET CONTEXT:
- Pet Name: {$petName}
- Species: {$species}
- Breed: {$breed}
- Age: {$age}
- Weight: {$weight}
- Overall Health Score: {$healthScore}/100
- Vaccination Status: {$vaxStatus}
- Medical History: {$healthSummary}
- Active Vaccinations: {$vaxSummary}

CRITICAL INSTRUCTIONS:
1. DYNAMIC PET CONTEXT (VERY IMPORTANT):
- Tailor all advice specifically to {$petName}, who is a {$species} ({$breed}), age {$age}, weight {$weight}.
- Never assume {$petName} is a dog or cat if {$petName} is a {$species}. For example, if {$petName} is a rabbit, all dietary, symptom, behavioral, and medical advice must strictly follow rabbit physiology (e.g. grass hay as the primary diet, risks of GI stasis, cecotropes, dental wear).
- Personalize the response addressing the owner and referring directly to {$petName} by name.

2. LANGUAGE RULES:
- DEFAULT LANGUAGE: SIMPLE ENGLISH. All normal responses must be written in simple, clear, easily understandable English.
- Use short paragraphs and plain explanations. Avoid complicated medical jargon or explain technical terms in simple English.
- For all regular questions (even if the user types with casual phrases or Tamil/Tanglish words), respond in simple English.
- TAMIL MEANING REQUESTS EXCEPTION: Only when the user specifically asks for the Tamil meaning of a word, phrase, or medical term (e.g., 'What is the Tamil meaning of vaccination?'), respond with the Tamil term and explanation in Tamil script (e.g., 'Vaccination என்பதன் தமிழ் பொருள் தடுப்பூசி. இது செல்லப்பிராணிகளை சில தொற்று நோய்களிலிருந்து பாதுகாக்க உதவுகிறது.').

3. CLEAR AND COMPLETE ANSWERS:
- Give a direct, helpful answer first.
- Clearly explain the reasons and possible causes.
- Provide safe, actionable care recommendations using numbered steps or bullet points.
- Detail when veterinary assistance is required and highlight emergency red-flag symptoms.
- Never claim a definitive diagnosis without an in-person veterinary examination.
- Provide a full, complete answer without trailing off.

4. NO INTERNAL THOUGHTS:
- Never narrate your internal planning or reasoning steps. Output only the final assistant response.";

        if ($lang === 'ta_meaning') {
            $prompt .= "\n\nTAMIL MEANING REQUEST: The user is specifically asking for the Tamil meaning of a term. Provide the exact Tamil word in Tamil script, explain its definition and importance for pet healthcare in Tamil.";
        } else {
            $prompt .= "\n\nLANGUAGE ENFORCEMENT: Write your response exclusively in simple, plain English.";
        }

        if (!empty($searchContext)) {
            $prompt .= "\n\nVETERINARY REFERENCE CONTEXT:\n" . $searchContext;
        }

        return $prompt;
    }

    /**
     * Detect whether user input is specifically requesting a Tamil meaning / translation
     */
    private function detectLanguage(string $text): string {
        if (preg_match('/\b(tamil meaning|meaning in tamil|tamil la enna|tamil translation|in tamil|tamil artham|tamil porul|தமிழ் பொருள்|தமிழில் என்ன)\b/iu', $text)) {
            return 'ta_meaning';
        }
        return 'en';
    }

    /**
     * Generate a concise ChatGPT-like title from the user's first prompt
     */
    private function generateConversationTitle(string $message): string {
        $clean = preg_replace('/[^\p{L}\p{N}\s]/u', '', $message);
        $clean = preg_replace('/\s+/', ' ', trim($clean));
        $words = explode(' ', $clean);
        if (empty($words) || empty($words[0])) {
            return 'New Pet Consultation';
        }
        $slice = array_slice($words, 0, 6);
        $title = implode(' ', $slice);
        if (mb_strlen($title) > 40) {
            $title = mb_substr($title, 0, 40) . '...';
        }
        return mb_convert_case(mb_substr($title, 0, 1), MB_CASE_UPPER, 'UTF-8') . mb_substr($title, 1);
    }

    /**
     * Checks if a message is unambiguously unrelated to pets or animals.
     */
    private function isUnrelatedToPets(string $message, array $petContext, array $recentHistory): bool {
        $msg = mb_strtolower(trim($message), 'UTF-8');

        // Allow polite short conversational openers
        if (in_array($msg, ['hi', 'hello', 'hey', 'good morning', 'good afternoon', 'good evening', 'help', 'start', 'vanakkam'], true)) {
            return false;
        }

        // Check if pet's name is explicitly mentioned in the message
        $petName = mb_strtolower($petContext['name'] ?? '', 'UTF-8');
        if (!empty($petName) && mb_strlen($petName) >= 2 && str_contains($msg, $petName)) {
            return false;
        }

        // Pet, animal, and veterinary vocabulary (English, Tamil, Tanglish)
        $petKeywords = [
            'pet', 'pets', 'dog', 'dogs', 'puppy', 'puppies', 'pup', 'canine', 'hound',
            'cat', 'cats', 'kitten', 'kittens', 'kitty', 'feline',
            'rabbit', 'rabbits', 'bunny', 'bunnies', 'hamster', 'hamsters', 'guinea pig', 'ferret',
            'bird', 'birds', 'parrot', 'parrots', 'budgie', 'canary', 'finch', 'cockatiel',
            'fish', 'goldfish', 'aquarium', 'turtle', 'reptile',
            'vet', 'vets', 'veterinar', 'clinic', 'animal hospital',
            'bark', 'barking', 'meow', 'meowing', 'purr', 'purring', 'tail', 'paws', 'fur', 'coat', 'claws', 'whiskers',
            'leash', 'collar', 'harness', 'litter', 'crate', 'kennel',
            'kibble', 'dog food', 'cat food', 'pet food', 'treats', 'chew', 'feeding', 'meal',
            'flea', 'fleas', 'tick', 'ticks', 'worm', 'worms', 'deworm', 'rabies', 'parvo', 'distemper', 'vaccin', 'vaccination',
            'shedding', 'groom', 'grooming', 'neuter', 'neutering', 'spay', 'spaying',
            'vomit', 'vomiting', 'diarrhea', 'letharg', 'limp', 'limping', 'scratch', 'scratching',
            'itch', 'itching', 'drool', 'drooling', 'pant', 'panting', 'dehydrat', 'hair loss', 'losing hair',
            'breed', 'pedigree', 'species', 'litterbox', 'potty', 'training', 'bite', 'biting',
            'safe food', 'toxic food', 'can eat', 'cannot eat', 'choke', 'choking', 'swallowed', 'ear infection',
            'hay', 'timothy hay', 'pellets', 'eating', 'appetite', 'not eating', 'droppings', 'teeth', 'dental', 'stasis', 'gi stasis',
            // Tamil script keywords
            'நாய்', 'நாய்க்கு', 'பூனை', 'பூனைக்கு', 'கிளி', 'பறவை', 'முயல்', 'உணவு', 'சாப்பாடு', 'தடுப்பூசி', 'மருந்து', 'வாந்தி', 'செல்லப்பிராணி', 'செல்லப்பிராணிகள்', 'காய்ச்சல்', 'நோய்', 'பராமரிப்பு',
            // Tanglish keywords
            'naai', 'poonai', 'kutti', 'kudukalam', 'kudukkalam', 'sapadu', 'saapadu', 'thaduppoosi', 'udambu', 'panradhu', 'pannalam'
        ];

        foreach ($petKeywords as $kw) {
            if (mb_strpos($msg, $kw) !== false) {
                return false;
            }
        }

        // Follow-up context check
        if (!empty($recentHistory)) {
            $followUpMarkers = [
                'he', 'she', 'him', 'her', 'it', 'his', 'hers', 'they', 'them',
                'symptom', 'vomit', 'eat', 'eating', 'food', 'medicine', 'pain', 'dose', 'portion',
                'how much', 'how long', 'is that', 'what about', 'what should i do',
                'yesterday', 'today', 'morning', 'night', 'fever', 'better', 'worse', 'still'
            ];
            foreach ($followUpMarkers as $marker) {
                if (preg_match('/\b' . preg_quote($marker, '/') . '\b/i', $msg)) {
                    return false;
                }
            }
        }

        // Patterns that clearly indicate general knowledge, programming, math, politics, trivia, or non-pet domains
        $unrelatedPatterns = [
            '/(what is|what\'s|tell me) (the )?capital of\b/i',
            '/(who is|who\'s) (the )?(president|prime minister|governor|king|queen|ceo)\b/i',
            '/who won (the )?(world cup|super bowl|election|oscar|match|game|ipl|olympics)\b/i',
            '/\b(write|create|generate|debug|explain)\s+(code|a? ?python|a? ?javascript|a? ?java|a? ?sql|a? ?c\+\+|a? ?html|a? ?css|a? ?script|a? ?function|an? ?algorithm)\b/i',
            '/\b(solve|calculate|evaluate)\s+(\d+|\bequation\b|\bmath\b|\bintegral\b)/i',
            '/how many (countries|states|continents|planets|elements)\b/i',
            '/what is the meaning of life\b/i',
            '/tell me a joke about (computers|engineers|politicians|programmers)\b/i',
            '/\b(stock price|bitcoin|cryptocurrency|crypto market|nasdaq|forex)\b/i',
            '/\b(capital of \w+|currency of \w+|president of \w+|prime minister of \w+)\b/i',
            '/who (invented|discovered)\b/i',
            '/when was [a-z0-9 ]+ (born|founded|built|released)\b/i',
            '/\b(movie review|box office|lyrics of|synopsis of)\b/i',
            '/(what is|who is|where is)\s+(the )?(weather|temperature|forecast|population of)\b/i',
            '/\b(photosynthesis|shakespeare|quantum|blockchain|machine learning|react native|flutter|database index|nginx|apache configuration)\b/i'
        ];

        foreach ($unrelatedPatterns as $pattern) {
            if (preg_match($pattern, $msg)) {
                return true;
            }
        }

        return false;
    }

    private function isAiRefusalResponse(string $text): bool {
        $trimmed = trim($text);
        // A genuine domain refusal is concise (under 160 characters). Long answers containing clinical explanations are not refusals.
        if (mb_strlen($trimmed) > 160) {
            return false;
        }
        $lower = mb_strtolower($trimmed, 'UTF-8');
        if (str_contains($lower, "i'm your pet care ai assistant") ||
            str_contains($lower, "i'm a pet care assistant") || 
            str_contains($lower, "i am a pet care assistant") ||
            (str_contains($lower, "pet care") && (str_contains($lower, "only") || str_contains($lower, "cannot")))) {
            return true;
        }
        return false;
    }

    private function isNonPetImageResponse(string $text): bool {
        $lower = mb_strtolower($text, 'UTF-8');
        if (str_contains($lower, "only analyze pet-related images") || 
            str_contains($lower, "does not appear to be a pet") ||
            str_contains($lower, "does not show a pet") ||
            str_contains($lower, "no animal or pet") ||
            (str_contains($lower, "pet care assistant") && str_contains($lower, "only analyze"))) {
            return true;
        }
        return false;
    }

    private function getRecentConversationHistory(PDO $pdo, int $userId, ?int $petId, int $limit, ?int $conversationId = null): array {
        if ($conversationId !== null) {
            $sql = 'SELECT sender, message FROM chat_messages WHERE user_id = :uid AND conversation_id = :cid ORDER BY id DESC LIMIT ' . $limit;
            $stmt = $pdo->prepare($sql);
            $stmt->execute([':uid' => $userId, ':cid' => $conversationId]);
        } else {
            $sql = 'SELECT sender, message FROM chat_messages WHERE user_id = :uid';
            $params = [':uid' => $userId];
            if ($petId !== null) {
                $sql .= ' AND (pet_id = :pid OR pet_id IS NULL)';
                $params[':pid'] = $petId;
            }
            $sql .= ' ORDER BY id DESC LIMIT ' . $limit;
            $stmt = $pdo->prepare($sql);
            $stmt->execute($params);
        }
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
        return array_reverse($rows);
    }

    private function saveMessageToDb(PDO $pdo, array $data): int {
        $stmt = $pdo->prepare('
            INSERT INTO chat_messages (conversation_id, user_id, pet_id, sender, message, image_url, message_type, sources, analysis_metadata, created_at)
            VALUES (:cid, :uid, :pid, :sender, :msg, :img, :mtype, :sources, :meta, NOW())
        ');
        $stmt->execute([
            ':cid' => $data['conversation_id'] ?? null,
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
