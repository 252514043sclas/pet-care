<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Utils\Database;
use App\Utils\Response;
use App\Utils\Validator;
use PDO;

class HealthController {
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

    public function conditions(): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT id, condition_name, species, severity, description FROM disease_guides ORDER BY id ASC');
        $stmt->execute();
        $conditions = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($conditions);
    }

    public function conditionDetail(int $id): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM disease_guides WHERE id = :id LIMIT 1');
        $stmt->execute([':id' => $id]);
        $condition = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$condition) {
            // Provide default condition guide (matches DiseaseDetailActivity)
            $condition = [
                'id' => 1,
                'condition_name' => 'Mite-related irritation',
                'species' => 'Rabbit',
                'severity' => 'Moderate',
                'badge' => 'Example only · Vet exam needed',
                'description' => 'Mites can cause flaky skin, itching or patchy fur in rabbits. Other problems can look similar, so this is a possible explanation—not a diagnosis.',
                'causes' => 'Contact with affected animals or contaminated bedding may spread mites. Only your vet can confirm the cause.',
                'interim_steps' => [
                    'Keep your pet’s bedding clean and dry. Avoid new scented products.',
                    'Record changes in skin, appetite and droppings for your vet.',
                    'Do not apply dog or cat flea products, creams or home remedies.',
                ],
                'vet_warning' => 'Book a rabbit-savvy vet exam for skin changes. Seek urgent care for reduced eating, fewer droppings, marked lethargy or painful, broken skin.',
            ];
            Response::success($condition);
        }

        $condition['interim_steps'] = json_decode($condition['interim_steps_json'] ?? '[]', true);
        $condition['badge'] = 'Example only · Vet exam needed';
        unset($condition['interim_steps_json']);

        Response::success($condition);
    }

    public function saveReport(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'report_title' => 'required|max:255',
        ]);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : $this->getActivePetId($pdo, (int)$currentUser['id']);

        if (!$petId) {
            Response::error('No pet profile found.', 400);
        }

        $reportData = $input['report_data'] ?? ['title' => $input['report_title'], 'saved_at' => date('Y-m-d H:i:s')];
        $reportJson = json_encode($reportData);

        $stmt = $pdo->prepare(
            'INSERT INTO saved_health_reports (user_id, pet_id, report_title, report_data_json, saved_at)
             VALUES (:user_id, :pet_id, :report_title, :report_data_json, NOW())'
        );
        $stmt->execute([
            ':user_id' => $currentUser['id'],
            ':pet_id' => $petId,
            ':report_title' => trim($input['report_title']),
            ':report_data_json' => $reportJson,
        ]);

        Response::success([
            'id' => (int)$pdo->lastInsertId(),
            'report_title' => trim($input['report_title']),
        ], 'Health report saved successfully', 201);
    }

    public function savedReports(array $currentUser): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'SELECT r.id, r.report_title, r.saved_at, p.name AS pet_name 
             FROM saved_health_reports r 
             INNER JOIN pets p ON r.pet_id = p.id 
             WHERE r.user_id = :user_id 
             ORDER BY r.id DESC'
        );
        $stmt->execute([':user_id' => $currentUser['id']]);
        $reports = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($reports);
    }
}

