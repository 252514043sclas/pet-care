<?php

declare(strict_types=1);

namespace App\Services;

use PDO;

class PetHealthScoringService {

    /**
     * Calculates the health score for a pet based on actual database records.
     * Returns an associative array containing:
     * - score: int|null (null if insufficient data)
     * - has_sufficient_data: bool
     * - status_label: string ("Excellent", "Good", "Fair", "Needs Attention", "Not enough health data")
     * - total_records: int
     * - breakdown: array of score factors
     * - health_records: list of recent health records
     * - recent_scans: list of recent health scans
     */
    public static function calculateScore(PDO $pdo, int $petId, int $userId): array {
        // 1. Fetch pet details
        $petStmt = $pdo->prepare('SELECT id, name, species, weight_kg, target_weight_kg FROM pets WHERE id = :id AND user_id = :user_id LIMIT 1');
        $petStmt->execute([':id' => $petId, ':user_id' => $userId]);
        $pet = $petStmt->fetch(PDO::FETCH_ASSOC);

        if (!$pet) {
            return [
                'score' => null,
                'has_sufficient_data' => false,
                'status_label' => 'Not enough health data',
                'total_records' => 0,
                'breakdown' => [],
                'health_records' => [],
                'recent_scans' => []
            ];
        }

        // 2. Fetch pet health records
        $hrStmt = $pdo->prepare('
            SELECT id, record_type, title, description, severity, recorded_date, vet_name, notes, created_at
            FROM pet_health_records
            WHERE pet_id = :pet_id AND user_id = :user_id
            ORDER BY recorded_date DESC, id DESC
        ');
        $hrStmt->execute([':pet_id' => $petId, ':user_id' => $userId]);
        $healthRecords = $hrStmt->fetchAll(PDO::FETCH_ASSOC);

        // 3. Fetch health scans
        $hsStmt = $pdo->prepare('
            SELECT id, scan_type, symptoms_json, diagnosis_title, match_percentage, severity, summary, created_at
            FROM health_scans
            WHERE pet_id = :pet_id AND user_id = :user_id
            ORDER BY id DESC
            LIMIT 5
        ');
        $hsStmt->execute([':pet_id' => $petId, ':user_id' => $userId]);
        $healthScans = $hsStmt->fetchAll(PDO::FETCH_ASSOC);

        // 4. Fetch weight entries count & latest entry
        $weStmt = $pdo->prepare('
            SELECT weight_kg, recorded_date
            FROM weight_entries
            WHERE pet_id = :pet_id AND user_id = :user_id
            ORDER BY id DESC
            LIMIT 1
        ');
        $weStmt->execute([':pet_id' => $petId, ':user_id' => $userId]);
        $latestWeightEntry = $weStmt->fetch(PDO::FETCH_ASSOC);

        // 5. Fetch vaccinations
        $vacStmt = $pdo->prepare('
            SELECT id, vaccine_name, vaccination_date, next_due_date, batch_number, clinic_name, notes
            FROM pet_vaccinations
            WHERE pet_id = :pet_id AND user_id = :user_id
            ORDER BY next_due_date ASC
        ');
        $vacStmt->execute([':pet_id' => $petId, ':user_id' => $userId]);
        $vaccines = $vacStmt->fetchAll(PDO::FETCH_ASSOC);

        $totalRecords = count($healthRecords) + count($healthScans) + ($latestWeightEntry ? 1 : 0) + count($vaccines);

        // Check data sufficiency rule: If 0 health records, 0 scans, 0 weight entries, and 0 vaccines
        if ($totalRecords === 0) {
            // Update pet table to reflect null score
            $upd = $pdo->prepare('UPDATE pets SET health_score = NULL, updated_at = NOW() WHERE id = :id');
            $upd->execute([':id' => $petId]);

            return [
                'score' => null,
                'has_sufficient_data' => false,
                'status_label' => 'Not enough health data',
                'total_records' => 0,
                'breakdown' => [
                    [
                        'factor' => 'Baseline Wellness Assessment',
                        'impact' => 0,
                        'description' => 'No recorded veterinary visits, checkups, or symptoms logged yet.'
                    ]
                ],
                'health_records' => [],
                'recent_scans' => []
            ];
        }

        // Calculate score starting from base 100
        $baseScore = 100;
        $breakdown = [];

        $breakdown[] = [
            'factor' => 'Baseline Wellness',
            'impact' => 100,
            'description' => 'Standard starting clinical wellness score.'
        ];

        // A. Health Records Impact
        $medicalDeductions = 0;
        $checkupBonus = 0;
        foreach ($healthRecords as $rec) {
            $sev = strtolower(trim((string)$rec['severity']));
            $type = strtolower(trim((string)$rec['record_type']));

            if ($sev === 'severe') {
                $medicalDeductions += 25;
            } elseif ($sev === 'moderate') {
                $medicalDeductions += 15;
            } elseif ($sev === 'mild') {
                $medicalDeductions += 6;
            } elseif ($type === 'checkup' || $sev === 'normal') {
                $checkupBonus += 2;
            }
        }

        if ($medicalDeductions > 0) {
            $cappedMed = min($medicalDeductions, 40);
            $baseScore -= $cappedMed;
            $breakdown[] = [
                'factor' => 'Medical History & Symptoms',
                'impact' => -$cappedMed,
                'description' => "Deductions for recorded health concerns and active symptoms."
            ];
        } elseif ($checkupBonus > 0) {
            $breakdown[] = [
                'factor' => 'Routine Veterinary Checkups',
                'impact' => 0,
                'description' => "Routine exams confirm normal wellness status."
            ];
        }

        // B. Health Scans Impact
        $scanDeduction = 0;
        foreach ($healthScans as $scan) {
            $sev = strtolower(trim((string)$scan['severity']));
            if ($sev === 'severe') {
                $scanDeduction += 15;
            } elseif ($sev === 'moderate') {
                $scanDeduction += 8;
            } elseif ($sev === 'mild') {
                $scanDeduction += 3;
            }
        }
        if ($scanDeduction > 0) {
            $cappedScan = min($scanDeduction, 25);
            $baseScore -= $cappedScan;
            $breakdown[] = [
                'factor' => 'Diagnostic Scan Indicators',
                'impact' => -$cappedScan,
                'description' => "Observations from symptom analysis and health scans."
            ];
        }

        // C. Weight Tracking Impact
        $currentWeight = $latestWeightEntry ? (float)$latestWeightEntry['weight_kg'] : (float)$pet['weight_kg'];
        $targetWeight = $pet['target_weight_kg'] !== null ? (float)$pet['target_weight_kg'] : null;

        if ($targetWeight !== null && $targetWeight > 0.1 && $currentWeight > 0.1) {
            $pctDiff = abs($currentWeight - $targetWeight) / $targetWeight * 100;
            if ($pctDiff > 20) {
                $baseScore -= 10;
                $breakdown[] = [
                    'factor' => 'Weight Variance',
                    'impact' => -10,
                    'description' => "Current weight ({$currentWeight} kg) deviates >20% from target ({$targetWeight} kg)."
                ];
            } elseif ($pctDiff > 10) {
                $baseScore -= 5;
                $breakdown[] = [
                    'factor' => 'Weight Variance',
                    'impact' => -5,
                    'description' => "Current weight ({$currentWeight} kg) is slightly off target ({$targetWeight} kg)."
                ];
            } else {
                $breakdown[] = [
                    'factor' => 'Weight Stability',
                    'impact' => 0,
                    'description' => "Weight is within optimal target range."
                ];
            }
        } elseif ($latestWeightEntry) {
            $breakdown[] = [
                'factor' => 'Weight Monitoring',
                'impact' => 0,
                'description' => "Active weight tracking recorded ({$currentWeight} kg)."
            ];
        }

        // D. Vaccination Status Impact
        $species = strtolower(trim((string)$pet['species']));
        $isVaccineRequiredSpecies = in_array($species, ['dog', 'cat', 'rabbit', 'pigeon', 'bird'], true);

        if ($isVaccineRequiredSpecies) {
            $vacStatus = self::calculateVaccineStatus($vaccines);
            if ($vacStatus === 'OVERDUE') {
                $baseScore -= 12;
                $breakdown[] = [
                    'factor' => 'Vaccination Status (Overdue)',
                    'impact' => -12,
                    'description' => 'Pet has overdue vaccinations, increasing risk of preventable illness.'
                ];
            } elseif ($vacStatus === 'DUE SOON') {
                $baseScore -= 3;
                $breakdown[] = [
                    'factor' => 'Vaccination Status (Due Soon)',
                    'impact' => -3,
                    'description' => 'Vaccine booster due in the next 30 days.'
                ];
            } elseif ($vacStatus === 'UP TO DATE') {
                $breakdown[] = [
                    'factor' => 'Vaccination Protection',
                    'impact' => 0,
                    'description' => 'All recorded vaccinations are completed and up to date.'
                ];
            } else {
                // NO RECORDS
                $baseScore -= 8;
                $breakdown[] = [
                    'factor' => 'Vaccination History Missing',
                    'impact' => -8,
                    'description' => 'No immunization records registered for this pet.'
                ];
            }
        } else {
            // For species like hamsters/fish that do not receive routine viral vaccines
            $breakdown[] = [
                'factor' => 'Species Preventative Care',
                'impact' => 0,
                'description' => "Routine viral vaccines not required for {$pet['species']}."
            ];
        }

        $finalScore = max(20, min(100, $baseScore));

        // Status Label
        $statusLabel = 'Excellent';
        if ($finalScore < 60) {
            $statusLabel = 'Needs Attention';
        } elseif ($finalScore < 75) {
            $statusLabel = 'Fair';
        } elseif ($finalScore < 90) {
            $statusLabel = 'Good';
        }

        // Persist computed score in database
        $upd = $pdo->prepare('UPDATE pets SET health_score = :score, updated_at = NOW() WHERE id = :id');
        $upd->execute([':score' => $finalScore, ':id' => $petId]);

        return [
            'score' => $finalScore,
            'has_sufficient_data' => true,
            'status_label' => $statusLabel,
            'total_records' => $totalRecords,
            'breakdown' => $breakdown,
            'health_records' => $healthRecords,
            'recent_scans' => $healthScans
        ];
    }

    /**
     * Calculates overall pet vaccination status from records.
     * Returns "UP TO DATE", "DUE SOON", "OVERDUE", or "NO RECORDS".
     */
    public static function calculateVaccineStatus(array $vaccines): string {
        if (empty($vaccines)) {
            return 'NO RECORDS';
        }

        $today = date('Y-m-d');
        $hasOverdue = false;
        $hasDueSoon = false;

        foreach ($vaccines as $vac) {
            $dueDate = trim((string)($vac['next_due_date'] ?? ''));
            if ($dueDate === '') {
                continue;
            }

            $dueTs = strtotime($dueDate);
            $todayTs = strtotime($today);
            $diffDays = (int)round(($dueTs - $todayTs) / 86400);

            if ($diffDays < 0) {
                $hasOverdue = true;
            } elseif ($diffDays <= 30) {
                $hasDueSoon = true;
            }
        }

        if ($hasOverdue) {
            return 'OVERDUE';
        }
        if ($hasDueSoon) {
            return 'DUE SOON';
        }
        return 'UP TO DATE';
    }

    /**
     * Computes individual status string and days left for a single vaccination record.
     */
    public static function annotateVaccine(array $vac): array {
        $today = date('Y-m-d');
        $dueDate = trim((string)($vac['next_due_date'] ?? ''));

        if ($dueDate === '') {
            $vac['status'] = 'UP TO DATE';
            $vac['days_until_due'] = 0;
            $vac['due_description'] = 'No due date specified';
            return $vac;
        }

        $dueTs = strtotime($dueDate);
        $todayTs = strtotime($today);
        $diffDays = (int)round(($dueTs - $todayTs) / 86400);

        $vac['days_until_due'] = $diffDays;

        if ($diffDays < 0) {
            $vac['status'] = 'OVERDUE';
            $vac['due_description'] = abs($diffDays) . ' days overdue';
        } elseif ($diffDays <= 30) {
            $vac['status'] = 'DUE SOON';
            $vac['due_description'] = $diffDays === 0 ? 'Due today' : "Due in {$diffDays} days";
        } else {
            $vac['status'] = 'UP TO DATE';
            $vac['due_description'] = "Valid for {$diffDays} days";
        }

        return $vac;
    }
}
