<?php
require_once __DIR__ . '/../src/Utils/Database.php';

use App\Utils\Database;

$pdo = Database::getConnection();

echo "=== PETS ===" . PHP_EOL;
$pets = $pdo->query('SELECT id, user_id, name, species, is_active FROM pets')->fetchAll(PDO::FETCH_ASSOC);
print_r($pets);

echo "=== MEAL SCHEDULES ===" . PHP_EOL;
$schedules = $pdo->query('SELECT id, pet_id, meal_name, scheduled_time, food_name, food_quantity, calories, is_reminder_enabled FROM meal_schedules')->fetchAll(PDO::FETCH_ASSOC);
print_r($schedules);

echo "=== MEAL ENTRIES ===" . PHP_EOL;
$entries = $pdo->query('SELECT id, pet_id, food_item, portion_size, portion_quantity, portion_unit, calories FROM meal_entries LIMIT 10')->fetchAll(PDO::FETCH_ASSOC);
print_r($entries);
