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
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("is_active") val isActive: Int = 1
)

data class CreatePetRequest(
    @SerializedName("name") val name: String,
    @SerializedName("species") val species: String,
    @SerializedName("breed") val breed: String,
    @SerializedName("age_years") val ageYears: String,
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("gender") val gender: String = "Female",
    @SerializedName("microchip_id") val microchipId: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_base64") val imageBase64: String? = null
)

data class UpdatePetRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("species") val species: String? = null,
    @SerializedName("breed") val breed: String? = null,
    @SerializedName("age_years") val ageYears: String? = null,
    @SerializedName("weight_kg") val weightKg: Double? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("microchip_id") val microchipId: String? = null,
    @SerializedName("health_score") val healthScore: Int? = null,
    @SerializedName("vaccination_status") val vaccinationStatus: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_base64") val imageBase64: String? = null,
    @SerializedName("is_active") val isActive: Int? = null
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
    @SerializedName("pet_id") val petId: Int = 0,
    @SerializedName("meal_name") val mealName: String,
    @SerializedName("scheduled_time") val scheduledTime: String,
    @SerializedName("food_name") val foodName: String = "",
    @SerializedName("food_quantity") val foodQuantity: String = "",
    @SerializedName("meal_description") val mealDescription: String = "",
    @SerializedName("instructions") val instructions: String = "",
    @SerializedName("calories") val calories: Int = 0,
    @SerializedName("is_reminder_enabled") val isReminderEnabled: Int = 1
)

data class CreateMealScheduleRequest(
    @SerializedName("pet_id") val petId: Int? = null,
    @SerializedName("meal_name") val mealName: String,
    @SerializedName("scheduled_time") val scheduledTime: String,
    @SerializedName("food_name") val foodName: String? = null,
    @SerializedName("food_quantity") val foodQuantity: String? = null,
    @SerializedName("meal_description") val mealDescription: String? = null,
    @SerializedName("instructions") val instructions: String? = null,
    @SerializedName("calories") val calories: Int = 0,
    @SerializedName("is_reminder_enabled") val isReminderEnabled: Int = 1
)

data class UpdateMealScheduleRequest(
    @SerializedName("meal_name") val mealName: String? = null,
    @SerializedName("scheduled_time") val scheduledTime: String? = null,
    @SerializedName("food_name") val foodName: String? = null,
    @SerializedName("food_quantity") val foodQuantity: String? = null,
    @SerializedName("meal_description") val mealDescription: String? = null,
    @SerializedName("instructions") val instructions: String? = null,
    @SerializedName("calories") val calories: Int? = null,
    @SerializedName("is_reminder_enabled") val isReminderEnabled: Int? = null
)

data class MealEntryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("pet_id") val petId: Int = 0,
    @SerializedName("pet_name") val petName: String = "",
    @SerializedName("pet_species") val petSpecies: String = "",
    @SerializedName("food_item") val foodItem: String,
    @SerializedName("portion_size") val portionSize: String = "",
    @SerializedName("portion_quantity") val portionQuantity: Double = 1.0,
    @SerializedName("portion_unit") val portionUnit: String = "g",
    @SerializedName("calories") val calories: Int? = null,
    @SerializedName("safety_status") val safetyStatus: String = "UNKNOWN",
    @SerializedName("safety_warning") val safetyWarning: String? = null,
    @SerializedName("meal_date") val mealDate: String = "",
    @SerializedName("meal_time") val mealTime: String = "",
    @SerializedName("created_at") val createdAt: String? = null
)

data class LogMealRequest(
    @SerializedName("pet_id") val petId: Int? = null,
    @SerializedName("pet_name") val petName: String? = null,
    @SerializedName("pet_species") val petSpecies: String? = null,
    @SerializedName("food_item") val foodItem: String,
    @SerializedName("portion_size") val portionSize: String,
    @SerializedName("portion_quantity") val portionQuantity: Double,
    @SerializedName("portion_unit") val portionUnit: String,
    @SerializedName("calories") val calories: Int? = null,
    @SerializedName("safety_status") val safetyStatus: String? = null,
    @SerializedName("safety_warning") val safetyWarning: String? = null,
    @SerializedName("meal_date") val mealDate: String? = null,
    @SerializedName("meal_time") val mealTime: String? = null
)

