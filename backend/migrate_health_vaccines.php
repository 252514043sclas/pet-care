<?php

declare(strict_types=1);

require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/src/Utils/Database.php';

use App\Utils\Database;

try {
    $pdo = Database::getConnection();

    // 1. Create pet_health_records table
    $sql1 = "
    CREATE TABLE IF NOT EXISTS `pet_health_records` (
        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `pet_id` INT UNSIGNED NOT NULL,
        `user_id` INT UNSIGNED NOT NULL,
        `record_type` VARCHAR(50) NOT NULL DEFAULT 'Checkup',
        `title` VARCHAR(150) NOT NULL,
        `description` TEXT NULL,
        `severity` VARCHAR(30) NOT NULL DEFAULT 'Normal',
        `recorded_date` VARCHAR(30) NOT NULL,
        `vet_name` VARCHAR(100) NULL,
        `notes` TEXT NULL,
        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (`pet_id`) REFERENCES `pets`(`id`) ON DELETE CASCADE,
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
        INDEX `idx_health_records_pet` (`pet_id`),
        INDEX `idx_health_records_user` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    ";
    $pdo->exec($sql1);
    echo "SUCCESS: pet_health_records table created or exists.\n";

    // 2. Create pet_vaccinations table
    $sql2 = "
    CREATE TABLE IF NOT EXISTS `pet_vaccinations` (
        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        `pet_id` INT UNSIGNED NOT NULL,
        `user_id` INT UNSIGNED NOT NULL,
        `vaccine_name` VARCHAR(150) NOT NULL,
        `vaccination_date` VARCHAR(30) NOT NULL,
        `next_due_date` VARCHAR(30) NOT NULL,
        `batch_number` VARCHAR(100) NULL,
        `clinic_name` VARCHAR(150) NULL,
        `notes` TEXT NULL,
        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (`pet_id`) REFERENCES `pets`(`id`) ON DELETE CASCADE,
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
        INDEX `idx_vaccinations_pet` (`pet_id`),
        INDEX `idx_vaccinations_user` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    ";
    $pdo->exec($sql2);
    echo "SUCCESS: pet_vaccinations table created or exists.\n";

    // Allow nullable health_score in pets table if not already
    $pdo->exec("ALTER TABLE `pets` MODIFY `health_score` INT NULL DEFAULT NULL");
    echo "SUCCESS: pets.health_score modified to nullable.\n";

} catch (Throwable $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
    exit(1);
}
