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
        try {
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
        } catch (\Throwable $e) {
            // Optional initial weight record
        }

        // Add default feeding schedule for new pet
        try {
            $schedStmt = $pdo->prepare(
                'INSERT INTO meal_schedules (pet_id, meal_name, scheduled_time, meal_description, calories, created_at) VALUES
                 (:p1, "Morning", "Morning • 8:00 AM", "2 Cups Premium Kibble / Fresh Timothy Hay", 350, NOW()),
                 (:p2, "Afternoon", "Afternoon • 1:00 PM", "Healthy Snack & Fresh Treat", 100, NOW()),
                 (:p3, "Evening", "Evening • 6:00 PM", "Evening Portion with Greens", 350, NOW())'
            );
            $schedStmt->execute([
                ':p1' => $petId,
                ':p2' => $petId,
                ':p3' => $petId,
            ]);
        } catch (\Throwable $e) {
            // Optional initial schedule
        }

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

        $allowed = ['name', 'species', 'breed', 'age_years', 'weight_kg', 'gender', 'microchip_id', 'health_score', 'vaccination_status', 'image_url', 'is_active'];
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

    public function uploadPhoto(array $currentUser, int $petId, array $input): void {
        $pdo = Database::getConnection();
        $checkStmt = $pdo->prepare('SELECT id, name FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $checkStmt->execute([':id' => $petId, ':user_id' => $currentUser['id']]);
        if (!$checkStmt->fetch()) {
            Response::notFound('Pet not found or unauthorized');
        }

        $uploadDir = dirname(__DIR__, 2) . '/public/uploads/pets/';
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0777, true);
        }

        $imageBinary = null;
        $extension = 'jpg';

        // Check if uploaded as multipart file
        if (!empty($_FILES['image']['tmp_name']) && is_uploaded_file($_FILES['image']['tmp_name'])) {
            $imageBinary = file_get_contents($_FILES['image']['tmp_name']);
            $finfo = finfo_open(FILEINFO_MIME_TYPE);
            $mime = finfo_file($finfo, $_FILES['image']['tmp_name']);
            finfo_close($finfo);
            if ($mime === 'image/png') $extension = 'png';
            elseif ($mime === 'image/webp') $extension = 'webp';
        } elseif (!empty($input['image_base64'])) {
            $data = $input['image_base64'];
            if (preg_match('#^data:image/(\w+);base64,#i', $data, $matches)) {
                $extension = strtolower($matches[1]) === 'png' ? 'png' : 'jpg';
                $data = substr($data, strpos($data, ',') + 1);
            }
            $imageBinary = base64_decode($data);
        }

        if (!$imageBinary) {
            Response::error('No valid image provided. Send image file or image_base64 string.', 400);
        }

        $filename = 'pet_' . $petId . '_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $extension;
        $filePath = $uploadDir . $filename;
        file_put_contents($filePath, $imageBinary);

        // Store relative path
        $relativeUrl = 'uploads/pets/' . $filename;
        $stmt = $pdo->prepare('UPDATE pets SET image_url = :url, updated_at = NOW() WHERE id = :id');
        $stmt->execute([':url' => $relativeUrl, ':id' => $petId]);

        $fetchStmt = $pdo->prepare('SELECT * FROM pets WHERE id = :id');
        $fetchStmt->execute([':id' => $petId]);
        $updatedPet = $fetchStmt->fetch(PDO::FETCH_ASSOC);

        Response::success([
            'image_url' => $relativeUrl,
            'pet' => $updatedPet,
        ], 'Pet photo updated successfully');
    }
}

