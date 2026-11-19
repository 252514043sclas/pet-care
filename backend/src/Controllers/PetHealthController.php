<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Services\PetHealthScoringService;
use App\Utils\Database;
use App\Utils\Response;
use App\Utils\Validator;
use PDO;

class PetHealthController {

    private function verifyPetOwnership(PDO $pdo, int $userId, int $petId): ?array {
        $stmt = $pdo->prepare('SELECT id, name, species, health_score, vaccination_status FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $stmt->execute([':id' => $petId, ':user_id' => $userId]);
        $pet = $stmt->fetch(PDO::FETCH_ASSOC);
        return $pet ?: null;
    }

    // ------------------------------------------------------------------------
    // Health Score & Health Records
    // ------------------------------------------------------------------------

    /**
     * GET /api/pets/{id}/health-score
     */
    public function getHealthScore(array $currentUser, int $petId): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $pet = $this->verifyPetOwnership($pdo, $userId, $petId);
        if (!$pet) {
            Response::notFound('Pet profile not found');
        }

        $result = PetHealthScoringService::calculateScore($pdo, $petId, $userId);
        Response::success($result);
    }

    /**
     * GET /api/pets/{id}/health-records
     */
    public function getHealthRecords(array $currentUser, int $petId): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $pet = $this->verifyPetOwnership($pdo, $userId, $petId);
        if (!$pet) {
            Response::notFound('Pet profile not found');
        }

        $stmt = $pdo->prepare('
            SELECT id, pet_id, record_type, title, description, severity, recorded_date, vet_name, notes, created_at, updated_at
            FROM pet_health_records
            WHERE pet_id = :pet_id AND user_id = :user_id
            ORDER BY recorded_date DESC, id DESC
        ');
        $stmt->execute([':pet_id' => $petId, ':user_id' => $userId]);
        $records = $stmt->fetchAll(PDO::FETCH_ASSOC);

        foreach ($records as &$item) {
            $item['id'] = (int)$item['id'];
            $item['pet_id'] = (int)$item['pet_id'];
        }

        Response::success($records);
    }

