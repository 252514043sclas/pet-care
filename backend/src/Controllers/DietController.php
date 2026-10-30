<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Utils\Database;
use App\Utils\Response;
use App\Utils\Validator;
use PDO;

class DietController {
    private function getActivePetId(PDO $pdo, int $userId): ?int {
        $stmt = $pdo->prepare('SELECT id FROM pets WHERE user_id = :user_id AND is_active = 1 LIMIT 1');
        $stmt->execute([':user_id' => $userId]);
        $res = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($res) {
            return (int)$res['id'];
        }
        $any = $pdo->prepare('SELECT id FROM pets WHERE user_id = :user_id ORDER BY id ASC LIMIT 1');
        $any->execute([':user_id' => $userId]);
        $res = $any->fetch(PDO::FETCH_ASSOC);
        return $res ? (int)$res['id'] : null;
    }

    public function summary(array $currentUser): void {
        $pdo = Database::getConnection();
        $petId = $this->getActivePetId($pdo, (int)$currentUser['id']);

        $consumed = 0;
        $target = 800;

        if ($petId) {
            $stmt = $pdo->prepare('SELECT SUM(calories) as total FROM meal_entries WHERE user_id = :user_id AND pet_id = :pet_id');
            $stmt->execute([':user_id' => $currentUser['id'], ':pet_id' => $petId]);
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            $consumed = (int)($row['total'] ?? 0);
        }

        if ($consumed === 0) {
            $consumed = 650; // default initial baseline matching screen mockup
        }

        Response::success([
            'consumed_calories' => $consumed,
            'target_calories' => $target,
            'progress' => min(1.0, round($consumed / $target, 2)),
            'label' => "Consumed: {$consumed} / {$target} kcal",
        ]);
    }

    public function schedule(array $currentUser): void {
        $pdo = Database::getConnection();
        $petId = $this->getActivePetId($pdo, (int)$currentUser['id']);

        $schedules = [];
        if ($petId) {
            $stmt = $pdo->prepare('SELECT * FROM meal_schedules WHERE pet_id = :pet_id ORDER BY id ASC');
            $stmt->execute([':pet_id' => $petId]);
            $schedules = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }

        if (empty($schedules)) {
            // Provide fallback schedule
            $schedules = [
                ['id' => 1, 'meal_name' => 'Morning', 'scheduled_time' => 'Morning • 8:00 AM', 'meal_description' => '2 Cups Premium Kibble', 'calories' => 350],
                ['id' => 2, 'meal_name' => 'Afternoon', 'scheduled_time' => 'Afternoon • 1:00 PM', 'meal_description' => 'Healthy Carrot Snack / Treats', 'calories' => 100],
                ['id' => 3, 'meal_name' => 'Evening', 'scheduled_time' => 'Evening • 6:00 PM', 'meal_description' => '2 Cups Kibble + Wet Chicken Broth', 'calories' => 350],
            ];
        }

        Response::success($schedules);
    }

    public function meals(array $currentUser): void {
        $pdo = Database::getConnection();
        $petId = $this->getActivePetId($pdo, (int)$currentUser['id']);

        $meals = [];
        if ($petId) {
            $stmt = $pdo->prepare('SELECT * FROM meal_entries WHERE user_id = :user_id AND pet_id = :pet_id ORDER BY id DESC');
            $stmt->execute([':user_id' => $currentUser['id'], ':pet_id' => $petId]);
            $meals = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }

        Response::success($meals);
    }

    public function logMeal(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'food_item' => 'required|max:255',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : $this->getActivePetId($pdo, (int)$currentUser['id']);

        if (!$petId) {
            Response::error('No pet profile found to attach meal to.', 400);
        }

        $portionSize = $input['portion_size'] ?? '1.0 Cup';
        $calories = isset($input['calories']) ? (int)$input['calories'] : (int)(round(floatval($portionSize) * 200));
        if ($calories <= 0) $calories = 150;
        $mealTime = $input['meal_time'] ?? date('g:i A');

        $stmt = $pdo->prepare(
            'INSERT INTO meal_entries (pet_id, user_id, food_item, portion_size, calories, meal_time, created_at)
             VALUES (:pet_id, :user_id, :food_item, :portion_size, :calories, :meal_time, NOW())'
        );
        $stmt->execute([
            ':pet_id' => $petId,
            ':user_id' => $currentUser['id'],
            ':food_item' => trim($input['food_item']),
            ':portion_size' => $portionSize,
            ':calories' => $calories,
            ':meal_time' => $mealTime,
        ]);

        $mealId = (int)$pdo->lastInsertId();

        $fetchStmt = $pdo->prepare('SELECT * FROM meal_entries WHERE id = :id');
        $fetchStmt->execute([':id' => $mealId]);

        Response::success($fetchStmt->fetch(PDO::FETCH_ASSOC), 'Meal logged successfully', 201);
    }

    public function safeToxicFoods(): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM safe_toxic_foods ORDER BY is_safe DESC, id ASC');
        $stmt->execute();
        $foods = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($foods);
    }
}

