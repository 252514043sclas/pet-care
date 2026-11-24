<?php

require_once __DIR__ . '/../src/Utils/Database.php';

try {
    $pdo = \App\Utils\Database::getConnection();
    echo "Creating chat_messages table if not exists...\n";

    $sql = "
    CREATE TABLE IF NOT EXISTS `chat_messages` (
        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `user_id` INT UNSIGNED NOT NULL,
        `pet_id` INT UNSIGNED NULL,
        `sender` VARCHAR(20) NOT NULL DEFAULT 'user',
        `message` TEXT NOT NULL,
        `image_url` VARCHAR(255) NULL,
        `message_type` VARCHAR(50) DEFAULT 'text',
        `sources` LONGTEXT NULL,
        `analysis_metadata` LONGTEXT NULL,
        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX `idx_user_created` (`user_id`, `created_at`),
        INDEX `idx_pet_created` (`pet_id`, `created_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    ";

    $pdo->exec($sql);
    echo "Table `chat_messages` created/verified successfully in MySQL `pet_care_db`.\n";

} catch (Exception $e) {
    echo "Error creating table: " . $e->getMessage() . "\n";
    exit(1);
}
