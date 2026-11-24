<?php

require_once __DIR__ . '/../src/Utils/Database.php';

echo "=================================================================\n";
echo "    END-TO-END FRONTEND <-> BACKEND <-> DATABASE LIVE VALIDATION \n";
echo "=================================================================\n\n";

$pdo = \App\Utils\Database::getConnection();
$baseUrl = 'http://127.0.0.1/petcare/public/api';

function curlRequest(string $method, string $url, ?array $body = null, ?string $token = null): array {
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    $headers = [
        'Content-Type: application/json',
        'Accept: application/json',
    ];
    if ($token) {
        $headers[] = "Authorization: Bearer {$token}";
    }
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    if ($body !== null) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body));
    }
    $res = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return ['code' => $code, 'json' => json_decode($res, true), 'raw' => $res];
}

// 1. Authenticate (Frontend Login Screen)
echo "[Step 1] Simulating Frontend Login Screen (POST /api/auth/login)...\n";
$loginRes = curlRequest('POST', "$baseUrl/auth/login", [
    'email' => 'kavya@petcare.com',
    'password' => 'password123'
]);

if ($loginRes['code'] !== 200) {
    die("Error: Login failed with HTTP {$loginRes['code']}: {$loginRes['raw']}\n");
}
$token = $loginRes['json']['data']['token'];
$userId = (int)$loginRes['json']['data']['user']['id'];
echo "  -> API Response: 200 OK. Bearer Token generated.\n";

// Verify user_tokens in MySQL directly
$tokenStmt = $pdo->prepare('SELECT COUNT(*) FROM user_tokens WHERE user_id = :uid AND expires_at > NOW()');
$tokenStmt->execute([':uid' => $userId]);
$activeTokens = $tokenStmt->fetchColumn();
echo "  -> Direct MySQL Verification: Found {$activeTokens} valid token(s) in 'user_tokens' table.\n\n";

// 2. Fetch Active Pet (Pet Profile / Dashboard)
$petStmt = $pdo->prepare('SELECT id, name FROM pets WHERE user_id = :uid ORDER BY is_active DESC LIMIT 1');
$petStmt->execute([':uid' => $userId]);
$pet = $petStmt->fetch(PDO::FETCH_ASSOC);
if (!$pet) {
    die("No pet found for user {$userId}\n");
}
$petId = (int)$pet['id'];
$petName = $pet['name'];
echo "[Step 2] Active Pet in MySQL: '{$petName}' (ID: {$petId})\n\n";

// 3. Weight Tracker: Frontend Add Weight -> Backend -> MySQL
echo "[Step 3] Simulating Frontend Weight Tracker: User records 14.75 kg (POST /api/pets/{$petId}/weights)...\n";
$testWeight = 14.75;
$addWeightRes = curlRequest('POST', "$baseUrl/pets/{$petId}/weights", [
    'weight_kg' => $testWeight,
    'notes' => 'E2E Live DB Integration Test'
], $token);

if ($addWeightRes['code'] !== 200 && $addWeightRes['code'] !== 201) {
    die("Error adding weight: HTTP {$addWeightRes['code']}: {$addWeightRes['raw']}\n");
}
$insertedWeightId = (int)($addWeightRes['json']['data']['id'] ?? 0);
echo "  -> API Response: Weight ID {$insertedWeightId} created.\n";

// Direct MySQL Check
$weightCheck = $pdo->prepare('SELECT id, pet_id, weight_kg, notes, recorded_date FROM weight_entries WHERE id = :wid');
$weightCheck->execute([':wid' => $insertedWeightId]);
$dbWeightRow = $weightCheck->fetch(PDO::FETCH_ASSOC);

if ($dbWeightRow && abs((float)$dbWeightRow['weight_kg'] - $testWeight) < 0.01) {
    echo "  -> Direct MySQL Verification: CONFIRMED! Row exists in MySQL 'weight_entries':\n";
    echo "     [ID: {$dbWeightRow['id']}, Pet ID: {$dbWeightRow['pet_id']}, Weight: {$dbWeightRow['weight_kg']} kg, Notes: '{$dbWeightRow['notes']}']\n\n";
} else {
    die("Error: Weight record not found in MySQL!\n");
}

// 4. Vaccine Tracker: Frontend Add Vaccine -> Backend -> MySQL
echo "[Step 4] Simulating Frontend Vaccine Tracker: User adds 'Rabies Booster' (POST /api/pets/{$petId}/vaccinations)...\n";
$addVacRes = curlRequest('POST', "$baseUrl/pets/{$petId}/vaccinations", [
    'vaccine_name' => 'Rabies Booster Test',
    'vaccination_date' => date('Y-m-d'),
    'next_due_date' => date('Y-m-d', strtotime('+1 year')),
    'clinic_name' => 'Dr. Sharma Pet Clinic',
    'notes' => 'E2E Live DB Integration Test'
], $token);

