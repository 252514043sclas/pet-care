# 🐾 Pet Care Application — Comprehensive Project Analysis & Architecture Plan

## 1. Executive Summary & Project Overview

This document provides a comprehensive architectural and code analysis of the **Pet Care** Android application. The current frontend is an Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3**. While the visual design system, layouts, animations, navigation, and theme are implemented, the application currently operates on static mock data, hardcoded strings, and placeholder callbacks without persistent backend integration.

This analysis details:
1. All screens, navigation destinations, and components.
2. An exhaustive audit of dummy data, hardcoded values, and non-functional user actions.
3. Persistent storage and data flow requirements.
4. Security vulnerabilities, architectural flaws, and missing components.
5. A screen-by-screen mapping to database tables and REST API endpoints.
6. A phased transformation plan into an enterprise-grade MVVM client powered by a PHP 8+ REST API and MySQL database.

---

## 2. Screen & Navigation Inventory

The application currently comprises 23 primary screen destinations implemented across 16 Activity classes and sub-composables:

| # | Screen / Activity | Purpose | Current Navigation Triggers & Destinations |
|---|-------------------|---------|---------------------------------------------|
| 1 | **SplashActivity** (`SplashScreen`) | Brand introduction and launch point. | "Get Started" button -> `OnboardingActivity`. |
| 2 | **OnboardingActivity** (`OnboardingScreen`) | 3-step feature carousel introducing Pet Care capabilities. | "Skip" / "Get Started" -> `LoginActivity`. |
| 3 | **LoginActivity** (`LoginScreen`) | User authentication & password reset modal. | "Log In" -> `HomeDashboardActivity`; "Sign Up" -> `SignUpActivity`; "Forgot Password?" -> In-dialog reset link. |
| 4 | **SignUpActivity** (`SignUpScreen`) | New user account creation. | "Create Account" -> `SelectPetTypeActivity`; Back button -> `LoginActivity`. |
| 5 | **SelectPetTypeActivity** (`SelectPetTypeScreen`) | Selection of pet species (Dog, Cat, Rabbit, Bird, Hamster, Other). | "Continue" -> `AddPetProfileActivity`; Back button -> `SignUpActivity`. |
| 6 | **AddPetProfileActivity** (`AddPetProfileScreen`) | Initial pet onboarding form (Name, Breed, Age, Weight). | "Save Profile" -> `HomeDashboardActivity`. |
| 7 | **HomeDashboardActivity** (`MainSlidingDashboardScreen`) | Core app container featuring a 5-tab HorizontalPager with floating bottom navigation. | Switches between 5 tabs: Home (0), Health (1), Diet (2), Care (3), Profile (4). Handles double back press exit dialog. |
| 8 | **HomeTabScreen** (Dashboard Tab 0) | Primary dashboard displaying active pet banner, quick care actions, and today's schedule. | Active Pet Card / Profile icon -> `PetDetailProfileActivity`; Quick Action cards -> Health tab, Diet tab, Care tab, `SelectPetTypeActivity`. |
| 9 | **HealthTabScreen** (Dashboard Tab 1) | Health & Diagnostics launcher with AI camera scan & symptom checker. | "AI Camera Scan" -> `PhotoScanActivity`; "Select Symptoms" -> `SymptomSelectionActivity`. |
| 10 | **DietTabScreen** (Dashboard Tab 2) | Nutrition hub with calorie metrics & quick links. | Quick Action cards -> `DietPlanActivity`, `MealLogActivity`, `SafeVsToxicFoodsActivity`, `WeightTrackerActivity`. |
| 11 | **CareTabScreen** (Dashboard Tab 3) | Care & Grooming hub with reminder and tutorial shortcuts. | Cards -> `AddReminderActivity`, `GroomingCalendarActivity`, `GroomingGuideActivity`. |
| 12 | **ProfileTabScreen** (Dashboard Tab 4) | High-level pet profile overview (weight, health score, vaccination status). | Click avatar/card -> `PetDetailProfileActivity`; "Add Another Pet" -> `SelectPetTypeActivity`. |
| 13 | **PetDetailProfileActivity** (`PetDetailProfileScreen`) | In-depth pet identity, physical stats, microchip ID, and vaccination record. | Back button -> finish to caller. |
| 14 | **HealthHomeActivity** (`HealthHomeScreen`) | Standalone health navigation center (alternative entry to diagnostics). | Cards -> `PhotoScanActivity`, `SymptomSelectionActivity`, `DiseaseDetailActivity`. |
| 15 | **PhotoScanActivity** (`PhotoScanScreen`) | Mock viewfinder for AI visual scanning of coat, eyes, or skin. | "Capture & Scan" -> `AnalyzingActivity`. |
| 16 | **SymptomSelectionActivity** (`SymptomSelectionScreen`) | Checklist of physical symptoms (itching, vomiting, lethargy, etc.). | "Analyze Symptoms" -> `AnalyzingActivity`. |
| 17 | **AnalyzingActivity** (`AnalyzingScreen`) | Animated loading indicator simulating AI diagnostic reasoning. | Timed delay (2.5s) -> `ResultActivity`. |
| 18 | **ResultActivity** (`ResultScreen`) | Diagnosis match results (condition, match confidence, severity, description). | "View Detailed Disease Info" -> `DiseaseDetailActivity`; "Back to Health Home" -> `HealthHomeActivity`. |
| 19 | **DiseaseDetailActivity** (`DiseaseDetailScreen`) | Condition clinical guide (causes, interim care steps, vet emergency warnings). | "Save Report" (currently no-op); "Find Vet" (currently no-op); Back button -> finish. |
| 20 | **DietHomeActivity** (`DietHomeScreen`) | Standalone nutrition center showing daily calorie consumption bar. | Cards -> `DietPlanActivity`, `MealLogActivity`, `SafeVsToxicFoodsActivity`, `WeightTrackerActivity`. |
| 21 | **DietPlanActivity** (`DietPlanScreen`) | Scheduled daily feeding breakdown (Morning, Afternoon, Evening). | Back button -> finish. |
| 22 | **MealLogActivity** (`MealLogScreen`) | Food item entry & portion size slider (0.5 to 3.0 cups). | "Log Meal" button -> finish without persisting. |
| 23 | **SafeVsToxicFoodsActivity** (`SafeVsToxicFoodsScreen`) | Educational library of safe and dangerous foods for pets. | Back button -> finish. |
| 24 | **WeightTrackerActivity** (`WeightTrackerScreen`) | Pet weight history, current vs target weight, and new weight entry. | "Record Weight" button -> finish without persisting. |
| 25 | **GroomingHomeActivity** (`GroomingHomeScreen`) | Standalone care hub for reminders, calendar, and tutorials. | Cards -> `AddReminderActivity`, `GroomingCalendarActivity`, `GroomingGuideActivity`. |
| 26 | **AddReminderActivity** (`AddReminderScreen`) | Creation form for grooming/wellness alert with push notification toggle. | "Save Reminder" button -> finish without persisting. |
| 27 | **GroomingCalendarActivity** (`GroomingCalendarScreen`) | Monthly chronological care and grooming task list. | Back button -> finish. |
| 28 | **GroomingGuideActivity** (`GroomingGuideScreen`) | Step-by-step grooming tutorials (Coat brushing, Nail trimming, Ear cleaning). | Back button -> finish. |

