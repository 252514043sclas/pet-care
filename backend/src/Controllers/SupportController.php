<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Utils\Database;
use App\Utils\Response;
use App\Utils\Validator;
use PDO;

class SupportController {
    public function submitReport(array $currentUser, array $input): void {
        $errors = Validator::validate($input, [
            'subject' => 'required|min:3|max:200',
            'description' => 'required|min:10',
        ]);

        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'INSERT INTO support_tickets (user_id, category, subject, description, status, created_at)
             VALUES (:user_id, :category, :subject, :description, "open", NOW())'
        );

        $stmt->execute([
            ':user_id' => $currentUser['id'],
            ':category' => trim($input['category'] ?? 'General'),
            ':subject' => trim($input['subject']),
            ':description' => trim($input['description']),
        ]);

        $ticketId = (int)$pdo->lastInsertId();

        Response::success([
            'ticket_id' => $ticketId,
            'subject' => trim($input['subject']),
            'status' => 'open',
            'created_at' => date('Y-m-d H:i:s'),
        ], 'Your problem report has been submitted. Our support team will review it shortly.', 201);
    }
}

