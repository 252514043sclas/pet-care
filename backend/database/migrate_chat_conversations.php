<?php

require_once __DIR__ . '/../src/Utils/Database.php';

use App\Utils\Database;

try {
    $pdo = Database::getConnection();

    echo "Running chat conversations migration...\n";

    $pdo->exec("
        CREATE TABLE IF NOT EXISTS `chat_conversations` (
            `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            `user_id` INT UNSIGNED NOT NULL,
            `pet_id` INT UNSIGNED NULL,
            `title` VARCHAR(255) NOT NULL,
            `language` VARCHAR(20) DEFAULT 'en',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            INDEX (`user_id`),
            FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    ");

    $cols = $pdo->query("SHOW COLUMNS FROM chat_messages LIKE 'conversation_id'")->fetchAll();
    if (empty($cols)) {
        $pdo->exec("ALTER TABLE chat_messages ADD COLUMN conversation_id INT UNSIGNED NULL AFTER user_id");
        $pdo->exec("ALTER TABLE chat_messages ADD INDEX (conversation_id)");
        $pdo->exec("ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE");
        echo "Added conversation_id column & foreign key to chat_messages.\n";
    } else {
        echo "conversation_id column already exists in chat_messages.\n";
    }

    echo "Migration completed successfully!\n";
} catch (\Throwable $e) {
    echo "Migration failed: " . $e->getMessage() . "\n";
    exit(1);
}