---

## 3. Dummy Data, Hardcoded Lists & Non-Functional Elements

### 3.1 Hardcoded Values & Mock Datasets
1. **User Identity & Auth**:
   - `LoginActivity`: Hardcoded transitions. Any input triggers `onLogin()`. No backend verification, password hashing, or token issuance.
   - `SignUpActivity`: Validates local field lengths only; skips user record persistence.
   - `LoginActivity` "Forgot Password": Emits a static local toast string (`"Password reset link sent to..."`) without generating a reset token or email.
2. **Active Pet Profile**:
   - Hardcoded in multiple locations (`HomeTabScreen`, `ProfileTabScreen`, `PetDetailProfileActivity`, `PetViewModel`):
     - Name: `"Clover"`
     - Breed: `"Holland Lop"`
     - Species: `"Rabbit"`
     - Age: `"1.2 yrs"` / `"1.2 Years"`
     - Weight: `"1.8 kg"` (conflicting with `WeightTrackerActivity` which hardcodes `"28.0 kg"` and `"Max"`)
     - Gender: `"Female"`
     - Microchip ID: `"#985142091"`
     - Health Score: `"95 / 100"`
     - Vaccination Status: `"Up to Date"`
3. **Daily Schedule & Reminders**:
   - `HomeTabScreen`: Hardcoded list with `"Morning Timothy Hay"` at 8:00 AM and `"Fur Brushing & Nail Check"` at 5:30 PM.
   - `GroomingCalendarActivity`: Hardcoded list referring to pet `"Max"`:
     - `"Oct 12, 10:00 AM - Bath & Shampoo Session"`
     - `"Oct 19, 2:00 PM - Nail Trimming & Filing"`
     - `"Oct 26, 11:00 AM - Ear Cleaning & Checkup"`
     - `"Nov 02, 10:00 AM - Full Grooming & Haircut"`
