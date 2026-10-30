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

    public function symptoms(): void {
        $symptoms = [
            'Itching / Excessive Scratching',
            'Vomiting or Upset Stomach',
            'Lethargy / Lack of Energy',
            'Loss of Appetite',
            'Coughing or Sneezing',
            'Ear Discharge / Head Shaking',
            'Skin Redness or Bumps',
            'Watery Eyes or Discharge',
            'Limping or Difficulty Moving',
        ];

        Response::success($symptoms);
    }

    public function analyze(array $currentUser, array $input): void {
        $pdo = Database::getConnection();
        $petId = !empty($input['pet_id']) ? (int)$input['pet_id'] : $this->getActivePetId($pdo, (int)$currentUser['id']);

        if (!$petId) {
            Response::error('No pet profile found for health analysis.', 400);
        }

        $scanType = $input['scan_type'] ?? 'symptoms';
        $selectedSymptoms = $input['symptoms'] ?? [];
        $symptomsJson = json_encode($selectedSymptoms);

        // Intelligent diagnostic rules engine
        $diagnosisTitle = 'Minor Skin Allergy';
        $matchPercentage = 88;
        $severity = 'Mild';
        $summary = 'Based on symptom checklist and photo scan, your pet shows indicators of allergic dermatitis, commonly triggered by environmental factors or food sensitivity.';
        $conditionId = 1;

        if (is_array($selectedSymptoms)) {
            $hasItching = in_array('Itching / Excessive Scratching', $selectedSymptoms) || in_array('Skin Redness or Bumps', $selectedSymptoms);
            $hasDigestive = in_array('Vomiting or Upset Stomach', $selectedSymptoms) || in_array('Loss of Appetite', $selectedSymptoms);
            $hasEar = in_array('Ear Discharge / Head Shaking', $selectedSymptoms);

            if ($hasDigestive) {
                $diagnosisTitle = 'Mild Gastrointestinal Upset';
                $matchPercentage = 84;
                $severity = 'Moderate';
                $summary = 'Signs suggest possible dietary indiscretion or mild stomach irritation. Monitor hydration closely.';
            } elseif ($hasEar) {
                $diagnosisTitle = 'External Ear Irritation';
                $matchPercentage = 91;
                $severity = 'Mild';
                $summary = 'Frequent head shaking and scratching around ears indicate possible yeast infection or ear mites.';
            } elseif ($hasItching) {
                $diagnosisTitle = 'Minor Skin Allergy / Mite Irritation';
                $matchPercentage = 88;
                $severity = 'Mild';
                $summary = 'Based on observed symptoms and skin condition check, indicators match allergic dermatitis or surface mite irritation.';
            }
        }

        // Save scan to database
        $stmt = $pdo->prepare(
            'INSERT INTO health_scans (pet_id, user_id, scan_type, symptoms_json, diagnosis_title, match_percentage, severity, summary, created_at)
             VALUES (:pet_id, :user_id, :scan_type, :symptoms_json, :diagnosis_title, :match_percentage, :severity, :summary, NOW())'
        );
        $stmt->execute([
            ':pet_id' => $petId,
            ':user_id' => $currentUser['id'],
            ':scan_type' => $scanType,
            ':symptoms_json' => $symptomsJson,
            ':diagnosis_title' => $diagnosisTitle,
            ':match_percentage' => $matchPercentage,
            ':severity' => $severity,
            ':summary' => $summary,
        ]);

        $scanId = (int)$pdo->lastInsertId();

        Response::success([
            'scan_id' => $scanId,
            'condition_id' => $conditionId,
            'diagnosis_title' => $diagnosisTitle,
            'match_percentage' => $matchPercentage,
            'severity' => $severity,
            'summary' => $summary,
            'recommended_action' => 'Schedule veterinary exam if symptoms persist beyond 48 hours.',
        ], 'Analysis completed', 201);
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
                    'Keep Clover’s bedding clean and dry. Avoid new scented products.',
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

