<?php
require_once __DIR__ . '/src/Utils/Database.php';

$pdo = App\Utils\Database::getConnection();
$user = $pdo->query("SELECT id, name, email FROM users WHERE id = 1")->fetch(PDO::FETCH_ASSOC);
if (!$user) {
    die("User 1 not found\n");
}

$pet = $pdo->query("SELECT id, name, species, breed, age_years, weight_kg FROM pets WHERE user_id = 1 LIMIT 1")->fetch(PDO::FETCH_ASSOC);

// Generate fresh token
$rawToken = bin2hex(random_bytes(32));
$tokenHash = hash('sha256', $rawToken);

// Insert into user_tokens
$pdo->prepare("INSERT INTO user_tokens (user_id, token_hash, expires_at) VALUES (1, ?, DATE_ADD(NOW(), INTERVAL 30 DAY))")->execute([$tokenHash]);

$petId = $pet['id'] ?? 1;
$petName = $pet['name'] ?? 'Clover';
$petSpecies = $pet['species'] ?? 'Rabbit';
$petBreed = $pet['breed'] ?? 'Holland Lop';
$petAge = $pet['age_years'] ?? '1.2 yrs';
$petWeight = $pet['weight_kg'] ?? '2.50';

$xml = '<?xml version=\'1.0\' encoding=\'utf-8\' standalone=\'yes\' ?>' . "\n" .
'<map>' . "\n" .
'    <string name="key_auth_token">' . $rawToken . '</string>' . "\n" .
'    <int name="key_user_id" value="1" />' . "\n" .
'    <string name="key_user_name">' . htmlspecialchars($user['name'], ENT_XML1) . '</string>' . "\n" .
'    <string name="key_user_email">' . htmlspecialchars($user['email'], ENT_XML1) . '</string>' . "\n" .
'    <int name="key_active_pet_id" value="' . $petId . '" />' . "\n" .
'    <string name="key_active_pet_name">' . htmlspecialchars($petName, ENT_XML1) . '</string>' . "\n" .
'    <string name="key_active_pet_type">' . htmlspecialchars($petSpecies, ENT_XML1) . '</string>' . "\n" .
'    <string name="key_active_pet_breed">' . htmlspecialchars($petBreed, ENT_XML1) . '</string>' . "\n" .
'    <string name="key_active_pet_age">' . htmlspecialchars($petAge, ENT_XML1) . '</string>' . "\n" .
'    <string name="key_active_pet_weight">' . htmlspecialchars($petWeight, ENT_XML1) . '</string>' . "\n" .
'    <string name="key_active_pet_gender">Female</string>' . "\n" .
'    <int name="key_active_pet_health_score" value="88" />' . "\n" .
'    <string name="key_active_pet_vaccination">OVERDUE</string>' . "\n" .
'    <boolean name="key_onboarding_completed" value="true" />' . "\n" .
'    <string name="key_theme_mode">system</string>' . "\n" .
'    <string name="key_font_size">normal</string>' . "\n" .
'</map>' . "\n";

file_put_contents(__DIR__ . '/session_prefs.xml', $xml);
echo "Generated session_prefs.xml with token: $rawToken for user {$user['name']} and pet $petName (ID: $petId)\n";