4. **Diet & Nutrition**:
   - `DietHomeActivity`: Hardcoded `"Consumed: 650 / 800 kcal"` and progress `650f / 800f`.
   - `DietPlanActivity`: Hardcoded meal cards (`"2 Cups Premium Kibble - 350 kcal"`, `"Healthy Carrot Snack - 100 kcal"`, `"2 Cups Kibble + Wet Broth - 350 kcal"`).
   - `SafeVsToxicFoodsActivity`: Static hardcoded list of 7 food items in Compose state.
5. **Health & AI Diagnostics**:
   - `PhotoScanActivity`: Camera viewfinder is a static grey box with a camera emoji. No actual camera or image upload.
   - `AnalyzingActivity`: Simulates analysis using `delay(2500)` before launching the result screen.
   - `ResultActivity`: Hardcoded assessment (`"Minor Skin Allergy"`, `"88% Match"`, `"Severity: Mild"`).
   - `DiseaseDetailActivity`: Fixed static content for `"Mite-related irritation"` and Clover.
6. **In-Memory ViewModel (`PetViewModel`)**:
   - Contains hardcoded defaults (`PetProfile()`, 3 `CareReminder`, 3 `MealEntry`, 3 `WeightEntry`).
   - Mutations in `PetViewModel` are in-memory only and lost on activity destruction or process death.
   - The activities do not observe or inject `PetViewModel`, causing complete state disconnection.

### 3.2 Non-Functional User Actions
- **AddPetProfileActivity "Save Profile"**: Only calls `startActivity(HomeDashboardActivity)`. Input fields are discarded.
- **MealLogActivity "Log Meal"**: Calls `finish()` without storing meal data.
- **WeightTrackerActivity "Record Weight"**: Calls `finish()` without recording weight.
- **AddReminderActivity "Save Reminder"**: Calls `finish()` without scheduling or persisting the reminder.
- **DiseaseDetailActivity "Save Report"**: Empty lambda `{ /* Save Report */ }`.
- **DiseaseDetailActivity "Find Vet"**: Empty lambda `{ /* Find Vet */ }`.

---

## 4. Missing Functionality, Architectural Issues & Security Concerns

### 4.1 Missing Networking & Infrastructure
1. **Network Permission**: Missing `<uses-permission android:name="android.permission.INTERNET" />` in `AndroidManifest.xml`.
2. **Cleartext Traffic Policy**: Android 9+ (API 28+) blocks unencrypted HTTP traffic by default. Local emulator communication (`http://10.0.2.2/...`) requires a `network-security-config.xml` or `android:usesCleartextTraffic="true"`.
3. **Networking Dependencies**: Neither Retrofit, OkHttp, nor JSON serialization (Moshi/Gson/Kotlinx Serialization) are installed in `libs.versions.toml` or `app/build.gradle.kts`.
4. **Lifecycle & ViewModel Compose**: Missing `androidx.lifecycle:lifecycle-viewmodel-compose` dependencies for standard MVVM integration inside Compose screens.

### 4.2 Architectural Issues
1. **Multiple Monolithic Activities**: The app is built with 16 separate `ComponentActivity` classes instead of a unified single-activity Compose architecture or shared ViewModels. As a result:
   - Data cannot be shared between screens in memory without passing Intents or having a centralized singleton repository/data layer.
2. **UI Coupling with Mock Logic**: Composable screens instantiate `remember { mutableStateOf(...) }` for business state instead of hoisting state to a ViewModel backed by a Repository.
3. **Unused / Stray Files**:
   - `app/splashactivity.kt` (scratch file with wrong package name `com.example.petcare`).
   - `app/frhrh.kt` (empty file).
   - `app/ghj.kts` (empty Kotlin script).

### 4.3 Security & Multi-Tenant Isolation Concerns
1. **Authentication Token Lifecycle**: Authentication must issue cryptographically secure random bearer tokens (e.g., 64-character SHA-256 tokens) stored with expiration timestamps.
2. **Password Security**: Passwords must be hashed using `password_hash($password, PASSWORD_BCRYPT)` with `cost >= 12` or `PASSWORD_ARGON2ID`.
3. **User Isolation / IDOR Vulnerability Prevention**: Every query for pets, meals, weight, reminders, or health records must explicitly enforce `WHERE user_id = :authenticated_user_id` to prevent Insecure Direct Object References.
4. **SQL Injection**: All MySQL access must strictly utilize PHP PDO with prepared statements and parameter binding.
5. **Credential Leakage**: No database passwords or private API keys may be hardcoded. Backend configuration must rely on `.env` files with `.env.example` templates.

