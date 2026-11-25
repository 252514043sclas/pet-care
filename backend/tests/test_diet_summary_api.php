<?php
require_once __DIR__ . '/../src/Utils/Database.php';
require_once __DIR__ . '/../src/Utils/Response.php';
require_once __DIR__ . '/../src/Controllers/DietController.php';

use App\Utils\Database;
use App\Controllers\DietController;

$controller = new DietController();

echo "=== TEST SUMMARY: USER 1, PET 1 (No schedule configured) ===" . PHP_EOL;
$user1 = ['id' => 1];
ob_start();
$controller->summary($user1, ['pet_id' => 1]);
$out = ob_get_clean();
echo $out . PHP_EOL;

echo "=== TEST SUMMARY: USER 13, PET 20 (Configured schedule) ===" . PHP_EOL;
$user13 = ['id' => 13];
ob_start();
$controller->summary($user13, ['pet_id' => 20]);
$out2 = ob_get_clean();
echo $out2 . PHP_EOL;
