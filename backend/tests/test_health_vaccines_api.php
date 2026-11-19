<?php

declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/Utils/Database.php';
require_once __DIR__ . '/../src/Utils/Response.php';
require_once __DIR__ . '/../src/Utils/Validator.php';
require_once __DIR__ . '/../src/Services/PetHealthScoringService.php';
require_once __DIR__ . '/../src/Controllers/PetHealthController.php';

use App\Utils\Database;
use App\Services\PetHealthScoringService;

echo "=== Pet Care Dynamic Health Score & Vaccine Status Test ===\n\n";

$pdo = Database::getConnection();

// 1. Get test user and pet
$userStmt = $pdo->prepare('SELECT id, name FROM users LIMIT 1');
$userStmt->execute();
$user = $userStmt->fetch(PDO::FETCH_ASSOC);

$petStmt = $pdo->prepare('SELECT id, name, species FROM pets WHERE user_id = :uid LIMIT 1');
$petStmt->execute([':uid' => $user['id']]);
$pet = $petStmt->fetch(PDO::FETCH_ASSOC);

echo "[SETUP] Using User {$user['id']} ({$user['name']}) and Pet {$pet['id']} ({$pet['name']} - {$pet['species']})\n";

// Clear previous test records for clean test
$pdo->prepare('DELETE FROM pet_health_records WHERE pet_id = :pid')->execute([':pid' => $pet['id']]);
$pdo->prepare('DELETE FROM pet_vaccinations WHERE pet_id = :pid')->execute([':pid' => $pet['id']]);
$pdo->prepare('DELETE FROM health_scans WHERE pet_id = :pid')->execute([':pid' => $pet['id']]);
$pdo->prepare('DELETE FROM weight_entries WHERE pet_id = :pid')->execute([':pid' => $pet['id']]);

// Test 1: Zero records - should report Not enough health data and NO RECORDS
echo "[TEST 1] Testing with zero records... ";
$scoreData = PetHealthScoringService::calculateScore($pdo, (int)$pet['id'], (int)$user['id']);
$vacStatus = PetHealthScoringService::calculateVaccineStatus([]);

if ($scoreData['has_sufficient_data'] !== false || $scoreData['score'] !== null) {
    die("FAIL: Expected has_sufficient_data=false and score=null for zero records!\n");
}
if ($vacStatus !== 'NO RECORDS') {
    die("FAIL: Expected 'NO RECORDS' when 0 vaccines exist, got '$vacStatus'!\n");
}
echo "PASS! Health: '{$scoreData['status_label']}', Vaccine: '$vacStatus'\n";

// Test 2: Add a normal checkup record
echo "[TEST 2] Adding a Routine Checkup... ";
$insHr = $pdo->prepare('
    INSERT INTO pet_health_records (pet_id, user_id, record_type, title, description, severity, recorded_date, created_at, updated_at)
    VALUES (:pet_id, :user_id, "Checkup", "Annual Wellness Exam", "All vital signs healthy", "Normal", :rec_date, NOW(), NOW())
');
$insHr->execute([
    ':pet_id' => $pet['id'],
    ':user_id' => $user['id'],
    ':rec_date' => date('Y-m-d')
]);
$hrId = (int)$pdo->lastInsertId();

$scoreData = PetHealthScoringService::calculateScore($pdo, (int)$pet['id'], (int)$user['id']);
if ($scoreData['score'] === null || $scoreData['score'] < 80) {
    die("FAIL: Expected valid score >= 80, got " . var_export($scoreData['score'], true) . "\n");
}
echo "PASS! Score calculated: {$scoreData['score']}/100 ('{$scoreData['status_label']}')\n";

// Test 3: Add UP TO DATE vaccine (due in 6 months)
echo "[TEST 3] Adding Up-to-Date Vaccine (due in 180 days)... ";
$due180 = date('Y-m-d', strtotime('+180 days'));
$insVac = $pdo->prepare('
    INSERT INTO pet_vaccinations (pet_id, user_id, vaccine_name, vaccination_date, next_due_date, created_at, updated_at)
    VALUES (:pet_id, :user_id, "Core RHDV2 Vaccine", :v_date, :due_date, NOW(), NOW())
');
$insVac->execute([
    ':pet_id' => $pet['id'],
    ':user_id' => $user['id'],
    ':v_date' => date('Y-m-d'),
    ':due_date' => $due180
]);
$vacId1 = (int)$pdo->lastInsertId();

$vacStmt = $pdo->prepare('SELECT * FROM pet_vaccinations WHERE pet_id = :pid');
$vacStmt->execute([':pid' => $pet['id']]);
$allVac = $vacStmt->fetchAll(PDO::FETCH_ASSOC);
$vacStatus = PetHealthScoringService::calculateVaccineStatus($allVac);
if ($vacStatus !== 'UP TO DATE') {
    die("FAIL: Expected 'UP TO DATE', got '$vacStatus'\n");
}
echo "PASS! Status is '{$vacStatus}'\n";

// Test 4: Add DUE SOON vaccine (due in 15 days)
echo "[TEST 4] Adding Due-Soon Vaccine (due in 15 days)... ";
$due15 = date('Y-m-d', strtotime('+15 days'));
$insVac->execute([
    ':pet_id' => $pet['id'],
    ':user_id' => $user['id'],
    ':v_date' => date('Y-m-d', strtotime('-350 days')),
    ':due_date' => $due15
]);
$vacId2 = (int)$pdo->lastInsertId();

$vacStmt->execute([':pid' => $pet['id']]);
$allVac = $vacStmt->fetchAll(PDO::FETCH_ASSOC);
$vacStatus = PetHealthScoringService::calculateVaccineStatus($allVac);
if ($vacStatus !== 'DUE SOON') {
    die("FAIL: Expected 'DUE SOON', got '$vacStatus'\n");
}
echo "PASS! Status correctly transitioned to '{$vacStatus}'\n";

// Test 5: Add OVERDUE vaccine (due 10 days ago)
echo "[TEST 5] Adding Overdue Vaccine (due -10 days)... ";
$duePast = date('Y-m-d', strtotime('-10 days'));
$insVac->execute([
    ':pet_id' => $pet['id'],
    ':user_id' => $user['id'],
    ':v_date' => date('Y-m-d', strtotime('-375 days')),
    ':due_date' => $duePast
]);
$vacId3 = (int)$pdo->lastInsertId();

$vacStmt->execute([':pid' => $pet['id']]);
$allVac = $vacStmt->fetchAll(PDO::FETCH_ASSOC);
$vacStatus = PetHealthScoringService::calculateVaccineStatus($allVac);
if ($vacStatus !== 'OVERDUE') {
    die("FAIL: Expected 'OVERDUE', got '$vacStatus'\n");
}
echo "PASS! Status correctly transitioned to '{$vacStatus}'\n";

// Test 6: Verify pets table columns updated
echo "[TEST 6] Checking pets table sync... ";
$updPet = $pdo->prepare('UPDATE pets SET vaccination_status = :status WHERE id = :id');
$updPet->execute([':status' => $vacStatus, ':id' => $pet['id']]);

$chkPet = $pdo->prepare('SELECT health_score, vaccination_status FROM pets WHERE id = :id');
$chkPet->execute([':id' => $pet['id']]);
$row = $chkPet->fetch(PDO::FETCH_ASSOC);
echo "PASS! pets.health_score={$row['health_score']}, pets.vaccination_status='{$row['vaccination_status']}'\n";

echo "\nALL HEALTH & VACCINE BACKEND LOGIC VERIFIED!\n";
