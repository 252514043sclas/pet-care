<?php

declare(strict_types=1);

$baseUrl = 'http://localhost/petcare/public/api';

function callApi(string $method, string $path, ?array $body = null, ?string $token = null): array {
    global $baseUrl;
    $url = $baseUrl . $path;
    $ch = curl_init($url);
    $headers = [
        'Content-Type: application/json',
        'Accept: application/json'
    ];
    if ($token) {
        $headers[] = 'Authorization: Bearer ' . $token;
    }
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    if ($body !== null) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body));
    }
    $raw = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    $json = json_decode((string)$raw, true) ?: [];
    return ['code' => $httpCode, 'body' => $json];
}

echo "=== PET PROFILE MULTI-USER ISOLATION TESTS ===\n\n";

$timestamp = time();
$emailUserA = "usera_{$timestamp}@test.com";
$emailUserB = "userb_{$timestamp}@test.com";
$password = "testpass123";

// ----------------------------------------------------
// TEST 1: Register new user A -> Profile must be empty
// ----------------------------------------------------
echo "TEST 1: Registering User A ({$emailUserA})...\n";
$regA = callApi('POST', '/auth/register', [
    'name' => 'User Alpha',
    'email' => $emailUserA,
    'password' => $password
]);

if ($regA['code'] !== 201 && $regA['code'] !== 200) {
    die("FAIL: Could not register User A: " . json_encode($regA) . "\n");
}
$tokenA = $regA['body']['data']['token'] ?? null;
$activePetA1 = $regA['body']['data']['active_pet'] ?? null;

if ($activePetA1 !== null) {
    die("FAIL: User A registration returned non-null active_pet: " . json_encode($activePetA1) . "\n");
}
echo "  ✓ active_pet is null on registration\n";

// Fetch pets for User A
$petsA1 = callApi('GET', '/pets', null, $tokenA);
$listA1 = $petsA1['body']['data'] ?? null;
if (!is_array($listA1) || count($listA1) !== 0) {
    die("FAIL: User A pets list is not empty: " . json_encode($listA1) . "\n");
}
echo "  ✓ GET /pets returned 0 pets for new User A\n";

// Fetch dashboard for User A
$dashA1 = callApi('GET', '/dashboard', null, $tokenA);
$dashActivePetA1 = $dashA1['body']['data']['active_pet'] ?? null;
if ($dashActivePetA1 !== null) {
    die("FAIL: User A dashboard has non-null active_pet: " . json_encode($dashActivePetA1) . "\n");
}
echo "  ✓ GET /dashboard returned null active_pet for User A\n";
echo "PASS: Test Scenario 1 passed (New user has completely empty profile).\n\n";

// ----------------------------------------------------
// TEST 2: Add pet for User A -> Details appear in Profile
// ----------------------------------------------------
echo "TEST 2: Adding Pet for User A (Milo the Dog)...\n";
$addPetA = callApi('POST', '/pets', [
    'name' => 'Milo',
    'species' => 'Dog',
    'breed' => 'Golden Retriever',
    'age_years' => '2 yrs',
    'weight_kg' => 15.5,
    'gender' => 'Male'
], $tokenA);

if ($addPetA['code'] !== 201 && $addPetA['code'] !== 200) {
    die("FAIL: Could not add pet for User A: " . json_encode($addPetA) . "\n");
}
$milo = $addPetA['body']['data'] ?? [];
if (($milo['name'] ?? '') !== 'Milo') {
    die("FAIL: Created pet name is not Milo: " . json_encode($milo) . "\n");
}
echo "  ✓ Pet Milo created with ID " . ($milo['id'] ?? 'unknown') . "\n";
echo "  ✓ Health score: " . ($milo['health_score'] ?? 'null') . "\n";
echo "  ✓ Vaccination status: " . ($milo['vaccination_status'] ?? 'null') . "\n";

// Check User A pets list now
$petsA2 = callApi('GET', '/pets', null, $tokenA);
$listA2 = $petsA2['body']['data'] ?? [];
if (count($listA2) !== 1 || $listA2[0]['name'] !== 'Milo') {
    die("FAIL: User A pets list does not contain Milo: " . json_encode($listA2) . "\n");
}
echo "  ✓ GET /pets returns exactly Milo for User A\n";

