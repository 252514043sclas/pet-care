<?php

declare(strict_types=1);

namespace App\Services;

class FoodNutritionSafetyService {

    public static function normalizeSpecies(?string $species): string {
        $s = strtolower(trim((string)$species));
        if ($s === '') return 'other';

        if (str_contains($s, 'dog') || str_contains($s, 'puppy') || str_contains($s, 'canine')) return 'dog';
        if (str_contains($s, 'cat') || str_contains($s, 'kitten') || str_contains($s, 'feline')) return 'cat';
        if (str_contains($s, 'rabbit') || str_contains($s, 'bunny') || str_contains($s, 'lop')) return 'rabbit';
        if (str_contains($s, 'guinea')) return 'guinea_pig';
        if (str_contains($s, 'hamster') || str_contains($s, 'gerbil')) return 'hamster';
        if (
            str_contains($s, 'bird') || str_contains($s, 'pigeon') || str_contains($s, 'parrot') ||
            str_contains($s, 'budgie') || str_contains($s, 'cockatiel') || str_contains($s, 'parakeet') ||
            str_contains($s, 'canary') || str_contains($s, 'finch') || str_contains($s, 'lovebird') ||
            str_contains($s, 'dove') || str_contains($s, 'macaw')
        ) {
            return 'bird';
        }
        if (
            str_contains($s, 'fish') || str_contains($s, 'goldfish') || str_contains($s, 'betta') ||
            str_contains($s, 'guppy') || str_contains($s, 'koi') || str_contains($s, 'tetra')
        ) {
            return 'fish';
        }
        return 'other';
    }

    public static function speciesDisplayName(string $normalized, ?string $rawSpecies = null): string {
        $raw = trim((string)$rawSpecies);
        if ($raw !== '') return $raw;
        return match ($normalized) {
            'dog' => 'Dog',
            'cat' => 'Cat',
            'rabbit' => 'Rabbit',
            'hamster' => 'Hamster',
            'guinea_pig' => 'Guinea Pig',
            'bird' => 'Bird',
            'fish' => 'Fish',
            default => 'Pet',
        };
    }

    public static function analyze(string $foodName, float $quantity, string $unit, ?string $rawSpecies): array {
        $cleanFood = trim($foodName);
        $normalizedUnit = self::normalizeUnit($unit);
        $speciesKey = self::normalizeSpecies($rawSpecies);
        $speciesLabel = self::speciesDisplayName($speciesKey, $rawSpecies);

        $safety = self::checkSafety($cleanFood, $speciesKey, $speciesLabel);
        $nutrition = self::calculateCalories($cleanFood, $quantity, $normalizedUnit);

        return [
            'food_name' => $cleanFood,
            'species' => $speciesLabel,
            'species_category' => $speciesKey,
            'portion_quantity' => round($quantity, 2),
            'portion_unit' => $normalizedUnit,
            'estimated_calories' => $nutrition['calories'],
            'calories_available' => $nutrition['available'],
            'calorie_source' => $nutrition['source'],
            'grams_estimated' => $nutrition['grams'],
            'safety_status' => $safety['status'],
            'safety_warning' => $safety['message'],
        ];
    }

    public static function normalizeUnit(?string $unit): string {
        $u = strtolower(trim((string)$unit));
        return match ($u) {
            'g', 'gram', 'grams' => 'g',
            'cup', 'cups' => 'cups',
            'piece', 'pieces', 'pc', 'pcs', 'slice', 'slices' => 'pieces',
            'tbsp', 'tablespoon', 'tablespoons' => 'tbsp',
            'tsp', 'teaspoon', 'teaspoons' => 'tsp',
            default => 'g',
        };
    }

    public static function parsePortion(?string $portionStr): array {
        $str = strtolower(trim((string)$portionStr));
        if ($str === '') {
            return ['quantity' => 1.0, 'unit' => 'g'];
        }

        if (preg_match('/^([\d\.]+)\s*(cups?|g|grams?|pieces?|pcs?|pc|slices?|tbsp|tablespoons?|tsp|teaspoons?|bowl|bowls?|ml)?/i', $str, $matches)) {
            $qty = (float)$matches[1];
            $rawUnit = !empty($matches[2]) ? strtolower($matches[2]) : '';
            if (str_starts_with($rawUnit, 'cup')) {
                $unit = 'cups';
            } elseif ($rawUnit === 'g' || str_starts_with($rawUnit, 'gram')) {
                $unit = 'g';
            } elseif (str_starts_with($rawUnit, 'piece') || str_starts_with($rawUnit, 'pc') || str_starts_with($rawUnit, 'slice')) {
                $unit = 'pieces';
            } elseif (str_starts_with($rawUnit, 'tbsp') || str_starts_with($rawUnit, 'tablespoon')) {
                $unit = 'tbsp';
            } elseif (str_starts_with($rawUnit, 'tsp') || str_starts_with($rawUnit, 'teaspoon')) {
                $unit = 'tsp';
            } elseif (str_starts_with($rawUnit, 'bowl')) {
                $qty = $qty * 1.5;
                $unit = 'cups';
            } elseif ($rawUnit === 'ml') {
                $unit = 'g';
            } else {
                $unit = ($qty < 10.0) ? 'pieces' : 'g';
            }
            return ['quantity' => max(0.01, $qty), 'unit' => $unit];
        }

        return ['quantity' => 1.0, 'unit' => 'g'];
    }

    private static function calculateCalories(string $foodName, float $quantity, string $unit): array {
        if ($foodName === '' || $quantity <= 0) {
            return ['available' => false, 'calories' => null, 'source' => 'Unavailable', 'grams' => null];
        }

        $lower = strtolower($foodName);
        $entry = self::lookupReferenceNutrition($lower);
        $source = 'USDA FoodData Reference';

        if ($entry === null) {
            $apiEntry = self::lookupOpenFoodFacts($foodName);
            if ($apiEntry !== null) {
                $entry = $apiEntry;
                $source = 'Open Food Facts Nutrition API';
            }
        }

        if ($entry === null) {
            return [
                'available' => false,
                'calories' => null,
                'source' => 'Nutrition data unavailable',
                'grams' => null,
            ];
        }

        $gramsPerUnit = match ($unit) {
            'g' => 1.0,
            'cups' => (float)($entry['cup_g'] ?? 120.0),
            'pieces' => (float)($entry['piece_g'] ?? 30.0),
            'tbsp' => (float)($entry['tbsp_g'] ?? 14.0),
            'tsp' => (float)($entry['tsp_g'] ?? 5.0),
            default => 1.0,
        };

        $totalGrams = $quantity * $gramsPerUnit;
        $kcalPer100g = (float)$entry['kcal_100g'];
        $estimatedKcal = (int)round(($kcalPer100g * $totalGrams) / 100.0);

        return [
            'available' => true,
            'calories' => max(0, $estimatedKcal),
            'source' => $source,
            'grams' => round($totalGrams, 1),
        ];
    }

