<?php
require_once __DIR__ . '/../src/Utils/Database.php';
require_once __DIR__ . '/../src/Services/FoodNutritionSafetyService.php';

use App\Utils\Database;
use App\Services\FoodNutritionSafetyService;

$pdo = Database::getConnection();

// Update default column value in MySQL
$pdo->exec("ALTER TABLE meal_schedules MODIFY COLUMN calories INT NOT NULL DEFAULT 0");

// Recalculate any existing row that has a food_name but calories == 0
$stmt = $pdo->query("SELECT m.id, m.pet_id, m.food_name, m.food_quantity, p.species 
                     FROM meal_schedules m 
                     JOIN pets p ON m.pet_id = p.id 
                     WHERE m.food_name IS NOT NULL AND TRIM(m.food_name) != '' AND m.calories = 0");
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

echo "Found " . count($rows) . " rows with food configured but 0 calories." . PHP_EOL;

foreach ($rows as $row) {
    $parsed = FoodNutritionSafetyService::parsePortion($row['food_quantity']);
    $analysis = FoodNutritionSafetyService::analyze($row['food_name'], $parsed['quantity'], $parsed['unit'], $row['species']);
    $kcal = (int)($analysis['estimated_calories'] ?? 0);
    if ($kcal > 0) {
        $up = $pdo->prepare("UPDATE meal_schedules SET calories = :cal WHERE id = :id");
        $up->execute([':cal' => $kcal, ':id' => $row['id']]);
        echo "Updated row {$row['id']} ({$row['food_name']}, {$row['food_quantity']}) -> {$kcal} kcal" . PHP_EOL;
    }
}

echo "Done." . PHP_EOL;