data class UpdateMealRequest(
    @SerializedName("food_item") val foodItem: String,
    @SerializedName("portion_size") val portionSize: String,
    @SerializedName("portion_quantity") val portionQuantity: Double,
    @SerializedName("portion_unit") val portionUnit: String,
    @SerializedName("pet_species") val petSpecies: String? = null,
    @SerializedName("calories") val calories: Int? = null,
    @SerializedName("safety_status") val safetyStatus: String? = null,
    @SerializedName("safety_warning") val safetyWarning: String? = null,
    @SerializedName("meal_date") val mealDate: String? = null,
    @SerializedName("meal_time") val mealTime: String? = null
)

data class AnalyzeFoodRequest(
    @SerializedName("food_name") val foodName: String,
    @SerializedName("portion_quantity") val portionQuantity: Double,
    @SerializedName("portion_unit") val portionUnit: String,
    @SerializedName("species") val species: String? = null,
    @SerializedName("pet_id") val petId: Int? = null
)

data class FoodAnalysisDto(
    @SerializedName("food_name") val foodName: String = "",
    @SerializedName("species") val species: String = "",
    @SerializedName("species_category") val speciesCategory: String = "other",
    @SerializedName("portion_quantity") val portionQuantity: Double = 1.0,
    @SerializedName("portion_unit") val portionUnit: String = "g",
    @SerializedName("estimated_calories") val estimatedCalories: Int? = null,
    @SerializedName("calories_available") val caloriesAvailable: Boolean = false,
    @SerializedName("calorie_source") val calorieSource: String = "",
    @SerializedName("grams_estimated") val gramsEstimated: Double? = null,
    @SerializedName("safety_status") val safetyStatus: String = "UNKNOWN",
    @SerializedName("safety_warning") val safetyWarning: String = ""
)

data class SafeToxicFoodDto(
    @SerializedName("id") val id: Int,
    @SerializedName("food_name") val foodName: String,
    @SerializedName("is_safe") val isSafe: Int,
    @SerializedName("description") val description: String
)

// Weight Tracker
data class WeightOverviewDto(
    @SerializedName("current_weight") val currentWeight: Double? = null,
    @SerializedName("target_weight") val targetWeight: Double? = null,
    @SerializedName("status") val status: String = "No Records",
    @SerializedName("history") val history: List<WeightEntryDto> = emptyList()
)

