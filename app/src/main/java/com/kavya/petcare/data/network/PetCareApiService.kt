package com.kavya.petcare.data.network

import com.kavya.petcare.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface PetCareApiService {

    // ----------------------------------------------------
    // Authentication
    // ----------------------------------------------------
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthData>>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<AuthData>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<ApiResponse<Map<String, String>>>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<Map<String, Any>>>

    @PUT("auth/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<UserDto>>

    @PUT("auth/password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<ApiResponse<Unit>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @POST("auth/avatar")
    suspend fun uploadUserAvatar(
        @Body request: UploadPhotoRequest
    ): Response<ApiResponse<Map<String, Any>>>

    // ----------------------------------------------------
    // Dashboard
    // ----------------------------------------------------
    @GET("dashboard")
    suspend fun getDashboard(): Response<ApiResponse<DashboardData>>

    // ----------------------------------------------------
    // Pets Management
    // ----------------------------------------------------
    @GET("pets")
    suspend fun getPets(): Response<ApiResponse<List<PetDto>>>

    @POST("pets")
    suspend fun createPet(
        @Body request: CreatePetRequest
    ): Response<ApiResponse<PetDto>>

    @GET("pets/{id}")
    suspend fun getPetById(
        @Path("id") petId: Int
    ): Response<ApiResponse<PetDto>>

    @PUT("pets/{id}")
    suspend fun updatePet(
        @Path("id") petId: Int,
        @Body request: UpdatePetRequest
    ): Response<ApiResponse<PetDto>>

    @DELETE("pets/{id}")
    suspend fun deletePet(
        @Path("id") petId: Int
    ): Response<ApiResponse<Unit>>

    @POST("pets/{id}/photo")
    suspend fun uploadPetPhoto(
        @Path("id") petId: Int,
        @Body request: UploadPhotoRequest
    ): Response<ApiResponse<UploadPhotoDto>>

    @GET("pets/{id}/weights")
    suspend fun getPetWeights(
        @Path("id") petId: Int
    ): Response<ApiResponse<WeightOverviewDto>>

    @POST("pets/{id}/weights")
    suspend fun addPetWeight(
        @Path("id") petId: Int,
        @Body request: AddWeightRequest
    ): Response<ApiResponse<WeightEntryDto>>

    @PUT("pets/{id}/weights/{weightId}")
    suspend fun updatePetWeight(
        @Path("id") petId: Int,
        @Path("weightId") weightId: Int,
        @Body request: UpdateWeightRequest
    ): Response<ApiResponse<WeightEntryDto>>

    @DELETE("pets/{id}/weights/{weightId}")
    suspend fun deletePetWeight(
        @Path("id") petId: Int,
        @Path("weightId") weightId: Int
    ): Response<ApiResponse<Map<String, Any>>>

    @PUT("pets/{id}/target-weight")
    suspend fun updateTargetWeight(
        @Path("id") petId: Int,
        @Body request: UpdateTargetWeightRequest
    ): Response<ApiResponse<TargetWeightResponseDto>>

    // ----------------------------------------------------
    // Diet & Nutrition
    // ----------------------------------------------------
    @GET("diet/summary")
    suspend fun getDietSummary(): Response<ApiResponse<DietSummaryDto>>

    @POST("diet/analyze-food")
    suspend fun analyzeFood(
        @Body request: AnalyzeFoodRequest
    ): Response<ApiResponse<FoodAnalysisDto>>

    @GET("diet/schedule")
    suspend fun getMealSchedule(
        @Query("pet_id") petId: Int? = null
    ): Response<ApiResponse<List<MealScheduleDto>>>

    @POST("diet/schedule")
    suspend fun createMealSchedule(
        @Body request: CreateMealScheduleRequest
    ): Response<ApiResponse<MealScheduleDto>>

    @PUT("diet/schedule/{id}")
    suspend fun updateMealSchedule(
        @Path("id") id: Int,
        @Body request: UpdateMealScheduleRequest
    ): Response<ApiResponse<MealScheduleDto>>

    @DELETE("diet/schedule/{id}")
    suspend fun deleteMealSchedule(
        @Path("id") id: Int
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("diet/schedule/{id}/toggle-reminder")
    suspend fun toggleMealReminder(
        @Path("id") id: Int
    ): Response<ApiResponse<MealScheduleDto>>

    @GET("diet/meals")
    suspend fun getLoggedMeals(
        @Query("pet_id") petId: Int? = null,
        @Query("date") date: String? = null
    ): Response<ApiResponse<List<MealEntryDto>>>

    @POST("diet/meals")
    suspend fun logMeal(
        @Body request: LogMealRequest
    ): Response<ApiResponse<MealEntryDto>>

    @PUT("diet/meals/{id}")
    suspend fun updateLoggedMeal(
        @Path("id") id: Int,
        @Body request: UpdateMealRequest
    ): Response<ApiResponse<MealEntryDto>>

    @DELETE("diet/meals/{id}")
    suspend fun deleteLoggedMeal(
        @Path("id") id: Int
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("diet/foods/safe-toxic")
    suspend fun getSafeToxicFoods(): Response<ApiResponse<List<SafeToxicFoodDto>>>

    // ----------------------------------------------------
    // Care Reminders & Grooming
    // ----------------------------------------------------
    @GET("reminders")
    suspend fun getReminders(): Response<ApiResponse<List<CareReminderDto>>>

    @POST("reminders")
    suspend fun createReminder(
        @Body request: CreateReminderRequest
    ): Response<ApiResponse<CareReminderDto>>

    @PUT("reminders/{id}/toggle")
    suspend fun toggleReminder(
        @Path("id") id: Int
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("reminders/calendar")
    suspend fun getCalendarSchedule(): Response<ApiResponse<List<CalendarTaskDto>>>

    // Dynamic Grooming Reminders
    @GET("grooming/reminders")
    suspend fun getGroomingReminders(
        @Query("pet_id") petId: Int? = null,
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<GroomingReminderDto>>>

    @POST("grooming/reminders")
    suspend fun createGroomingReminder(
        @Body request: CreateGroomingReminderRequest
    ): Response<ApiResponse<GroomingReminderDto>>

    @GET("grooming/reminders/{id}")
    suspend fun getGroomingReminderById(
        @Path("id") id: Int
    ): Response<ApiResponse<GroomingReminderDto>>

    @PUT("grooming/reminders/{id}")
    suspend fun updateGroomingReminder(
        @Path("id") id: Int,
        @Body request: UpdateGroomingReminderRequest
    ): Response<ApiResponse<GroomingReminderDto>>

    @PATCH("grooming/reminders/{id}/status")
    suspend fun updateGroomingReminderStatus(
        @Path("id") id: Int,
        @Body request: UpdateGroomingStatusRequest
    ): Response<ApiResponse<GroomingReminderDto>>

    @DELETE("grooming/reminders/{id}")
    suspend fun deleteGroomingReminder(
        @Path("id") id: Int
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("grooming/guides")
    suspend fun getGroomingGuides(): Response<ApiResponse<List<GroomingGuideDto>>>

    // ----------------------------------------------------
    // Health & AI Diagnostics
    // ----------------------------------------------------
    @GET("health/symptoms")
    suspend fun getSymptoms(): Response<ApiResponse<List<String>>>

    @POST("health/scan")
    suspend fun runHealthScan(
        @Body request: HealthScanRequest
    ): Response<ApiResponse<HealthScanResultDto>>

    @GET("health/conditions")
    suspend fun getConditions(): Response<ApiResponse<List<ConditionGuideDto>>>

    @GET("health/conditions/{id}")
    suspend fun getConditionDetail(
        @Path("id") id: Int
    ): Response<ApiResponse<ConditionGuideDto>>

    @POST("health/reports/save")
    suspend fun saveHealthReport(
        @Body request: SaveReportRequest
    ): Response<ApiResponse<Map<String, Any>>>

    // ----------------------------------------------------
    // Dynamic Pet Health Score & Health Records
    // ----------------------------------------------------
    @GET("pets/{id}/health-score")
    suspend fun getPetHealthScore(
        @Path("id") petId: Int
    ): Response<ApiResponse<PetHealthScoreOverviewDto>>

    @GET("pets/{id}/health-records")
    suspend fun getPetHealthRecords(
        @Path("id") petId: Int
    ): Response<ApiResponse<List<PetHealthRecordDto>>>

    @POST("pets/{id}/health-records")
    suspend fun addPetHealthRecord(
        @Path("id") petId: Int,
        @Body request: CreateHealthRecordRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @PUT("pets/{id}/health-records/{recordId}")
    suspend fun updatePetHealthRecord(
        @Path("id") petId: Int,
        @Path("recordId") recordId: Int,
        @Body request: CreateHealthRecordRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @DELETE("pets/{id}/health-records/{recordId}")
    suspend fun deletePetHealthRecord(
        @Path("id") petId: Int,
        @Path("recordId") recordId: Int
    ): Response<ApiResponse<Map<String, Any>>>

    // ----------------------------------------------------
    // Dynamic Pet Vaccinations
    // ----------------------------------------------------
    @GET("pets/{id}/vaccinations")
    suspend fun getPetVaccinations(
        @Path("id") petId: Int
    ): Response<ApiResponse<VaccinationOverviewDto>>

    @POST("pets/{id}/vaccinations")
    suspend fun addPetVaccination(
        @Path("id") petId: Int,
        @Body request: CreateVaccinationRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @PUT("pets/{id}/vaccinations/{vaccineId}")
    suspend fun updatePetVaccination(
        @Path("id") petId: Int,
        @Path("vaccineId") vaccineId: Int,
        @Body request: CreateVaccinationRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @DELETE("pets/{id}/vaccinations/{vaccineId}")
    suspend fun deletePetVaccination(
        @Path("id") petId: Int,
        @Path("vaccineId") vaccineId: Int
    ): Response<ApiResponse<Map<String, Any>>>

    // ----------------------------------------------------
    // User Preferences & Support
    // ----------------------------------------------------
    @GET("user/preferences")
    suspend fun getPreferences(): Response<ApiResponse<PreferencesDto>>

    @PUT("user/preferences")
    suspend fun updatePreferences(
        @Body request: UpdatePreferencesRequest
    ): Response<ApiResponse<PreferencesDto>>

    @POST("support/report")
    suspend fun submitSupportReport(
        @Body request: SupportReportRequest
    ): Response<ApiResponse<SupportTicketDto>>
}

