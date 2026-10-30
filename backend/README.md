# 🐾 Pet Care Backend (PHP 8+ & MySQL)

Welcome to the backend service for the **Pet Care** Android application.

## 🛠️ Stack & Technologies
- **Language**: PHP 8.0+ (Tested on PHP 8.2.12)
- **Database**: MySQL / MariaDB (Tested on MariaDB 10.4.32)
- **Database Driver**: PHP PDO with prepared statements
- **Authentication**: SHA-256 mobile bearer tokens with expiration
- **Routing**: Centralized RESTful router in `public/index.php`

---

## 📁 Directory Structure
```
backend/
├── config/
│   └── database.php       # Database configuration loader
├── public/
│   ├── .htaccess          # URL rewrite rules for Apache
│   └── index.php          # Main REST router entrypoint
├── src/
│   ├── Controllers/       # REST controllers (Auth, Pet, Dashboard, etc.)
│   ├── Middleware/        # Auth & CORS middleware
│   └── Utils/             # Database PDO singleton, JSON Response & Validator
├── database/
│   ├── schema.sql         # Normalized MySQL table schema
│   ├── seed.sql           # Realistic development seed dataset
│   └── migrations/        # Initial database migration script
├── tests/
│   └── test_api.php       # Automated test suite
├── .env.example           # Environment template
└── README.md
```

---

## 🚀 Setup & Execution Guide

### 1. Database Setup with XAMPP
1. Start **MySQL** from the XAMPP Control Panel.
2. Open PowerShell or Command Prompt in the project root:
   ```powershell
   cmd /c "C:\xampp\mysql\bin\mysql.exe -u root < database\schema.sql"
   cmd /c "C:\xampp\mysql\bin\mysql.exe -u root < database\seed.sql"
   ```

### 2. Running with Apache (XAMPP)
A directory junction has been established between this `backend/` folder and `C:\xampp\htdocs\petcare`:
- Start **Apache** from XAMPP Control Panel.
- Access the API:
  - Localhost: `http://localhost/petcare/public/api/health-check`
  - Android Emulator: `http://10.0.2.2/petcare/public/api/`
  - Physical Android Device: `http://<your-lan-ip>/petcare/public/api/`

### 3. Alternative: Running with PHP Built-in Server
If Apache is not running, you can launch PHP's standalone server:
```powershell
& "C:\xampp\php\php.exe" -S 0.0.0.0:8000 -t backend\public
```
Then use Base URL:
- Android Emulator: `http://10.0.2.2:8000/api/`

---

## 🧪 Testing the Backend
Execute the automated test suite anytime using PHP CLI:
```powershell
& "C:\xampp\php\php.exe" backend\tests\test_api.php
```
All 8 test suites will execute and verify database connections, authentication, pet profiles, meals, weights, reminders, and diagnostic scans.

