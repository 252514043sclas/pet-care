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

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

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
        @Body request: Map<String, Any>
    ): Response<ApiResponse<PetDto>>

    @DELETE("pets/{id}")
    suspend fun deletePet(
        @Path("id") petId: Int
    ): Response<ApiResponse<Unit>>

    @GET("pets/{id}/weights")
    suspend fun getPetWeights(
        @Path("id") petId: Int
    ): Response<ApiResponse<WeightOverviewDto>>

    @POST("pets/{id}/weights")
    suspend fun addPetWeight(
        @Path("id") petId: Int,
        @Body request: AddWeightRequest
    ): Response<ApiResponse<WeightEntryDto>>

    // ----------------------------------------------------
    // Diet & Nutrition
    // ----------------------------------------------------
    @GET("diet/summary")
    suspend fun getDietSummary(): Response<ApiResponse<DietSummaryDto>>

    @GET("diet/schedule")
    suspend fun getMealSchedule(): Response<ApiResponse<List<MealScheduleDto>>>

    @GET("diet/meals")
    suspend fun getLoggedMeals(): Response<ApiResponse<List<MealEntryDto>>>

    @POST("diet/meals")
    suspend fun logMeal(
        @Body request: LogMealRequest
    ): Response<ApiResponse<MealEntryDto>>

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
}

