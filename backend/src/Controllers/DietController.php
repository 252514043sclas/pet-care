<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Services\FoodNutritionSafetyService;
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
        $target = 0;

        if ($petId) {
            $stmt = $pdo->prepare('SELECT COALESCE(SUM(calories), 0) as total FROM meal_entries WHERE user_id = :user_id AND pet_id = :pet_id');
            $stmt->execute([':user_id' => $currentUser['id'], ':pet_id' => $petId]);
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            $consumed = (int)($row['total'] ?? 0);

            $targetStmt = $pdo->prepare('SELECT COALESCE(SUM(calories), 0) as target_total FROM meal_schedules WHERE pet_id = :pet_id');
            $targetStmt->execute([':pet_id' => $petId]);
            $targetRow = $targetStmt->fetch(PDO::FETCH_ASSOC);
            $target = (int)($targetRow['target_total'] ?? 0);
        }

        $remaining = max(0, $target - $consumed);
        $progress = $target > 0 ? min(1.0, round($consumed / $target, 2)) : 0.0;
        $progressPercentage = $target > 0 ? (int)round(($consumed / $target) * 100) : 0;
        $hasSchedule = $target > 0;

        $message = $target === 0
            ? 'Set up your feeding schedule to view your daily calorie target.'
            : "Target: {$target} kcal/day";

        $label = $target === 0
            ? '0 kcal / day'
            : "Consumed: {$consumed} / {$target} kcal";

        Response::success([
            'pet_id' => $petId,
            'consumed_calories' => $consumed,
            'target_calories' => $target,
            'remaining_calories' => $remaining,
            'progress' => $progress,
            'progress_ratio' => $progress,
            'progress_percentage' => $progressPercentage,
            'has_schedule' => $hasSchedule,
            'message' => $message,
            'label' => $label,
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

        if ($calories <= 0 && $foodName !== '') {
            $petStmt = $pdo->prepare('SELECT species FROM pets WHERE id = :id LIMIT 1');
            $petStmt->execute([':id' => $petId]);
            $petRow = $petStmt->fetch(PDO::FETCH_ASSOC);
            $species = $petRow ? (string)$petRow['species'] : null;

            $parsedPortion = FoodNutritionSafetyService::parsePortion($foodQuantity);
            $analysis = FoodNutritionSafetyService::analyze($foodName, $parsedPortion['quantity'], $parsedPortion['unit'], $species);
            if (!empty($analysis['estimated_calories']) && (int)$analysis['estimated_calories'] > 0) {
                $calories = (int)$analysis['estimated_calories'];
            }
        }

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

        $check = $pdo->prepare('SELECT m.id, m.pet_id, m.food_name, m.food_quantity, m.calories, p.species FROM meal_schedules m JOIN pets p ON m.pet_id = p.id WHERE m.id = :id AND p.user_id = :user_id LIMIT 1');
        $check->execute([':id' => $id, ':user_id' => $currentUser['id']]);
        $existing = $check->fetch(PDO::FETCH_ASSOC);
        if (!$existing) {
            Response::notFound('Meal schedule not found or unauthorized');
        }

        // If food_name or food_quantity changed and user didn't explicitly supply positive calories, recalculate
        if ((array_key_exists('food_name', $input) || array_key_exists('food_quantity', $input)) && (!isset($input['calories']) || (int)$input['calories'] <= 0)) {
            $effFood = isset($input['food_name']) ? trim((string)$input['food_name']) : (string)$existing['food_name'];
            $effQty = isset($input['food_quantity']) ? trim((string)$input['food_quantity']) : (string)$existing['food_quantity'];
            if ($effFood !== '') {
                $parsedPortion = FoodNutritionSafetyService::parsePortion($effQty);
                $analysis = FoodNutritionSafetyService::analyze($effFood, $parsedPortion['quantity'], $parsedPortion['unit'], (string)$existing['species']);
                if (!empty($analysis['estimated_calories']) && (int)$analysis['estimated_calories'] > 0) {
                    $input['calories'] = (int)$analysis['estimated_calories'];
                }
            }
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

    private function formatMealRow(array $row): array {
        $qty = isset($row['portion_quantity']) ? round((float)$row['portion_quantity'], 2) : 1.0;
        $unit = !empty($row['portion_unit']) ? (string)$row['portion_unit'] : 'cups';
        $portionSize = !empty($row['portion_size']) ? (string)$row['portion_size'] : (rtrim(rtrim(number_format($qty, 2, '.', ''), '0'), '.') . ' ' . $unit);
        $mealDate = !empty($row['meal_date'])
            ? (string)$row['meal_date']
            : (!empty($row['created_at']) ? date('M j, Y', strtotime((string)$row['created_at'])) : date('M j, Y'));

        $petName = !empty($row['pet_name']) ? (string)$row['pet_name'] : (string)($row['resolved_pet_name'] ?? '');
        $petSpecies = !empty($row['pet_species']) ? (string)$row['pet_species'] : (string)($row['resolved_pet_species'] ?? '');
        $foodItem = (string)$row['food_item'];

        $safetyStatus = !empty($row['safety_status']) ? strtoupper((string)$row['safety_status']) : 'UNKNOWN';
        $safetyWarning = ($row['safety_warning'] !== null && trim((string)$row['safety_warning']) !== '')
            ? (string)$row['safety_warning']
            : null;

        // For any legacy meal row missing a species-specific explanation, derive it dynamically
        if ($safetyWarning === null && $foodItem !== '') {
            $analysis = FoodNutritionSafetyService::analyze($foodItem, max(0.01, $qty), $unit, $petSpecies);
            $safetyStatus = $analysis['safety_status'];
            $safetyWarning = $analysis['safety_warning'];
        }

        return [
            'id' => (int)$row['id'],
            'pet_id' => (int)$row['pet_id'],
            'pet_name' => $petName,
            'pet_species' => $petSpecies,
            'food_item' => $foodItem,
            'portion_size' => $portionSize,
            'portion_quantity' => $qty,
            'portion_unit' => $unit,
            'calories' => ($row['calories'] !== null && $row['calories'] !== '') ? (int)$row['calories'] : null,
            'safety_status' => $safetyStatus,
            'safety_warning' => $safetyWarning,
            'meal_date' => $mealDate,
            'meal_time' => (string)($row['meal_time'] ?? ''),
            'created_at' => $row['created_at'] ?? null,
        ];
    }

    public function analyzeFood(array $currentUser, array $input): void {
        $foodName = trim((string)($input['food_name'] ?? $input['food_item'] ?? ''));
        if ($foodName === '') {
            Response::error('Food name is required.', 422);
        }

        $quantity = isset($input['portion_quantity']) ? (float)$input['portion_quantity'] : 1.0;
        if ($quantity <= 0) {
            Response::error('Portion quantity must be greater than zero.', 422);
        }

        $unit = trim((string)($input['portion_unit'] ?? 'g'));
        $species = trim((string)($input['species'] ?? $input['pet_species'] ?? ''));

        if ($species === '') {
            $pdo = Database::getConnection();
            $petId = $this->resolvePetId($pdo, $currentUser, $input);
            if ($petId) {
                $pStmt = $pdo->prepare('SELECT species FROM pets WHERE id = :id LIMIT 1');
                $pStmt->execute([':id' => $petId]);
                $pRow = $pStmt->fetch(PDO::FETCH_ASSOC);
                if ($pRow) {
                    $species = (string)$pRow['species'];
                }
            }
        }

        $analysis = FoodNutritionSafetyService::analyze($foodName, $quantity, $unit, $species);
        Response::success($analysis, 'Food analyzed successfully');
    }

    public function meals(array $currentUser, array $input = []): void {
        $pdo = Database::getConnection();
        $petId = $this->resolvePetId($pdo, $currentUser, $input);
        $dateFilter = isset($input['date']) ? trim((string)$input['date']) : '';

        $meals = [];
        if ($petId) {
            if ($dateFilter !== '') {
                $stmt = $pdo->prepare(
                    'SELECT m.*, p.name AS resolved_pet_name, p.species AS resolved_pet_species
                     FROM meal_entries m
                     LEFT JOIN pets p ON m.pet_id = p.id
                     WHERE m.user_id = :user_id AND m.pet_id = :pet_id AND m.meal_date = :meal_date
                     ORDER BY m.created_at DESC, m.id DESC'
                );
                $stmt->execute([
                    ':user_id' => $currentUser['id'],
                    ':pet_id' => $petId,
                    ':meal_date' => $dateFilter,
                ]);
            } else {
                $stmt = $pdo->prepare(
                    'SELECT m.*, p.name AS resolved_pet_name, p.species AS resolved_pet_species
                     FROM meal_entries m
                     LEFT JOIN pets p ON m.pet_id = p.id
                     WHERE m.user_id = :user_id AND m.pet_id = :pet_id
                     ORDER BY m.created_at DESC, m.id DESC'
                );
                $stmt->execute([':user_id' => $currentUser['id'], ':pet_id' => $petId]);
            }
            $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
            $meals = array_map([$this, 'formatMealRow'], $rows);
        }

        Response::success($meals);
    }

    public function logMeal(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'food_item' => 'required|max:255',
        ]);
        if (!empty($errors)) {
            Response::error('Food name is required.', 422, $errors);
        }

        $pdo = Database::getConnection();
        $petId = $this->resolvePetId($pdo, $currentUser, $input);

        if (!$petId) {
            Response::error('No pet profile found to attach meal to.', 400);
        }

        $petStmt = $pdo->prepare('SELECT name, species FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $petStmt->execute([':id' => $petId, ':user_id' => $currentUser['id']]);
        $petRow = $petStmt->fetch(PDO::FETCH_ASSOC);

        $petName = !empty($input['pet_name']) ? trim((string)$input['pet_name']) : (string)($petRow['name'] ?? '');
        $petSpecies = !empty($input['pet_species']) ? trim((string)$input['pet_species']) : (string)($petRow['species'] ?? '');

        $foodItem = trim((string)$input['food_item']);
        $portionQty = isset($input['portion_quantity']) ? (float)$input['portion_quantity'] : 0.0;
        if ($portionQty <= 0) {
            Response::error('Portion quantity must be greater than zero.', 422);
        }

        $allowedUnits = ['g', 'cups', 'pieces', 'tbsp', 'tsp'];
        $rawUnit = strtolower(trim((string)($input['portion_unit'] ?? 'g')));
        $portionUnit = FoodNutritionSafetyService::normalizeUnit($rawUnit);
        if (!in_array($portionUnit, $allowedUnits, true)) {
            Response::error('Invalid portion unit.', 422);
        }

        $qtyFormatted = rtrim(rtrim(number_format($portionQty, 2, '.', ''), '0'), '.');
        $portionSize = !empty($input['portion_size']) ? trim((string)$input['portion_size']) : "{$qtyFormatted} {$portionUnit}";

        $analysis = FoodNutritionSafetyService::analyze($foodItem, $portionQty, $portionUnit, $petSpecies);

        $calories = array_key_exists('calories', $input)
            ? (($input['calories'] !== null && $input['calories'] !== '') ? (int)$input['calories'] : null)
            : $analysis['estimated_calories'];

        $safetyStatus = !empty($input['safety_status'])
            ? strtoupper(trim((string)$input['safety_status']))
            : $analysis['safety_status'];
        if (!in_array($safetyStatus, ['SAFE', 'TOXIC', 'UNKNOWN'], true)) {
            $safetyStatus = $analysis['safety_status'];
        }

        $safetyWarning = array_key_exists('safety_warning', $input) && $input['safety_warning'] !== null
            ? trim((string)$input['safety_warning'])
            : $analysis['safety_warning'];

        $mealDate = !empty($input['meal_date']) ? trim((string)$input['meal_date']) : date('M j, Y');
        $mealTime = !empty($input['meal_time']) ? trim((string)$input['meal_time']) : date('g:i A');

        $stmt = $pdo->prepare(
            'INSERT INTO meal_entries
             (pet_id, pet_name, pet_species, user_id, food_item, portion_size, portion_quantity, portion_unit, calories, safety_status, safety_warning, meal_date, meal_time, created_at)
             VALUES
             (:pet_id, :pet_name, :pet_species, :user_id, :food_item, :portion_size, :portion_quantity, :portion_unit, :calories, :safety_status, :safety_warning, :meal_date, :meal_time, NOW())'
        );
        $stmt->execute([
            ':pet_id' => $petId,
            ':pet_name' => $petName,
            ':pet_species' => $petSpecies,
            ':user_id' => $currentUser['id'],
            ':food_item' => $foodItem,
            ':portion_size' => $portionSize,
            ':portion_quantity' => $portionQty,
            ':portion_unit' => $portionUnit,
            ':calories' => $calories,
            ':safety_status' => $safetyStatus,
            ':safety_warning' => $safetyWarning,
            ':meal_date' => $mealDate,
            ':meal_time' => $mealTime,
        ]);

        $mealId = (int)$pdo->lastInsertId();

        $fetchStmt = $pdo->prepare('SELECT * FROM meal_entries WHERE id = :id');
        $fetchStmt->execute([':id' => $mealId]);
        $row = $fetchStmt->fetch(PDO::FETCH_ASSOC);

        Response::success($this->formatMealRow($row), 'Meal logged successfully', 201);
    }

    public function updateMeal(array $currentUser, int $id, array $input): void {
        $pdo = Database::getConnection();

        $check = $pdo->prepare('SELECT * FROM meal_entries WHERE id = :id AND user_id = :user_id LIMIT 1');
        $check->execute([':id' => $id, ':user_id' => $currentUser['id']]);
        $existing = $check->fetch(PDO::FETCH_ASSOC);
        if (!$existing) {
            Response::notFound('Meal entry not found or unauthorized');
        }

        $foodItem = isset($input['food_item']) ? trim((string)$input['food_item']) : (string)$existing['food_item'];
        if ($foodItem === '') {
            Response::error('Food name cannot be empty.', 422);
        }

        $portionQty = isset($input['portion_quantity']) ? (float)$input['portion_quantity'] : (float)$existing['portion_quantity'];
        if ($portionQty <= 0) {
            Response::error('Portion quantity must be greater than zero.', 422);
        }

        $rawUnit = isset($input['portion_unit']) ? (string)$input['portion_unit'] : (string)$existing['portion_unit'];
        $portionUnit = FoodNutritionSafetyService::normalizeUnit($rawUnit);

        $qtyFormatted = rtrim(rtrim(number_format($portionQty, 2, '.', ''), '0'), '.');
        $portionSize = !empty($input['portion_size']) ? trim((string)$input['portion_size']) : "{$qtyFormatted} {$portionUnit}";

        $petSpecies = !empty($input['pet_species']) ? trim((string)$input['pet_species']) : (string)($existing['pet_species'] ?? '');
        $analysis = FoodNutritionSafetyService::analyze($foodItem, $portionQty, $portionUnit, $petSpecies);

        $calories = array_key_exists('calories', $input)
            ? (($input['calories'] !== null && $input['calories'] !== '') ? (int)$input['calories'] : null)
            : $analysis['estimated_calories'];

        $safetyStatus = !empty($input['safety_status'])
            ? strtoupper(trim((string)$input['safety_status']))
            : $analysis['safety_status'];
        if (!in_array($safetyStatus, ['SAFE', 'TOXIC', 'UNKNOWN'], true)) {
            $safetyStatus = $analysis['safety_status'];
        }

        $safetyWarning = array_key_exists('safety_warning', $input) && $input['safety_warning'] !== null
            ? trim((string)$input['safety_warning'])
            : $analysis['safety_warning'];

        $mealDate = !empty($input['meal_date']) ? trim((string)$input['meal_date']) : (string)($existing['meal_date'] ?? date('M j, Y'));
        $mealTime = !empty($input['meal_time']) ? trim((string)$input['meal_time']) : (string)$existing['meal_time'];

        $up = $pdo->prepare(
            'UPDATE meal_entries SET
                food_item = :food_item,
                portion_size = :portion_size,
                portion_quantity = :portion_quantity,
                portion_unit = :portion_unit,
                calories = :calories,
                safety_status = :safety_status,
                safety_warning = :safety_warning,
                meal_date = :meal_date,
                meal_time = :meal_time,
                updated_at = NOW()
             WHERE id = :id AND user_id = :user_id'
        );
        $up->execute([
            ':food_item' => $foodItem,
            ':portion_size' => $portionSize,
            ':portion_quantity' => $portionQty,
            ':portion_unit' => $portionUnit,
            ':calories' => $calories,
            ':safety_status' => $safetyStatus,
            ':safety_warning' => $safetyWarning,
            ':meal_date' => $mealDate,
            ':meal_time' => $mealTime,
            ':id' => $id,
            ':user_id' => $currentUser['id'],
        ]);

        $fetch = $pdo->prepare('SELECT * FROM meal_entries WHERE id = :id');
        $fetch->execute([':id' => $id]);
        $updated = $fetch->fetch(PDO::FETCH_ASSOC);

        Response::success($this->formatMealRow($updated), 'Meal entry updated successfully');
    }

    public function deleteMeal(array $currentUser, int $id): void {
        $pdo = Database::getConnection();
        $check = $pdo->prepare('SELECT id FROM meal_entries WHERE id = :id AND user_id = :user_id LIMIT 1');
        $check->execute([':id' => $id, ':user_id' => $currentUser['id']]);
        if (!$check->fetch()) {
            Response::notFound('Meal entry not found or unauthorized');
        }

        $del = $pdo->prepare('DELETE FROM meal_entries WHERE id = :id AND user_id = :user_id');
        $del->execute([':id' => $id, ':user_id' => $currentUser['id']]);

        Response::success(['id' => $id], 'Meal entry deleted successfully');
    }

    public function safeToxicFoods(): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM safe_toxic_foods ORDER BY is_safe DESC, id ASC');
        $stmt->execute();
        $foods = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($foods);
    }
}
