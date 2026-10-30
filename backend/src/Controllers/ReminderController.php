<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Utils\Database;
use App\Utils\Response;
use App\Utils\Validator;
use PDO;

class ReminderController {
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

    public function index(array $currentUser): void {
        $pdo = Database::getConnection();
        $petId = $this->getActivePetId($pdo, (int)$currentUser['id']);

        $reminders = [];
        if ($petId) {
            $stmt = $pdo->prepare('SELECT * FROM care_reminders WHERE user_id = :user_id AND pet_id = :pet_id ORDER BY is_completed ASC, id DESC');
            $stmt->execute([':user_id' => $currentUser['id'], ':pet_id' => $petId]);
            $reminders = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }

        Response::success($reminders);
    }

    public function store(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'title' => 'required|max:255',
            'reminder_time' => 'required',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : $this->getActivePetId($pdo, (int)$currentUser['id']);

        if (!$petId) {
            Response::error('No pet profile found.', 400);
        }

        $title = trim($input['title']);
        $time = trim($input['reminder_time']);
        $category = $input['category'] ?? 'Grooming';
        $notifyPush = isset($input['notify_push']) ? (int)$input['notify_push'] : 1;

        $stmt = $pdo->prepare(
            'INSERT INTO care_reminders (pet_id, user_id, title, reminder_time, category, notify_push, is_completed, created_at)
             VALUES (:pet_id, :user_id, :title, :reminder_time, :category, :notify_push, 0, NOW())'
        );
        $stmt->execute([
            ':pet_id' => $petId,
            ':user_id' => $currentUser['id'],
            ':title' => $title,
            ':reminder_time' => $time,
            ':category' => $category,
            ':notify_push' => $notifyPush,
        ]);

        $id = (int)$pdo->lastInsertId();

        $fetchStmt = $pdo->prepare('SELECT * FROM care_reminders WHERE id = :id');
        $fetchStmt->execute([':id' => $id]);

        Response::success($fetchStmt->fetch(PDO::FETCH_ASSOC), 'Reminder saved successfully', 201);
    }

    public function toggle(array $currentUser, int $id): void {
        $pdo = Database::getConnection();
        $check = $pdo->prepare('SELECT id, is_completed FROM care_reminders WHERE id = :id AND user_id = :user_id LIMIT 1');
        $check->execute([':id' => $id, ':user_id' => $currentUser['id']]);
        $reminder = $check->fetch(PDO::FETCH_ASSOC);

        if (!$reminder) {
            Response::notFound('Reminder not found');
        }

        $newStatus = $reminder['is_completed'] ? 0 : 1;
        $upd = $pdo->prepare('UPDATE care_reminders SET is_completed = :status, updated_at = NOW() WHERE id = :id');
        $upd->execute([':status' => $newStatus, ':id' => $id]);

        Response::success(['id' => $id, 'is_completed' => (bool)$newStatus], 'Status updated');
    }

    public function calendar(array $currentUser): void {
        $pdo = Database::getConnection();
        $petId = $this->getActivePetId($pdo, (int)$currentUser['id']);

        $tasks = [];
        if ($petId) {
            $stmt = $pdo->prepare('SELECT id, reminder_time as date_time, title as task, is_completed FROM care_reminders WHERE user_id = :user_id AND pet_id = :pet_id ORDER BY id ASC');
            $stmt->execute([':user_id' => $currentUser['id'], ':pet_id' => $petId]);
            $tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }

        if (empty($tasks)) {
            $tasks = [
                ['id' => 1, 'date_time' => 'Oct 12, 10:00 AM', 'task' => 'Bath & Shampoo Session', 'is_completed' => false],
                ['id' => 2, 'date_time' => 'Oct 19, 2:00 PM', 'task' => 'Nail Trimming & Filing', 'is_completed' => false],
                ['id' => 3, 'date_time' => 'Oct 26, 11:00 AM', 'task' => 'Ear Cleaning & Checkup', 'is_completed' => false],
                ['id' => 4, 'date_time' => 'Nov 02, 10:00 AM', 'task' => 'Full Grooming & Haircut', 'is_completed' => false],
            ];
        }

        Response::success($tasks);
    }

    public function guides(): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM grooming_guides ORDER BY id ASC');
        $stmt->execute();
        $guides = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($guides);
    }
}

