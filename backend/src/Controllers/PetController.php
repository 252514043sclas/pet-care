<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Utils\Database;
use App\Utils\Response;
use App\Utils\Validator;
use PDO;

class PetController {
    public function index(array $currentUser): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM pets WHERE user_id = :user_id ORDER BY is_active DESC, id ASC');
        $stmt->execute([':user_id' => $currentUser['id']]);
        $pets = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($pets);
    }

    public function store(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'name' => 'required|max:100',
            'species' => 'required|max:50',
            'breed' => 'required|max:100',
            'age_years' => 'required',
            'weight_kg' => 'required|numeric',
        ]);

        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();

        // Check if user already has an active pet
        $checkActive = $pdo->prepare('SELECT id FROM pets WHERE user_id = :user_id AND is_active = 1 LIMIT 1');
        $checkActive->execute([':user_id' => $currentUser['id']]);
        $isActive = $checkActive->fetch() ? 0 : 1;

        $stmt = $pdo->prepare(
            'INSERT INTO pets (user_id, name, species, breed, age_years, weight_kg, gender, microchip_id, health_score, vaccination_status, is_active, created_at)
             VALUES (:user_id, :name, :species, :breed, :age_years, :weight_kg, :gender, :microchip_id, :health_score, :vaccination_status, :is_active, NOW())'
        );

        $stmt->execute([
            ':user_id' => $currentUser['id'],
            ':name' => trim($input['name']),
            ':species' => trim($input['species']),
            ':breed' => trim($input['breed']),
            ':age_years' => trim((string)$input['age_years']),
            ':weight_kg' => (float)$input['weight_kg'],
            ':gender' => $input['gender'] ?? 'Female',
            ':microchip_id' => $input['microchip_id'] ?? ('#' . rand(100000000, 999999999)),
            ':health_score' => isset($input['health_score']) ? (int)$input['health_score'] : 95,
            ':vaccination_status' => $input['vaccination_status'] ?? 'Up to Date',
            ':is_active' => $isActive,
        ]);

        $petId = (int)$pdo->lastInsertId();

        // Add initial weight entry
        $weightStmt = $pdo->prepare(
            'INSERT INTO weight_entries (pet_id, user_id, weight_kg, recorded_date, notes, created_at)
             VALUES (:pet_id, :user_id, :weight_kg, :recorded_date, :notes, NOW())'
        );
        $weightStmt->execute([
            ':pet_id' => $petId,
            ':user_id' => $currentUser['id'],
            ':weight_kg' => (float)$input['weight_kg'],
            ':recorded_date' => date('M j'),
            ':notes' => 'Initial weight record',
        ]);

        // Add default feeding schedule for new pet
        $schedStmt = $pdo->prepare(
            'INSERT INTO meal_schedules (pet_id, meal_name, scheduled_time, meal_description, calories) VALUES
             (:pet_id, "Morning", "Morning • 8:00 AM", "2 Cups Premium Kibble / Fresh Timothy Hay", 350),
             (:pet_id, "Afternoon", "Afternoon • 1:00 PM", "Healthy Snack & Fresh Treat", 100),
             (:pet_id, "Evening", "Evening • 6:00 PM", "Evening Portion with Greens", 350)'
        );
        $schedStmt->execute([':pet_id' => $petId]);

        // Fetch newly created pet
        $fetchStmt = $pdo->prepare('SELECT * FROM pets WHERE id = :id');
        $fetchStmt->execute([':id' => $petId]);
        $newPet = $fetchStmt->fetch(PDO::FETCH_ASSOC);

        Response::success($newPet, 'Pet profile created successfully', 201);
    }

    public function show(array $currentUser, int $petId): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $stmt->execute([':id' => $petId, ':user_id' => $currentUser['id']]);
        $pet = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$pet) {
            Response::notFound('Pet not found or unauthorized');
        }

        Response::success($pet);
    }

    public function update(array $currentUser, int $petId, array $input): void {
        $pdo = Database::getConnection();
        $checkStmt = $pdo->prepare('SELECT id FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $checkStmt->execute([':id' => $petId, ':user_id' => $currentUser['id']]);
        if (!$checkStmt->fetch()) {
            Response::notFound('Pet not found or unauthorized');
        }

        $fields = [];
        $params = [':id' => $petId, ':user_id' => $currentUser['id']];

        $allowed = ['name', 'species', 'breed', 'age_years', 'weight_kg', 'gender', 'microchip_id', 'health_score', 'vaccination_status', 'is_active'];
        foreach ($allowed as $field) {
            if (array_key_exists($field, $input)) {
                $fields[] = "`$field` = :$field";
                $params[":$field"] = $input[$field];
            }
        }

        if (empty($fields)) {
            Response::error('No fields provided for update', 400);
        }

        // If setting active, unset other pets
        if (!empty($input['is_active'])) {
            $unset = $pdo->prepare('UPDATE pets SET is_active = 0 WHERE user_id = :user_id');
            $unset->execute([':user_id' => $currentUser['id']]);
        }

        $sql = 'UPDATE pets SET ' . implode(', ', $fields) . ', updated_at = NOW() WHERE id = :id AND user_id = :user_id';
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);

        $fetchStmt = $pdo->prepare('SELECT * FROM pets WHERE id = :id');
        $fetchStmt->execute([':id' => $petId]);
        $pet = $fetchStmt->fetch(PDO::FETCH_ASSOC);

        Response::success($pet, 'Pet profile updated successfully');
    }

    public function destroy(array $currentUser, int $petId): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('DELETE FROM pets WHERE id = :id AND user_id = :user_id');
        $stmt->execute([':id' => $petId, ':user_id' => $currentUser['id']]);

        if ($stmt->rowCount() === 0) {
            Response::notFound('Pet not found or unauthorized');
        }

        Response::success(null, 'Pet profile deleted successfully');
    }

    public function weights(array $currentUser, int $petId): void {
        $pdo = Database::getConnection();
        // Verify ownership
        $check = $pdo->prepare('SELECT id, weight_kg FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $check->execute([':id' => $petId, ':user_id' => $currentUser['id']]);
        $pet = $check->fetch(PDO::FETCH_ASSOC);
        if (!$pet) {
            Response::notFound('Pet not found');
        }

        $stmt = $pdo->prepare('SELECT * FROM weight_entries WHERE pet_id = :pet_id ORDER BY id ASC');
        $stmt->execute([':pet_id' => $petId]);
        $weights = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success([
            'current_weight' => (float)$pet['weight_kg'],
            'target_weight' => round((float)$pet['weight_kg'] * 0.95, 1),
            'status' => 'Optimal',
            'history' => $weights,
        ]);
    }

    public function addWeight(array $currentUser, int $petId, array $input): void {
        $errors = Validator::validate($input, [
            'weight_kg' => 'required|numeric',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $check = $pdo->prepare('SELECT id FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $check->execute([':id' => $petId, ':user_id' => $currentUser['id']]);
        if (!$check->fetch()) {
            Response::notFound('Pet not found');
        }

        $weightKg = (float)$input['weight_kg'];
        $date = !empty($input['recorded_date']) ? trim($input['recorded_date']) : date('M j');
        $notes = $input['notes'] ?? null;

        $stmt = $pdo->prepare(
            'INSERT INTO weight_entries (pet_id, user_id, weight_kg, recorded_date, notes, created_at)
             VALUES (:pet_id, :user_id, :weight_kg, :recorded_date, :notes, NOW())'
        );
        $stmt->execute([
            ':pet_id' => $petId,
            ':user_id' => $currentUser['id'],
            ':weight_kg' => $weightKg,
            ':recorded_date' => $date,
            ':notes' => $notes,
        ]);

        // Update pet's weight_kg
        $updatePet = $pdo->prepare('UPDATE pets SET weight_kg = :w, updated_at = NOW() WHERE id = :id');
        $updatePet->execute([':w' => $weightKg, ':id' => $petId]);

        Response::success([
            'id' => (int)$pdo->lastInsertId(),
            'weight_kg' => $weightKg,
            'recorded_date' => $date,
        ], 'Weight recorded successfully', 201);
    }
}

