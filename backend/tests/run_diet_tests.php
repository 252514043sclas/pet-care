<?php
// Test both Pet 1 (0 schedule) and Pet 20 (configured schedule)
$cmd1 = 'C:\xampp\php\php.exe -r "require \'backend/src/Utils/Database.php\'; require \'backend/src/Utils/Response.php\'; require \'backend/src/Controllers/DietController.php\'; (new App\Controllers\DietController())->summary([\'id\' => 1], [\'pet_id\' => 1]);"';
$cmd2 = 'C:\xampp\php\php.exe -r "require \'backend/src/Utils/Database.php\'; require \'backend/src/Utils/Response.php\'; require \'backend/src/Controllers/DietController.php\'; (new App\Controllers\DietController())->summary([\'id\' => 13], [\'pet_id\' => 20]);"';

echo "PET 1:" . PHP_EOL;
passthru($cmd1);
echo PHP_EOL . "PET 20:" . PHP_EOL;
passthru($cmd2);
echo PHP_EOL;
