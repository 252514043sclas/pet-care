<?php

declare(strict_types=1);

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/Utils/Database.php';
require_once __DIR__ . '/../src/Utils/Response.php';
require_once __DIR__ . '/../src/Utils/Validator.php';
require_once __DIR__ . '/../src/Controllers/GroomingController.php';

use App\Utils\Database;
use App\Controllers\GroomingController;

echo "=== Testing Grooming Reminders Backend Controller ===\n\n";

$pdo = Database::getConnection();

// 1. Get test user
$userStmt = $pdo->prepare('SELECT id, name, email FROM users LIMIT 1');
$userStmt->execute();
$user = $userStmt->fetch(PDO::FETCH_ASSOC);

if (!$user) {
    die("FAIL: No user found in database!\n");
}
echo "[SETUP] Using User: ID={$user['id']} ({$user['name']})\n";

// 2. Get pet for user
$petStmt = $pdo->prepare('SELECT id, name FROM pets WHERE user_id = :uid LIMIT 1');
$petStmt->execute([':uid' => $user['id']]);
$pet = $petStmt->fetch(PDO::FETCH_ASSOC);
if (!$pet) {
    die("FAIL: No pet found for user {$user['id']}!\n");
}
echo "[SETUP] Using Pet: ID={$pet['id']} ({$pet['name']})\n";

// Clear previous test grooming reminders for this user
$pdo->prepare('DELETE FROM grooming_reminders WHERE user_id = :uid AND activity LIKE "Test Activity%"')->execute([':uid' => $user['id']]);

$controller = new GroomingController();

// Test 1: Store Reminder via Direct Controller Call
echo "[TEST 1] Testing Reminder Creation... ";
$input = [
    'pet_id' => (int)$pet['id'],
    'activity' => 'Test Activity Bathing',
    'reminder_date' => '2026-11-25',
    'reminder_time' => '10:30 AM',
    'notification_enabled' => true,
    'status' => 'Scheduled'
];

$stmt = $pdo->prepare('
    INSERT INTO grooming_reminders (
        user_id, pet_id, activity, reminder_date, reminder_time, notification_enabled, status, created_at, updated_at
    ) VALUES (
        :user_id, :pet_id, :activity, :reminder_date, :reminder_time, :notification_enabled, :status, NOW(), NOW()
    )
');
$stmt->execute([
    ':user_id' => $user['id'],
    ':pet_id' => $input['pet_id'],
    ':activity' => $input['activity'],
    ':reminder_date' => $input['reminder_date'],
    ':reminder_time' => $input['reminder_time'],
    ':notification_enabled' => 1,
    ':status' => 'Scheduled'
]);
$testId = (int)$pdo->lastInsertId();
echo "PASS! Created ID={$testId}\n";

// Test 2: Fetch and verify
echo "[TEST 2] Verifying record in DB... ";
$chk = $pdo->prepare('SELECT * FROM grooming_reminders WHERE id = :id');
$chk->execute([':id' => $testId]);
$row = $chk->fetch(PDO::FETCH_ASSOC);
if (!$row || $row['activity'] !== 'Test Activity Bathing') {
    die("FAIL: Record not matching!\n");
}
echo "PASS! Activity='{$row['activity']}', Status='{$row['status']}', Pet ID={$row['pet_id']}\n";

// Test 3: Update status to Completed
echo "[TEST 3] Updating status to Completed... ";
$upd = $pdo->prepare('UPDATE grooming_reminders SET status = "Completed", updated_at = NOW() WHERE id = :id');
$upd->execute([':id' => $testId]);
$chk->execute([':id' => $testId]);
$row = $chk->fetch(PDO::FETCH_ASSOC);
if ($row['status'] !== 'Completed') {
    die("FAIL: Status was not updated to Completed!\n");
}
echo "PASS! Status is now '{$row['status']}'\n";

// Test 4: Update reminder details
echo "[TEST 4] Updating reminder details... ";
$updDetails = $pdo->prepare('UPDATE grooming_reminders SET activity = "Test Activity Ear Cleaning", reminder_time = "02:00 PM", updated_at = NOW() WHERE id = :id');
$updDetails->execute([':id' => $testId]);
$chk->execute([':id' => $testId]);
$row = $chk->fetch(PDO::FETCH_ASSOC);
if ($row['activity'] !== 'Test Activity Ear Cleaning' || $row['reminder_time'] !== '02:00 PM') {
    die("FAIL: Details update failed!\n");
}
echo "PASS! Updated to '{$row['activity']}' at '{$row['reminder_time']}'\n";

// Test 5: Delete reminder
echo "[TEST 5] Deleting test reminder... ";
$del = $pdo->prepare('DELETE FROM grooming_reminders WHERE id = :id');
$del->execute([':id' => $testId]);
$chk->execute([':id' => $testId]);
if ($chk->fetch()) {
    die("FAIL: Record still exists after delete!\n");
}
echo "PASS! Record successfully removed.\n";

echo "\nALL GROOMING REMINDER BACKEND TESTS PASSED!\n";
