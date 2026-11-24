<?php

declare(strict_types=1);

echo "=== Verifying Full Frontend-Backend HTTP Integration ===\n\n";

$baseUrl = 'http://127.0.0.1/petcare/public/api';

function apiRequest(string $method, string $url, ?array $body = null, ?string $token = null): array {
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);

    $headers = [
        'Accept: application/json',
        'Content-Type: application/json'
    ];
    if ($token !== null) {
        $headers[] = 'Authorization: Bearer ' . $token;
    }
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

    if ($body !== null) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body));
    }

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($response === false) {
        return ['code' => $httpCode, 'error' => $curlError, 'data' => null];
    }

    $json = json_decode($response, true);
    return ['code' => $httpCode, 'raw' => $response, 'json' => $json];
}

// 1. Login
$login = apiRequest('POST', "$baseUrl/auth/login", [
    'email' => 'kavya@petcare.com',
    'password' => 'password123'
]);

if ($login['code'] !== 200 || empty($login['json']['data']['token'])) {
    die("FAIL: Login endpoint failed! HTTP {$login['code']}: " . ($login['raw'] ?? ''));
}
$token = $login['json']['data']['token'];
$userName = $login['json']['data']['user']['name'];
echo "[1] POST /api/auth/login : PASS (HTTP 200, User '{$userName}', Token verified)\n";

// 2. GET /api/auth/me
$me = apiRequest('GET', "$baseUrl/auth/me", null, $token);
if ($me['code'] !== 200) {
    die("FAIL: /api/auth/me failed! HTTP {$me['code']}\n");
}
echo "[2] GET /api/auth/me : PASS (Authenticated as " . ($me['json']['data']['user']['email'] ?? $userName) . ")\n";

// 3. GET /api/pets
$pets = apiRequest('GET', "$baseUrl/pets", null, $token);
if ($pets['code'] !== 200 || !is_array($pets['json']['data'])) {
    die("FAIL: /api/pets failed! HTTP {$pets['code']}\n");
}
$petList = $pets['json']['data'];
$firstPet = !empty($petList) ? $petList[0] : null;
$petId = $firstPet ? (int)$firstPet['id'] : 1;
echo "[3] GET /api/pets : PASS (" . count($petList) . " pet(s) found. Active: " . ($firstPet['name'] ?? 'None') . ")\n";

// 4. GET /api/dashboard
$dash = apiRequest('GET', "$baseUrl/dashboard", null, $token);
if ($dash['code'] !== 200) {
    die("FAIL: /api/dashboard failed! HTTP {$dash['code']}\n");
}
echo "[4] GET /api/dashboard : PASS (Dashboard aggregated data returned)\n";

// 5. GET /api/pets/{id}/health-score
$hs = apiRequest('GET', "$baseUrl/pets/$petId/health-score", null, $token);
if ($hs['code'] !== 200) {
    die("FAIL: /api/pets/{id}/health-score failed! HTTP {$hs['code']}\n");
}
$score = $hs['json']['data']['score'] ?? 'No data';
$status = $hs['json']['data']['status_label'] ?? 'Unknown';
echo "[5] GET /api/pets/{id}/health-score : PASS (Score: {$score}, Status: '{$status}')\n";

// 6. GET /api/pets/{id}/health-records
$hr = apiRequest('GET', "$baseUrl/pets/$petId/health-records", null, $token);
if ($hr['code'] !== 200) {
    die("FAIL: /api/pets/{id}/health-records failed! HTTP {$hr['code']}\n");
}
echo "[6] GET /api/pets/{id}/health-records : PASS (" . count($hr['json']['data'] ?? []) . " records)\n";

// 7. GET /api/pets/{id}/vaccinations
$vac = apiRequest('GET', "$baseUrl/pets/$petId/vaccinations", null, $token);
if ($vac['code'] !== 200) {
    die("FAIL: /api/pets/{id}/vaccinations failed! HTTP {$vac['code']}\n");
}
$vacStatus = $vac['json']['data']['overall_status'] ?? 'Unknown';
echo "[7] GET /api/pets/{id}/vaccinations : PASS (Status: '{$vacStatus}')\n";

// 8. GET /api/diet/schedule
$ds = apiRequest('GET', "$baseUrl/diet/schedule?pet_id=$petId", null, $token);
if ($ds['code'] !== 200) {
    die("FAIL: /api/diet/schedule failed! HTTP {$ds['code']}\n");
}
echo "[8] GET /api/diet/schedule : PASS (" . count($ds['json']['data'] ?? []) . " schedules)\n";

// 9. GET /api/diet/meals
$meals = apiRequest('GET', "$baseUrl/diet/meals?pet_id=$petId", null, $token);
if ($meals['code'] !== 200) {
    die("FAIL: /api/diet/meals failed! HTTP {$meals['code']}\n");
}
echo "[9] GET /api/diet/meals : PASS (" . count($meals['json']['data'] ?? []) . " meal entries)\n";

// 10. GET /api/pets/{id}/weights
$weights = apiRequest('GET', "$baseUrl/pets/$petId/weights", null, $token);
if ($weights['code'] !== 200) {
    die("FAIL: /api/pets/{id}/weights failed! HTTP {$weights['code']}\n");
}
echo "[10] GET /api/pets/{id}/weights : PASS (Current weight: " . ($weights['json']['data']['current_weight'] ?? '0') . " kg)\n";

// 11. GET /api/grooming/reminders
$grooming = apiRequest('GET', "$baseUrl/grooming/reminders?pet_id=$petId", null, $token);
if ($grooming['code'] !== 200) {
    die("FAIL: /api/grooming/reminders failed! HTTP {$grooming['code']}\n");
}
echo "[11] GET /api/grooming/reminders : PASS (" . count($grooming['json']['data'] ?? []) . " reminders)\n";

// 12. GET /api/user/preferences
$prefs = apiRequest('GET', "$baseUrl/user/preferences", null, $token);
if ($prefs['code'] !== 200) {
    die("FAIL: /api/user/preferences failed! HTTP {$prefs['code']}\n");
}
echo "[12] GET /api/user/preferences : PASS (Theme: " . ($prefs['json']['data']['theme_mode'] ?? 'system') . ")\n";

// 13. GET /api/health/conditions
$conditions = apiRequest('GET', "$baseUrl/health/conditions");
if ($conditions['code'] !== 200) {
    die("FAIL: /api/health/conditions failed! HTTP {$conditions['code']}\n");
}
echo "[13] GET /api/health/conditions : PASS (" . count($conditions['json']['data'] ?? []) . " conditions)\n";

// 14. GET /api/grooming/guides
$guides = apiRequest('GET', "$baseUrl/grooming/guides");
if ($guides['code'] !== 200) {
    die("FAIL: /api/grooming/guides failed! HTTP {$guides['code']}\n");
}
echo "[14] GET /api/grooming/guides : PASS (" . count($guides['json']['data'] ?? []) . " guides)\n";

// 15. GET /api/diet/foods/safe-toxic
$foods = apiRequest('GET', "$baseUrl/diet/foods/safe-toxic");
if ($foods['code'] !== 200) {
    die("FAIL: /api/diet/foods/safe-toxic failed! HTTP {$foods['code']}\n");
}
echo "[15] GET /api/diet/foods/safe-toxic : PASS (" . count($foods['json']['data'] ?? []) . " food items)\n";

echo "\n>>> ALL 15 FRONTEND-BACKEND INTEGRATION ENDPOINTS TESTED & 100% OPERATIONAL! <<<\n";
