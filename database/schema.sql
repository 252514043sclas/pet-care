-- ========================================================
-- Pet Care Application — MySQL Database Schema
-- Version: 1.0.0
-- Database: pet_care_db
-- ========================================================

CREATE DATABASE IF NOT EXISTS `pet_care_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `pet_care_db`;

-- Drop tables in reverse order of foreign key dependency
DROP TABLE IF EXISTS `saved_health_reports`;
DROP TABLE IF EXISTS `health_scans`;
DROP TABLE IF EXISTS `disease_guides`;
DROP TABLE IF EXISTS `grooming_guides`;
DROP TABLE IF EXISTS `safe_toxic_foods`;
DROP TABLE IF EXISTS `meal_schedules`;
DROP TABLE IF EXISTS `meal_entries`;
DROP TABLE IF EXISTS `care_reminders`;
DROP TABLE IF EXISTS `weight_entries`;
DROP TABLE IF EXISTS `pets`;
DROP TABLE IF EXISTS `password_resets`;
DROP TABLE IF EXISTS `user_tokens`;
DROP TABLE IF EXISTS `users`;

-- 1. Users Table
CREATE TABLE `users` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `avatar_url` VARCHAR(255) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. User Authentication Tokens (Mobile API Session Tokens)
CREATE TABLE `user_tokens` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT UNSIGNED NOT NULL,
    `token_hash` VARCHAR(64) NOT NULL UNIQUE,
    `device_name` VARCHAR(150) NULL,
    `expires_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_tokens_hash` (`token_hash`),
    INDEX `idx_tokens_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Password Resets Table
CREATE TABLE `password_resets` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(150) NOT NULL,
    `token_hash` VARCHAR(64) NOT NULL,
    `expires_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_resets_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Pets Table
CREATE TABLE `pets` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT UNSIGNED NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `species` VARCHAR(50) NOT NULL, -- Dog, Cat, Rabbit, Bird, Hamster, Other
    `breed` VARCHAR(100) NOT NULL,
    `age_years` VARCHAR(50) NOT NULL, -- e.g. "1.2 yrs"
    `weight_kg` DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    `gender` VARCHAR(20) NOT NULL DEFAULT 'Female',
    `microchip_id` VARCHAR(50) NULL,
    `health_score` INT NOT NULL DEFAULT 95,
    `vaccination_status` VARCHAR(100) NOT NULL DEFAULT 'Up to Date',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_pets_user` (`user_id`),
    INDEX `idx_pets_active` (`user_id`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Weight Tracker Entries
CREATE TABLE `weight_entries` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `pet_id` INT UNSIGNED NOT NULL,
    `user_id` INT UNSIGNED NOT NULL,
    `weight_kg` DECIMAL(6,2) NOT NULL,
    `recorded_date` VARCHAR(50) NOT NULL, -- e.g. "Jan 1" or "Oct 29"
    `notes` VARCHAR(255) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`pet_id`) REFERENCES `pets`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_weight_pet` (`pet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Care & Grooming Reminders
CREATE TABLE `care_reminders` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `pet_id` INT UNSIGNED NOT NULL,
    `user_id` INT UNSIGNED NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `reminder_time` VARCHAR(100) NOT NULL, -- e.g. "8:00 AM" or "Oct 15, 10:00 AM"
    `category` VARCHAR(50) NOT NULL DEFAULT 'Care', -- Care, Grooming, Nutrition, Medication
    `notify_push` TINYINT(1) NOT NULL DEFAULT 1,
    `is_completed` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`pet_id`) REFERENCES `pets`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_reminders_user_pet` (`user_id`, `pet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Logged Meal Entries
CREATE TABLE `meal_entries` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `pet_id` INT UNSIGNED NOT NULL,
    `user_id` INT UNSIGNED NOT NULL,
    `food_item` VARCHAR(255) NOT NULL,
    `portion_size` VARCHAR(50) NOT NULL, -- e.g. "1.5 Cups"
    `calories` INT NOT NULL DEFAULT 150,
    `meal_time` VARCHAR(50) NOT NULL, -- e.g. "8:00 AM"
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`pet_id`) REFERENCES `pets`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_meals_pet` (`pet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Meal Schedule & Daily Plan
CREATE TABLE `meal_schedules` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `pet_id` INT UNSIGNED NOT NULL,
    `meal_name` VARCHAR(100) NOT NULL, -- Morning, Afternoon, Evening
    `scheduled_time` VARCHAR(50) NOT NULL, -- "Morning • 8:00 AM"
    `meal_description` VARCHAR(255) NOT NULL,
    `calories` INT NOT NULL DEFAULT 350,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`pet_id`) REFERENCES `pets`(`id`) ON DELETE CASCADE,
    INDEX `idx_meal_schedule_pet` (`pet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Safe vs. Toxic Foods Knowledgebase
CREATE TABLE `safe_toxic_foods` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `food_name` VARCHAR(150) NOT NULL,
    `is_safe` TINYINT(1) NOT NULL DEFAULT 1, -- 1 = Safe (✅), 0 = Toxic (❌)
    `description` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_safe_foods_status` (`is_safe`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. AI Health Scans & Diagnostic Checks
CREATE TABLE `health_scans` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `pet_id` INT UNSIGNED NOT NULL,
    `user_id` INT UNSIGNED NOT NULL,
    `scan_type` VARCHAR(50) NOT NULL, -- "photo", "symptoms"
    `symptoms_json` TEXT NULL,
    `diagnosis_title` VARCHAR(255) NOT NULL,
    `match_percentage` INT NOT NULL DEFAULT 85,
    `severity` VARCHAR(50) NOT NULL DEFAULT 'Mild',
    `summary` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`pet_id`) REFERENCES `pets`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_scans_pet` (`pet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Disease & Condition Guides
CREATE TABLE `disease_guides` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `condition_name` VARCHAR(255) NOT NULL,
    `species` VARCHAR(50) NOT NULL DEFAULT 'Rabbit',
    `severity` VARCHAR(50) NOT NULL DEFAULT 'Moderate',
    `description` TEXT NOT NULL,
    `causes` TEXT NOT NULL,
    `interim_steps_json` TEXT NOT NULL,
    `vet_warning` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Saved Health Reports
CREATE TABLE `saved_health_reports` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT UNSIGNED NOT NULL,
    `pet_id` INT UNSIGNED NOT NULL,
    `report_title` VARCHAR(255) NOT NULL,
    `report_data_json` TEXT NOT NULL,
    `saved_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`pet_id`) REFERENCES `pets`(`id`) ON DELETE CASCADE,
    INDEX `idx_reports_user_pet` (`user_id`, `pet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Grooming Guides & Tutorials
CREATE TABLE `grooming_guides` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `category` VARCHAR(100) NOT NULL,
    `steps` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

