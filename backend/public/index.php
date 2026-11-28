<?php

declare(strict_types=1);

// Prevent timeouts for local AI model generation
@set_time_limit(300);
@ini_set('max_execution_time', '300');

// Automatic class loader for App\ namespace
spl_autoload_register(function ($class) {
    $prefix = 'App\\';
    $baseDir = __DIR__ . '/../src/';

    $len = strlen($prefix);
    if (strncmp($prefix, $class, $len) !== 0) {
        return;
    }

    $relativeClass = substr($class, $len);
    $file = $baseDir . str_replace('\\', '/', $relativeClass) . '.php';

    if (file_exists($file)) {
        require_once $file;
    }
});

use App\Middleware\CorsMiddleware;
use App\Middleware\AuthMiddleware;
use App\Controllers\AuthController;
use App\Controllers\PetController;
use App\Controllers\DashboardController;
use App\Controllers\DietController;
use App\Controllers\HealthController;
use App\Controllers\PetHealthController;
use App\Controllers\ReminderController;
use App\Controllers\GroomingController;
use App\Controllers\PreferenceController;
use App\Controllers\SupportController;
use App\Controllers\ChatController;
use App\Utils\Response;

// Apply CORS
CorsMiddleware::handle();

// Parse request method and URI
$requestMethod = $_SERVER['REQUEST_METHOD'] ?? 'GET';
$rawUri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);

// Normalize URI: if it contains /api, route relative to /api
if (($apiPos = strpos($rawUri, '/api')) !== false) {
    $uri = substr($rawUri, $apiPos);
} else {
    $uri = '/' . trim($rawUri, '/');
}

// Parse JSON body if present
$rawBody = file_get_contents('php://input');
$body = [];
if (!empty($rawBody)) {
    $decoded = json_decode($rawBody, true);
    if (is_array($decoded)) {
        $body = $decoded;
    }
}
$input = array_merge($_GET, $_POST, $body);

