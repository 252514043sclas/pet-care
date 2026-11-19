<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Utils\Database;
use App\Utils\Response;
use App\Utils\Validator;
use PDO;

class GroomingController {
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

    private function verifyPetOwnership(PDO $pdo, int $userId, int $petId): bool {
        $stmt = $pdo->prepare('SELECT id FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $stmt->execute([':id' => $petId, ':user_id' => $userId]);
        return (bool)$stmt->fetch(PDO::FETCH_ASSOC);
    }

    /**
     * GET /api/grooming/reminders
     * Supports optional query params: pet_id, status ('Scheduled' | 'Completed')
     */
    public function index(array $currentUser): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $petIdParam = isset($_GET['pet_id']) && is_numeric($_GET['pet_id']) ? (int)$_GET['pet_id'] : null;
        $statusParam = isset($_GET['status']) ? trim((string)$_GET['status']) : null;

        $query = '
            SELECT 
                r.id,
                r.user_id,
                r.pet_id,
                p.name AS pet_name,
                p.species AS pet_species,
                r.activity,
                r.reminder_date,
                r.reminder_time,
                r.notification_enabled,
                r.status,
                r.created_at,
                r.updated_at
            FROM grooming_reminders r
            LEFT JOIN pets p ON r.pet_id = p.id
            WHERE r.user_id = :user_id
        ';

        $params = [':user_id' => $userId];

        if ($petIdParam !== null && $petIdParam > 0) {
            $query .= ' AND r.pet_id = :pet_id';
            $params[':pet_id'] = $petIdParam;
        }

        if ($statusParam !== null && $statusParam !== '') {
            $query .= ' AND r.status = :status';
            $params[':status'] = $statusParam;
        }

        $query .= ' ORDER BY r.reminder_date ASC, r.reminder_time ASC, r.id DESC';

        $stmt = $pdo->prepare($query);
        $stmt->execute($params);
        $records = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Normalize boolean notification_enabled
        foreach ($records as &$item) {
            $item['id'] = (int)$item['id'];
            $item['user_id'] = (int)$item['user_id'];
            $item['pet_id'] = (int)$item['pet_id'];
            $item['notification_enabled'] = (bool)$item['notification_enabled'];
            $item['pet_name'] = $item['pet_name'] ?? 'Pet';
            $item['pet_species'] = $item['pet_species'] ?? 'Pet';
        }

        Response::success($records);
    }

    /**
     * GET /api/grooming/reminders/{id}
     */
    public function show(array $currentUser, int $id): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $stmt = $pdo->prepare('
            SELECT 
                r.id,
                r.user_id,
                r.pet_id,
                p.name AS pet_name,
                p.species AS pet_species,
                r.activity,
                r.reminder_date,
                r.reminder_time,
                r.notification_enabled,
                r.status,
                r.created_at,
                r.updated_at
            FROM grooming_reminders r
            LEFT JOIN pets p ON r.pet_id = p.id
            WHERE r.id = :id AND r.user_id = :user_id
            LIMIT 1
        ');
        $stmt->execute([':id' => $id, ':user_id' => $userId]);
        $reminder = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$reminder) {
            Response::notFound('Grooming reminder not found');
        }

        $reminder['id'] = (int)$reminder['id'];
        $reminder['user_id'] = (int)$reminder['user_id'];
        $reminder['pet_id'] = (int)$reminder['pet_id'];
        $reminder['notification_enabled'] = (bool)$reminder['notification_enabled'];
        $reminder['pet_name'] = $reminder['pet_name'] ?? 'Pet';
        $reminder['pet_species'] = $reminder['pet_species'] ?? 'Pet';

