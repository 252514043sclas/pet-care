<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Utils\Database;
use App\Utils\Response;
use PDO;

class DashboardController {
    public function index(array $currentUser): void {
        $pdo = Database::getConnection();

        // 1. Fetch active pet
        $petStmt = $pdo->prepare('SELECT * FROM pets WHERE user_id = :user_id AND is_active = 1 LIMIT 1');
        $petStmt->execute([':user_id' => $currentUser['id']]);
        $activePet = $petStmt->fetch(PDO::FETCH_ASSOC);

        if (!$activePet) {
            // Fallback to any pet
            $anyPet = $pdo->prepare('SELECT * FROM pets WHERE user_id = :user_id ORDER BY id ASC LIMIT 1');
            $anyPet->execute([':user_id' => $currentUser['id']]);
            $activePet = $anyPet->fetch(PDO::FETCH_ASSOC) ?: null;
        }

        $schedule = [];
        $mealsToday = [];
        $remindersToday = [];
        $totalCalories = 0;
        $targetCalories = 800;

        if ($activePet) {
            $petId = (int)$activePet['id'];

            // 2. Fetch care reminders
            $remStmt = $pdo->prepare(
                'SELECT id, title, reminder_time, category, is_completed, notify_push 
                 FROM care_reminders 
                 WHERE user_id = :user_id AND pet_id = :pet_id 
                 ORDER BY is_completed ASC, id ASC'
            );
            $remStmt->execute([':user_id' => $currentUser['id'], ':pet_id' => $petId]);
            $remindersToday = $remStmt->fetchAll(PDO::FETCH_ASSOC);

            // 3. Fetch logged meals
            $mealStmt = $pdo->prepare(
                'SELECT id, food_item, portion_size, calories, meal_time, created_at 
                 FROM meal_entries 
                 WHERE user_id = :user_id AND pet_id = :pet_id 
                 ORDER BY id ASC'
            );
            $mealStmt->execute([':user_id' => $currentUser['id'], ':pet_id' => $petId]);
            $mealsToday = $mealStmt->fetchAll(PDO::FETCH_ASSOC);

            foreach ($mealsToday as $meal) {
                $totalCalories += (int)$meal['calories'];
            }

            // 4. Construct unified Today's Schedule for the Home Tab
            // Include top 2-3 reminders or meal schedules
            $schedStmt = $pdo->prepare(
                'SELECT meal_name AS title, scheduled_time AS time, "meal" AS type 
                 FROM meal_schedules 
                 WHERE pet_id = :pet_id 
                 LIMIT 2'
            );
            $schedStmt->execute([':pet_id' => $petId]);
            $mealScheds = $schedStmt->fetchAll(PDO::FETCH_ASSOC);

            foreach ($mealScheds as $m) {
                $schedule[] = [
                    'id' => 'm_' . rand(100, 999),
                    'title' => $m['title'],
                    'time' => $m['time'],
                    'type' => 'meal',
                    'is_completed' => false,
                ];
            }

            foreach (array_slice($remindersToday, 0, 3) as $r) {
                $schedule[] = [
                    'id' => (string)$r['id'],
                    'title' => $r['title'],
                    'time' => $r['reminder_time'],
                    'type' => 'reminder',
                    'is_completed' => (bool)$r['is_completed'],
                ];
            }
        }

        Response::success([
            'user' => $currentUser,
            'active_pet' => $activePet,
            'today_schedule' => $schedule,
            'diet_summary' => [
                'consumed_calories' => $totalCalories ?: 650,
                'target_calories' => $targetCalories,
                'progress_ratio' => min(1.0, round(($totalCalories ?: 650) / $targetCalories, 2)),
            ],
            'reminders_count' => count($remindersToday),
            'meals_count' => count($mealsToday),
        ]);
    }
}