try {
    // ------------------------------------------------------------------------
    // Public Endpoints
    // ------------------------------------------------------------------------

    // Health check / root
    if ($uri === '/' || $uri === '/api' || $uri === '/api/health-check' || $uri === '/api/health') {
        (new ChatController())->health();
    }

    // Auth Public
    if ($requestMethod === 'POST' && $uri === '/api/auth/register') {
        (new AuthController())->register($input);
    }

    if ($requestMethod === 'POST' && $uri === '/api/auth/login') {
        (new AuthController())->login($input);
    }

    if ($requestMethod === 'POST' && $uri === '/api/auth/forgot-password') {
        (new AuthController())->forgotPassword($input);
    }

    // Knowledgebase & Reference (Public / Cached)
    if ($requestMethod === 'GET' && $uri === '/api/diet/foods/safe-toxic') {
        (new DietController())->safeToxicFoods();
    }

    if ($requestMethod === 'GET' && $uri === '/api/health/conditions') {
        (new HealthController())->conditions();
    }

    if ($requestMethod === 'GET' && preg_match('#^/api/health/conditions/(\d+)$#', $uri, $matches)) {
        (new HealthController())->conditionDetail((int)$matches[1]);
    }

    if ($requestMethod === 'GET' && $uri === '/api/grooming/guides') {
        (new ReminderController())->guides();
    }

    // ------------------------------------------------------------------------
    // Authenticated Endpoints (Bearer Token Required)
    // ------------------------------------------------------------------------
    $user = AuthMiddleware::authenticate();

    // Current User
    if ($requestMethod === 'GET' && $uri === '/api/auth/me') {
        (new AuthController())->me($user);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'POST') && $uri === '/api/auth/profile') {
        (new AuthController())->updateProfile($user, $input);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'POST') && $uri === '/api/auth/password') {
        (new AuthController())->changePassword($user, $input);
    }

    if ($requestMethod === 'POST' && $uri === '/api/auth/logout') {
        (new AuthController())->logout($user);
    }

    if ($requestMethod === 'POST' && $uri === '/api/auth/avatar') {
        (new AuthController())->uploadAvatar($user, $input);
    }

    // User Preferences (Notifications, Theme, Font Size)
    if ($requestMethod === 'GET' && $uri === '/api/user/preferences') {
        (new PreferenceController())->getPreferences($user);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'POST') && $uri === '/api/user/preferences') {
        (new PreferenceController())->updatePreferences($user, $input);
    }

    // Support & Issue Reports
    if ($requestMethod === 'POST' && $uri === '/api/support/report') {
        (new SupportController())->submitReport($user, $input);
    }

    // Dashboard
    if ($requestMethod === 'GET' && $uri === '/api/dashboard') {
        (new DashboardController())->index($user);
    }

    // Pets CRUD
    if ($requestMethod === 'GET' && $uri === '/api/pets') {
        (new PetController())->index($user);
    }

    if ($requestMethod === 'POST' && $uri === '/api/pets') {
        (new PetController())->store($user, $input);
    }

    if ($requestMethod === 'GET' && preg_match('#^/api/pets/(\d+)$#', $uri, $matches)) {
        (new PetController())->show($user, (int)$matches[1]);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'PATCH') && preg_match('#^/api/pets/(\d+)$#', $uri, $matches)) {
        (new PetController())->update($user, (int)$matches[1], $input);
    }

    if ($requestMethod === 'DELETE' && preg_match('#^/api/pets/(\d+)$#', $uri, $matches)) {
        (new PetController())->destroy($user, (int)$matches[1]);
    }

    if ($requestMethod === 'POST' && preg_match('#^/api/pets/(\d+)/photo$#', $uri, $matches)) {
        (new PetController())->uploadPhoto($user, (int)$matches[1], $input);
    }

    // Pet Weights
    if ($requestMethod === 'GET' && preg_match('#^/api/pets/(\d+)/weights$#', $uri, $matches)) {
        (new PetController())->weights($user, (int)$matches[1]);
    }

    if ($requestMethod === 'POST' && preg_match('#^/api/pets/(\d+)/weights$#', $uri, $matches)) {
        (new PetController())->addWeight($user, (int)$matches[1], $input);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'PATCH') && preg_match('#^/api/pets/(\d+)/weights/(\d+)$#', $uri, $matches)) {
        (new PetController())->updateWeight($user, (int)$matches[1], (int)$matches[2], $input);
    }

    if ($requestMethod === 'DELETE' && preg_match('#^/api/pets/(\d+)/weights/(\d+)$#', $uri, $matches)) {
        (new PetController())->deleteWeight($user, (int)$matches[1], (int)$matches[2]);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'POST' || $requestMethod === 'PATCH') && preg_match('#^/api/pets/(\d+)/target-weight$#', $uri, $matches)) {
        (new PetController())->updateTargetWeight($user, (int)$matches[1], $input);
    }

    // Diet & Nutrition
    if ($requestMethod === 'GET' && $uri === '/api/diet/summary') {
        (new DietController())->summary($user, $input);
    }

    if ($requestMethod === 'POST' && $uri === '/api/diet/analyze-food') {
        (new DietController())->analyzeFood($user, $input);
    }

    if ($requestMethod === 'GET' && $uri === '/api/diet/schedule') {
        (new DietController())->schedule($user, $input);
    }

    if ($requestMethod === 'POST' && $uri === '/api/diet/schedule') {
        (new DietController())->createSchedule($user, $input);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'POST') && preg_match('#^/api/diet/schedule/(\d+)/toggle-reminder$#', $uri, $matches)) {
        (new DietController())->toggleReminder($user, (int)$matches[1]);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'POST' || $requestMethod === 'PATCH') && preg_match('#^/api/diet/schedule/(\d+)$#', $uri, $matches)) {
        (new DietController())->updateSchedule($user, (int)$matches[1], $input);
    }

    if ($requestMethod === 'DELETE' && preg_match('#^/api/diet/schedule/(\d+)$#', $uri, $matches)) {
        (new DietController())->deleteSchedule($user, (int)$matches[1]);
    }

    if ($requestMethod === 'GET' && $uri === '/api/diet/meals') {
        (new DietController())->meals($user, $input);
    }

    if ($requestMethod === 'POST' && $uri === '/api/diet/meals') {
        (new DietController())->logMeal($user, $input);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'PATCH') && preg_match('#^/api/diet/meals/(\d+)$#', $uri, $matches)) {
        (new DietController())->updateMeal($user, (int)$matches[1], $input);
    }

    if ($requestMethod === 'DELETE' && preg_match('#^/api/diet/meals/(\d+)$#', $uri, $matches)) {
        (new DietController())->deleteMeal($user, (int)$matches[1]);
    }

    // Health & Diagnostics
    if ($requestMethod === 'POST' && $uri === '/api/health/reports/save') {
        (new HealthController())->saveReport($user, $input);
    }

    if ($requestMethod === 'GET' && $uri === '/api/health/reports') {
        (new HealthController())->savedReports($user);
    }

    // Pet Dynamic Health Score & Health Records
    if ($requestMethod === 'GET' && preg_match('#^/api/pets/(\d+)/health-score$#', $uri, $matches)) {
        (new PetHealthController())->getHealthScore($user, (int)$matches[1]);
    }

    if ($requestMethod === 'GET' && preg_match('#^/api/pets/(\d+)/health-records$#', $uri, $matches)) {
        (new PetHealthController())->getHealthRecords($user, (int)$matches[1]);
    }

    if ($requestMethod === 'POST' && preg_match('#^/api/pets/(\d+)/health-records$#', $uri, $matches)) {
        (new PetHealthController())->addHealthRecord($user, (int)$matches[1], $input);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'POST' || $requestMethod === 'PATCH') && preg_match('#^/api/pets/(\d+)/health-records/(\d+)$#', $uri, $matches)) {
        (new PetHealthController())->updateHealthRecord($user, (int)$matches[1], (int)$matches[2], $input);
    }

    if ($requestMethod === 'DELETE' && preg_match('#^/api/pets/(\d+)/health-records/(\d+)$#', $uri, $matches)) {
        (new PetHealthController())->deleteHealthRecord($user, (int)$matches[1], (int)$matches[2]);
    }

    // Pet Dynamic Vaccinations & Vaccine Status
    if ($requestMethod === 'GET' && preg_match('#^/api/pets/(\d+)/vaccinations$#', $uri, $matches)) {
        (new PetHealthController())->getVaccinations($user, (int)$matches[1]);
    }

    if ($requestMethod === 'POST' && preg_match('#^/api/pets/(\d+)/vaccinations$#', $uri, $matches)) {
        (new PetHealthController())->addVaccination($user, (int)$matches[1], $input);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'POST' || $requestMethod === 'PATCH') && preg_match('#^/api/pets/(\d+)/vaccinations/(\d+)$#', $uri, $matches)) {
        (new PetHealthController())->updateVaccination($user, (int)$matches[1], (int)$matches[2], $input);
    }

    if ($requestMethod === 'DELETE' && preg_match('#^/api/pets/(\d+)/vaccinations/(\d+)$#', $uri, $matches)) {
        (new PetHealthController())->deleteVaccination($user, (int)$matches[1], (int)$matches[2]);
    }

    // Reminders & Grooming
    if ($requestMethod === 'GET' && $uri === '/api/reminders') {
        (new ReminderController())->index($user);
    }

    if ($requestMethod === 'POST' && $uri === '/api/reminders') {
        (new ReminderController())->store($user, $input);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'POST') && preg_match('#^/api/reminders/(\d+)/toggle$#', $uri, $matches)) {
        (new ReminderController())->toggle($user, (int)$matches[1]);
    }

    if ($requestMethod === 'GET' && $uri === '/api/reminders/calendar') {
        (new ReminderController())->calendar($user);
    }

    // Dynamic Grooming Calendar Reminders
    if ($requestMethod === 'GET' && $uri === '/api/grooming/reminders') {
        (new GroomingController())->index($user);
    }

    if ($requestMethod === 'POST' && $uri === '/api/grooming/reminders') {
        (new GroomingController())->store($user, $input);
    }

    if ($requestMethod === 'GET' && preg_match('#^/api/grooming/reminders/(\d+)$#', $uri, $matches)) {
        (new GroomingController())->show($user, (int)$matches[1]);
    }

    if (($requestMethod === 'PUT' || $requestMethod === 'POST' || $requestMethod === 'PATCH') && preg_match('#^/api/grooming/reminders/(\d+)$#', $uri, $matches)) {
        (new GroomingController())->update($user, (int)$matches[1], $input);
    }

    if (($requestMethod === 'PATCH' || $requestMethod === 'PUT' || $requestMethod === 'POST') && preg_match('#^/api/grooming/reminders/(\d+)/status$#', $uri, $matches)) {
        (new GroomingController())->updateStatus($user, (int)$matches[1], $input);
    }

    if ($requestMethod === 'DELETE' && preg_match('#^/api/grooming/reminders/(\d+)$#', $uri, $matches)) {
        (new GroomingController())->destroy($user, (int)$matches[1]);
    }

    // ------------------------------------------------------------------------
    // AI Chatbot, Multimodal Vision, Nutrition & Search (Qwen3-VL 2B)
    // ------------------------------------------------------------------------
    if ($requestMethod === 'GET' && $uri === '/api/chat/conversations') {
        (new ChatController())->conversations($user);
    }

    if ($requestMethod === 'POST' && $uri === '/api/chat/conversations') {
        (new ChatController())->createConversation($user, $input);
    }

    if ($requestMethod === 'GET' && preg_match('#^/api/chat/conversations/(\d+)$#', $uri, $matches)) {
        (new ChatController())->getConversation($user, (int)$matches[1]);
    }

    if ($requestMethod === 'DELETE' && preg_match('#^/api/chat/conversations/(\d+)$#', $uri, $matches)) {
        (new ChatController())->deleteConversation($user, (int)$matches[1]);
    }

    if ($requestMethod === 'POST' && $uri === '/api/chat') {
        (new ChatController())->chat($user, $input);
    }

    if ($requestMethod === 'POST' && $uri === '/api/chat/image') {
        (new ChatController())->analyzeImage($user, $input);
    }

    if ($requestMethod === 'POST' && $uri === '/api/nutrition') {
        (new ChatController())->nutrition($user, $input);
    }

    if ($requestMethod === 'POST' && $uri === '/api/search') {
        (new ChatController())->search($user, $input);
    }

    if ($requestMethod === 'GET' && $uri === '/api/chat/history') {
        (new ChatController())->history($user, $input);
    }

    if ($requestMethod === 'DELETE' && $uri === '/api/chat/history') {
        (new ChatController())->clearHistory($user, $input);
    }

    // Route not found
    Response::notFound("Endpoint not found: {$requestMethod} {$uri}");

} catch (\Throwable $e) {
    Response::error(
        'Internal Server Error: ' . $e->getMessage(),
        500,
        [
            'file' => basename($e->getFile()),
            'line' => $e->getLine(),
        ]
    );
}