if ($addVacRes['code'] !== 200 && $addVacRes['code'] !== 201) {
    die("Error adding vaccine: HTTP {$addVacRes['code']}: {$addVacRes['raw']}\n");
}
echo "  -> API Response: Vaccine added successfully.\n";

// Direct MySQL Check
$vacCheck = $pdo->prepare('SELECT id, vaccine_name, clinic_name, next_due_date FROM pet_vaccinations WHERE pet_id = :pid AND vaccine_name = :vname');
$vacCheck->execute([':pid' => $petId, ':vname' => 'Rabies Booster Test']);
$dbVacRow = $vacCheck->fetch(PDO::FETCH_ASSOC);

if ($dbVacRow) {
    echo "  -> Direct MySQL Verification: CONFIRMED! Row exists in MySQL 'pet_vaccinations':\n";
    echo "     [ID: {$dbVacRow['id']}, Vaccine: '{$dbVacRow['vaccine_name']}', Clinic: '{$dbVacRow['clinic_name']}', Next Due: '{$dbVacRow['next_due_date']}']\n\n";
} else {
    die("Error: Vaccine record not found in MySQL!\n");
}

// 5. Grooming Reminder: Frontend Add Reminder -> Backend -> MySQL
echo "[Step 5] Simulating Frontend Grooming Calendar: User schedules 'Full Coat Brushing' (POST /api/grooming/reminders)...\n";
$addGroomRes = curlRequest('POST', "$baseUrl/grooming/reminders", [
    'pet_id' => $petId,
    'activity' => 'Full Coat Brushing',
    'reminder_date' => date('Y-m-d', strtotime('+3 days')),
    'reminder_time' => '10:00 AM',
    'notification_enabled' => true,
    'status' => 'Pending'
], $token);

if ($addGroomRes['code'] !== 200 && $addGroomRes['code'] !== 201) {
    die("Error adding grooming reminder: HTTP {$addGroomRes['code']}: {$addGroomRes['raw']}\n");
}
$groomId = (int)($addGroomRes['json']['data']['id'] ?? 0);
echo "  -> API Response: Grooming reminder ID {$groomId} created.\n";

// Direct MySQL Check
$groomCheck = $pdo->prepare('SELECT id, activity, reminder_date, reminder_time, status FROM grooming_reminders WHERE id = :gid');
$groomCheck->execute([':gid' => $groomId]);
$dbGroomRow = $groomCheck->fetch(PDO::FETCH_ASSOC);

if ($dbGroomRow) {
    echo "  -> Direct MySQL Verification: CONFIRMED! Row exists in MySQL 'grooming_reminders':\n";
    echo "     [ID: {$dbGroomRow['id']}, Activity: '{$dbGroomRow['activity']}', Date: {$dbGroomRow['reminder_date']}, Time: '{$dbGroomRow['reminder_time']}']\n\n";
} else {
    die("Error: Grooming reminder not found in MySQL!\n");
}

// 6. Cleanup Test Entries to Keep User Data Pristine
echo "[Step 6] Cleaning up test records from MySQL...\n";
if ($insertedWeightId > 0) {
    $delWeight = curlRequest('DELETE', "$baseUrl/pets/{$petId}/weights/{$insertedWeightId}", null, $token);
    echo "  -> Deleted test weight ID {$insertedWeightId}: " . ($delWeight['code'] === 200 ? 'SUCCESS' : 'FAILED') . "\n";
}
// Clean any leftover test weights
$pdo->prepare("DELETE FROM weight_entries WHERE notes = 'E2E Live DB Integration Test'")->execute();

if ($dbVacRow) {
    $pdo->prepare('DELETE FROM pet_vaccinations WHERE id = :vid')->execute([':vid' => $dbVacRow['id']]);
    echo "  -> Cleaned test vaccine ID {$dbVacRow['id']}: SUCCESS\n";
}
if ($groomId > 0) {
    $delGroom = curlRequest('DELETE', "$baseUrl/grooming/reminders/{$groomId}", null, $token);
    echo "  -> Deleted test grooming reminder ID {$groomId}: " . ($delGroom['code'] === 200 ? 'SUCCESS' : 'FAILED') . "\n";
}

echo "\n>>> VERIFICATION COMPLETE: ALL 3 TIERS (FRONTEND API -> BACKEND PHP -> MYSQL DATABASE) ARE 100% CONNECTED AND OPERATIONAL! <<<\n";
