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

        // Process initial pet image if provided
        $imageUrl = null;
        if (!empty($input['image_base64'])) {
            $uploadDir = dirname(__DIR__, 2) . '/public/uploads/pets/';
            if (!is_dir($uploadDir)) {
                mkdir($uploadDir, 0777, true);
            }
            $data = $input['image_base64'];
            $extension = 'jpg';
            if (preg_match('#^data:image/(\w+);base64,#i', $data, $matches)) {
                $extension = strtolower($matches[1]) === 'png' ? 'png' : 'jpg';
                $data = substr($data, strpos($data, ',') + 1);
            }
            $imageBinary = base64_decode($data);
            if ($imageBinary) {
                $filename = 'pet_' . $currentUser['id'] . '_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $extension;
                file_put_contents($uploadDir . $filename, $imageBinary);
                $imageUrl = 'uploads/pets/' . $filename;
            }
        } elseif (!empty($input['image_url'])) {
            $imageUrl = trim((string)$input['image_url']);
        }

        $stmt = $pdo->prepare(
            'INSERT INTO pets (user_id, name, species, breed, age_years, weight_kg, gender, microchip_id, health_score, vaccination_status, image_url, is_active, created_at)
             VALUES (:user_id, :name, :species, :breed, :age_years, :weight_kg, :gender, :microchip_id, :health_score, :vaccination_status, :image_url, :is_active, NOW())'
        );

        $stmt->execute([
            ':user_id' => $currentUser['id'],
            ':name' => trim($input['name']),
            ':species' => trim($input['species']),
            ':breed' => trim($input['breed']),
            ':age_years' => trim((string)$input['age_years']),
            ':weight_kg' => (float)$input['weight_kg'],
            ':gender' => $input['gender'] ?? 'Female',
            ':microchip_id' => !empty($input['microchip_id']) ? trim((string)$input['microchip_id']) : null,
            ':health_score' => isset($input['health_score']) ? (int)$input['health_score'] : null,
            ':vaccination_status' => !empty($input['vaccination_status']) ? trim((string)$input['vaccination_status']) : 'Not Recorded',
            ':image_url' => $imageUrl,
            ':is_active' => $isActive,
        ]);

        $petId = (int)$pdo->lastInsertId();

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

        if (!empty($input['image_base64'])) {
            $uploadDir = dirname(__DIR__, 2) . '/public/uploads/pets/';
            if (!is_dir($uploadDir)) {
                mkdir($uploadDir, 0777, true);
            }
            $data = $input['image_base64'];
            $extension = 'jpg';
            if (preg_match('#^data:image/(\w+);base64,#i', $data, $matches)) {
                $extension = strtolower($matches[1]) === 'png' ? 'png' : 'jpg';
                $data = substr($data, strpos($data, ',') + 1);
            }
            $imageBinary = base64_decode($data);
            if ($imageBinary) {
                $filename = 'pet_' . $petId . '_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $extension;
                file_put_contents($uploadDir . $filename, $imageBinary);
                // Remove existing image_url if already set in fields
                $fields = array_filter($fields, fn($f) => strpos($f, 'image_url') === false);
                $fields[] = "`image_url` = :image_url";
                $params[':image_url'] = 'uploads/pets/' . $filename;
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

        $fetchStmt = $pdo->prepare('SELECT * FROM pets WHERE id = :id AND user_id = :user_id');
        $fetchStmt->execute([':id' => $petId, ':user_id' => $currentUser['id']]);
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

    private function syncPetLatestWeight(PDO $pdo, int $petId): ?float {
        $latestStmt = $pdo->prepare('SELECT weight_kg FROM weight_entries WHERE pet_id = :pet_id ORDER BY created_at DESC, id DESC LIMIT 1');
        $latestStmt->execute([':pet_id' => $petId]);
        $latestRow = $latestStmt->fetch(PDO::FETCH_ASSOC);

        if ($latestRow) {
            $latestWeight = (float)$latestRow['weight_kg'];
            $updatePet = $pdo->prepare('UPDATE pets SET weight_kg = :w, updated_at = NOW() WHERE id = :id');
            $updatePet->execute([':w' => $latestWeight, ':id' => $petId]);
            return $latestWeight;
        }
        return null;
    }

    private function formatWeightRow(array $row): array {
        return [
            'id' => (int)$row['id'],
            'pet_id' => (int)$row['pet_id'],
            'weight_kg' => round((float)$row['weight_kg'], 2),
            'recorded_date' => (string)$row['recorded_date'],
            'notes' => $row['notes'] !== null ? (string)$row['notes'] : null,
            'created_at' => $row['created_at'] ?? null,
        ];
    }

    public function weights(array $currentUser, int $petId): void {
        $pdo = Database::getConnection();
        $check = $pdo->prepare('SELECT id, weight_kg, target_weight_kg FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $check->execute([':id' => $petId, ':user_id' => $currentUser['id']]);
        $pet = $check->fetch(PDO::FETCH_ASSOC);
        if (!$pet) {
            Response::notFound('Pet not found');
        }

        $stmt = $pdo->prepare('SELECT * FROM weight_entries WHERE pet_id = :pet_id AND user_id = :user_id ORDER BY created_at DESC, id DESC');
        $stmt->execute([':pet_id' => $petId, ':user_id' => $currentUser['id']]);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $history = array_map([$this, 'formatWeightRow'], $rows);

        $currentWeight = !empty($history) ? (float)$history[0]['weight_kg'] : null;
        $targetWeight = ($pet['target_weight_kg'] !== null && (float)$pet['target_weight_kg'] > 0)
            ? round((float)$pet['target_weight_kg'], 2)
            : null;

        $status = 'No Records';
        if ($currentWeight !== null) {
            if ($targetWeight !== null) {
                $diffRatio = ($currentWeight - $targetWeight) / max(0.01, $targetWeight);
                if (abs($diffRatio) <= 0.05) {
                    $status = 'On Target';
                } elseif ($diffRatio > 0.05) {
                    $status = 'Above Target';
                } else {
                    $status = 'Below Target';
                }
            } else {
                $status = 'Tracked';
            }
        }

        Response::success([
            'current_weight' => $currentWeight,
            'target_weight' => $targetWeight,
            'status' => $status,
            'history' => $history,
        ]);
    }

    public function addWeight(array $currentUser, int $petId, array $input): void {
        $errors = Validator::validate($input, [
            'weight_kg' => 'required|numeric',
        ]);
        if (!empty($errors)) {
            Response::error('Please enter a valid weight.', 422, $errors);
        }

        $weightKg = round((float)$input['weight_kg'], 2);
        if ($weightKg <= 0 || $weightKg > 500) {
            Response::error('Weight must be a valid positive number between 0.01 and 500 kg.', 422);
        }

        $pdo = Database::getConnection();
        $check = $pdo->prepare('SELECT id FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $check->execute([':id' => $petId, ':user_id' => $currentUser['id']]);
        if (!$check->fetch()) {
            Response::notFound('Pet not found');
        }

        $date = !empty($input['recorded_date']) ? trim((string)$input['recorded_date']) : date('M j, Y');
        $notes = isset($input['notes']) && trim((string)$input['notes']) !== '' ? trim((string)$input['notes']) : null;

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

        $newId = (int)$pdo->lastInsertId();
        $this->syncPetLatestWeight($pdo, $petId);

        $fetch = $pdo->prepare('SELECT * FROM weight_entries WHERE id = :id');
        $fetch->execute([':id' => $newId]);
        $row = $fetch->fetch(PDO::FETCH_ASSOC);

        Response::success($this->formatWeightRow($row), 'Weight recorded successfully', 201);
    }

    public function updateWeight(array $currentUser, int $petId, int $weightId, array $input): void {
        $errors = Validator::validate($input, [
            'weight_kg' => 'required|numeric',
        ]);
        if (!empty($errors)) {
            Response::error('Please enter a valid weight.', 422, $errors);
        }

        $weightKg = round((float)$input['weight_kg'], 2);
        if ($weightKg <= 0 || $weightKg > 500) {
            Response::error('Weight must be a valid positive number between 0.01 and 500 kg.', 422);
        }

        $pdo = Database::getConnection();
        $check = $pdo->prepare('SELECT * FROM weight_entries WHERE id = :wid AND pet_id = :pet_id AND user_id = :user_id LIMIT 1');
        $check->execute([':wid' => $weightId, ':pet_id' => $petId, ':user_id' => $currentUser['id']]);
        $existing = $check->fetch(PDO::FETCH_ASSOC);
        if (!$existing) {
            Response::notFound('Weight record not found or unauthorized');
        }

        $date = isset($input['recorded_date']) && trim((string)$input['recorded_date']) !== ''
            ? trim((string)$input['recorded_date'])
            : $existing['recorded_date'];
        $notes = array_key_exists('notes', $input)
            ? (trim((string)$input['notes']) !== '' ? trim((string)$input['notes']) : null)
            : $existing['notes'];

        $up = $pdo->prepare(
            'UPDATE weight_entries SET weight_kg = :weight_kg, recorded_date = :recorded_date, notes = :notes, updated_at = NOW()
             WHERE id = :wid AND pet_id = :pet_id AND user_id = :user_id'
        );
        $up->execute([
            ':weight_kg' => $weightKg,
            ':recorded_date' => $date,
            ':notes' => $notes,
            ':wid' => $weightId,
            ':pet_id' => $petId,
            ':user_id' => $currentUser['id'],
        ]);

        $this->syncPetLatestWeight($pdo, $petId);

        $fetch = $pdo->prepare('SELECT * FROM weight_entries WHERE id = :id');
        $fetch->execute([':id' => $weightId]);
        $row = $fetch->fetch(PDO::FETCH_ASSOC);

        Response::success($this->formatWeightRow($row), 'Weight entry updated successfully');
    }

    public function deleteWeight(array $currentUser, int $petId, int $weightId): void {
        $pdo = Database::getConnection();
        $check = $pdo->prepare('SELECT id FROM weight_entries WHERE id = :wid AND pet_id = :pet_id AND user_id = :user_id LIMIT 1');
        $check->execute([':wid' => $weightId, ':pet_id' => $petId, ':user_id' => $currentUser['id']]);
        if (!$check->fetch()) {
            Response::notFound('Weight record not found or unauthorized');
        }

        $del = $pdo->prepare('DELETE FROM weight_entries WHERE id = :wid AND pet_id = :pet_id AND user_id = :user_id');
        $del->execute([':wid' => $weightId, ':pet_id' => $petId, ':user_id' => $currentUser['id']]);

        $this->syncPetLatestWeight($pdo, $petId);

        Response::success(['id' => $weightId], 'Weight entry deleted successfully');
    }

    public function updateTargetWeight(array $currentUser, int $petId, array $input): void {
        $pdo = Database::getConnection();
        $check = $pdo->prepare('SELECT id FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $check->execute([':id' => $petId, ':user_id' => $currentUser['id']]);
        if (!$check->fetch()) {
            Response::notFound('Pet not found or unauthorized');
        }

        $raw = $input['target_weight_kg'] ?? null;
        if ($raw === null || $raw === '') {
            $target = null;
        } else {
            if (!is_numeric($raw) || (float)$raw <= 0 || (float)$raw > 500) {
                Response::error('Target weight must be a valid positive number.', 422);
            }
            $target = round((float)$raw, 2);
        }

        $up = $pdo->prepare('UPDATE pets SET target_weight_kg = :tw, updated_at = NOW() WHERE id = :id AND user_id = :user_id');
        $up->execute([':tw' => $target, ':id' => $petId, ':user_id' => $currentUser['id']]);

        Response::success(['target_weight' => $target], 'Target weight updated successfully');
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
        $stmt = $pdo->prepare('UPDATE pets SET image_url = :url, updated_at = NOW() WHERE id = :id AND user_id = :user_id');
        $stmt->execute([':url' => $relativeUrl, ':id' => $petId, ':user_id' => $currentUser['id']]);

        $fetchStmt = $pdo->prepare('SELECT * FROM pets WHERE id = :id AND user_id = :user_id');
        $fetchStmt->execute([':id' => $petId, ':user_id' => $currentUser['id']]);
        $updatedPet = $fetchStmt->fetch(PDO::FETCH_ASSOC);

        Response::success([
            'image_url' => $relativeUrl,
            'pet' => $updatedPet,
        ], 'Pet photo updated successfully');
    }
}