    /**
     * POST /api/pets/{id}/health-records
     */
    public function addHealthRecord(array $currentUser, int $petId, array $input): void {
        $errors = Validator::validate($input, [
            'title' => 'required|max:150',
            'recorded_date' => 'required',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $pet = $this->verifyPetOwnership($pdo, $userId, $petId);
        if (!$pet) {
            Response::notFound('Pet profile not found');
        }

        $recordType = !empty($input['record_type']) ? trim((string)$input['record_type']) : 'Checkup';
        $title = trim((string)$input['title']);
        $description = !empty($input['description']) ? trim((string)$input['description']) : null;
        $severity = !empty($input['severity']) ? ucfirst(strtolower(trim((string)$input['severity']))) : 'Normal';
        $recordedDate = trim((string)$input['recorded_date']);
        $vetName = !empty($input['vet_name']) ? trim((string)$input['vet_name']) : null;
        $notes = !empty($input['notes']) ? trim((string)$input['notes']) : null;

        $stmt = $pdo->prepare('
            INSERT INTO pet_health_records (
                pet_id, user_id, record_type, title, description, severity, recorded_date, vet_name, notes, created_at, updated_at
            ) VALUES (
                :pet_id, :user_id, :record_type, :title, :description, :severity, :recorded_date, :vet_name, :notes, NOW(), NOW()
            )
        ');
        $stmt->execute([
            ':pet_id' => $petId,
            ':user_id' => $userId,
            ':record_type' => $recordType,
            ':title' => $title,
            ':description' => $description,
            ':severity' => $severity,
            ':recorded_date' => $recordedDate,
            ':vet_name' => $vetName,
            ':notes' => $notes,
        ]);

        $recordId = (int)$pdo->lastInsertId();

        // Recalculate health score immediately
        $healthScoreData = PetHealthScoringService::calculateScore($pdo, $petId, $userId);

        $fetchStmt = $pdo->prepare('SELECT * FROM pet_health_records WHERE id = :id');
        $fetchStmt->execute([':id' => $recordId]);
        $newRecord = $fetchStmt->fetch(PDO::FETCH_ASSOC);
        $newRecord['id'] = (int)$newRecord['id'];
        $newRecord['pet_id'] = (int)$newRecord['pet_id'];

        Response::success([
            'record' => $newRecord,
            'health_score' => $healthScoreData
        ], 'Health record added successfully', 201);
    }

    /**
     * PUT /api/pets/{id}/health-records/{recordId}
     */
    public function updateHealthRecord(array $currentUser, int $petId, int $recordId, array $input): void {
        $errors = Validator::validate($input, [
            'title' => 'required|max:150',
            'recorded_date' => 'required',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $pet = $this->verifyPetOwnership($pdo, $userId, $petId);
        if (!$pet) {
            Response::notFound('Pet profile not found');
        }

        $checkStmt = $pdo->prepare('SELECT id FROM pet_health_records WHERE id = :id AND pet_id = :pet_id AND user_id = :user_id LIMIT 1');
        $checkStmt->execute([':id' => $recordId, ':pet_id' => $petId, ':user_id' => $userId]);
        if (!$checkStmt->fetch()) {
            Response::notFound('Health record not found');
        }

        $recordType = !empty($input['record_type']) ? trim((string)$input['record_type']) : 'Checkup';
        $title = trim((string)$input['title']);
        $description = !empty($input['description']) ? trim((string)$input['description']) : null;
        $severity = !empty($input['severity']) ? ucfirst(strtolower(trim((string)$input['severity']))) : 'Normal';
        $recordedDate = trim((string)$input['recorded_date']);
        $vetName = !empty($input['vet_name']) ? trim((string)$input['vet_name']) : null;
        $notes = !empty($input['notes']) ? trim((string)$input['notes']) : null;

        $upd = $pdo->prepare('
            UPDATE pet_health_records
            SET record_type = :record_type,
                title = :title,
                description = :description,
                severity = :severity,
                recorded_date = :recorded_date,
                vet_name = :vet_name,
                notes = :notes,
                updated_at = NOW()
            WHERE id = :id AND pet_id = :pet_id AND user_id = :user_id
        ');
        $upd->execute([
            ':record_type' => $recordType,
            ':title' => $title,
            ':description' => $description,
            ':severity' => $severity,
            ':recorded_date' => $recordedDate,
            ':vet_name' => $vetName,
            ':notes' => $notes,
            ':id' => $recordId,
            ':pet_id' => $petId,
            ':user_id' => $userId,
        ]);

        $healthScoreData = PetHealthScoringService::calculateScore($pdo, $petId, $userId);

        $fetchStmt = $pdo->prepare('SELECT * FROM pet_health_records WHERE id = :id');
        $fetchStmt->execute([':id' => $recordId]);
        $updatedRecord = $fetchStmt->fetch(PDO::FETCH_ASSOC);
        $updatedRecord['id'] = (int)$updatedRecord['id'];
        $updatedRecord['pet_id'] = (int)$updatedRecord['pet_id'];

        Response::success([
            'record' => $updatedRecord,
            'health_score' => $healthScoreData
        ], 'Health record updated successfully');
    }

    /**
     * DELETE /api/pets/{id}/health-records/{recordId}
     */
    public function deleteHealthRecord(array $currentUser, int $petId, int $recordId): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $pet = $this->verifyPetOwnership($pdo, $userId, $petId);
        if (!$pet) {
            Response::notFound('Pet profile not found');
        }

        $checkStmt = $pdo->prepare('SELECT id FROM pet_health_records WHERE id = :id AND pet_id = :pet_id AND user_id = :user_id LIMIT 1');
        $checkStmt->execute([':id' => $recordId, ':pet_id' => $petId, ':user_id' => $userId]);
        if (!$checkStmt->fetch()) {
            Response::notFound('Health record not found');
        }

        $del = $pdo->prepare('DELETE FROM pet_health_records WHERE id = :id AND pet_id = :pet_id AND user_id = :user_id');
        $del->execute([':id' => $recordId, ':pet_id' => $petId, ':user_id' => $userId]);

        $healthScoreData = PetHealthScoringService::calculateScore($pdo, $petId, $userId);

        Response::success([
            'id' => $recordId,
            'health_score' => $healthScoreData
        ], 'Health record deleted successfully');
    }

    // ------------------------------------------------------------------------
    // Vaccination Records & Status
    // ------------------------------------------------------------------------

    /**
     * GET /api/pets/{id}/vaccinations
     */
    public function getVaccinations(array $currentUser, int $petId): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $pet = $this->verifyPetOwnership($pdo, $userId, $petId);
        if (!$pet) {
            Response::notFound('Pet profile not found');
        }

        $stmt = $pdo->prepare('
            SELECT id, pet_id, vaccine_name, vaccination_date, next_due_date, batch_number, clinic_name, notes, created_at, updated_at
            FROM pet_vaccinations
            WHERE pet_id = :pet_id AND user_id = :user_id
            ORDER BY next_due_date ASC
        ');
        $stmt->execute([':pet_id' => $petId, ':user_id' => $userId]);
        $rawVaccines = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $annotated = [];
        foreach ($rawVaccines as $item) {
            $item['id'] = (int)$item['id'];
            $item['pet_id'] = (int)$item['pet_id'];
            $annotated[] = PetHealthScoringService::annotateVaccine($item);
        }

        $overallStatus = PetHealthScoringService::calculateVaccineStatus($rawVaccines);

        // Synchronize pets table
        $upd = $pdo->prepare('UPDATE pets SET vaccination_status = :status, updated_at = NOW() WHERE id = :id');
        $upd->execute([':status' => $overallStatus, ':id' => $petId]);

        Response::success([
            'overall_status' => $overallStatus,
            'total_vaccines' => count($annotated),
            'vaccinations' => $annotated
        ]);
    }

    /**
     * POST /api/pets/{id}/vaccinations
     */
    public function addVaccination(array $currentUser, int $petId, array $input): void {
        $errors = Validator::validate($input, [
            'vaccine_name' => 'required|max:150',
            'vaccination_date' => 'required',
            'next_due_date' => 'required',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $pet = $this->verifyPetOwnership($pdo, $userId, $petId);
        if (!$pet) {
            Response::notFound('Pet profile not found');
        }

        $vaccineName = trim((string)$input['vaccine_name']);
        $vaccinationDate = trim((string)$input['vaccination_date']);
        $nextDueDate = trim((string)$input['next_due_date']);
        $batchNumber = !empty($input['batch_number']) ? trim((string)$input['batch_number']) : null;
        $clinicName = !empty($input['clinic_name']) ? trim((string)$input['clinic_name']) : null;
        $notes = !empty($input['notes']) ? trim((string)$input['notes']) : null;

        $stmt = $pdo->prepare('
            INSERT INTO pet_vaccinations (
                pet_id, user_id, vaccine_name, vaccination_date, next_due_date, batch_number, clinic_name, notes, created_at, updated_at
            ) VALUES (
                :pet_id, :user_id, :vaccine_name, :vaccination_date, :next_due_date, :batch_number, :clinic_name, :notes, NOW(), NOW()
            )
        ');
        $stmt->execute([
            ':pet_id' => $petId,
            ':user_id' => $userId,
            ':vaccine_name' => $vaccineName,
            ':vaccination_date' => $vaccinationDate,
            ':next_due_date' => $nextDueDate,
            ':batch_number' => $batchNumber,
            ':clinic_name' => $clinicName,
            ':notes' => $notes,
        ]);

        $newId = (int)$pdo->lastInsertId();

        // Recalculate overall status & update pets table
        $allStmt = $pdo->prepare('SELECT * FROM pet_vaccinations WHERE pet_id = :pet_id AND user_id = :user_id ORDER BY next_due_date ASC');
        $allStmt->execute([':pet_id' => $petId, ':user_id' => $userId]);
        $all = $allStmt->fetchAll(PDO::FETCH_ASSOC);

        $overallStatus = PetHealthScoringService::calculateVaccineStatus($all);
        $upd = $pdo->prepare('UPDATE pets SET vaccination_status = :status, updated_at = NOW() WHERE id = :id');
        $upd->execute([':status' => $overallStatus, ':id' => $petId]);

        // Recalculate health score (as vaccination status affects health score)
        $healthScoreData = PetHealthScoringService::calculateScore($pdo, $petId, $userId);

        $fetchStmt = $pdo->prepare('SELECT * FROM pet_vaccinations WHERE id = :id');
        $fetchStmt->execute([':id' => $newId]);
        $created = PetHealthScoringService::annotateVaccine($fetchStmt->fetch(PDO::FETCH_ASSOC));
        $created['id'] = (int)$created['id'];
        $created['pet_id'] = (int)$created['pet_id'];

        Response::success([
            'vaccine' => $created,
            'overall_status' => $overallStatus,
            'health_score' => $healthScoreData
        ], 'Vaccination record added successfully', 201);
    }

    /**
     * PUT /api/pets/{id}/vaccinations/{vaccineId}
     */
    public function updateVaccination(array $currentUser, int $petId, int $vaccineId, array $input): void {
        $errors = Validator::validate($input, [
            'vaccine_name' => 'required|max:150',
            'vaccination_date' => 'required',
            'next_due_date' => 'required',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $pet = $this->verifyPetOwnership($pdo, $userId, $petId);
        if (!$pet) {
            Response::notFound('Pet profile not found');
        }

        $checkStmt = $pdo->prepare('SELECT id FROM pet_vaccinations WHERE id = :id AND pet_id = :pet_id AND user_id = :user_id LIMIT 1');
        $checkStmt->execute([':id' => $vaccineId, ':pet_id' => $petId, ':user_id' => $userId]);
        if (!$checkStmt->fetch()) {
            Response::notFound('Vaccination record not found');
        }

        $vaccineName = trim((string)$input['vaccine_name']);
        $vaccinationDate = trim((string)$input['vaccination_date']);
        $nextDueDate = trim((string)$input['next_due_date']);
        $batchNumber = !empty($input['batch_number']) ? trim((string)$input['batch_number']) : null;
        $clinicName = !empty($input['clinic_name']) ? trim((string)$input['clinic_name']) : null;
        $notes = !empty($input['notes']) ? trim((string)$input['notes']) : null;

        $upd = $pdo->prepare('
            UPDATE pet_vaccinations
            SET vaccine_name = :vaccine_name,
                vaccination_date = :vaccination_date,
                next_due_date = :next_due_date,
                batch_number = :batch_number,
                clinic_name = :clinic_name,
                notes = :notes,
                updated_at = NOW()
            WHERE id = :id AND pet_id = :pet_id AND user_id = :user_id
        ');
        $upd->execute([
            ':vaccine_name' => $vaccineName,
            ':vaccination_date' => $vaccinationDate,
            ':next_due_date' => $nextDueDate,
            ':batch_number' => $batchNumber,
            ':clinic_name' => $clinicName,
            ':notes' => $notes,
            ':id' => $vaccineId,
            ':pet_id' => $petId,
            ':user_id' => $userId,
        ]);

        // Recalculate overall status & update pets table
        $allStmt = $pdo->prepare('SELECT * FROM pet_vaccinations WHERE pet_id = :pet_id AND user_id = :user_id ORDER BY next_due_date ASC');
        $allStmt->execute([':pet_id' => $petId, ':user_id' => $userId]);
        $all = $allStmt->fetchAll(PDO::FETCH_ASSOC);

        $overallStatus = PetHealthScoringService::calculateVaccineStatus($all);
        $updPet = $pdo->prepare('UPDATE pets SET vaccination_status = :status, updated_at = NOW() WHERE id = :id');
        $updPet->execute([':status' => $overallStatus, ':id' => $petId]);

        $healthScoreData = PetHealthScoringService::calculateScore($pdo, $petId, $userId);

        $fetchStmt = $pdo->prepare('SELECT * FROM pet_vaccinations WHERE id = :id');
        $fetchStmt->execute([':id' => $vaccineId]);
        $updated = PetHealthScoringService::annotateVaccine($fetchStmt->fetch(PDO::FETCH_ASSOC));
        $updated['id'] = (int)$updated['id'];
        $updated['pet_id'] = (int)$updated['pet_id'];

        Response::success([
            'vaccine' => $updated,
            'overall_status' => $overallStatus,
            'health_score' => $healthScoreData
        ], 'Vaccination record updated successfully');
    }

    /**
     * DELETE /api/pets/{id}/vaccinations/{vaccineId}
     */
    public function deleteVaccination(array $currentUser, int $petId, int $vaccineId): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $pet = $this->verifyPetOwnership($pdo, $userId, $petId);
        if (!$pet) {
            Response::notFound('Pet profile not found');
        }

        $checkStmt = $pdo->prepare('SELECT id FROM pet_vaccinations WHERE id = :id AND pet_id = :pet_id AND user_id = :user_id LIMIT 1');
        $checkStmt->execute([':id' => $vaccineId, ':pet_id' => $petId, ':user_id' => $userId]);
        if (!$checkStmt->fetch()) {
            Response::notFound('Vaccination record not found');
        }

        $del = $pdo->prepare('DELETE FROM pet_vaccinations WHERE id = :id AND pet_id = :pet_id AND user_id = :user_id');
        $del->execute([':id' => $vaccineId, ':pet_id' => $petId, ':user_id' => $userId]);

        // Recalculate overall status & update pets table
        $allStmt = $pdo->prepare('SELECT * FROM pet_vaccinations WHERE pet_id = :pet_id AND user_id = :user_id ORDER BY next_due_date ASC');
        $allStmt->execute([':pet_id' => $petId, ':user_id' => $userId]);
        $all = $allStmt->fetchAll(PDO::FETCH_ASSOC);

        $overallStatus = PetHealthScoringService::calculateVaccineStatus($all);
        $updPet = $pdo->prepare('UPDATE pets SET vaccination_status = :status, updated_at = NOW() WHERE id = :id');
        $updPet->execute([':status' => $overallStatus, ':id' => $petId]);

        $healthScoreData = PetHealthScoringService::calculateScore($pdo, $petId, $userId);

        Response::success([
            'id' => $vaccineId,
            'overall_status' => $overallStatus,
            'health_score' => $healthScoreData
        ], 'Vaccination record deleted successfully');
    }
}
