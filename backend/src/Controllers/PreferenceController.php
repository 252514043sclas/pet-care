<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Utils\Database;
use App\Utils\Response;
use PDO;

class PreferenceController {
    public function getPreferences(array $currentUser): void {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT * FROM user_preferences WHERE user_id = :user_id LIMIT 1');
        $stmt->execute([':user_id' => $currentUser['id']]);
        $prefs = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$prefs) {
            // Create default preferences
            $ins = $pdo->prepare(
                'INSERT INTO user_preferences (user_id, notif_vaccination, notif_medication, notif_appointments, theme_mode, font_size)
                 VALUES (:user_id, 1, 1, 1, "system", "normal")'
            );
            $ins->execute([':user_id' => $currentUser['id']]);

            $stmt->execute([':user_id' => $currentUser['id']]);
            $prefs = $stmt->fetch(PDO::FETCH_ASSOC);
        }

        Response::success([
            'notif_vaccination' => (bool)$prefs['notif_vaccination'],
            'notif_medication' => (bool)$prefs['notif_medication'],
            'notif_appointments' => (bool)$prefs['notif_appointments'],
            'theme_mode' => $prefs['theme_mode'],
            'font_size' => $prefs['font_size'],
        ]);
    }

    public function updatePreferences(array $currentUser, array $input): void {
        $pdo = Database::getConnection();

        // Ensure record exists
        $stmt = $pdo->prepare('SELECT id FROM user_preferences WHERE user_id = :user_id LIMIT 1');
        $stmt->execute([':user_id' => $currentUser['id']]);
        if (!$stmt->fetch()) {
            $ins = $pdo->prepare(
                'INSERT INTO user_preferences (user_id, notif_vaccination, notif_medication, notif_appointments, theme_mode, font_size)
                 VALUES (:user_id, 1, 1, 1, "system", "normal")'
            );
            $ins->execute([':user_id' => $currentUser['id']]);
        }

        $fields = [];
        $params = [':user_id' => $currentUser['id']];

        if (isset($input['notif_vaccination'])) {
            $fields[] = 'notif_vaccination = :nv';
            $params[':nv'] = (int)(bool)$input['notif_vaccination'];
        }
        if (isset($input['notif_medication'])) {
            $fields[] = 'notif_medication = :nm';
            $params[':nm'] = (int)(bool)$input['notif_medication'];
        }
        if (isset($input['notif_appointments'])) {
            $fields[] = 'notif_appointments = :na';
            $params[':na'] = (int)(bool)$input['notif_appointments'];
        }
        if (isset($input['theme_mode'])) {
            $fields[] = 'theme_mode = :tm';
            $params[':tm'] = in_array($input['theme_mode'], ['light', 'dark', 'system'], true) ? $input['theme_mode'] : 'system';
        }
        if (isset($input['font_size'])) {
            $fields[] = 'font_size = :fs';
            $params[':fs'] = in_array($input['font_size'], ['small', 'normal', 'large'], true) ? $input['font_size'] : 'normal';
        }

        if (!empty($fields)) {
            $sql = 'UPDATE user_preferences SET ' . implode(', ', $fields) . ', updated_at = NOW() WHERE user_id = :user_id';
            $updateStmt = $pdo->prepare($sql);
            $updateStmt->execute($params);
        }

        $fetchStmt = $pdo->prepare('SELECT * FROM user_preferences WHERE user_id = :user_id LIMIT 1');
        $fetchStmt->execute([':user_id' => $currentUser['id']]);
        $prefs = $fetchStmt->fetch(PDO::FETCH_ASSOC);

        Response::success([
            'notif_vaccination' => (bool)$prefs['notif_vaccination'],
            'notif_medication' => (bool)$prefs['notif_medication'],
            'notif_appointments' => (bool)$prefs['notif_appointments'],
            'theme_mode' => $prefs['theme_mode'],
            'font_size' => $prefs['font_size'],
        ], 'Preferences updated successfully');
    }
}

