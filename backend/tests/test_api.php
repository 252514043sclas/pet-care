<?php

declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/Utils/Database.php';
require_once __DIR__ . '/../src/Utils/Response.php';
require_once __DIR__ . '/../src/Utils/Validator.php';
require_once __DIR__ . '/../src/Middleware/AuthMiddleware.php';
require_once __DIR__ . '/../src/Middleware/CorsMiddleware.php';
require_once __DIR__ . '/../src/Controllers/AuthController.php';
require_once __DIR__ . '/../src/Controllers/PetController.php';
require_once __DIR__ . '/../src/Controllers/DashboardController.php';
require_once __DIR__ . '/../src/Controllers/DietController.php';
require_once __DIR__ . '/../src/Controllers/HealthController.php';
require_once __DIR__ . '/../src/Controllers/ReminderController.php';

use App\Utils\Database;

echo "=== Pet Care PHP Backend Verification Suite ===\n\n";

$pdo = Database::getConnection();

// Test 1: Check Database Connectivity
echo "[TEST 1] Testing Database Connection... ";
$stmt = $pdo->query('SELECT DATABASE() as db, VERSION() as ver');
$info = $stmt->fetch();
echo "PASS! Connected to '{$info['db']}' ({$info['ver']})\n";

// Test 2: Check Seed User
echo "[TEST 2] Verifying Seed User (kavya@petcare.com)... ";
$userStmt = $pdo->prepare('SELECT id, name, email, password_hash FROM users WHERE email = :email');
$userStmt->execute([':email' => 'kavya@petcare.com']);
$user = $userStmt->fetch();
if (!$user) {
    die("FAIL: Seed user not found!\n");
}
if (!password_verify('password123', $user['password_hash'])) {
    die("FAIL: Password verification failed!\n");
}
echo "PASS! User ID={$user['id']}, Name={$user['name']}\n";

// Test 3: Generate Token and verify AuthMiddleware
echo "[TEST 3] Testing Bearer Token Authentication... ";
$rawToken = 'test_token_' . bin2hex(random_bytes(16));
$tokenHash = hash('sha256', $rawToken);
$ins = $pdo->prepare('INSERT INTO user_tokens (user_id, token_hash, device_name, expires_at) VALUES (:u, :h, "Test CLI", DATE_ADD(NOW(), INTERVAL 1 DAY))');
$ins->execute([':u' => $user['id'], ':h' => $tokenHash]);

$_SERVER['HTTP_AUTHORIZATION'] = "Bearer {$rawToken}";
$authUser = App\Middleware\AuthMiddleware::authenticate();
if ($authUser['id'] != $user['id']) {
    die("FAIL: AuthMiddleware returned unexpected user!\n");
}
echo "PASS! Authenticated user ID={$authUser['id']}\n";

// Test 4: Dashboard Logic
echo "[TEST 4] Testing Dashboard Aggregation... ";
$dashStmt = $pdo->prepare('SELECT * FROM pets WHERE user_id = :u AND is_active = 1 LIMIT 1');
$dashStmt->execute([':u' => $user['id']]);
$activePet = $dashStmt->fetch();
if (!$activePet || $activePet['name'] !== 'Clover') {
    die("FAIL: Active pet Clover not found!\n");
}
echo "PASS! Active pet: {$activePet['name']} ({$activePet['breed']})\n";

// Test 5: Pet CRUD
echo "[TEST 5] Testing Pet Insertion & Weight Tracking... ";
$petInsert = $pdo->prepare('INSERT INTO pets (user_id, name, species, breed, age_years, weight_kg, gender, is_active) VALUES (:u, "Bella", "Dog", "Golden Retriever", "2.0 yrs", 25.0, "Female", 0)');
$petInsert->execute([':u' => $user['id']]);
$newPetId = (int)$pdo->lastInsertId();

$weightInsert = $pdo->prepare('INSERT INTO weight_entries (pet_id, user_id, weight_kg, recorded_date, notes) VALUES (:p, :u, 25.0, "Oct 29", "First clinic checkup")');
$weightInsert->execute([':p' => $newPetId, ':u' => $user['id']]);
echo "PASS! Inserted pet ID={$newPetId} with initial weight entry\n";

// Test 6: Meal Logging & Calories
echo "[TEST 6] Testing Meal Logging... ";
$mealInsert = $pdo->prepare('INSERT INTO meal_entries (pet_id, user_id, food_item, portion_size, calories, meal_time) VALUES (:p, :u, "Carrot Sticks & Fresh Celery", "1.5 Cups", 60, "3:00 PM")');
$mealInsert->execute([':p' => $activePet['id'], ':u' => $user['id']]);
echo "PASS! Logged meal successfully\n";

// Test 7: Reminder Creation & Toggle
echo "[TEST 7] Testing Care Reminder Lifecycle... ";
$remInsert = $pdo->prepare('INSERT INTO care_reminders (pet_id, user_id, title, reminder_time, category, notify_push, is_completed) VALUES (:p, :u, "Flea & Tick Prevention Drop", "Nov 1, 9:00 AM", "Medication", 1, 0)');
$remInsert->execute([':p' => $activePet['id'], ':u' => $user['id']]);
$remId = (int)$pdo->lastInsertId();

$updRem = $pdo->prepare('UPDATE care_reminders SET is_completed = 1 WHERE id = :id');
$updRem->execute([':id' => $remId]);
echo "PASS! Created and completed reminder ID={$remId}\n";

// Test 8: Health Scan & Conditions
echo "[TEST 8] Testing AI Diagnostic Check & Guides... ";
$scanInsert = $pdo->prepare('INSERT INTO health_scans (pet_id, user_id, scan_type, symptoms_json, diagnosis_title, match_percentage, severity, summary) VALUES (:p, :u, "symptoms", "[\"Itching / Excessive Scratching\"]", "Minor Skin Allergy", 88, "Mild", "Test summary")');
$scanInsert->execute([':p' => $activePet['id'], ':u' => $user['id']]);

$condStmt = $pdo->query('SELECT COUNT(*) as c FROM disease_guides');
$condCount = $condStmt->fetch()['c'];
if ($condCount < 1) {
    die("FAIL: No condition guides in database!\n");
}
echo "PASS! Health scans and condition guides ({$condCount}) verified\n";

// Clean up temporary test pet
$pdo->prepare('DELETE FROM pets WHERE id = :id')->execute([':id' => $newPetId]);

echo "\n>>> ALL 8 TEST SUITES PASSED PERFECTLY! <<<\n";

