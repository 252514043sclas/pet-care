<?php

require_once __DIR__ . '/../src/Utils/Database.php';

echo "=========================================================\n";
echo "        PET CARE APP - DATABASE CONNECTIVITY AUDIT       \n";
echo "=========================================================\n\n";

try {
    $pdo = \App\Utils\Database::getConnection();
    echo "[1] MySQL Database Connection: CONNECTED (SUCCESS)\n";

    $version = $pdo->query('SELECT VERSION()')->fetchColumn();
    $currentDb = $pdo->query('SELECT DATABASE()')->fetchColumn();
    echo "    - Database Server : MySQL {$version}\n";
    echo "    - Connected Schema: {$currentDb}\n\n";

    echo "[2] Schema Inspection (All Tables & Record Counts):\n";
    $tables = $pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
    
    foreach ($tables as $table) {
        $count = $pdo->query("SELECT COUNT(*) FROM `{$table}`")->fetchColumn();
        printf("    %-30s | %5d rows\n", $table, $count);
    }
    echo "\n";

    echo "[4] Table Schemas & Foreign Keys for Key Modules:\n";
    $keyTables = ['users', 'pets', 'pet_vaccinations', 'weight_entries', 'grooming_reminders', 'meal_schedules', 'meal_entries', 'pet_health_records'];
    foreach ($keyTables as $tbl) {
        echo "    Table: {$tbl}\n";
        $cols = $pdo->query("DESCRIBE `{$tbl}`")->fetchAll(PDO::FETCH_ASSOC);
        foreach ($cols as $col) {
            echo "      - {$col['Field']} : {$col['Type']}" . ($col['Key'] ? " [{$col['Key']}]" : '') . "\n";
        }
    }
    echo "\n";

    echo "[3] Active User & Pet Data Verification:\n";
    $users = $pdo->query('SELECT id, name, email, created_at FROM users LIMIT 5')->fetchAll(PDO::FETCH_ASSOC);
    foreach ($users as $u) {
        echo "    User ID {$u['id']}: {$u['name']} ({$u['email']})\n";
        $pets = $pdo->query("SELECT id, name, species, breed, age_years, is_active FROM pets WHERE user_id = {$u['id']}")->fetchAll(PDO::FETCH_ASSOC);
        foreach ($pets as $p) {
            echo "      -> Pet ID {$p['id']}: {$p['name']} ({$p['species']} - {$p['breed']}), Active=" . ($p['is_active'] ? 'YES' : 'NO') . "\n";
        }
    }
    echo "\n";

    echo "[4] Table Schemas & Foreign Keys for Key Modules:\n";
    $keyTables = ['users', 'pets', 'pet_vaccinations', 'weight_entries', 'grooming_reminders', 'meal_schedules', 'meal_entries', 'pet_health_records'];
    foreach ($keyTables as $tbl) {
        echo "    Table: {$tbl}\n";
        $cols = $pdo->query("DESCRIBE `{$tbl}`")->fetchAll(PDO::FETCH_ASSOC);
        foreach ($cols as $col) {
            echo "      - {$col['Field']} : {$col['Type']}" . ($col['Key'] ? " [{$col['Key']}]" : '') . "\n";
        }
    }
    echo "\n";

} catch (Exception $e) {
    echo "DATABASE CONNECTION ERROR: " . $e->getMessage() . "\n";
    exit(1);
}
