<?php

declare(strict_types=1);

namespace App\Middleware;

use App\Utils\Database;
use App\Utils\Response;
use PDO;

class AuthMiddleware {
    public static function authenticate(): array {
        $authHeader = '';
        if (function_exists('getallheaders')) {
            $headers = getallheaders();
            $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
        }

        if (!$authHeader && isset($_SERVER['HTTP_AUTHORIZATION'])) {
            $authHeader = $_SERVER['HTTP_AUTHORIZATION'];
        }
        if (!$authHeader && isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
            $authHeader = $_SERVER['REDIRECT_HTTP_AUTHORIZATION'];
        }

        if (!$authHeader || !preg_match('/Bearer\s+(\S+)/i', $authHeader, $matches)) {
            Response::unauthorized('Authentication token is required.');
        }

        $token = $matches[1];
        $tokenHash = hash('sha256', $token);

        $pdo = Database::getConnection();
        $stmt = $pdo->prepare(
            'SELECT u.id, u.name, u.email, u.avatar_url, u.created_at, t.expires_at 
             FROM users u
             INNER JOIN user_tokens t ON u.id = t.user_id
             WHERE t.token_hash = :token_hash
             LIMIT 1'
        );
        $stmt->execute([':token_hash' => $tokenHash]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$user) {
            Response::unauthorized('Invalid or revoked authentication token.');
        }

        if (strtotime($user['expires_at']) < time()) {
            Response::unauthorized('Authentication token has expired. Please log in again.');
        }

        // Return user context
        unset($user['expires_at']);
        return $user;
    }
}
