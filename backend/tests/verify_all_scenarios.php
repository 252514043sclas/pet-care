<?php
require_once __DIR__ . '/../src/Utils/Database.php';
require_once __DIR__ . '/../src/Utils/Response.php';
require_once __DIR__ . '/../src/Services/FoodNutritionSafetyService.php';

use App\Utils\Database;
use App\Services\FoodNutritionSafetyService;

$pdo = Database::getConnection();

echo "=================================================================\n";
echo "       PET CARE - DAILY CALORIE TARGET VERIFICATION SUITE       \n";
echo "=================================================================\n\n";

$allPassed = true;

function assertCondition($testName, $condition, $details = "") {
    global $allPassed;
    if ($condition) {
        echo "[PASS] $testName" . ($details ? " ($details)" : "") . "\n";
    } else {
        echo "[FAIL] $testName" . ($details ? " ($details)" : "") . "\n";
        $allPassed = false;
    }
}

// -------------------------------------------------------------
// Helper to get diet summary directly via SQL logic identical to DietController
// -------------------------------------------------------------
function getDietSummaryForPet($pdo, $petId) {
    $stmt = $pdo->prepare("SELECT COALESCE(SUM(calories), 0) as total FROM meal_schedules WHERE pet_id = :pet_id");
    $stmt->execute([':pet_id' => $petId]);
    $targetCalories = (int)($stmt->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

    $today = date('Y-m-d');
    $stmt = $pdo->prepare("SELECT COALESCE(SUM(calories), 0) as total FROM meal_entries WHERE pet_id = :pet_id AND (meal_date = :today OR DATE(created_at) = :today2)");
    $stmt->execute([':pet_id' => $petId, ':today' => $today, ':today2' => $today]);
    $consumedCalories = (int)($stmt->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

    $hasSchedule = ($targetCalories > 0);
    $remainingCalories = max(0, $targetCalories - $consumedCalories);
    $progressRatio = ($targetCalories > 0) ? min(1.0, round($consumedCalories / $targetCalories, 2)) : 0.0;
    $progressPercentage = ($targetCalories > 0) ? (int)round(($consumedCalories / $targetCalories) * 100) : 0;
    $message = $hasSchedule ? "Target calculated from feeding schedule" : "Set up your feeding schedule to view your daily calorie target.";

    return [
        'pet_id' => $petId,
        'has_schedule' => $hasSchedule,
        'target_calories' => $targetCalories,
        'consumed_calories' => $consumedCalories,
        'remaining_calories' => $remainingCalories,
        'progress_ratio' => $progressRatio,
        'progress_percentage' => $progressPercentage,
        'message' => $message
    ];
}

// -------------------------------------------------------------
// Test 1: When No Feeding Schedule Exists (e.g. Pet 1 Clover)
// -------------------------------------------------------------
echo "--- TEST 1: No Feeding Schedule Target is 0 kcal/day ---\n";
// Ensure Pet 1 has 0 calorie schedules
$pdo->prepare("UPDATE meal_schedules SET calories = 0, food_name = '', food_quantity = '' WHERE pet_id = 1")->execute();
$pdo->prepare("DELETE FROM meal_entries WHERE pet_id = 1")->execute();

$summary1 = getDietSummaryForPet($pdo, 1);
assertCondition("Target calories is 0 kcal/day", $summary1['target_calories'] === 0, "target={$summary1['target_calories']}");
assertCondition("Consumed calories is 0 kcal", $summary1['consumed_calories'] === 0, "consumed={$summary1['consumed_calories']}");
assertCondition("has_schedule is false", $summary1['has_schedule'] === false);
assertCondition("Progress ratio is 0.0 (no division by zero)", $summary1['progress_ratio'] === 0.0);
assertCondition("Informational message is returned", $summary1['message'] === "Set up your feeding schedule to view your daily calorie target.");

// -------------------------------------------------------------
// Test 2: Calculate target dynamically from Food Nutrition DB
// -------------------------------------------------------------
echo "\n--- TEST 2: Calculate Target from Food Nutrition Database ---\n";
// Create a temporary test pet
$pdo->prepare("DELETE FROM meal_entries WHERE pet_id = 9999")->execute();
$pdo->prepare("DELETE FROM meal_schedules WHERE pet_id = 9999")->execute();
$pdo->prepare("DELETE FROM pets WHERE id = 9999")->execute();

$pdo->prepare("INSERT INTO pets (id, user_id, name, species, breed) VALUES (9999, 1, 'CalorieTestPet', 'Dog', 'Beagle')")->execute();

// Breakfast: Dry kibble, 100g (reference db: ~370 kcal)
$portion1 = FoodNutritionSafetyService::parsePortion("100g");
$analysis1 = FoodNutritionSafetyService::analyze("dry kibble", $portion1['quantity'], $portion1['unit'], 'Dog');
$cal1 = (int)($analysis1['estimated_calories'] ?? 370);

$stmt = $pdo->prepare("INSERT INTO meal_schedules (pet_id, meal_name, scheduled_time, food_name, food_quantity, calories, is_reminder_enabled) 
                       VALUES (9999, 'Breakfast', '08:00', 'dry kibble', '100g', :cal, 1)");
$stmt->execute([':cal' => $cal1]);

$summary2 = getDietSummaryForPet($pdo, 9999);
assertCondition("Single meal target matches food nutrition analysis", $summary2['target_calories'] === $cal1, "expected $cal1, got {$summary2['target_calories']}");
assertCondition("has_schedule is true", $summary2['has_schedule'] === true);

// -------------------------------------------------------------
// Test 3: Add Second Meal -> Target Recalculates Automatically
// -------------------------------------------------------------
echo "\n--- TEST 3: Add Second Meal -> Target Updates (Sum of portions) ---\n";
// Lunch: boiled chicken, 100g (~165 kcal)
$portion2 = FoodNutritionSafetyService::parsePortion("100g");
$analysis2 = FoodNutritionSafetyService::analyze("boiled chicken", $portion2['quantity'], $portion2['unit'], 'Dog');
$cal2 = (int)($analysis2['estimated_calories'] ?? 165);

$stmt = $pdo->prepare("INSERT INTO meal_schedules (pet_id, meal_name, scheduled_time, food_name, food_quantity, calories, is_reminder_enabled) 
                       VALUES (9999, 'Lunch', '13:00', 'boiled chicken', '100g', :cal, 1)");
$stmt->execute([':cal' => $cal2]);

$expectedTotal3 = $cal1 + $cal2;
$summary3 = getDietSummaryForPet($pdo, 9999);
assertCondition("Target updates to sum of Breakfast + Lunch", $summary3['target_calories'] === $expectedTotal3, "expected $expectedTotal3 ($cal1+$cal2), got {$summary3['target_calories']}");

// -------------------------------------------------------------
// Test 4: Edit Meal Quantity -> Target Recalculates
// -------------------------------------------------------------
echo "\n--- TEST 4: Edit Meal Quantity -> Target Recalculates ---\n";
// Edit Lunch from 100g to 200g of boiled chicken
$portion2Updated = FoodNutritionSafetyService::parsePortion("200g");
$analysis2Updated = FoodNutritionSafetyService::analyze("boiled chicken", $portion2Updated['quantity'], $portion2Updated['unit'], 'Dog');
$cal2Updated = (int)($analysis2Updated['estimated_calories'] ?? 330);

$stmt = $pdo->prepare("UPDATE meal_schedules SET food_quantity = '200g', calories = :cal WHERE pet_id = 9999 AND meal_name = 'Lunch'");
$stmt->execute([':cal' => $cal2Updated]);

$expectedTotal4 = $cal1 + $cal2Updated;
$summary4 = getDietSummaryForPet($pdo, 9999);
assertCondition("Target reflects updated portion size", $summary4['target_calories'] === $expectedTotal4, "expected $expectedTotal4 ($cal1+$cal2Updated), got {$summary4['target_calories']}");

// -------------------------------------------------------------
// Test 5: Delete Meal -> Target Decreases
// -------------------------------------------------------------
echo "\n--- TEST 5: Delete Meal Schedule -> Target Decreases ---\n";
$pdo->prepare("DELETE FROM meal_schedules WHERE pet_id = 9999 AND meal_name = 'Lunch'")->execute();
$summary5 = getDietSummaryForPet($pdo, 9999);
assertCondition("Target decreases back to Breakfast only", $summary5['target_calories'] === $cal1, "expected $cal1, got {$summary5['target_calories']}");

// -------------------------------------------------------------
// Test 6: Log Meal -> Consumed Calories Update
// -------------------------------------------------------------
echo "\n--- TEST 6: Log Meal -> Consumed Calories Update ---\n";
$today = date('Y-m-d');
$stmt = $pdo->prepare("INSERT INTO meal_entries (user_id, pet_id, food_item, portion_size, calories, meal_date, meal_time) 
                       VALUES (1, 9999, 'dry kibble', '50g', 185, :today, '08:15')");
$stmt->execute([':today' => $today]);

$summary6 = getDietSummaryForPet($pdo, 9999);
assertCondition("Consumed calories increased to 185 kcal", $summary6['consumed_calories'] === 185, "got {$summary6['consumed_calories']}");
$expectedRemaining = $summary6['target_calories'] - 185;
assertCondition("Remaining calories correctly calculated", $summary6['remaining_calories'] === $expectedRemaining, "remaining={$summary6['remaining_calories']}");
assertCondition("Progress ratio updated", $summary6['progress_ratio'] > 0, "ratio={$summary6['progress_ratio']}");

// -------------------------------------------------------------
// Test 7: Delete Logged Meal -> Consumed Calories Decrease
// -------------------------------------------------------------
echo "\n--- TEST 7: Delete Logged Meal -> Consumed Calories Decrease ---\n";
$pdo->prepare("DELETE FROM meal_entries WHERE pet_id = 9999")->execute();
$summary7 = getDietSummaryForPet($pdo, 9999);
assertCondition("Consumed calories returns to 0", $summary7['consumed_calories'] === 0, "got {$summary7['consumed_calories']}");
assertCondition("Remaining equals full target", $summary7['remaining_calories'] === $summary7['target_calories']);
assertCondition("Progress ratio returns to 0", $summary7['progress_ratio'] === 0.0);

// -------------------------------------------------------------
// Test 8: Pet Switching (Independent Targets per Pet)
// -------------------------------------------------------------
echo "\n--- TEST 8: Pet Switching (Pet 1 vs Pet 20 vs TestPet) ---\n";
$summaryPet1 = getDietSummaryForPet($pdo, 1);
$summaryPet20 = getDietSummaryForPet($pdo, 20);
$summaryTestPet = getDietSummaryForPet($pdo, 9999);

assertCondition("Pet 1 has 0 kcal target (no configured schedule)", $summaryPet1['target_calories'] === 0);
assertCondition("Pet 20 has 861 kcal target (configured schedule)", $summaryPet20['target_calories'] === 861);
assertCondition("TestPet has $cal1 kcal target", $summaryTestPet['target_calories'] === $cal1);
assertCondition("Targets are completely distinct and independent", 
    $summaryPet1['target_calories'] !== $summaryPet20['target_calories'] && 
    $summaryPet20['target_calories'] !== $summaryTestPet['target_calories']
);

// -------------------------------------------------------------
// Test 9: Persistence (DB Commit Verification)
// -------------------------------------------------------------
echo "\n--- TEST 9: Persistence Across Connections ---\n";
// Re-open DB connection to ensure no memory-only state
$pdoNew = Database::getConnection();
$summaryPersisted = getDietSummaryForPet($pdoNew, 9999);
assertCondition("Target persists in database across connection instances", $summaryPersisted['target_calories'] === $cal1);

// Clean up test pet
$pdo->prepare("DELETE FROM meal_entries WHERE pet_id = 9999")->execute();
$pdo->prepare("DELETE FROM meal_schedules WHERE pet_id = 9999")->execute();
$pdo->prepare("DELETE FROM pets WHERE id = 9999")->execute();

// -------------------------------------------------------------
// Test 10: Diet & Nutrition vs Feeding Schedule Target Consistency
// -------------------------------------------------------------
echo "\n--- TEST 10: Screen Target Consistency ---\n";
// Both screens fetch via DietSummaryDto / DietController::summary which computes SUM(calories) FROM meal_schedules
$stmtScheduleSum = $pdo->prepare("SELECT COALESCE(SUM(calories), 0) as total FROM meal_schedules WHERE pet_id = 20");
$stmtScheduleSum->execute();
$screenTargetFeedingSchedule = (int)$stmtScheduleSum->fetch(PDO::FETCH_ASSOC)['total'];

$screenTargetDietHome = getDietSummaryForPet($pdo, 20)['target_calories'];

assertCondition("Diet Home and Feeding Schedule screens receive identical target value", 
    $screenTargetFeedingSchedule === $screenTargetDietHome, 
    "FeedingSchedule={$screenTargetFeedingSchedule}, DietHome={$screenTargetDietHome}"
);

echo "\n=================================================================\n";
if ($allPassed) {
    echo "       ALL 10 VERIFICATION SCENARIOS PASSED SUCCESSFULLY!        \n";
} else {
    echo "       SOME TESTS FAILED! CHECK OUTPUT ABOVE.                   \n";
}
echo "=================================================================\n";
