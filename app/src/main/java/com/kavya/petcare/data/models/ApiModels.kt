package com.kavya.petcare.data.models

import com.google.gson.annotations.SerializedName

// Generic API Response Wrapper
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

// User DTO
data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

// Auth Data
data class AuthData(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto,
    @SerializedName("active_pet") val activePet: PetDto? = null
)

// Auth Requests
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_name") val deviceName: String = "Android Device"
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_name") val deviceName: String = "Android Device"
)

data class ForgotPasswordRequest(
    @SerializedName("email") val email: String
)

// Pet DTO
data class PetDto(
    @SerializedName("id") val id: Int = 1,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("name") val name: String = "Clover",
    @SerializedName("species") val species: String = "Rabbit",
    @SerializedName("breed") val breed: String = "Holland Lop",
    @SerializedName("age_years") val ageYears: String = "1.2 yrs",
    @SerializedName("weight_kg") val weightKg: Double = 1.80,
    @SerializedName("gender") val gender: String = "Female",
    @SerializedName("microchip_id") val microchipId: String? = "#985142091",
    @SerializedName("health_score") val healthScore: Int = 95,
    @SerializedName("vaccination_status") val vaccinationStatus: String = "Up to Date",
    @SerializedName("is_active") val isActive: Int = 1
)

data class CreatePetRequest(
    @SerializedName("name") val name: String,
    @SerializedName("species") val species: String,
    @SerializedName("breed") val breed: String,
    @SerializedName("age_years") val ageYears: String,
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("gender") val gender: String = "Female",
    @SerializedName("microchip_id") val microchipId: String? = null
)

// Dashboard Data
data class DashboardData(
    @SerializedName("user") val user: UserDto?,
    @SerializedName("active_pet") val activePet: PetDto?,
    @SerializedName("today_schedule") val todaySchedule: List<TodayScheduleItemDto> = emptyList(),
    @SerializedName("diet_summary") val dietSummary: DietSummaryDto?,
    @SerializedName("reminders_count") val remindersCount: Int = 0,
    @SerializedName("meals_count") val mealsCount: Int = 0
)

data class TodayScheduleItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("time") val time: String,
    @SerializedName("type") val type: String = "reminder",
    @SerializedName("is_completed") val isCompleted: Boolean = false
)

// Diet Data
data class DietSummaryDto(
    @SerializedName("consumed_calories") val consumedCalories: Int = 650,
    @SerializedName("target_calories") val targetCalories: Int = 800,
    @SerializedName("progress_ratio") val progressRatio: Float = 0.81f,
    @SerializedName("label") val label: String? = null
)

data class MealScheduleDto(
    @SerializedName("id") val id: Int,
    @SerializedName("meal_name") val mealName: String,
    @SerializedName("scheduled_time") val scheduledTime: String,
    @SerializedName("meal_description") val mealDescription: String,
    @SerializedName("calories") val calories: Int
)

data class MealEntryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("pet_id") val petId: Int,
    @SerializedName("food_item") val foodItem: String,
    @SerializedName("portion_size") val portionSize: String,
    @SerializedName("calories") val calories: Int,
    @SerializedName("meal_time") val mealTime: String
)

data class LogMealRequest(
    @SerializedName("food_item") val foodItem: String,
    @SerializedName("portion_size") val portionSize: String,
    @SerializedName("calories") val calories: Int? = null,
    @SerializedName("meal_time") val mealTime: String? = null
)

data class SafeToxicFoodDto(
    @SerializedName("id") val id: Int,
    @SerializedName("food_name") val foodName: String,
    @SerializedName("is_safe") val isSafe: Int,
    @SerializedName("description") val description: String
)

// Weight Tracker
data class WeightOverviewDto(
    @SerializedName("current_weight") val currentWeight: Double = 1.80,
    @SerializedName("target_weight") val targetWeight: Double = 1.70,
    @SerializedName("status") val status: String = "Optimal",
    @SerializedName("history") val history: List<WeightEntryDto> = emptyList()
)

data class WeightEntryDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("recorded_date") val recordedDate: String,
    @SerializedName("notes") val notes: String? = null
)

data class AddWeightRequest(
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("recorded_date") val recordedDate: String? = null,
    @SerializedName("notes") val notes: String? = null
)

// Reminders & Grooming
data class CareReminderDto(
    @SerializedName("id") val id: Int,
    @SerializedName("pet_id") val petId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("reminder_time") val reminderTime: String,
    @SerializedName("category") val category: String = "Grooming",
    @SerializedName("notify_push") val notifyPush: Int = 1,
    @SerializedName("is_completed") val isCompleted: Int = 0
)

data class CreateReminderRequest(
    @SerializedName("title") val title: String,
    @SerializedName("reminder_time") val reminderTime: String,
    @SerializedName("category") val category: String = "Grooming",
    @SerializedName("notify_push") val notifyPush: Int = 1
)

data class CalendarTaskDto(
    @SerializedName("id") val id: Int,
    @SerializedName("date_time") val dateTime: String,
    @SerializedName("task") val task: String,
    @SerializedName("is_completed") val isCompleted: Boolean = false
)

data class GroomingGuideDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("steps") val steps: String
)

// Health & Diagnostics
data class HealthScanResultDto(
    @SerializedName("scan_id") val scanId: Int,
    @SerializedName("condition_id") val conditionId: Int = 1,
    @SerializedName("diagnosis_title") val diagnosisTitle: String,
    @SerializedName("match_percentage") val matchPercentage: Int,
    @SerializedName("severity") val severity: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("recommended_action") val recommendedAction: String? = null
)

data class HealthScanRequest(
    @SerializedName("scan_type") val scanType: String = "symptoms",
    @SerializedName("symptoms") val symptoms: List<String> = emptyList()
)

data class ConditionGuideDto(
    @SerializedName("id") val id: Int = 1,
    @SerializedName("condition_name") val conditionName: String,
    @SerializedName("species") val species: String = "Rabbit",
    @SerializedName("severity") val severity: String = "Moderate",
    @SerializedName("badge") val badge: String = "Example only · Vet exam needed",
    @SerializedName("description") val description: String,
    @SerializedName("causes") val causes: String,
    @SerializedName("interim_steps") val interimSteps: List<String>? = emptyList(),
    @SerializedName("vet_warning") val vetWarning: String
)

data class SaveReportRequest(
    @SerializedName("report_title") val reportTitle: String,
    @SerializedName("report_data") val reportData: Map<String, Any>? = null
)

