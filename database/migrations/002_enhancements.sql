-- ========================================================
-- Pet Care Database Migration 002: Feature Enhancements
-- Adds pet image_url, user_preferences, and support_tickets
-- ========================================================

USE `pet_care_db`;

-- 1. Add image_url to pets if not already present
SET @dbname = DATABASE();
SET @tablename = "pets";
SET @columnname = "image_url";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  "ALTER TABLE pets ADD COLUMN image_url VARCHAR(255) NULL AFTER vaccination_status;"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. User Notification & Appearance Preferences
CREATE TABLE IF NOT EXISTS `user_preferences` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT UNSIGNED NOT NULL UNIQUE,
    `notif_vaccination` TINYINT(1) NOT NULL DEFAULT 1,
    `notif_medication` TINYINT(1) NOT NULL DEFAULT 1,
    `notif_appointments` TINYINT(1) NOT NULL DEFAULT 1,
    `theme_mode` VARCHAR(20) NOT NULL DEFAULT 'system', -- 'system', 'light', 'dark'
    `font_size` VARCHAR(20) NOT NULL DEFAULT 'normal', -- 'small', 'normal', 'large'
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Support Tickets & Problem Reports
CREATE TABLE IF NOT EXISTS `support_tickets` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT UNSIGNED NOT NULL,
    `category` VARCHAR(100) NOT NULL DEFAULT 'General',
    `subject` VARCHAR(200) NOT NULL,
    `description` TEXT NOT NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'open', -- 'open', 'in_progress', 'resolved'
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_support_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

