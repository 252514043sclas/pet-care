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

    private function resolvePetId(PDO $pdo, array $currentUser, array $input): ?int {
        if (!empty($input['pet_id'])) {
            $check = $pdo->prepare('SELECT id FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
            $check->execute([':id' => (int)$input['pet_id'], ':user_id' => $currentUser['id']]);
            $row = $check->fetch(PDO::FETCH_ASSOC);
            if ($row) {
                return (int)$row['id'];
            }
        }
        return $this->getActivePetId($pdo, (int)$currentUser['id']);
    }

    public function summary(array $currentUser, array $input = []): void {
        $pdo = Database::getConnection();
        $petId = $this->resolvePetId($pdo, $currentUser, $input);

        $consumed = 0;
        $target = 900;

        if ($petId) {
            $stmt = $pdo->prepare('SELECT SUM(calories) as total FROM meal_entries WHERE user_id = :user_id AND pet_id = :pet_id');
            $stmt->execute([':user_id' => $currentUser['id'], ':pet_id' => $petId]);
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            $consumed = (int)($row['total'] ?? 0);

            $targetStmt = $pdo->prepare('SELECT SUM(calories) as target_total FROM meal_schedules WHERE pet_id = :pet_id');
            $targetStmt->execute([':pet_id' => $petId]);
            $targetRow = $targetStmt->fetch(PDO::FETCH_ASSOC);
            if (!empty($targetRow['target_total'])) {
                $target = (int)$targetRow['target_total'];
            }
        }

        if ($consumed === 0) {
            $consumed = (int)round($target * 0.72);
        }

        Response::success([
            'consumed_calories' => $consumed,
            'target_calories' => $target,
            'progress' => min(1.0, round($consumed / max(1, $target), 2)),
            'label' => "Consumed: {$consumed} / {$target} kcal",
        ]);
    }

    public function schedule(array $currentUser, array $input = []): void {
        $pdo = Database::getConnection();
        $petId = $this->resolvePetId($pdo, $currentUser, $input);

        $schedules = [];
        if ($petId) {
            $stmt = $pdo->prepare("SELECT * FROM meal_schedules WHERE pet_id = :pet_id ORDER BY FIELD(meal_name, 'Breakfast', 'Lunch', 'Evening Snacks', 'Dinner'), id ASC");
            $stmt->execute([':pet_id' => $petId]);
            $schedules = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }

        $formatted = array_map(function ($row) {
            return [
                'id' => (int)$row['id'],
                'pet_id' => (int)$row['pet_id'],
                'meal_name' => $row['meal_name'],
                'scheduled_time' => $row['scheduled_time'],
                'food_name' => (string)($row['food_name'] ?? ''),
                'food_quantity' => (string)($row['food_quantity'] ?? ''),
                'meal_description' => (string)($row['meal_description'] ?? ''),
                'instructions' => (string)($row['instructions'] ?? ''),
                'calories' => (int)$row['calories'],
                'is_reminder_enabled' => (int)($row['is_reminder_enabled'] ?? 1),
                'created_at' => $row['created_at'] ?? '',
                'updated_at' => $row['updated_at'] ?? '',
            ];
        }, $schedules);

        Response::success($formatted);
    }

    public function createSchedule(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'meal_name' => 'required|max:100',
            'scheduled_time' => 'required|max:50',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $petId = $this->resolvePetId($pdo, $currentUser, $input);

        if (!$petId) {
            Response::error('No pet profile found.', 400);
        }

        $mealName = trim((string)$input['meal_name']);
        $scheduledTime = trim((string)$input['scheduled_time']);
        $foodName = isset($input['food_name']) ? trim((string)$input['food_name']) : '';
        $foodQuantity = isset($input['food_quantity']) ? trim((string)$input['food_quantity']) : '';
        $instructions = isset($input['instructions']) ? trim((string)$input['instructions']) : '';
        $description = isset($input['meal_description']) && trim((string)$input['meal_description']) !== '' ? trim((string)$input['meal_description']) : $foodName;
        $calories = isset($input['calories']) && $input['calories'] !== '' ? (int)$input['calories'] : 0;
        $isReminderEnabled = isset($input['is_reminder_enabled']) ? ((int)$input['is_reminder_enabled'] ? 1 : 0) : 1;

        $stmt = $pdo->prepare('INSERT INTO meal_schedules (pet_id, meal_name, scheduled_time, food_name, food_quantity, meal_description, instructions, calories, is_reminder_enabled, created_at)
            VALUES (:pet_id, :meal_name, :scheduled_time, :food_name, :food_quantity, :meal_description, :instructions, :calories, :is_reminder_enabled, NOW())');
        $stmt->execute([
            ':pet_id' => $petId,
            ':meal_name' => $mealName,
            ':scheduled_time' => $scheduledTime,
            ':food_name' => $foodName,
            ':food_quantity' => $foodQuantity,
            ':meal_description' => $description,
            ':instructions' => $instructions,
            ':calories' => $calories,
            ':is_reminder_enabled' => $isReminderEnabled
        ]);

        $newId = (int)$pdo->lastInsertId();
        $fetchStmt = $pdo->prepare('SELECT * FROM meal_schedules WHERE id = :id');
        $fetchStmt->execute([':id' => $newId]);
        $row = $fetchStmt->fetch(PDO::FETCH_ASSOC);

        $formatted = [
            'id' => (int)$row['id'],
            'pet_id' => (int)$row['pet_id'],
            'meal_name' => $row['meal_name'],
            'scheduled_time' => $row['scheduled_time'],
            'food_name' => (string)($row['food_name'] ?? ''),
            'food_quantity' => (string)($row['food_quantity'] ?? ''),
            'meal_description' => (string)($row['meal_description'] ?? ''),
            'instructions' => (string)($row['instructions'] ?? ''),
            'calories' => (int)$row['calories'],
            'is_reminder_enabled' => (int)($row['is_reminder_enabled'] ?? 1),
            'created_at' => $row['created_at'] ?? '',
            'updated_at' => $row['updated_at'] ?? '',
        ];

        Response::success($formatted, 'Meal schedule created successfully', 201);
    }

    public function updateSchedule(array $currentUser, int $id, array $input): void {
        $pdo = Database::getConnection();

        $check = $pdo->prepare('SELECT m.id FROM meal_schedules m JOIN pets p ON m.pet_id = p.id WHERE m.id = :id AND p.user_id = :user_id LIMIT 1');
        $check->execute([':id' => $id, ':user_id' => $currentUser['id']]);
        if (!$check->fetch()) {
            Response::notFound('Meal schedule not found or unauthorized');
        }

        $fields = [];
        $params = [':id' => $id];

        $allowed = ['meal_name', 'scheduled_time', 'food_name', 'food_quantity', 'meal_description', 'instructions', 'calories', 'is_reminder_enabled'];
        foreach ($allowed as $f) {
            if (array_key_exists($f, $input)) {
                $fields[] = "`$f` = :$f";
                $params[":$f"] = ($f === 'calories' || $f === 'is_reminder_enabled') ? (int)$input[$f] : trim((string)$input[$f]);
            }
        }

        if (empty($fields)) {
            Response::error('No fields provided to update', 400);
        }

        $sql = 'UPDATE meal_schedules SET ' . implode(', ', $fields) . ', updated_at = NOW() WHERE id = :id';
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);

        $fetchStmt = $pdo->prepare('SELECT * FROM meal_schedules WHERE id = :id');
        $fetchStmt->execute([':id' => $id]);
        $updated = $fetchStmt->fetch(PDO::FETCH_ASSOC);

        $formatted = [
            'id' => (int)$updated['id'],
            'pet_id' => (int)$updated['pet_id'],
            'meal_name' => $updated['meal_name'],
            'scheduled_time' => $updated['scheduled_time'],
            'food_name' => (string)($updated['food_name'] ?? ''),
            'food_quantity' => (string)($updated['food_quantity'] ?? ''),
            'meal_description' => (string)($updated['meal_description'] ?? ''),
            'instructions' => (string)($updated['instructions'] ?? ''),
            'calories' => (int)$updated['calories'],
            'is_reminder_enabled' => (int)($updated['is_reminder_enabled'] ?? 1),
            'created_at' => $updated['created_at'] ?? '',
            'updated_at' => $updated['updated_at'] ?? '',
        ];

        Response::success($formatted, 'Meal schedule updated successfully');
    }

    public function deleteSchedule(array $currentUser, int $id): void {
        $pdo = Database::getConnection();

        $check = $pdo->prepare('SELECT m.id FROM meal_schedules m JOIN pets p ON m.pet_id = p.id WHERE m.id = :id AND p.user_id = :user_id LIMIT 1');
        $check->execute([':id' => $id, ':user_id' => $currentUser['id']]);
        if (!$check->fetch()) {
            Response::notFound('Meal schedule not found or unauthorized');
        }

        $stmt = $pdo->prepare('DELETE FROM meal_schedules WHERE id = :id');
        $stmt->execute([':id' => $id]);

        Response::success(['id' => $id], 'Meal schedule deleted successfully');
    }

    public function toggleReminder(array $currentUser, int $id): void {
        $pdo = Database::getConnection();

        $check = $pdo->prepare('SELECT m.id, m.is_reminder_enabled FROM meal_schedules m JOIN pets p ON m.pet_id = p.id WHERE m.id = :id AND p.user_id = :user_id LIMIT 1');
        $check->execute([':id' => $id, ':user_id' => $currentUser['id']]);
        $row = $check->fetch(PDO::FETCH_ASSOC);
        if (!$row) {
            Response::notFound('Meal schedule not found or unauthorized');
        }

        $newVal = ((int)$row['is_reminder_enabled'] === 1) ? 0 : 1;
        $up = $pdo->prepare('UPDATE meal_schedules SET is_reminder_enabled = :val, updated_at = NOW() WHERE id = :id');
        $up->execute([':val' => $newVal, ':id' => $id]);

        $fetchStmt = $pdo->prepare('SELECT * FROM meal_schedules WHERE id = :id');
        $fetchStmt->execute([':id' => $id]);

        Response::success($fetchStmt->fetch(PDO::FETCH_ASSOC), 'Reminder status updated');
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