---

## 5. Screen-by-Screen Component, Data, Database & API Mapping

| Screen / UI Component | UI Input / Elements | Required Backend Data | Database Table(s) | REST API Endpoint(s) |
|---|---|---|---|---|
| **LoginScreen** | Email, Password, Forgot Password dialog | User credentials, token issuance | `users`, `user_tokens`, `password_resets` | `POST /api/auth/login`<br>`POST /api/auth/forgot-password` |
| **SignUpScreen** | Full Name, Email, Password | User registration payload | `users`, `user_tokens` | `POST /api/auth/register` |
| **SelectPetTypeScreen** | Species selection (Dog, Cat, Rabbit, etc.) | Standard pet types catalog | `pet_species` / Static Catalog | `GET /api/pets/species` |
| **AddPetProfileScreen** | Pet Name, Breed, Age, Weight, Species | New pet creation payload | `pets`, `pet_profiles` | `POST /api/pets` |
| **HomeTabScreen** | Active Pet Banner, Quick Actions, Today's Schedule | Active pet data, today's meals, upcoming care reminders | `pets`, `care_reminders`, `meal_entries` | `GET /api/dashboard`<br>`GET /api/pets/active` |
| **ProfileTabScreen** & **PetDetailProfileScreen** | Full Pet Details (Weight, Breed, Age, Gender, Microchip, Health Score, Vaccination) | Detailed pet health stats, microchip, vaccination status | `pets`, `pet_health_records`, `vaccinations` | `GET /api/pets/{id}`<br>`PUT /api/pets/{id}`<br>`GET /api/pets/{id}/health-summary` |
| **PhotoScanScreen** & **SymptomSelectionScreen** | Photo upload / symptom checklist | List of symptoms, scan image upload | `symptoms`, `health_scans` | `GET /api/health/symptoms`<br>`POST /api/health/scan` |
| **AnalyzingScreen** & **ResultScreen** | Analysis progress, match percentage, condition summary | Diagnostic evaluation results | `health_scans`, `disease_guides` | `GET /api/health/scans/{id}` |
| **DiseaseDetailScreen** | Condition info, causes, interim steps, vet alerts, "Save Report", "Find Vet" | Clinical condition data, saved reports, vet clinics directory | `disease_guides`, `saved_health_reports`, `veterinary_clinics` | `GET /api/health/conditions/{id}`<br>`POST /api/health/reports/save`<br>`GET /api/vets/nearby` |
| **DietHomeScreen** & **DietPlanScreen** | Calorie goal progress, daily meal schedule | Daily calorie metrics, active pet meal plan | `diet_plans`, `meal_schedules`, `meal_entries` | `GET /api/diet/summary`<br>`GET /api/diet/schedule` |
| **MealLogScreen** | Food item, portion size (cups), meal time | New meal entry payload | `meal_entries` | `POST /api/diet/meals`<br>`GET /api/diet/meals` |
| **SafeVsToxicFoodsScreen** | Searchable safe/toxic food directory | Foods catalog with safety indicators & warnings | `safe_toxic_foods` | `GET /api/diet/foods/safe-toxic` |
| **WeightTrackerScreen** | Weight entry field, weight history chart, current vs target weight | Historic weight log, target weight | `weight_entries`, `pets` | `GET /api/pets/{id}/weights`<br>`POST /api/pets/{id}/weights` |
| **CareTabScreen** & **GroomingHomeScreen** | Shortcuts to reminders, calendar, guides | Upcoming grooming stats | `care_reminders`, `grooming_schedules` | `GET /api/grooming/overview` |
| **AddReminderScreen** | Reminder title, date/time, push toggle, category | New care reminder payload | `care_reminders` | `POST /api/reminders` |
| **GroomingCalendarScreen** | Monthly scheduled tasks list | Chronological reminder & appointment list | `care_reminders`, `grooming_schedules` | `GET /api/reminders/calendar`<br>`PUT /api/reminders/{id}/toggle` |
| **GroomingGuideScreen** | Step-by-step tutorials | Care tutorials catalog | `grooming_guides` | `GET /api/grooming/guides` |