// Check User A dashboard
$dashA2 = callApi('GET', '/dashboard', null, $tokenA);
$dashActivePetA2 = $dashA2['body']['data']['active_pet'] ?? null;
if (($dashActivePetA2['name'] ?? '') !== 'Milo') {
    die("FAIL: User A dashboard does not show Milo: " . json_encode($dashActivePetA2) . "\n");
}
echo "  ✓ GET /dashboard shows Milo as active pet\n";
echo "PASS: Test Scenario 2 passed (Add pet saves and populates profile).\n\n";

// ----------------------------------------------------
// TEST 3: Logout & Login with User A -> Milo reappears
// ----------------------------------------------------
echo "TEST 3: Logging out and logging back in as User A...\n";
callApi('POST', '/auth/logout', null, $tokenA);

$loginA = callApi('POST', '/auth/login', [
    'email' => $emailUserA,
    'password' => $password
]);
if ($loginA['code'] !== 200) {
    die("FAIL: Could not log in as User A: " . json_encode($loginA) . "\n");
}
$tokenA2 = $loginA['body']['data']['token'] ?? null;
$loginActivePetA = $loginA['body']['data']['active_pet'] ?? null;
if (($loginActivePetA['name'] ?? '') !== 'Milo') {
    die("FAIL: Login did not return Milo as active_pet: " . json_encode($loginActivePetA) . "\n");
}
echo "  ✓ Login returned Milo as active_pet for User A\n";
echo "PASS: Test Scenario 3 passed (Saved pet details persist across logins).\n\n";

// ----------------------------------------------------
// TEST 4: Register User B -> Profile must be completely EMPTY
// ----------------------------------------------------
echo "TEST 4: Registering User B ({$emailUserB})...\n";
$regB = callApi('POST', '/auth/register', [
    'name' => 'User Beta',
    'email' => $emailUserB,
    'password' => $password
]);
if ($regB['code'] !== 201 && $regB['code'] !== 200) {
    die("FAIL: Could not register User B: " . json_encode($regB) . "\n");
}
$tokenB = $regB['body']['data']['token'] ?? null;
$activePetB1 = $regB['body']['data']['active_pet'] ?? null;
if ($activePetB1 !== null) {
    die("FAIL: User B has non-null active_pet: " . json_encode($activePetB1) . "\n");
}

$petsB1 = callApi('GET', '/pets', null, $tokenB);
$listB1 = $petsB1['body']['data'] ?? null;
if (!is_array($listB1) || count($listB1) !== 0) {
    die("FAIL: User B sees pets belonging to other users: " . json_encode($listB1) . "\n");
}
echo "  ✓ User B has 0 pets (no Milo, no Clover, no Buddy!)\n";
echo "PASS: Test Scenario 4 passed (Different new email starts with empty profile).\n\n";

// ----------------------------------------------------
// TEST 5: Add Pet for User B -> Strict Isolation Verified
// ----------------------------------------------------
echo "TEST 5: Adding Pet for User B (Luna the Cat)...\n";
$addPetB = callApi('POST', '/pets', [
    'name' => 'Luna',
    'species' => 'Cat',
    'breed' => 'Siamese',
    'age_years' => '1 yr',
    'weight_kg' => 4.2,
    'gender' => 'Female'
], $tokenB);

if ($addPetB['code'] !== 201 && $addPetB['code'] !== 200) {
    die("FAIL: Could not add pet for User B: " . json_encode($addPetB) . "\n");
}
echo "  ✓ Pet Luna created for User B\n";

// Check User B pets
$petsB2 = callApi('GET', '/pets', null, $tokenB);
$listB2 = $petsB2['body']['data'] ?? [];
if (count($listB2) !== 1 || $listB2[0]['name'] !== 'Luna') {
    die("FAIL: User B does not see only Luna: " . json_encode($listB2) . "\n");
}
echo "  ✓ User B sees ONLY Luna (count: 1)\n";

// Check User A pets still only has Milo
$petsA3 = callApi('GET', '/pets', null, $tokenA2);
$listA3 = $petsA3['body']['data'] ?? [];
if (count($listA3) !== 1 || $listA3[0]['name'] !== 'Milo') {
    die("FAIL: User A does not see only Milo: " . json_encode($listA3) . "\n");
}
echo "  ✓ User A sees ONLY Milo (count: 1)\n";
echo "PASS: Test Scenario 5 passed (Strict multi-user isolation confirmed).\n\n";

echo "=== ALL 5 TEST SCENARIOS PASSED SUCCESSFULLY! ===\n";
