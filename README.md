# 🐾 Pet Care — Modern Android App

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-green.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![Platform](https://img.shields.io/badge/Platform-Android%20%28Min%20SDK%2024%29-brightgreen.svg?logo=android)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

**Pet Care** is a modern, intelligent, feature-rich Android mobile application built with **Kotlin**, **Jetpack Compose**, and **Material 3**. Designed with a deep emerald and sage-mint visual theme, the app offers pet owners a seamless, beautiful experience for monitoring pet health, diet, grooming schedules, and weight growth.

---

## ✨ Features & Highlights

- **📱 23 Interactive Screen Architecture**:
  - **Onboarding & Auth**: Splash screen, interactive intro carousel, Login with **Forgot Password** recovery flow, Sign Up, Pet Type selection, and Pet Profile creation.
  - **Home Dashboard**: Modern dashboard with active pet status card, quick care action cards, today's schedule, and sliding bottom navigation bar.
  - **AI Health & Diagnostics**: Camera photo scan mockup, multi-symptom selection checklist, AI scanning animation, diagnostic match confidence results, and detailed **Condition Guides** (causes, interim care steps, vet examination warnings, Save Report, and Find Vet).
  - **Diet & Nutrition**: Daily calorie tracker, meal schedules (Breakfast, Lunch, Dinner), meal logger, Safe vs. Toxic foods reference guide, and weight growth tracker.
  - **Grooming & Reminders**: Scheduled grooming reminders, monthly care calendar, and step-by-step grooming tutorials.
  - **Pet Profile**: Complete pet medical stats, health score (95/100), and weight history.

- **🎨 Modern Design System**:
  - Deep Emerald (`#0F4C3A`), Sage Mint (`#48A9A6`), and Warm Cream (`#F6F8F3`) color palette.
  - Asymmetric rounded cards (`RoundedCornerShape(20.dp)`), status badges, and official **Material 3 Vector Icons** (100% emoji-free UI).

- **🔄 Smart Sliding Navigation & Back Press Handling**:
  - **Horizontal Pager**: Smooth sliding transitions when swiping or tapping tabs.
  - **Custom BackHandler**: Pressing back on non-home tabs smoothly returns to Home.
  - **Exit Confirmation**: Custom modern alert dialog (*"Do you want to exit Pet Care?"*) on Home back press.
  - **Explicit Top App Bar Back Arrows**: Seamless back navigation on all detail/sub-screens.

- **🏗️ Architecture**:
  - **MVVM Pattern**: StateFlow-backed reactive `PetViewModel` for state management and data persistence.
  - **Gradle Version Catalog**: `libs.versions.toml` setup with Compose BOM.

---

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9 / 2.0
- **UI Framework**: Jetpack Compose with Material 3 (`androidx.compose.material3`)
- **Architecture**: Model-View-ViewModel (MVVM)
- **State Management**: Kotlin StateFlow & MutableState
- **Build System**: Gradle 8.7 / AGP 8.5 with Version Catalogs (`libs.versions.toml`)
- **Min SDK**: Android 7.0 (API Level 24)
- **Target SDK**: Android 15 (API Level 35)

---

## 📁 Project Structure

```
PetCare/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/kavya/petcare/
│   │   │   │   ├── SplashActivity.kt
│   │   │   │   ├── OnboardingActivity.kt
│   │   │   │   ├── LoginActivity.kt
│   │   │   │   ├── SignUpActivity.kt
│   │   │   │   ├── SelectPetTypeActivity.kt
│   │   │   │   ├── AddPetProfileActivity.kt
│   │   │   │   ├── HomeDashboardActivity.kt
│   │   │   │   ├── PetDetailProfileActivity.kt
│   │   │   │   ├── HealthHomeActivity.kt
│   │   │   │   ├── PhotoScanActivity.kt
│   │   │   │   ├── SymptomSelectionActivity.kt
│   │   │   │   ├── AnalyzingActivity.kt
│   │   │   │   ├── ResultActivity.kt
│   │   │   │   ├── DiseaseDetailActivity.kt
│   │   │   │   ├── DietHomeActivity.kt
│   │   │   │   ├── DietPlanActivity.kt
│   │   │   │   ├── MealLogActivity.kt
│   │   │   │   ├── SafeVsToxicFoodsActivity.kt
│   │   │   │   ├── WeightTrackerActivity.kt
│   │   │   │   ├── GroomingHomeActivity.kt
│   │   │   │   ├── AddReminderActivity.kt
│   │   │   │   ├── GroomingCalendarActivity.kt
│   │   │   │   ├── GroomingGuideActivity.kt
│   │   │   │   ├── PetViewModel.kt
│   │   │   │   └── ui/
│   │   │   │       ├── PetDesignSystem.kt
│   │   │   │       └── theme/Theme.kt
│   │   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or Android Studio Meerkat
- JDK 17 or JDK 21 configured under **File > Settings > Build Tools > Gradle > Gradle JDK**

### Installation
1. **Clone the repository**:
   ```bash
   git clone https://github.com/252514043sclas/pet-care.git
   cd pet-care
   ```
2. **Open in Android Studio**.
3. **Sync Gradle**: Click **Sync Project with Gradle Files**.
4. **Run**: Select your connected physical device or Android Emulator and press `Shift + F10` or click the green **Run** button.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