---

## 6. End-to-End Implementation & Transformation Architecture

### 6.1 Database Architecture (MySQL)
Normalized schema with foreign key constraints, indexes, and user ownership:
- `users`: Core account identity (`id`, `name`, `email`, `password_hash`, `created_at`, `updated_at`).
- `user_tokens`: Mobile API bearer tokens (`id`, `user_id`, `token_hash`, `expires_at`, `created_at`).
- `password_resets`: Password recovery tokens (`id`, `email`, `token_hash`, `expires_at`, `created_at`).
- `pets`: User pets (`id`, `user_id`, `name`, `species`, `breed`, `age_years`, `weight_kg`, `gender`, `microchip_id`, `health_score`, `vaccination_status`, `is_active`, `created_at`).
- `weight_entries`: Growth and weight tracking (`id`, `pet_id`, `user_id`, `weight_kg`, `recorded_date`, `notes`, `created_at`).
- `care_reminders`: Scheduled alerts (`id`, `pet_id`, `user_id`, `title`, `reminder_datetime`, `category`, `notify_push`, `is_completed`, `created_at`).
- `meal_entries`: Logged meals (`id`, `pet_id`, `user_id`, `food_item`, `portion_size`, `calories`, `meal_time`, `created_at`).
- `meal_schedules`: Standard feeding schedules (`id`, `pet_id`, `meal_name`, `scheduled_time`, `calories`, `description`).
- `safe_toxic_foods`: Safe/toxic foods catalog (`id`, `species`, `food_name`, `is_safe`, `description`, `symptoms`).
- `health_scans`: Symptom and diagnostic logs (`id`, `pet_id`, `user_id`, `symptoms_json`, `diagnosis_title`, `match_percentage`, `severity`, `summary`, `created_at`).
- `disease_guides`: Veterinary condition guide articles (`id`, `condition_name`, `species`, `severity`, `description`, `causes`, `care_steps_json`, `vet_warning`).
- `saved_health_reports`: User bookmarked clinical reports (`id`, `user_id`, `pet_id`, `report_title`, `report_json`, `saved_at`).
- `grooming_guides`: Grooming step-by-step tutorials (`id`, `title`, `steps`, `category`).

### 6.2 PHP 8+ Backend Architecture
A clean RESTful PHP backend structure located in `backend/`:
```
backend/
├── config/
│   └── database.php
├── database/
│   ├── schema.sql
│   ├── seed.sql
│   └── migrations/
├── src/
│   ├── Controllers/
│   │   ├── AuthController.php
│   │   ├── PetController.php
│   │   ├── DashboardController.php
│   │   ├── DietController.php
│   │   ├── HealthController.php
│   │   └── ReminderController.php
│   ├── Middleware/
│   │   ├── AuthMiddleware.php
│   │   └── CorsMiddleware.php
│   ├── Models/
│   ├── Repositories/
│   ├── Services/
│   └── Utils/
│       ├── Database.php
│       ├── Response.php
│       └── Validator.php
├── public/
│   └── index.php
├── .env.example
├── composer.json
└── README.md
```

### 6.3 Android MVVM & Data Layer Architecture
1. **Network Configuration**:
   - `Retrofit` + `OkHttp` logging interceptor.
   - `AuthInterceptor` adding `Authorization: Bearer <token>` to all authenticated requests.
   - `SessionManager` wrapping `SharedPreferences` to persist user token, active pet ID, and user profile across app restarts.
2. **DTO Layer**:
   - Type-safe request/response Kotlin data classes for all endpoints.
3. **Repository Layer**:
   - `AuthRepository`: Registration, login, logout, password reset, session restore.
   - `PetRepository`: Pet CRUD, active pet selection, weight logging, weight history.
   - `DashboardRepository`: Unified home dashboard fetching.
   - `DietRepository`: Daily calorie metrics, meal logging, meal schedule, safe/toxic foods.
   - `HealthRepository`: Symptom list, AI diagnostic scan submission, condition guide details, report saving.
   - `ReminderRepository`: Reminder creation, calendar schedule fetching, reminder completion toggle.
4. **ViewModel Layer**:
   - ViewModels exposing reactive `StateFlow<UiState<T>>` with `Loading`, `Success`, `Empty`, and `Error` states.
   - Clear UI event channels for one-time navigation or snackbar events.
5. **Compose UI Layer**:
   - Seamlessly connect existing Compose UI to ViewModels while preserving 100% of the existing color palette, Material 3 typography, layouts, animations, and icons.

