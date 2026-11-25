<?php
require_once __DIR__ . '/../src/Utils/Database.php';
$pdo = \App\Utils\Database::getConnection();

echo "=== PETS ===\n";
$stmt = $pdo->query("SELECT id, user_id, name, species FROM pets");
$pets = $stmt->fetchAll(PDO::FETCH_ASSOC);
print_r($pets);

echo "\n=== MEAL SCHEDULES PER PET ===\n";
$stmt = $pdo->query("SELECT pet_id, count(*) as count, sum(calories) as total_cal FROM meal_schedules GROUP BY pet_id");
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));

echo "\n=== ALL MEAL SCHEDULES ===\n";
$stmt = $pdo->query("SELECT id, pet_id, meal_name, food_name, food_quantity, calories FROM meal_schedules");
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
