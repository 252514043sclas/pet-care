<?php

declare(strict_types=1);

require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/src/Utils/Database.php';

use App\Utils\Database;

try {
    $pdo = Database::getConnection();

    $sql = "
    CREATE TABLE IF NOT EXISTS `grooming_reminders` (
        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `user_id` INT UNSIGNED NOT NULL,
        `pet_id` INT UNSIGNED NOT NULL,
        `activity` VARCHAR(100) NOT NULL,
        `reminder_date` VARCHAR(30) NOT NULL,
        `reminder_time` VARCHAR(20) NOT NULL,
        `notification_enabled` TINYINT(1) NOT NULL DEFAULT 1,
        `status` VARCHAR(20) NOT NULL DEFAULT 'Scheduled',
        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
        FOREIGN KEY (`pet_id`) REFERENCES `pets`(`id`) ON DELETE CASCADE,
        INDEX `idx_grooming_user_pet` (`user_id`, `pet_id`),
        INDEX `idx_grooming_status` (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    ";

    $pdo->exec($sql);
    echo "SUCCESS: grooming_reminders table created or already exists.\n";

    // Show columns
    $stmt = $pdo->query("DESCRIBE grooming_reminders");
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    foreach ($columns as $col) {
        echo " - {$col['Field']} ({$col['Type']}) Null={$col['Null']} Default={$col['Default']}\n";
    }

} catch (Throwable $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
    exit(1);
}