        Response::success($reminder);
    }

    /**
     * POST /api/grooming/reminders
     */
    public function store(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'activity' => 'required|max:100',
            'reminder_date' => 'required',
            'reminder_time' => 'required',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : $this->getActivePetId($pdo, $userId);
        if (!$petId || !$this->verifyPetOwnership($pdo, $userId, $petId)) {
            Response::error('Invalid or unselected pet profile.', 400);
        }

        $activity = trim((string)$input['activity']);
        $reminderDate = trim((string)$input['reminder_date']);
        $reminderTime = trim((string)$input['reminder_time']);
        $notificationEnabled = isset($input['notification_enabled']) ? ($input['notification_enabled'] ? 1 : 0) : 1;
        $status = !empty($input['status']) && in_array(ucfirst(trim((string)$input['status'])), ['Scheduled', 'Completed'], true)
            ? ucfirst(trim((string)$input['status']))
            : 'Scheduled';

        $stmt = $pdo->prepare('
            INSERT INTO grooming_reminders (
                user_id,
                pet_id,
                activity,
                reminder_date,
                reminder_time,
                notification_enabled,
                status,
                created_at,
                updated_at
            ) VALUES (
                :user_id,
                :pet_id,
                :activity,
                :reminder_date,
                :reminder_time,
                :notification_enabled,
                :status,
                NOW(),
                NOW()
            )
        ');

        $stmt->execute([
            ':user_id' => $userId,
            ':pet_id' => $petId,
            ':activity' => $activity,
            ':reminder_date' => $reminderDate,
            ':reminder_time' => $reminderTime,
            ':notification_enabled' => $notificationEnabled,
            ':status' => $status,
        ]);

        $newId = (int)$pdo->lastInsertId();

        $this->show($currentUser, $newId);
    }

    /**
     * PUT /api/grooming/reminders/{id}
     */
    public function update(array $currentUser, int $id, array $input): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $checkStmt = $pdo->prepare('SELECT id, pet_id FROM grooming_reminders WHERE id = :id AND user_id = :user_id LIMIT 1');
        $checkStmt->execute([':id' => $id, ':user_id' => $userId]);
        $existing = $checkStmt->fetch(PDO::FETCH_ASSOC);

        if (!$existing) {
            Response::notFound('Grooming reminder not found');
        }

        $errors = Validator::validate($input, [
            'activity' => 'required|max:100',
            'reminder_date' => 'required',
            'reminder_time' => 'required',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : (int)$existing['pet_id'];
        if (!$this->verifyPetOwnership($pdo, $userId, $petId)) {
            Response::error('Invalid pet profile.', 400);
        }

        $activity = trim((string)$input['activity']);
        $reminderDate = trim((string)$input['reminder_date']);
        $reminderTime = trim((string)$input['reminder_time']);
        $notificationEnabled = isset($input['notification_enabled']) ? ($input['notification_enabled'] ? 1 : 0) : 1;
        $status = !empty($input['status']) && in_array(ucfirst(trim((string)$input['status'])), ['Scheduled', 'Completed'], true)
            ? ucfirst(trim((string)$input['status']))
            : null;

        if ($status !== null) {
            $updStmt = $pdo->prepare('
                UPDATE grooming_reminders
                SET pet_id = :pet_id,
                    activity = :activity,
                    reminder_date = :reminder_date,
                    reminder_time = :reminder_time,
                    notification_enabled = :notification_enabled,
                    status = :status,
                    updated_at = NOW()
                WHERE id = :id AND user_id = :user_id
            ');
            $updStmt->execute([
                ':pet_id' => $petId,
                ':activity' => $activity,
                ':reminder_date' => $reminderDate,
                ':reminder_time' => $reminderTime,
                ':notification_enabled' => $notificationEnabled,
                ':status' => $status,
                ':id' => $id,
                ':user_id' => $userId,
            ]);
        } else {
            $updStmt = $pdo->prepare('
                UPDATE grooming_reminders
                SET pet_id = :pet_id,
                    activity = :activity,
                    reminder_date = :reminder_date,
                    reminder_time = :reminder_time,
                    notification_enabled = :notification_enabled,
                    updated_at = NOW()
                WHERE id = :id AND user_id = :user_id
            ');
            $updStmt->execute([
                ':pet_id' => $petId,
                ':activity' => $activity,
                ':reminder_date' => $reminderDate,
                ':reminder_time' => $reminderTime,
                ':notification_enabled' => $notificationEnabled,
                ':id' => $id,
                ':user_id' => $userId,
            ]);
        }

        $this->show($currentUser, $id);
    }

    /**
     * PATCH /api/grooming/reminders/{id}/status
     */
    public function updateStatus(array $currentUser, int $id, array $input): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $checkStmt = $pdo->prepare('SELECT id, status FROM grooming_reminders WHERE id = :id AND user_id = :user_id LIMIT 1');
        $checkStmt->execute([':id' => $id, ':user_id' => $userId]);
        $existing = $checkStmt->fetch(PDO::FETCH_ASSOC);

        if (!$existing) {
            Response::notFound('Grooming reminder not found');
        }

        $currentStatus = $existing['status'] ?? 'Scheduled';
        $newStatus = !empty($input['status']) ? ucfirst(trim((string)$input['status'])) : ($currentStatus === 'Scheduled' ? 'Completed' : 'Scheduled');

        if (!in_array($newStatus, ['Scheduled', 'Completed'], true)) {
            $newStatus = 'Completed';
        }

        $upd = $pdo->prepare('
            UPDATE grooming_reminders 
            SET status = :status, updated_at = NOW() 
            WHERE id = :id AND user_id = :user_id
        ');
        $upd->execute([
            ':status' => $newStatus,
            ':id' => $id,
            ':user_id' => $userId,
        ]);

        $this->show($currentUser, $id);
    }

    /**
     * DELETE /api/grooming/reminders/{id}
     */
    public function destroy(array $currentUser, int $id): void {
        $pdo = Database::getConnection();
        $userId = (int)$currentUser['id'];

        $checkStmt = $pdo->prepare('SELECT id FROM grooming_reminders WHERE id = :id AND user_id = :user_id LIMIT 1');
        $checkStmt->execute([':id' => $id, ':user_id' => $userId]);
        if (!$checkStmt->fetch()) {
            Response::notFound('Grooming reminder not found');
        }

        $delStmt = $pdo->prepare('DELETE FROM grooming_reminders WHERE id = :id AND user_id = :user_id');
        $delStmt->execute([':id' => $id, ':user_id' => $userId]);

        Response::success(['id' => $id], 'Grooming reminder deleted successfully');
    }
}
