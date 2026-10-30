<?php

declare(strict_types=1);

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
use App\Controllers\ReminderController;
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
    if ($uri === '/' || $uri === '/api' || $uri === '/api/health-check') {
        Response::success([
            'status' => 'operational',
            'api' => 'Pet Care REST API',
            'version' => '1.0.0',
            'timestamp' => date('Y-m-d H:i:s'),
        ], 'API is online');
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

    if ($requestMethod === 'GET' && $uri === '/api/health/symptoms') {
        (new HealthController())->symptoms();
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

    if ($requestMethod === 'POST' && $uri === '/api/auth/logout') {
        (new AuthController())->logout($user);
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

    // Pet Weights
    if ($requestMethod === 'GET' && preg_match('#^/api/pets/(\d+)/weights$#', $uri, $matches)) {
        (new PetController())->weights($user, (int)$matches[1]);
    }

    if ($requestMethod === 'POST' && preg_match('#^/api/pets/(\d+)/weights$#', $uri, $matches)) {
        (new PetController())->addWeight($user, (int)$matches[1], $input);
    }

    // Diet & Nutrition
    if ($requestMethod === 'GET' && $uri === '/api/diet/summary') {
        (new DietController())->summary($user);
    }

    if ($requestMethod === 'GET' && $uri === '/api/diet/schedule') {
        (new DietController())->schedule($user);
    }

    if ($requestMethod === 'GET' && $uri === '/api/diet/meals') {
        (new DietController())->meals($user);
    }

    if ($requestMethod === 'POST' && $uri === '/api/diet/meals') {
        (new DietController())->logMeal($user, $input);
    }

    // Health & Diagnostics
    if ($requestMethod === 'POST' && $uri === '/api/health/scan') {
        (new HealthController())->analyze($user, $input);
    }

    if ($requestMethod === 'POST' && $uri === '/api/health/reports/save') {
        (new HealthController())->saveReport($user, $input);
    }

    if ($requestMethod === 'GET' && $uri === '/api/health/reports') {
        (new HealthController())->savedReports($user);
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

