<?php
require_once __DIR__ . '/src/Utils/Database.php';
$pdo = App\Utils\Database::getConnection();
$stmt = $pdo->query('SELECT id, sender, message, message_type FROM chat_messages WHERE user_id = 1 ORDER BY id DESC LIMIT 4');
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
