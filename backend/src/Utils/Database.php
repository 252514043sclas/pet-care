<?php

declare(strict_types=1);

namespace App\Utils;

use PDO;
use PDOException;

class Database {
    private static ?PDO $instance = null;

    public static function getConnection(): PDO {
        if (self::$instance === null) {
            $config = require __DIR__ . '/../../config/database.php';
            $dsn = sprintf(
                'mysql:host=%s;port=%s;dbname=%s;charset=%s',
                $config['host'],
                $config['port'],
                $config['dbname'],
                $config['charset']
            );

            try {
                self::$instance = new PDO(
                    $dsn,
                    $config['username'],
                    $config['password'],
                    $config['options']
                );
            } catch (PDOException $e) {
                // If database doesn't exist, try connecting to MySQL server to create it
                if ($e->getCode() == 1049) {
                    $serverDsn = sprintf(
                        'mysql:host=%s;port=%s;charset=%s',
                        $config['host'],
                        $config['port'],
                        $config['charset']
                    );
                    $pdo = new PDO($serverDsn, $config['username'], $config['password'], $config['options']);
                    $pdo->exec(sprintf('CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci', $config['dbname']));
                    self::$instance = new PDO($dsn, $config['username'], $config['password'], $config['options']);
                } else {
                    throw $e;
                }
            }
        }

        return self::$instance;
    }
}

