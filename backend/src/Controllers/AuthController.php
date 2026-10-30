<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Utils\Database;
use App\Utils\Response;
use App\Utils\Validator;
use PDO;

class AuthController {
    public function register(array $input): void {
        $errors = Validator::validate($input, [
            'name' => 'required|min:2|max:100',
            'email' => 'required|email|max:150',
            'password' => 'required|min:6',
        ]);

        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();

        // Check if email already exists
        $stmt = $pdo->prepare('SELECT id FROM users WHERE email = :email LIMIT 1');
        $stmt->execute([':email' => strtolower(trim($input['email']))]);
        if ($stmt->fetch()) {
            Response::error('A user with this email address already exists.', 409);
        }

        $passwordHash = password_hash($input['password'], PASSWORD_BCRYPT, ['cost' => 12]);

        $insertStmt = $pdo->prepare(
            'INSERT INTO users (name, email, password_hash, created_at) VALUES (:name, :email, :password_hash, NOW())'
        );
        $insertStmt->execute([
            ':name' => trim($input['name']),
            ':email' => strtolower(trim($input['email'])),
            ':password_hash' => $passwordHash,
        ]);

        $userId = (int)$pdo->lastInsertId();

        // Issue auth token
        $rawToken = bin2hex(random_bytes(32));
        $tokenHash = hash('sha256', $rawToken);
        $expiresAt = date('Y-m-d H:i:s', strtotime('+30 days'));

        $tokenStmt = $pdo->prepare(
            'INSERT INTO user_tokens (user_id, token_hash, device_name, expires_at) VALUES (:user_id, :token_hash, :device_name, :expires_at)'
        );
        $tokenStmt->execute([
            ':user_id' => $userId,
            ':token_hash' => $tokenHash,
            ':device_name' => $input['device_name'] ?? 'Android Device',
            ':expires_at' => $expiresAt,
        ]);

        Response::success([
            'token' => $rawToken,
            'user' => [
                'id' => $userId,
                'name' => trim($input['name']),
                'email' => strtolower(trim($input['email'])),
            ],
        ], 'Registration successful', 201);
    }

    public function login(array $input): void {
        $errors = Validator::validate($input, [
            'email' => 'required|email',
            'password' => 'required',
        ]);

        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $pdo = Database::getConnection();
        $stmt = $pdo->prepare('SELECT id, name, email, password_hash, avatar_url FROM users WHERE email = :email LIMIT 1');
        $stmt->execute([':email' => strtolower(trim($input['email']))]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$user || !password_verify($input['password'], $user['password_hash'])) {
            Response::error('Invalid email or password.', 401);
        }

        // Issue auth token
        $rawToken = bin2hex(random_bytes(32));
        $tokenHash = hash('sha256', $rawToken);
        $expiresAt = date('Y-m-d H:i:s', strtotime('+30 days'));

        $tokenStmt = $pdo->prepare(
            'INSERT INTO user_tokens (user_id, token_hash, device_name, expires_at) VALUES (:user_id, :token_hash, :device_name, :expires_at)'
        );
        $tokenStmt->execute([
            ':user_id' => $user['id'],
            ':token_hash' => $tokenHash,
            ':device_name' => $input['device_name'] ?? 'Android Device',
            ':expires_at' => $expiresAt,
        ]);

        // Find active pet for user
        $petStmt = $pdo->prepare('SELECT * FROM pets WHERE user_id = :user_id AND is_active = 1 LIMIT 1');
        $petStmt->execute([':user_id' => $user['id']]);
        $activePet = $petStmt->fetch(PDO::FETCH_ASSOC);

        unset($user['password_hash']);

        Response::success([
            'token' => $rawToken,
            'user' => $user,
            'active_pet' => $activePet ?: null,
        ], 'Login successful');
    }

    public function forgotPassword(array $input): void {
        $errors = Validator::validate($input, [
            'email' => 'required|email',
        ]);

        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $email = strtolower(trim($input['email']));
        $pdo = Database::getConnection();

        $stmt = $pdo->prepare('SELECT id FROM users WHERE email = :email LIMIT 1');
        $stmt->execute([':email' => $email]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($user) {
            $resetToken = bin2hex(random_bytes(24));
            $resetHash = hash('sha256', $resetToken);
            $expiresAt = date('Y-m-d H:i:s', strtotime('+2 hours'));

            $insStmt = $pdo->prepare(
                'INSERT INTO password_resets (email, token_hash, expires_at) VALUES (:email, :token_hash, :expires_at)'
            );
            $insStmt->execute([
                ':email' => $email,
                ':token_hash' => $resetHash,
                ':expires_at' => $expiresAt,
            ]);
        }

        // Always return success message for security (prevents user enumeration)
        Response::success([
            'email' => $email,
        ], "If an account exists for {$email}, a password reset link has been dispatched.");
    }

    public function me(array $currentUser): void {
        $pdo = Database::getConnection();
        $petStmt = $pdo->prepare('SELECT * FROM pets WHERE user_id = :user_id ORDER BY is_active DESC, id ASC');
        $petStmt->execute([':user_id' => $currentUser['id']]);
        $pets = $petStmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success([
            'user' => $currentUser,
            'pets' => $pets,
        ]);
    }

    public function logout(array $currentUser): void {
        $authHeader = '';
        if (function_exists('getallheaders')) {
            $headers = getallheaders();
            $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
        }
        if (!$authHeader && isset($_SERVER['HTTP_AUTHORIZATION'])) {
            $authHeader = $_SERVER['HTTP_AUTHORIZATION'];
        }
        if (preg_match('/Bearer\s+(\S+)/i', $authHeader, $matches)) {
            $tokenHash = hash('sha256', $matches[1]);
            $pdo = Database::getConnection();
            $stmt = $pdo->prepare('DELETE FROM user_tokens WHERE token_hash = :token_hash');
            $stmt->execute([':token_hash' => $tokenHash]);
        }

        Response::success(null, 'Logged out successfully');
    }
}