    private static function lookupOpenFoodFacts(string $foodName): ?array {
        if (!function_exists('curl_init')) {
            return null;
        }

        $query = urlencode($foodName);
        $url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms={$query}&search_simple=1&action=process&json=1&page_size=5&fields=product_name,nutriments";

        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CONNECTTIMEOUT => 2,
            CURLOPT_TIMEOUT => 3,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_HTTPHEADER => [
                'User-Agent: PetCareAndroidApp/1.0 (nutrition-lookup)',
                'Accept: application/json',
            ],
            CURLOPT_SSL_VERIFYPEER => false,
        ]);

        $raw = curl_exec($ch);
        $httpCode = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if (!$raw || $httpCode !== 200) {
            return null;
        }

        $decoded = json_decode($raw, true);
        if (!is_array($decoded) || empty($decoded['products'])) {
            return null;
        }

        $tokens = array_values(array_filter(preg_split('/[\s,\-]+/', strtolower($foodName)), fn($t) => strlen($t) >= 3));

        foreach ($decoded['products'] as $product) {
            $nutriments = $product['nutriments'] ?? [];
            $kcal = $nutriments['energy-kcal_100g'] ?? null;
            if (!is_numeric($kcal) || (float)$kcal <= 0 || (float)$kcal > 950) {
                continue;
            }
            $pName = strtolower((string)($product['product_name'] ?? ''));
            if (!empty($tokens) && $pName !== '') {
                $matched = false;
                foreach ($tokens as $tok) {
                    if (str_contains($pName, $tok)) {
                        $matched = true;
                        break;
                    }
                }
                if (!$matched) continue;
            }

            return [
                'kcal_100g' => (float)$kcal,
                'cup_g' => 120.0,
                'piece_g' => 30.0,
                'tbsp_g' => 14.0,
                'tsp_g' => 5.0,
            ];
        }

        return null;
    }

    private static function lookupReferenceNutrition(string $lower): ?array {
        $db = [
            'timothy hay'       => ['kcal_100g' => 195, 'cup_g' => 30,  'piece_g' => 15,  'tbsp_g' => 4,  'tsp_g' => 1.5],
            'alfalfa hay'       => ['kcal_100g' => 230, 'cup_g' => 35,  'piece_g' => 15,  'tbsp_g' => 5,  'tsp_g' => 2],
            'hay'               => ['kcal_100g' => 195, 'cup_g' => 30,  'piece_g' => 15,  'tbsp_g' => 4,  'tsp_g' => 1.5],
            'rabbit pellet'     => ['kcal_100g' => 240, 'cup_g' => 110, 'piece_g' => 2,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'rabbit pellets'    => ['kcal_100g' => 240, 'cup_g' => 110, 'piece_g' => 2,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'guinea pig pellet' => ['kcal_100g' => 250, 'cup_g' => 110, 'piece_g' => 2,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'hamster pellet'    => ['kcal_100g' => 320, 'cup_g' => 115, 'piece_g' => 3,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'fish flake'        => ['kcal_100g' => 370, 'cup_g' => 40,  'piece_g' => 0.5, 'tbsp_g' => 3,  'tsp_g' => 1],
            'fish pellet'       => ['kcal_100g' => 360, 'cup_g' => 100, 'piece_g' => 0.5, 'tbsp_g' => 8,  'tsp_g' => 2.5],
            'bloodworm'         => ['kcal_100g' => 85,  'cup_g' => 100, 'piece_g' => 2,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'brine shrimp'      => ['kcal_100g' => 90,  'cup_g' => 100, 'piece_g' => 2,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'bird seed'         => ['kcal_100g' => 440, 'cup_g' => 140, 'piece_g' => 1,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'pigeon mix'        => ['kcal_100g' => 355, 'cup_g' => 150, 'piece_g' => 1,   'tbsp_g' => 11, 'tsp_g' => 4],
            'wet dog food'      => ['kcal_100g' => 105, 'cup_g' => 220, 'piece_g' => 50,  'tbsp_g' => 15, 'tsp_g' => 5],
            'wet cat food'      => ['kcal_100g' => 95,  'cup_g' => 220, 'piece_g' => 50,  'tbsp_g' => 15, 'tsp_g' => 5],
            'wet food'          => ['kcal_100g' => 100, 'cup_g' => 220, 'piece_g' => 50,  'tbsp_g' => 15, 'tsp_g' => 5],
            'dry dog food'      => ['kcal_100g' => 365, 'cup_g' => 115, 'piece_g' => 2,   'tbsp_g' => 9,  'tsp_g' => 3],
            'dry cat food'      => ['kcal_100g' => 380, 'cup_g' => 115, 'piece_g' => 1.5, 'tbsp_g' => 9,  'tsp_g' => 3],
            'dry food'          => ['kcal_100g' => 365, 'cup_g' => 115, 'piece_g' => 2,   'tbsp_g' => 9,  'tsp_g' => 3],
            'dry kibble'        => ['kcal_100g' => 365, 'cup_g' => 115, 'piece_g' => 2,   'tbsp_g' => 9,  'tsp_g' => 3],
            'dog kibble'        => ['kcal_100g' => 360, 'cup_g' => 115, 'piece_g' => 2,   'tbsp_g' => 9,  'tsp_g' => 3],
            'cat kibble'        => ['kcal_100g' => 385, 'cup_g' => 115, 'piece_g' => 1.5, 'tbsp_g' => 9,  'tsp_g' => 3],
            'pedigree'          => ['kcal_100g' => 345, 'cup_g' => 110, 'piece_g' => 2,   'tbsp_g' => 9,  'tsp_g' => 3],
            'royal canin'       => ['kcal_100g' => 380, 'cup_g' => 110, 'piece_g' => 2,   'tbsp_g' => 9,  'tsp_g' => 3],
            'whiskas'           => ['kcal_100g' => 375, 'cup_g' => 110, 'piece_g' => 1.5, 'tbsp_g' => 9,  'tsp_g' => 3],
            'drool'             => ['kcal_100g' => 355, 'cup_g' => 110, 'piece_g' => 2,   'tbsp_g' => 9,  'tsp_g' => 3],
            'kibble'            => ['kcal_100g' => 365, 'cup_g' => 115, 'piece_g' => 2,   'tbsp_g' => 9,  'tsp_g' => 3],
            'pellet'            => ['kcal_100g' => 280, 'cup_g' => 110, 'piece_g' => 2,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'pellets'           => ['kcal_100g' => 280, 'cup_g' => 110, 'piece_g' => 2,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'cerelac'           => ['kcal_100g' => 415, 'cup_g' => 100, 'piece_g' => 15,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'curd rice'         => ['kcal_100g' => 140, 'cup_g' => 200, 'piece_g' => 30,  'tbsp_g' => 15, 'tsp_g' => 5],
            'curd'              => ['kcal_100g' => 98,  'cup_g' => 200, 'piece_g' => 20,  'tbsp_g' => 15, 'tsp_g' => 5],
            'milk'              => ['kcal_100g' => 60,  'cup_g' => 240, 'piece_g' => 30,  'tbsp_g' => 15, 'tsp_g' => 5],
            'dog biscuit'       => ['kcal_100g' => 350, 'cup_g' => 90,  'piece_g' => 14,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'dog treat'         => ['kcal_100g' => 330, 'cup_g' => 90,  'piece_g' => 10,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'cat treat'         => ['kcal_100g' => 360, 'cup_g' => 85,  'piece_g' => 2,   'tbsp_g' => 9,  'tsp_g' => 3],
            'chicken breast'    => ['kcal_100g' => 165, 'cup_g' => 140, 'piece_g' => 120, 'tbsp_g' => 12, 'tsp_g' => 4],
            'boiled chicken'    => ['kcal_100g' => 165, 'cup_g' => 140, 'piece_g' => 85,  'tbsp_g' => 12, 'tsp_g' => 4],
            'chicken'           => ['kcal_100g' => 165, 'cup_g' => 140, 'piece_g' => 85,  'tbsp_g' => 12, 'tsp_g' => 4],
            'turkey'            => ['kcal_100g' => 147, 'cup_g' => 140, 'piece_g' => 85,  'tbsp_g' => 12, 'tsp_g' => 4],
            'salmon'            => ['kcal_100g' => 208, 'cup_g' => 150, 'piece_g' => 100, 'tbsp_g' => 14, 'tsp_g' => 4.5],
            'tuna'              => ['kcal_100g' => 132, 'cup_g' => 150, 'piece_g' => 85,  'tbsp_g' => 14, 'tsp_g' => 4.5],
            'sardine'           => ['kcal_100g' => 208, 'cup_g' => 150, 'piece_g' => 24,  'tbsp_g' => 14, 'tsp_g' => 4.5],
            'beef'              => ['kcal_100g' => 250, 'cup_g' => 140, 'piece_g' => 85,  'tbsp_g' => 14, 'tsp_g' => 4.5],
            'lamb'              => ['kcal_100g' => 258, 'cup_g' => 140, 'piece_g' => 85,  'tbsp_g' => 14, 'tsp_g' => 4.5],
            'pork'              => ['kcal_100g' => 242, 'cup_g' => 140, 'piece_g' => 85,  'tbsp_g' => 14, 'tsp_g' => 4.5],
            'liver'             => ['kcal_100g' => 175, 'cup_g' => 140, 'piece_g' => 50,  'tbsp_g' => 14, 'tsp_g' => 4.5],
            'boiled egg'        => ['kcal_100g' => 155, 'cup_g' => 136, 'piece_g' => 50,  'tbsp_g' => 14, 'tsp_g' => 5],
            'egg'               => ['kcal_100g' => 155, 'cup_g' => 136, 'piece_g' => 50,  'tbsp_g' => 14, 'tsp_g' => 5],
            'shrimp'            => ['kcal_100g' => 99,  'cup_g' => 145, 'piece_g' => 10,  'tbsp_g' => 12, 'tsp_g' => 4],
            'baby carrot'       => ['kcal_100g' => 35,  'cup_g' => 128, 'piece_g' => 10,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'carrot'            => ['kcal_100g' => 41,  'cup_g' => 128, 'piece_g' => 61,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'sweet potato'      => ['kcal_100g' => 86,  'cup_g' => 133, 'piece_g' => 130, 'tbsp_g' => 14, 'tsp_g' => 5],
            'potato'            => ['kcal_100g' => 77,  'cup_g' => 150, 'piece_g' => 150, 'tbsp_g' => 14, 'tsp_g' => 5],
            'pumpkin'           => ['kcal_100g' => 26,  'cup_g' => 245, 'piece_g' => 50,  'tbsp_g' => 15, 'tsp_g' => 5],
            'romaine lettuce'   => ['kcal_100g' => 17,  'cup_g' => 47,  'piece_g' => 28,  'tbsp_g' => 5,  'tsp_g' => 2],
            'iceberg lettuce'   => ['kcal_100g' => 14,  'cup_g' => 55,  'piece_g' => 25,  'tbsp_g' => 5,  'tsp_g' => 2],
            'lettuce'           => ['kcal_100g' => 15,  'cup_g' => 47,  'piece_g' => 25,  'tbsp_g' => 5,  'tsp_g' => 2],
            'spinach'           => ['kcal_100g' => 23,  'cup_g' => 30,  'piece_g' => 10,  'tbsp_g' => 5,  'tsp_g' => 2],
            'kale'              => ['kcal_100g' => 49,  'cup_g' => 67,  'piece_g' => 20,  'tbsp_g' => 6,  'tsp_g' => 2],
            'broccoli'          => ['kcal_100g' => 34,  'cup_g' => 91,  'piece_g' => 30,  'tbsp_g' => 8,  'tsp_g' => 3],
            'cauliflower'       => ['kcal_100g' => 25,  'cup_g' => 107, 'piece_g' => 30,  'tbsp_g' => 8,  'tsp_g' => 3],
            'cucumber'          => ['kcal_100g' => 15,  'cup_g' => 104, 'piece_g' => 15,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'zucchini'          => ['kcal_100g' => 17,  'cup_g' => 124, 'piece_g' => 40,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'bell pepper'       => ['kcal_100g' => 31,  'cup_g' => 149, 'piece_g' => 119, 'tbsp_g' => 10, 'tsp_g' => 3.5],
            'capsicum'          => ['kcal_100g' => 31,  'cup_g' => 149, 'piece_g' => 119, 'tbsp_g' => 10, 'tsp_g' => 3.5],
            'green bean'        => ['kcal_100g' => 31,  'cup_g' => 100, 'piece_g' => 6,   'tbsp_g' => 8,  'tsp_g' => 3],
            'pea'               => ['kcal_100g' => 81,  'cup_g' => 145, 'piece_g' => 1,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'celery'            => ['kcal_100g' => 14,  'cup_g' => 101, 'piece_g' => 40,  'tbsp_g' => 8,  'tsp_g' => 3],
            'cilantro'          => ['kcal_100g' => 23,  'cup_g' => 16,  'piece_g' => 5,   'tbsp_g' => 2,  'tsp_g' => 1],
            'coriander'         => ['kcal_100g' => 23,  'cup_g' => 16,  'piece_g' => 5,   'tbsp_g' => 2,  'tsp_g' => 1],
            'parsley'           => ['kcal_100g' => 36,  'cup_g' => 60,  'piece_g' => 10,  'tbsp_g' => 4,  'tsp_g' => 1.5],
            'cabbage'           => ['kcal_100g' => 25,  'cup_g' => 89,  'piece_g' => 30,  'tbsp_g' => 8,  'tsp_g' => 3],
            'beetroot'          => ['kcal_100g' => 43,  'cup_g' => 136, 'piece_g' => 82,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'tomato'            => ['kcal_100g' => 18,  'cup_g' => 180, 'piece_g' => 123, 'tbsp_g' => 14, 'tsp_g' => 5],
            'onion'             => ['kcal_100g' => 40,  'cup_g' => 160, 'piece_g' => 110, 'tbsp_g' => 10, 'tsp_g' => 3.5],
            'garlic'            => ['kcal_100g' => 149, 'cup_g' => 136, 'piece_g' => 3,   'tbsp_g' => 9,  'tsp_g' => 3],
            'corn'              => ['kcal_100g' => 86,  'cup_g' => 154, 'piece_g' => 90,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'mushroom'          => ['kcal_100g' => 22,  'cup_g' => 70,  'piece_g' => 18,  'tbsp_g' => 7,  'tsp_g' => 2.5],
            'apple'             => ['kcal_100g' => 52,  'cup_g' => 125, 'piece_g' => 182, 'tbsp_g' => 10, 'tsp_g' => 3.5],
            'banana'            => ['kcal_100g' => 89,  'cup_g' => 150, 'piece_g' => 118, 'tbsp_g' => 14, 'tsp_g' => 5],
            'blueberry'         => ['kcal_100g' => 57,  'cup_g' => 148, 'piece_g' => 2,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'blueberries'       => ['kcal_100g' => 57,  'cup_g' => 148, 'piece_g' => 2,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'strawberry'        => ['kcal_100g' => 32,  'cup_g' => 152, 'piece_g' => 18,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'strawberries'      => ['kcal_100g' => 32,  'cup_g' => 152, 'piece_g' => 18,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'watermelon'        => ['kcal_100g' => 30,  'cup_g' => 152, 'piece_g' => 100, 'tbsp_g' => 12, 'tsp_g' => 4],
            'mango'             => ['kcal_100g' => 60,  'cup_g' => 165, 'piece_g' => 200, 'tbsp_g' => 12, 'tsp_g' => 4],
            'papaya'            => ['kcal_100g' => 43,  'cup_g' => 145, 'piece_g' => 150, 'tbsp_g' => 12, 'tsp_g' => 4],
            'pear'              => ['kcal_100g' => 57,  'cup_g' => 140, 'piece_g' => 178, 'tbsp_g' => 12, 'tsp_g' => 4],
            'peach'             => ['kcal_100g' => 39,  'cup_g' => 154, 'piece_g' => 150, 'tbsp_g' => 12, 'tsp_g' => 4],
            'pineapple'         => ['kcal_100g' => 50,  'cup_g' => 165, 'piece_g' => 84,  'tbsp_g' => 12, 'tsp_g' => 4],
            'cantaloupe'        => ['kcal_100g' => 34,  'cup_g' => 160, 'piece_g' => 69,  'tbsp_g' => 12, 'tsp_g' => 4],
            'raspberry'         => ['kcal_100g' => 52,  'cup_g' => 123, 'piece_g' => 4,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'blackberry'        => ['kcal_100g' => 43,  'cup_g' => 144, 'piece_g' => 5,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'kiwi'              => ['kcal_100g' => 61,  'cup_g' => 180, 'piece_g' => 76,  'tbsp_g' => 12, 'tsp_g' => 4],
            'guava'             => ['kcal_100g' => 68,  'cup_g' => 165, 'piece_g' => 55,  'tbsp_g' => 12, 'tsp_g' => 4],
            'orange'            => ['kcal_100g' => 47,  'cup_g' => 180, 'piece_g' => 131, 'tbsp_g' => 12, 'tsp_g' => 4],
            'lemon'             => ['kcal_100g' => 29,  'cup_g' => 212, 'piece_g' => 58,  'tbsp_g' => 14, 'tsp_g' => 5],
            'grape'             => ['kcal_100g' => 69,  'cup_g' => 151, 'piece_g' => 5,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'raisin'            => ['kcal_100g' => 299, 'cup_g' => 145, 'piece_g' => 1,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'cherry'            => ['kcal_100g' => 63,  'cup_g' => 154, 'piece_g' => 8,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'avocado'           => ['kcal_100g' => 160, 'cup_g' => 150, 'piece_g' => 150, 'tbsp_g' => 15, 'tsp_g' => 5],
            'brown rice'        => ['kcal_100g' => 111, 'cup_g' => 195, 'piece_g' => 30,  'tbsp_g' => 14, 'tsp_g' => 5],
            'white rice'        => ['kcal_100g' => 130, 'cup_g' => 158, 'piece_g' => 30,  'tbsp_g' => 14, 'tsp_g' => 5],
            'rice'              => ['kcal_100g' => 130, 'cup_g' => 158, 'piece_g' => 30,  'tbsp_g' => 14, 'tsp_g' => 5],
            'oatmeal'           => ['kcal_100g' => 71,  'cup_g' => 234, 'piece_g' => 30,  'tbsp_g' => 15, 'tsp_g' => 5],
            'oats'              => ['kcal_100g' => 389, 'cup_g' => 81,  'piece_g' => 20,  'tbsp_g' => 6,  'tsp_g' => 2],
            'quinoa'            => ['kcal_100g' => 120, 'cup_g' => 185, 'piece_g' => 30,  'tbsp_g' => 14, 'tsp_g' => 5],
            'millet'            => ['kcal_100g' => 378, 'cup_g' => 174, 'piece_g' => 10,  'tbsp_g' => 11, 'tsp_g' => 3.5],
            'wheat'             => ['kcal_100g' => 340, 'cup_g' => 165, 'piece_g' => 10,  'tbsp_g' => 11, 'tsp_g' => 3.5],
            'barley'            => ['kcal_100g' => 354, 'cup_g' => 170, 'piece_g' => 10,  'tbsp_g' => 11, 'tsp_g' => 3.5],
            'sunflower seed'    => ['kcal_100g' => 584, 'cup_g' => 140, 'piece_g' => 0.5, 'tbsp_g' => 9,  'tsp_g' => 3],
            'pumpkin seed'      => ['kcal_100g' => 559, 'cup_g' => 129, 'piece_g' => 0.5, 'tbsp_g' => 9,  'tsp_g' => 3],
            'flaxseed'          => ['kcal_100g' => 534, 'cup_g' => 168, 'piece_g' => 1,   'tbsp_g' => 10, 'tsp_g' => 3.5],
            'chia seed'         => ['kcal_100g' => 486, 'cup_g' => 160, 'piece_g' => 1,   'tbsp_g' => 12, 'tsp_g' => 4],
            'lentil'            => ['kcal_100g' => 116, 'cup_g' => 198, 'piece_g' => 10,  'tbsp_g' => 12, 'tsp_g' => 4],
            'chickpea'          => ['kcal_100g' => 164, 'cup_g' => 164, 'piece_g' => 2,   'tbsp_g' => 12, 'tsp_g' => 4],
            'peanut butter'     => ['kcal_100g' => 588, 'cup_g' => 258, 'piece_g' => 16,  'tbsp_g' => 16, 'tsp_g' => 5.3],
            'peanut'            => ['kcal_100g' => 567, 'cup_g' => 146, 'piece_g' => 1,   'tbsp_g' => 9,  'tsp_g' => 3],
            'almond'            => ['kcal_100g' => 579, 'cup_g' => 143, 'piece_g' => 1.2, 'tbsp_g' => 9,  'tsp_g' => 3],
            'walnut'            => ['kcal_100g' => 654, 'cup_g' => 117, 'piece_g' => 4,   'tbsp_g' => 8,  'tsp_g' => 2.5],
            'macadamia'         => ['kcal_100g' => 718, 'cup_g' => 134, 'piece_g' => 2.5, 'tbsp_g' => 9,  'tsp_g' => 3],
            'bread'             => ['kcal_100g' => 265, 'cup_g' => 45,  'piece_g' => 28,  'tbsp_g' => 8,  'tsp_g' => 3],
            'pasta'             => ['kcal_100g' => 131, 'cup_g' => 140, 'piece_g' => 20,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'plain yogurt'      => ['kcal_100g' => 61,  'cup_g' => 245, 'piece_g' => 50,  'tbsp_g' => 15, 'tsp_g' => 5],
            'yogurt'            => ['kcal_100g' => 61,  'cup_g' => 245, 'piece_g' => 50,  'tbsp_g' => 15, 'tsp_g' => 5],
            'curd'              => ['kcal_100g' => 61,  'cup_g' => 245, 'piece_g' => 50,  'tbsp_g' => 15, 'tsp_g' => 5],
            'cottage cheese'    => ['kcal_100g' => 98,  'cup_g' => 226, 'piece_g' => 30,  'tbsp_g' => 14, 'tsp_g' => 5],
            'cheese'            => ['kcal_100g' => 402, 'cup_g' => 113, 'piece_g' => 28,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'milk'              => ['kcal_100g' => 61,  'cup_g' => 244, 'piece_g' => 30,  'tbsp_g' => 15, 'tsp_g' => 5],
            'chocolate'         => ['kcal_100g' => 546, 'cup_g' => 170, 'piece_g' => 10,  'tbsp_g' => 12, 'tsp_g' => 4],
            'cocoa'             => ['kcal_100g' => 228, 'cup_g' => 86,  'piece_g' => 5,   'tbsp_g' => 5,  'tsp_g' => 2],
            'biscuit'           => ['kcal_100g' => 420, 'cup_g' => 80,  'piece_g' => 12,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'cookie'            => ['kcal_100g' => 488, 'cup_g' => 80,  'piece_g' => 16,  'tbsp_g' => 10, 'tsp_g' => 3.5],
            'cake'              => ['kcal_100g' => 350, 'cup_g' => 100, 'piece_g' => 65,  'tbsp_g' => 12, 'tsp_g' => 4],
            'honey'             => ['kcal_100g' => 304, 'cup_g' => 339, 'piece_g' => 10,  'tbsp_g' => 21, 'tsp_g' => 7],
            'spirulina'         => ['kcal_100g' => 290, 'cup_g' => 112, 'piece_g' => 1,   'tbsp_g' => 7,  'tsp_g' => 2.5],
            'mealworm'          => ['kcal_100g' => 206, 'cup_g' => 80,  'piece_g' => 0.2, 'tbsp_g' => 6,  'tsp_g' => 2],
        ];

        foreach ($db as $keyword => $nutrients) {
            if (str_contains($lower, $keyword)) {
                return $nutrients;
            }
        }
        return null;
    }

    private static function checkSafety(string $foodName, string $species, string $speciesLabel): array {
        $lower = strtolower(trim($foodName));
        if ($lower === '') {
            return [
                'status' => 'UNKNOWN',
                'message' => 'Enter a food name to check pet safety.',
            ];
        }

        $universalToxins = [
            ['keys' => ['chocolate', 'cocoa', 'cacao', 'brownie'], 'reason' => 'Contains theobromine and caffeine, which are severely toxic and potentially fatal to pets.'],
            ['keys' => ['xylitol', 'birch sugar', 'sugar-free gum', 'sugar free gum'], 'reason' => 'Contains xylitol, which causes rapid life-threatening hypoglycemia and liver failure.'],
            ['keys' => ['alcohol', 'beer', 'wine', 'liquor', 'vodka', 'whiskey'], 'reason' => 'Alcohol causes severe central nervous system depression, coma, and death in animals.'],
            ['keys' => ['caffeine', 'coffee', 'espresso', 'energy drink', 'black tea', 'green tea'], 'reason' => 'Caffeine causes heart arrhythmias, tremors, seizures, and organ failure in pets.'],
            ['keys' => ['onion', 'garlic', 'chive', 'leek', 'shallot', 'scallion'], 'reason' => 'Allium vegetables destroy red blood cells and cause hemolytic anemia and severe GI toxicity.'],
            ['keys' => ['apple seed', 'cherry pit', 'peach pit', 'plum pit', 'apricot pit', 'fruit pit'], 'reason' => 'Fruit seeds and pits contain cyanogenic glycosides (cyanide) and pose a choking hazard.'],
            ['keys' => ['rhubarb'], 'reason' => 'Contains soluble oxalates that cause kidney failure and severe digestive distress.'],
            ['keys' => ['nutmeg'], 'reason' => 'Contains myristicin, causing hallucinations, increased heart rate, and seizures.'],
            ['keys' => ['raw yeast dough', 'yeast dough'], 'reason' => 'Expands in the stomach and ferments into ethanol, causing dangerous bloating and alcohol poisoning.'],
            ['keys' => ['moldy', 'rotten', 'spoiled'], 'reason' => 'Contains tremorgenic mycotoxins that cause severe tremors and organ damage.'],
            ['keys' => ['macadamia'], 'reason' => 'Macadamia nuts cause severe weakness, tremors, hyperthermia, and neurological toxicity.'],
            ['keys' => ['cooked bone', 'chicken bone'], 'reason' => 'Cooked bones splinter easily and can cause choking or fatal internal perforations.'],
        ];

        foreach ($universalToxins as $toxin) {
            if (self::matchesAny($lower, $toxin['keys'])) {
                return [
                    'status' => 'TOXIC',
                    'message' => "TOXIC for {$speciesLabel}: {$toxin['reason']} Do NOT feed this item.",
                ];
            }
        }

        $speciesToxins = [
            'dog' => [
                ['keys' => ['grape', 'raisin', 'currant', 'sultana'], 'reason' => 'Grapes and raisins can cause sudden, fatal acute kidney failure in dogs.'],
                ['keys' => ['avocado'], 'reason' => 'Contains persin and high fat; pits also pose an obstruction risk for dogs.'],
                ['keys' => ['raw potato', 'green potato', 'tomato leaf', 'tomato stem'], 'reason' => 'Contains solanine, which is toxic to dogs.'],
                ['keys' => ['walnut', 'black walnut', 'pecan'], 'reason' => 'Can cause GI upset, pancreatitis, and mycotoxin seizures in dogs.'],
                ['keys' => ['lemon', 'lime', 'grapefruit'], 'reason' => 'High citric acid and essential oils can cause GI irritation and CNS depression in dogs.'],
                ['keys' => ['salt', 'salty chip', 'pretzel', 'candy', 'sweets'], 'reason' => 'Excessive salt or sugar can cause sodium ion poisoning, vomiting, or pancreatitis in dogs.'],
                ['keys' => ['cat food', 'cat kibble', 'whiskas'], 'reason' => 'Cat food is too high in protein and fat for dogs and can trigger pancreatitis or GI upset.'],
            ],
            'cat' => [
                ['keys' => ['grape', 'raisin', 'currant', 'sultana'], 'reason' => 'Grapes and raisins can cause acute kidney failure in cats.'],
                ['keys' => ['lily', 'lilies'], 'reason' => 'Extremely lethal to cats, causing rapid kidney failure even in tiny amounts.'],
                ['keys' => ['orange', 'lemon', 'lime', 'grapefruit', 'citrus', 'tangerine'], 'reason' => 'Citrus essential oils and psoralens are toxic to cats and cause vomiting and CNS depression.'],
                ['keys' => ['avocado'], 'reason' => 'Contains persin and high fat that can trigger vomiting, diarrhea, and pancreatitis in cats.'],
                ['keys' => ['raw potato', 'green potato', 'raw tomato'], 'reason' => 'Contains solanine, which is toxic to cats.'],
                ['keys' => ['milk', 'cream', 'ice cream'], 'reason' => 'Most adult cats are lactose intolerant; dairy causes painful cramping and severe diarrhea.'],
                ['keys' => ['dog food', 'dog kibble', 'pedigree'], 'reason' => 'Dog food lacks essential taurine and arachidonic acid that cats require to prevent heart and eye failure.'],
                ['keys' => ['raw fish', 'raw egg'], 'reason' => 'Contains thiaminase/avidin and bacterial risks that cause vitamin deficiency and illness in cats.'],
            ],
            'rabbit' => [
                ['keys' => ['avocado'], 'reason' => 'Avocado contains persin, which is highly toxic and potentially fatal to rabbits.'],
                ['keys' => ['iceberg lettuce'], 'reason' => 'Iceberg lettuce contains lactucarium and excess water that causes severe diarrhea in rabbits.'],
                ['keys' => ['potato', 'sweet potato', 'raw bean', 'kidney bean'], 'reason' => 'High starch and solanine disrupt rabbit gut flora and cause dangerous GI stasis.'],
                ['keys' => ['meat', 'chicken', 'beef', 'pork', 'turkey', 'salmon', 'tuna', 'fish', 'egg', 'shrimp', 'liver'], 'reason' => 'Rabbits are strict herbivores; animal proteins cannot be digested properly and cause fatal GI issues.'],
                ['keys' => ['milk', 'cheese', 'yogurt', 'curd', 'dairy', 'cream'], 'reason' => 'Rabbits cannot digest dairy; it causes life-threatening enterotoxemia and GI stasis.'],
                ['keys' => ['bread', 'pasta', 'cracker', 'cookie', 'cake', 'biscuit', 'candy', 'cereal', 'oatmeal'], 'reason' => 'Processed carbs and sugars cause fatal GI stasis and bacterial overgrowth in rabbits.'],
                ['keys' => ['seed', 'nut', 'almond', 'peanut', 'walnut', 'corn', 'cat food', 'dog food'], 'reason' => 'High-fat seeds, nuts, corn hulls, and carnivore kibble cause intestinal blockages and liver disease in rabbits.'],
            ],
            'guinea_pig' => [
                ['keys' => ['avocado'], 'reason' => 'Contains persin and high fat, which is toxic to guinea pigs.'],
                ['keys' => ['iceberg lettuce'], 'reason' => 'Contains lactucarium and causes diarrhea and dehydration in guinea pigs.'],
                ['keys' => ['potato', 'raw bean', 'mushroom'], 'reason' => 'Contains toxic alkaloids and indigestible starches unsafe for guinea pigs.'],
                ['keys' => ['meat', 'chicken', 'beef', 'pork', 'turkey', 'salmon', 'tuna', 'fish', 'egg', 'shrimp'], 'reason' => 'Guinea pigs are strict herbivores and must never eat meat, fish, or eggs.'],
                ['keys' => ['milk', 'cheese', 'yogurt', 'curd', 'dairy'], 'reason' => 'Guinea pigs are lactose intolerant; dairy causes severe GI distress.'],
                ['keys' => ['bread', 'pasta', 'cookie', 'cake', 'biscuit', 'candy', 'seed', 'nut', 'peanut', 'almond', 'corn'], 'reason' => 'Seeds, nuts, and baked carbs pose choking hazards and cause digestive stasis in guinea pigs.'],
                ['keys' => ['cat food', 'dog food', 'rabbit pellet'], 'reason' => 'Wrong species formulation (lacks stabilized Vitamin C required by guinea pigs).'],
            ],
            'hamster' => [
                ['keys' => ['avocado'], 'reason' => 'Contains persin and excessive fat, which is toxic to hamsters.'],
                ['keys' => ['orange', 'lemon', 'lime', 'grapefruit', 'citrus', 'pineapple'], 'reason' => 'Highly acidic citrus fruits cause severe stomach upset and diarrhea in hamsters.'],
                ['keys' => ['almond', 'bitter almond'], 'reason' => 'Bitter almonds contain cyanide compounds toxic to hamsters.'],
                ['keys' => ['raw potato', 'raw bean', 'kidney bean', 'tomato leaf', 'iceberg lettuce'], 'reason' => 'Raw potatoes/beans contain toxins and iceberg lettuce causes wet-tail/diarrhea in hamsters.'],
                ['keys' => ['candy', 'cookie', 'cake', 'salty', 'chip', 'honey'], 'reason' => 'High sugar and sticky foods can stick in cheek pouches and trigger diabetes in hamsters.'],
            ],
            'bird' => [
                ['keys' => ['avocado'], 'reason' => 'Avocado contains persin, which causes acute myocardial necrosis and sudden death in birds and pigeons.'],
                ['keys' => ['mushroom'], 'reason' => 'Mushrooms can cause severe digestive upset and liver failure in birds.'],
                ['keys' => ['milk', 'cheese', 'yogurt', 'curd', 'dairy', 'cream'], 'reason' => 'Birds lack lactase enzymes and cannot digest dairy products, leading to severe diarrhea.'],
                ['keys' => ['salt', 'salty', 'chip', 'pretzel', 'cracker', 'candy', 'cookie', 'cake'], 'reason' => 'Even small amounts of salt cause dehydration, kidney dysfunction, and death in birds.'],
                ['keys' => ['raw bean', 'dried bean', 'kidney bean'], 'reason' => 'Uncooked beans contain hemagglutinin, which is deadly to birds.'],
                ['keys' => ['tomato leaf', 'raw potato', 'cassava', 'peanut butter'], 'reason' => 'Contains solanine/toxins or poses a severe beak/crop choking hazard for birds.'],
                ['keys' => ['cat food', 'dog food'], 'reason' => 'Formulated for mammals with excessive iron, fat, and salt unsuitable for avian kidneys.'],
            ],
            'fish' => [
                ['keys' => ['avocado'], 'reason' => 'Contains persin and oils that foul water and are toxic to fish.'],
                ['keys' => ['bread', 'cracker', 'cookie', 'cake', 'pasta', 'biscuit'], 'reason' => 'Bread swells inside the fish digestive tract, causing swim bladder disease, constipation, and ammonia spikes.'],
                ['keys' => ['milk', 'cheese', 'yogurt', 'dairy', 'butter'], 'reason' => 'Fish cannot digest dairy; it pollutes aquarium water and harms fish organs.'],
                ['keys' => ['beef', 'pork', 'lamb', 'chicken', 'turkey', 'bacon', 'sausage'], 'reason' => 'Mammalian and poultry fats cannot be metabolized properly by fish and cause fatty liver disease.'],
                ['keys' => ['salt', 'spiced', 'fried', 'candy', 'chip', 'cat food', 'dog food'], 'reason' => 'Seasoned human food and pet kibble contain fats, preservatives, and salts dangerous to fish and tank water.'],
            ],
            'other' => [
                ['keys' => ['avocado'], 'reason' => 'Avocado is toxic to many small animals, birds, reptiles, and exotic pets.'],
            ],
        ];

        foreach ($speciesToxins[$species] ?? [] as $rule) {
            if (self::matchesAny($lower, $rule['keys'])) {
                return [
                    'status' => 'TOXIC',
                    'message' => "TOXIC for {$speciesLabel}: {$rule['reason']} Do NOT feed this item.",
                ];
            }
        }

        $speciesSafe = [
            'dog' => [
                ['keys' => ['dog kibble', 'wet dog food', 'dog biscuit', 'dog treat', 'pedigree', 'royal canin', 'drool', 'kibble'], 'note' => 'Formulated for canine nutrition. Feed according to your dog\'s daily calorie needs.'],
                ['keys' => ['chicken', 'turkey', 'salmon', 'beef', 'lamb', 'pork', 'liver', 'sardine', 'tuna', 'shrimp'], 'note' => 'Safe for dogs when fully cooked, plain, boneless, and unseasoned.'],
                ['keys' => ['egg', 'boiled egg'], 'note' => 'Fully cooked plain eggs are a nutritious protein source for dogs.'],
                ['keys' => ['carrot', 'baby carrot', 'sweet potato', 'pumpkin', 'green bean', 'pea', 'broccoli', 'cauliflower', 'cucumber', 'zucchini', 'celery', 'spinach', 'bell pepper', 'capsicum', 'lettuce'], 'note' => 'Safe vegetable for dogs in appropriate portions (serve plain; steam hard veggies if needed).'],
                ['keys' => ['apple', 'banana', 'blueberry', 'blueberries', 'strawberry', 'strawberries', 'watermelon', 'mango', 'papaya', 'pear', 'peach', 'pineapple', 'cantaloupe', 'raspberry', 'blackberry', 'orange'], 'note' => 'Safe fruit for dogs in moderate treat amounts. Always remove seeds, cores, and pits first.'],
                ['keys' => ['rice', 'brown rice', 'white rice', 'oatmeal', 'oats', 'quinoa', 'barley'], 'note' => 'Plain cooked grains are gentle on digestion and safe for dogs.'],
                ['keys' => ['peanut butter', 'peanut'], 'note' => 'Safe for dogs ONLY if unsalted and strictly xylitol-free (verify ingredient label).'],
                ['keys' => ['plain yogurt', 'yogurt', 'curd', 'cottage cheese', 'cheese'], 'note' => 'Safe in small occasional amounts for dogs that tolerate dairy; use plain unsweetened varieties.'],
            ],
            'cat' => [
                ['keys' => ['cat kibble', 'wet cat food', 'cat treat', 'whiskas', 'royal canin', 'kibble', 'wet food'], 'note' => 'Complete feline nutrition. Ensure fresh water is always available.'],
                ['keys' => ['chicken', 'turkey', 'salmon', 'tuna', 'sardine', 'beef', 'lamb', 'liver', 'shrimp'], 'note' => 'Safe for cats when thoroughly cooked, plain, boneless, and without onion/garlic seasoning.'],
                ['keys' => ['boiled egg', 'egg'], 'note' => 'Small portions of fully cooked plain egg are safe for cats.'],
                ['keys' => ['pumpkin', 'carrot', 'pea', 'cucumber', 'zucchini', 'green bean', 'broccoli', 'spinach'], 'note' => 'Safe in small cooked/plain portions as a light fiber boost for cats (obligate carnivores).'],
                ['keys' => ['blueberry', 'blueberries', 'strawberry', 'strawberries', 'watermelon', 'cantaloupe', 'banana', 'apple'], 'note' => 'Safe only in tiny occasional treat pieces for cats (remove seeds/rind; cats need meat-based diets).'],
                ['keys' => ['oatmeal', 'rice'], 'note' => 'Plain cooked amounts in tiny portions are safe for cats, though meat should remain primary.'],
            ],
            'rabbit' => [
                ['keys' => ['timothy hay', 'alfalfa hay', 'hay'], 'note' => 'Essential staple forage for rabbits (should make up ~80% of daily diet for teeth and gut health).'],
                ['keys' => ['rabbit pellet', 'pellet'], 'note' => 'Safe for rabbits in measured portions alongside unlimited hay and fresh water.'],
                ['keys' => ['romaine lettuce', 'cilantro', 'coriander', 'parsley', 'kale', 'spinach', 'celery', 'bell pepper', 'capsicum', 'cucumber', 'zucchini', 'broccoli', 'cabbage', 'lettuce'], 'note' => 'Safe leafy green/vegetable for rabbits when washed thoroughly and introduced gradually.'],
                ['keys' => ['carrot', 'baby carrot', 'apple', 'banana', 'blueberry', 'blueberries', 'strawberry', 'strawberries', 'pear', 'peach', 'papaya', 'pineapple', 'watermelon', 'raspberry', 'blackberry'], 'note' => 'Safe for rabbits ONLY in small treat portions due to natural sugar content. Remove seeds/pits.'],
            ],
            'guinea_pig' => [
                ['keys' => ['timothy hay', 'alfalfa hay', 'hay'], 'note' => 'Essential unlimited daily staple for guinea pig digestion and dental health.'],
                ['keys' => ['guinea pig pellet', 'pellet'], 'note' => 'Safe when formulated with stabilized Vitamin C for guinea pigs.'],
                ['keys' => ['bell pepper', 'capsicum', 'romaine lettuce', 'cilantro', 'coriander', 'parsley', 'cucumber', 'zucchini', 'celery', 'kale', 'spinach', 'broccoli', 'tomato', 'carrot', 'baby carrot', 'cabbage', 'lettuce'], 'note' => 'Safe fresh vegetable for guinea pigs (bell peppers & leafy greens provide essential Vitamin C).'],
                ['keys' => ['orange', 'kiwi', 'guava', 'strawberry', 'strawberries', 'blueberry', 'blueberries', 'apple', 'banana', 'pear', 'watermelon', 'papaya', 'cantaloupe'], 'note' => 'Safe Vitamin C fruit treat for guinea pigs in small portions; remove all seeds.'],
            ],
            'hamster' => [
                ['keys' => ['hamster pellet', 'pellet', 'bird seed', 'sunflower seed', 'pumpkin seed', 'flaxseed', 'chia seed', 'millet', 'oats', 'oatmeal', 'barley', 'wheat', 'quinoa', 'rice', 'corn', 'lentil', 'pea'], 'note' => 'Safe seed/grain/pellet for hamsters in balanced small portions (limit high-fat seeds).'],
                ['keys' => ['carrot', 'baby carrot', 'cucumber', 'broccoli', 'cauliflower', 'zucchini', 'bell pepper', 'capsicum', 'spinach', 'kale', 'romaine lettuce', 'celery', 'green bean', 'sweet potato'], 'note' => 'Safe fresh vegetable for hamsters in tiny washed pieces; remove uneaten fresh food promptly.'],
                ['keys' => ['apple', 'banana', 'blueberry', 'blueberries', 'strawberry', 'strawberries', 'pear', 'peach', 'raspberry', 'blackberry', 'watermelon', 'grape'], 'note' => 'Safe fruit for hamsters in very small occasional treat pieces (remove seeds; avoid excess sugar).'],
                ['keys' => ['boiled chicken', 'chicken', 'turkey', 'boiled egg', 'egg', 'mealworm', 'plain yogurt', 'cottage cheese'], 'note' => 'Safe occasional protein treat for omnivorous hamsters when plain and fully cooked.'],
            ],
            'bird' => [
                ['keys' => ['bird seed', 'pigeon mix', 'millet', 'wheat', 'barley', 'corn', 'oats', 'rice', 'quinoa', 'lentil', 'pea', 'chickpea', 'sunflower seed', 'pumpkin seed', 'flaxseed', 'chia seed', 'pellet'], 'note' => 'Safe grains/seeds/pellets for birds & pigeons. Serve raw/soaked or plain cooked without salt.'],
                ['keys' => ['carrot', 'broccoli', 'spinach', 'kale', 'romaine lettuce', 'lettuce', 'bell pepper', 'capsicum', 'cucumber', 'zucchini', 'sweet potato', 'pumpkin', 'green bean', 'celery', 'cilantro', 'parsley', 'cauliflower', 'beetroot'], 'note' => 'Safe nutritious vegetable for birds; wash thoroughly and chop to bird-appropriate size.'],
                ['keys' => ['apple', 'banana', 'blueberry', 'blueberries', 'strawberry', 'strawberries', 'grape', 'watermelon', 'mango', 'papaya', 'pear', 'peach', 'pineapple', 'kiwi', 'guava', 'orange', 'cantaloupe', 'raspberry', 'blackberry'], 'note' => 'Safe fruit for birds in moderate portions. Always remove all seeds and pits before serving.'],
                ['keys' => ['boiled egg', 'mealworm'], 'note' => 'Safe occasional protein boost for birds when thoroughly cooked/prepared plain.'],
            ],
            'fish' => [
                ['keys' => ['fish flake', 'fish pellet', 'pellet', 'bloodworm', 'brine shrimp', 'spirulina', 'mealworm', 'shrimp'], 'note' => 'Safe species-appropriate fish food. Feed only what your fish consumes within 2 minutes.'],
                ['keys' => ['pea', 'cucumber', 'zucchini', 'spinach', 'romaine lettuce', 'lettuce', 'carrot', 'broccoli'], 'note' => 'Safe vegetable treat for omnivorous/herbivorous fish when blanched/boiled soft and deskinned (especially peas).'],
            ],
            'other' => [
                ['keys' => ['hay', 'timothy hay'], 'note' => 'Generally safe forage for herbivorous small pets, though always confirm dietary needs for your exact species.'],
            ],
        ];

        foreach ($speciesSafe[$species] ?? [] as $rule) {
            if (self::matchesAny($lower, $rule['keys'])) {
                return [
                    'status' => 'SAFE',
                    'message' => "SAFE for {$speciesLabel}: {$rule['note']}",
                ];
            }
        }

        return [
            'status' => 'UNKNOWN',
            'message' => "Safety information for \"{$foodName}\" could not be verified for {$speciesLabel}. Please verify this food with a qualified veterinarian before feeding.",
        ];
    }

    private static function matchesAny(string $haystack, array $keywords): bool {
        foreach ($keywords as $kw) {
            if ($kw !== '' && str_contains($haystack, $kw)) {
                return true;
            }
        }
        return false;
    }
}