data class WeightEntryDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("pet_id") val petId: Int = 0,
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("recorded_date") val recordedDate: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AddWeightRequest(
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("recorded_date") val recordedDate: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class UpdateWeightRequest(
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("recorded_date") val recordedDate: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class UpdateTargetWeightRequest(
    @SerializedName("target_weight_kg") val targetWeightKg: Double?
)

data class TargetWeightResponseDto(
    @SerializedName("target_weight") val targetWeight: Double? = null
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

// Dynamic Grooming Calendar Reminders
data class GroomingReminderDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("user_id") val userId: Int = 0,
    @SerializedName("pet_id") val petId: Int = 0,
    @SerializedName("pet_name") val petName: String = "Pet",
    @SerializedName("pet_species") val petSpecies: String = "Dog",
    @SerializedName("activity") val activity: String = "",
    @SerializedName("reminder_date") val reminderDate: String = "",
    @SerializedName("reminder_time") val reminderTime: String = "",
    @SerializedName("notification_enabled") val notificationEnabled: Boolean = true,
    @SerializedName("status") val status: String = "Scheduled", // "Scheduled" or "Completed"
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    val isCompleted: Boolean
        get() = status.equals("Completed", ignoreCase = true)
}

data class CreateGroomingReminderRequest(
    @SerializedName("pet_id") val petId: Int,
    @SerializedName("activity") val activity: String,
    @SerializedName("reminder_date") val reminderDate: String,
    @SerializedName("reminder_time") val reminderTime: String,
    @SerializedName("notification_enabled") val notificationEnabled: Boolean = true,
    @SerializedName("status") val status: String = "Scheduled"
)

data class UpdateGroomingReminderRequest(
    @SerializedName("pet_id") val petId: Int,
    @SerializedName("activity") val activity: String,
    @SerializedName("reminder_date") val reminderDate: String,
    @SerializedName("reminder_time") val reminderTime: String,
    @SerializedName("notification_enabled") val notificationEnabled: Boolean = true,
    @SerializedName("status") val status: String? = null
)

data class UpdateGroomingStatusRequest(
    @SerializedName("status") val status: String
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

// User Preferences DTO
data class PreferencesDto(
    @SerializedName("notif_vaccination") val notifVaccination: Boolean = true,
    @SerializedName("notif_medication") val notifMedication: Boolean = true,
    @SerializedName("notif_appointments") val notifAppointments: Boolean = true,
    @SerializedName("theme_mode") val themeMode: String = "system",
    @SerializedName("font_size") val fontSize: String = "normal"
)

data class UpdatePreferencesRequest(
    @SerializedName("notif_vaccination") val notifVaccination: Boolean? = null,
    @SerializedName("notif_medication") val notifMedication: Boolean? = null,
    @SerializedName("notif_appointments") val notifAppointments: Boolean? = null,
    @SerializedName("theme_mode") val themeMode: String? = null,
    @SerializedName("font_size") val fontSize: String? = null
)

data class UpdateProfileRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class UploadPhotoRequest(
    @SerializedName("image_base64") val imageBase64: String
)

data class SupportReportRequest(
    @SerializedName("category") val category: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("description") val description: String
)

// Support Ticket DTO
data class SupportTicketDto(
    @SerializedName("ticket_id") val ticketId: Int,
    @SerializedName("subject") val subject: String,
    @SerializedName("status") val status: String
)

// Pet Photo Upload Response DTO
data class UploadPhotoDto(
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("pet") val pet: PetDto? = null
)

// ----------------------------------------------------
// Pet Health Records & Dynamic Wellness Scoring
// ----------------------------------------------------
data class PetHealthRecordDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("pet_id") val petId: Int = 0,
    @SerializedName("record_type") val recordType: String = "Checkup", // Checkup, Symptom, Vet Visit, Medication, Surgery, General
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("severity") val severity: String = "Normal", // Normal, Mild, Moderate, Severe
    @SerializedName("recorded_date") val recordedDate: String = "",
    @SerializedName("vet_name") val vetName: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class CreateHealthRecordRequest(
    @SerializedName("record_type") val recordType: String = "Checkup",
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("severity") val severity: String = "Normal",
    @SerializedName("recorded_date") val recordedDate: String,
    @SerializedName("vet_name") val vetName: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class HealthScoreFactorDto(
    @SerializedName("factor") val factor: String = "",
    @SerializedName("impact") val impact: Int = 0,
    @SerializedName("description") val description: String = ""
)

data class PetHealthScoreOverviewDto(
    @SerializedName("score") val score: Int? = null,
    @SerializedName("has_sufficient_data") val hasSufficientData: Boolean = false,
    @SerializedName("status_label") val statusLabel: String = "Not enough health data",
    @SerializedName("total_records") val totalRecords: Int = 0,
    @SerializedName("breakdown") val breakdown: List<HealthScoreFactorDto> = emptyList(),
    @SerializedName("health_records") val healthRecords: List<PetHealthRecordDto> = emptyList(),
    @SerializedName("recent_scans") val recentScans: List<HealthScanResultDto> = emptyList()
)

// ----------------------------------------------------
// Pet Vaccinations & Status
// ----------------------------------------------------
data class PetVaccinationDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("pet_id") val petId: Int = 0,
    @SerializedName("vaccine_name") val vaccineName: String = "",
    @SerializedName("vaccination_date") val vaccinationDate: String = "",
    @SerializedName("next_due_date") val nextDueDate: String = "",
    @SerializedName("batch_number") val batchNumber: String? = null,
    @SerializedName("clinic_name") val clinicName: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("status") val status: String = "UP TO DATE", // UP TO DATE, DUE SOON, OVERDUE
    @SerializedName("days_until_due") val daysUntilDue: Int = 0,
    @SerializedName("due_description") val dueDescription: String = "",
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class CreateVaccinationRequest(
    @SerializedName("vaccine_name") val vaccineName: String,
    @SerializedName("vaccination_date") val vaccinationDate: String,
    @SerializedName("next_due_date") val nextDueDate: String,
    @SerializedName("batch_number") val batchNumber: String? = null,
    @SerializedName("clinic_name") val clinicName: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class VaccinationOverviewDto(
    @SerializedName("overall_status") val overallStatus: String = "NO RECORDS",
    @SerializedName("total_vaccines") val totalVaccines: Int = 0,
    @SerializedName("vaccinations") val vaccinations: List<PetVaccinationDto> = emptyList()
)

// ----------------------------------------------------
// AI Pet Care Chatbot & Multimodal Vision (Qwen3-VL 2B)
// ----------------------------------------------------
data class ChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("pet_id") val petId: Int? = null,
    @SerializedName("enable_search") val enableSearch: Boolean = true
)

data class SearchSourceDto(
    @SerializedName("title") val title: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("snippet") val snippet: String = "",
    @SerializedName("source") val source: String = "",
    @SerializedName("date") val date: String? = null
)

data class VisionMetadataDto(
    @SerializedName("pet_type") val petType: String? = null,
    @SerializedName("observations") val observations: String? = null,
    @SerializedName("possible_conditions") val possibleConditions: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("confidence") val confidence: String? = null,
    @SerializedName("recommended_care") val recommendedCare: String? = null,
    @SerializedName("vet_recommendation") val vetRecommendation: String? = null
)

data class ChatResponseDto(
    @SerializedName("message") val message: String,
    @SerializedName("message_id") val messageId: Int? = null,
    @SerializedName("user_message_id") val userMessageId: Int? = null,
    @SerializedName("sender") val sender: String = "assistant",
    @SerializedName("sources") val sources: List<SearchSourceDto> = emptyList(),
    @SerializedName("model") val model: String? = null,
    @SerializedName("elapsed_seconds") val elapsedSeconds: Double? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class ChatImageRequest(
    @SerializedName("image") val imageBase64: String,
    @SerializedName("prompt") val prompt: String = "",
    @SerializedName("pet_id") val petId: Int? = null
)

data class ChatImageResponseDto(
    @SerializedName("message") val message: String,
    @SerializedName("message_id") val messageId: Int? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("metadata") val metadata: VisionMetadataDto? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("elapsed_seconds") val elapsedSeconds: Double? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class ChatMessageDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("user_id") val userId: Int = 0,
    @SerializedName("pet_id") val petId: Int? = null,
    @SerializedName("sender") val sender: String = "user", // "user" or "assistant"
    @SerializedName("message") val message: String = "",
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("message_type") val messageType: String = "text",
    @SerializedName("sources") val sources: List<SearchSourceDto> = emptyList(),
    @SerializedName("analysis_metadata") val analysisMetadata: VisionMetadataDto? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class NutritionRequest(
    @SerializedName("query") val query: String,
    @SerializedName("pet_id") val petId: Int? = null
)

data class NutritionResponseDto(
    @SerializedName("message") val message: String,
    @SerializedName("message_id") val messageId: Int? = null,
    @SerializedName("user_message_id") val userMessageId: Int? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class SearchRequest(
    @SerializedName("query") val query: String,
    @SerializedName("max_results") val maxResults: Int = 4
)

data class SearchResponseDto(
    @SerializedName("query") val query: String,
    @SerializedName("total_results") val totalResults: Int = 0,
    @SerializedName("results") val results: List<SearchSourceDto> = emptyList(),
    @SerializedName("trusted_sources_included") val trustedSourcesIncluded: Boolean = true
)

data class OllamaHealthDto(
    @SerializedName("status") val status: String = "offline",
    @SerializedName("message") val message: String = "",
    @SerializedName("model_loaded") val modelLoaded: Boolean = false,
    @SerializedName("model") val model: String = "qwen3-vl:2b",
    @SerializedName("available_models") val availableModels: List<String> = emptyList()
)

data class SystemHealthDto(
    @SerializedName("status") val status: String = "unknown",
    @SerializedName("database") val database: String = "unknown",
    @SerializedName("ai_service") val aiService: OllamaHealthDto? = null,
    @SerializedName("search_provider") val searchProvider: String? = null,
    @SerializedName("server_time") val serverTime: String? = null
)


