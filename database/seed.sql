-- ========================================================
-- Pet Care Application — Development Seed Data
-- ========================================================

USE `pet_care_db`;

-- 1. Demo User: password is "password123"
-- Generated with password_hash('password123', PASSWORD_BCRYPT)
INSERT INTO `users` (`id`, `name`, `email`, `password_hash`, `created_at`) VALUES
(1, 'Kavya', 'kavya@petcare.com', '$2y$10$BtEcDFdSucMXYuKqftESQeZR5tL7b.rqbAIL5KlxJ34lwAezZEbmO', NOW());

-- 2. Demo User Mobile Bearer Token for immediate test access
-- SHA-256 hash of "demo_token_secret_12345"
INSERT INTO `user_tokens` (`user_id`, `token_hash`, `device_name`, `expires_at`, `created_at`) VALUES
(1, '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'Android Studio Emulator Pixel 7', DATE_ADD(NOW(), INTERVAL 30 DAY), NOW());

-- 3. Default Active Pet: Clover
INSERT INTO `pets` (`id`, `user_id`, `name`, `species`, `breed`, `age_years`, `weight_kg`, `gender`, `microchip_id`, `health_score`, `vaccination_status`, `is_active`, `created_at`) VALUES
(1, 1, 'Clover', 'Rabbit', 'Holland Lop', '1.2 yrs', 1.80, 'Female', '#985142091', 95, 'Up to Date', 1, NOW());

-- 4. Weight Tracker History for Clover
INSERT INTO `weight_entries` (`pet_id`, `user_id`, `weight_kg`, `recorded_date`, `notes`, `created_at`) VALUES
(1, 1, 1.60, 'Jan 1', 'Routine weight check', NOW() - INTERVAL 90 DAY),
(1, 1, 1.70, 'Feb 1', 'Healthy steady growth', NOW() - INTERVAL 60 DAY),
(1, 1, 1.80, 'Mar 1', 'Optimal target weight maintained', NOW() - INTERVAL 30 DAY);

-- 5. Care & Grooming Reminders for Clover
INSERT INTO `care_reminders` (`id`, `pet_id`, `user_id`, `title`, `reminder_time`, `category`, `notify_push`, `is_completed`, `created_at`) VALUES
(1, 1, 1, 'Morning Timothy Hay & Greens', '8:00 AM', 'Nutrition', 1, 0, NOW()),
(2, 1, 1, 'Fur Brushing & Nail Check', '5:30 PM', 'Grooming', 1, 0, NOW()),
(3, 1, 1, 'Evening Fresh Water Refill', '8:00 PM', 'Care', 1, 0, NOW()),
(4, 1, 1, 'Bath & Shampoo Session', 'Oct 12, 10:00 AM', 'Grooming', 1, 0, NOW()),
(5, 1, 1, 'Nail Trimming & Filing', 'Oct 19, 2:00 PM', 'Grooming', 1, 0, NOW()),
(6, 1, 1, 'Ear Cleaning & Checkup', 'Oct 26, 11:00 AM', 'Grooming', 1, 0, NOW()),
(7, 1, 1, 'Full Grooming & Wellness Check', 'Nov 02, 10:00 AM', 'Grooming', 1, 0, NOW());

-- 6. Logged Meals for Clover
INSERT INTO `meal_entries` (`id`, `pet_id`, `user_id`, `food_item`, `portion_size`, `calories`, `meal_time`, `created_at`) VALUES
(1, 1, 1, 'Fresh Timothy Hay', 'Unlimited', 150, '8:00 AM', NOW()),
(2, 1, 1, 'Romaine Lettuce & Fresh Herbs', '1.0 Cup', 45, '12:30 PM', NOW()),
(3, 1, 1, 'Pellet Portion & Water Refill', '0.5 Cup', 80, '6:00 PM', NOW());

-- 7. Daily Feeding Schedule Plan
INSERT INTO `meal_schedules` (`id`, `pet_id`, `meal_name`, `scheduled_time`, `meal_description`, `calories`) VALUES
(1, 1, 'Morning', 'Morning • 8:00 AM', '2 Cups Premium Kibble / Fresh Timothy Hay', 350),
(2, 1, 'Afternoon', 'Afternoon • 1:00 PM', 'Healthy Carrot Snack & Fresh Green Treat', 100),
(3, 1, 'Evening', 'Evening • 6:00 PM', 'Pellets Portion with Crisp Romaine Lettuce', 350);

-- 8. Safe vs. Toxic Foods Library
INSERT INTO `safe_toxic_foods` (`id`, `food_name`, `is_safe`, `description`) VALUES
(1, '✅ Carrots', 1, 'Safe & nutritious crunchy snack for dental health and vitamins.'),
(2, '✅ Apples (No Seeds)', 1, 'Great source of vitamins A & C. Always remove toxic seeds before feeding.'),
(3, '❌ Chocolate', 0, 'Toxic! Contains theobromine which causes cardiac and nervous system damage.'),
(4, '❌ Grapes & Raisins', 0, 'Toxic! Highly dangerous and can cause acute fatal kidney failure.'),
(5, '❌ Onions & Garlic', 0, 'Toxic! Destroys red blood cells leading to life-threatening hemolytic anemia.'),
(6, '✅ Plain Cooked Chicken', 1, 'Safe, high-quality lean protein source without any salt, onions or seasonings.'),
(7, '❌ Macadamia Nuts', 0, 'Toxic! Triggers vomiting, hypothermia, tremors, and neurological weakness.'),
(8, '✅ Blueberries', 1, 'Safe antioxidant-rich treat that promotes cell health and immune vitality.'),
(9, '❌ Xylitol (Artificial Sweetener)', 0, 'Extremely toxic! Causes rapid hypoglycemia and liver necrosis.');

-- 9. AI Health Scans Seed
INSERT INTO `health_scans` (`id`, `pet_id`, `user_id`, `scan_type`, `symptoms_json`, `diagnosis_title`, `match_percentage`, `severity`, `summary`, `created_at`) VALUES
(1, 1, 1, 'symptoms', '["Itching / Excessive Scratching", "Skin Redness or Bumps"]', 'Minor Skin Allergy', 88, 'Mild', 'Based on symptom checklist and photo scan, your pet shows indicators of allergic dermatitis, commonly triggered by environmental factors or food sensitivity.', NOW());

-- 10. Disease and Condition Guides
INSERT INTO `disease_guides` (`id`, `condition_name`, `species`, `severity`, `description`, `causes`, `interim_steps_json`, `vet_warning`) VALUES
(1, 'Mite-related irritation', 'Rabbit', 'Moderate',
'Mites can cause flaky skin, itching or patchy fur in rabbits. Other problems can look similar, so this is a possible explanation—not a diagnosis.',
'Contact with affected animals or contaminated bedding may spread mites. Only your vet can confirm the cause.',
'["Keep Clover’s bedding clean and dry. Avoid new scented products.", "Record changes in skin, appetite and droppings for your vet.", "Do not apply dog or cat flea products, creams or home remedies."]',
'Book a rabbit-savvy vet exam for skin changes. Seek urgent care for reduced eating, fewer droppings, marked lethargy or painful, broken skin.');

-- 11. Grooming Guides & Care Tutorials
INSERT INTO `grooming_guides` (`id`, `title`, `category`, `steps`) VALUES
(1, 'Coat Brushing & Detangling', 'Coat Care', '1. Use a slicker brush to remove loose undercoat.\n2. Gently work through tangles without pulling.\n3. Finish with a bristle brush to distribute natural oils.'),
(2, 'Safe Nail Trimming', 'Nails & Paws', '1. Hold your pet\'s paw firmly but gently.\n2. Identify the quick (pink blood vessel inside nail).\n3. Clip only the transparent tip at a 45-degree angle.'),
(3, 'Ear Cleaning Routine', 'Ears & Hygiene', '1. Apply vet-approved ear cleaning solution.\n2. Gently massage the base of the ear.\n3. Wipe away debris with a clean cotton ball or pad.');
