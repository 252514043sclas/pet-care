package com.kavya.petcare.data.repository

import android.content.Context
import com.kavya.petcare.data.models.*
import com.kavya.petcare.data.network.NetworkConfig
import com.kavya.petcare.data.network.PetCareApiService
import com.kavya.petcare.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PetCareRepository(private val context: Context) {

    private val api: PetCareApiService get() = NetworkConfig.getApiService(context)
    private val session = SessionManager.getInstance(context)

    private fun <T> parseErrorMessage(response: retrofit2.Response<T>, defaultMsg: String): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                if (json.has("message")) json.get("message").asString else defaultMsg
            } else {
                defaultMsg
            }
        } catch (_: Exception) {
            defaultMsg
        }
    }

    private fun formatNetworkError(e: Throwable): String {
        val currentUrl = NetworkConfig.getCurrentBaseUrl(context)
        return when (e) {
            is java.net.ConnectException -> "Cannot connect to server at $currentUrl. Please ensure XAMPP Apache is running on port 80."
            is java.net.SocketTimeoutException -> "Connection timed out connecting to $currentUrl. Please check server responsiveness."
            is java.net.UnknownHostException -> "Unknown host at $currentUrl. Please verify your network and server URL."
            else -> "Network error: ${e.localizedMessage ?: "Could not connect to server"}"
        }
    }

    // ----------------------------------------------------
    // Authentication
    // ----------------------------------------------------
    suspend fun login(email: String, pass: String): Result<AuthData> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(LoginRequest(email = email.trim(), password = pass))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                session.saveAuthSession(
                    token = authData.token,
                    userId = authData.user.id,
                    userName = authData.user.name,
                    email = authData.user.email,
                    activePetId = authData.activePet?.id
                )
                if (!authData.user.avatarUrl.isNullOrBlank()) {
                    session.setUserAvatarUrl(authData.user.avatarUrl)
                }
                authData.activePet?.let { p ->
                    session.saveActivePetDetails(
                        id = p.id,
                        name = p.name,
                        type = p.species,
                        breed = p.breed,
                        age = p.ageYears,
                        weight = "${p.weightKg} kg",
                        imageUrl = p.imageUrl,
                        gender = p.gender,
                        microchipId = p.microchipId,
                        healthScore = p.healthScore,
                        vaccinationStatus = p.vaccinationStatus
                    )
                }
                Result.success(authData)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Login failed. Please verify credentials.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun register(name: String, email: String, pass: String): Result<AuthData> = withContext(Dispatchers.IO) {
        try {
            val response = api.register(RegisterRequest(name = name.trim(), email = email.trim(), password = pass))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                session.saveAuthSession(
                    token = authData.token,
                    userId = authData.user.id,
                    userName = authData.user.name,
                    email = authData.user.email
                )
                Result.success(authData)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Registration failed.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun forgotPassword(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.forgotPassword(ForgotPasswordRequest(email = email.trim()))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.message ?: "Password reset link sent.")
            } else {
                Result.failure(Exception(response.body()?.message ?: "Could not process request"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun validateSession(): Boolean = withContext(Dispatchers.IO) {
        val token = session.getToken()
        if (token.isNullOrBlank()) {
            return@withContext false
        }

        try {
            val response = api.getCurrentUser()
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                val userMap = data?.get("user") as? Map<*, *>
                if (userMap != null) {
                    val name = userMap["name"] as? String
                    val email = userMap["email"] as? String
                    val id = (userMap["id"] as? Number)?.toInt()
                    val avatarUrl = userMap["avatar_url"] as? String
                    session.updateUserInfo(
                        userId = id,
                        userName = name,
                        email = email,
                        avatarUrl = avatarUrl
                    )
                }

                val petsList = data?.get("pets") as? List<Map<String, Any?>>
                if (!petsList.isNullOrEmpty()) {
                    val activePetMap = petsList.find { (it["is_active"] as? Number)?.toInt() == 1 }
                        ?: petsList.find { (it["id"] as? Number)?.toInt() == session.getActivePetId() }
                        ?: petsList.firstOrNull()

                    if (activePetMap != null) {
                        val petId = (activePetMap["id"] as? Number)?.toInt() ?: session.getActivePetId()
                        val petName = activePetMap["name"] as? String ?: "Pet"
                        val species = activePetMap["species"] as? String ?: "Dog"
                        val breed = activePetMap["breed"] as? String ?: "Companion"
                        val age = activePetMap["age_years"] as? String ?: "1.0 yr"
                        val weightKg = activePetMap["weight_kg"]?.toString() ?: "2.0"
                        val weight = if (weightKg.endsWith("kg")) weightKg else "$weightKg kg"
                        val imageUrl = activePetMap["image_url"] as? String
                        val gender = activePetMap["gender"] as? String ?: "Female"
                        val microchip = activePetMap["microchip_id"] as? String
                        val healthScore = (activePetMap["health_score"] as? Number)?.toInt() ?: 95
                        val vaccination = activePetMap["vaccination_status"] as? String ?: "Up to Date"

                        session.saveActivePetDetails(
                            id = petId,
                            name = petName,
                            type = species,
                            breed = breed,
                            age = age,
                            weight = weight,
                            imageUrl = imageUrl,
                            gender = gender,
                            microchipId = microchip,
                            healthScore = healthScore,
                            vaccinationStatus = vaccination
                        )
                    }
                }
                true
            } else if (response.code() == 401) {
                session.clearSession()
                false
            } else {
                true
            }
        } catch (e: Exception) {
            android.util.Log.w("PetCareRepository", "validateSession network error: ${e.message}, retaining existing session")
            true
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.logout()
        } catch (_: Exception) {
            // Network/server errors during logout are non-blocking
        } finally {
            session.clearSession()
        }
        Result.success(Unit)
    }

    // ----------------------------------------------------
    // Dashboard & Active Pet
    // ----------------------------------------------------
    suspend fun getDashboard(): Result<DashboardData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDashboard()
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                data.activePet?.let { p ->
                    session.saveActivePetDetails(
                        id = p.id,
                        name = p.name,
                        type = p.species,
                        breed = p.breed,
                        age = p.ageYears,
                        weight = "${p.weightKg} kg",
                        imageUrl = p.imageUrl,
                        gender = p.gender,
                        microchipId = p.microchipId,
                        healthScore = p.healthScore,
                        vaccinationStatus = p.vaccinationStatus
                    )
                }
                if (!data.user?.avatarUrl.isNullOrBlank()) {
                    session.setUserAvatarUrl(data.user?.avatarUrl)
                }
                Result.success(data)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load dashboard"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ----------------------------------------------------
    // Pets
    // ----------------------------------------------------
    suspend fun getPets(): Result<List<PetDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPets()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load pets"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun createPet(
        name: String,
        species: String,
        breed: String,
        ageYears: String,
        weightKg: Double,
        gender: String = "Female",
        imageBase64: String? = null,
        imageUrl: String? = null
    ): Result<PetDto> = withContext(Dispatchers.IO) {
        try {
            val req = CreatePetRequest(
                name = name.trim(),
                species = species.trim(),
                breed = breed.trim(),
                ageYears = ageYears.trim(),
                weightKg = weightKg,
                gender = gender,
                imageBase64 = imageBase64,
                imageUrl = imageUrl
            )
            val response = api.createPet(req)
            if (response.isSuccessful && response.body()?.success == true) {
                val pet = response.body()!!.data!!
                session.setActivePetId(pet.id)
                session.saveActivePetDetails(
                    id = pet.id,
                    name = pet.name,
                    type = pet.species,
                    breed = pet.breed,
                    age = pet.ageYears,
                    weight = "${pet.weightKg} kg",
                    imageUrl = pet.imageUrl,
                    gender = pet.gender,
                    microchipId = pet.microchipId,
                    healthScore = pet.healthScore,
                    vaccinationStatus = pet.vaccinationStatus
                )
                Result.success(pet)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to create pet"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getPetById(id: Int): Result<PetDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPetById(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to fetch pet"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getPetWeights(petId: Int): Result<WeightOverviewDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPetWeights(petId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load weight records"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun addWeight(petId: Int, weightKg: Double, date: String? = null): Result<WeightEntryDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.addPetWeight(petId, AddWeightRequest(weightKg = weightKg, recordedDate = date))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to record weight"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ----------------------------------------------------
    // Diet & Nutrition
    // ----------------------------------------------------
    suspend fun getDietSummary(): Result<DietSummaryDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDietSummary()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load diet summary"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getMealSchedule(): Result<List<MealScheduleDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMealSchedule()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load meal schedule"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun logMeal(foodItem: String, portionSize: String, calories: Int? = null): Result<MealEntryDto> = withContext(Dispatchers.IO) {
        try {
            val req = LogMealRequest(foodItem = foodItem, portionSize = portionSize, calories = calories)
            val response = api.logMeal(req)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to log meal"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getSafeToxicFoods(): Result<List<SafeToxicFoodDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSafeToxicFoods()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load foods guide"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ----------------------------------------------------
    // Reminders & Grooming
    // ----------------------------------------------------
    suspend fun getReminders(): Result<List<CareReminderDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getReminders()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load reminders"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun createReminder(title: String, time: String, category: String = "Grooming", notify: Boolean = true): Result<CareReminderDto> = withContext(Dispatchers.IO) {
        try {
            val req = CreateReminderRequest(title = title, reminderTime = time, category = category, notifyPush = if (notify) 1 else 0)
            val response = api.createReminder(req)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to save reminder"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun toggleReminder(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.toggleReminder(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Could not toggle reminder"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getCalendarSchedule(): Result<List<CalendarTaskDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCalendarSchedule()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load calendar"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getGroomingGuides(): Result<List<GroomingGuideDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getGroomingGuides()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load guides"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ----------------------------------------------------
    // Health & AI Diagnostics
    // ----------------------------------------------------
    suspend fun getSymptoms(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSymptoms()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load symptoms"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun runHealthScan(symptoms: List<String>, scanType: String = "symptoms"): Result<HealthScanResultDto> = withContext(Dispatchers.IO) {
        try {
            val req = HealthScanRequest(scanType = scanType, symptoms = symptoms)
            val response = api.runHealthScan(req)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Diagnostic scan failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getConditionDetail(id: Int = 1): Result<ConditionGuideDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getConditionDetail(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load condition guide"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun saveHealthReport(title: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.saveHealthReport(SaveReportRequest(reportTitle = title))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save report"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ----------------------------------------------------
    // Pet Modifications & Photo Upload
    // ----------------------------------------------------
    suspend fun updatePetDetails(id: Int, request: UpdatePetRequest): Result<PetDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.updatePet(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                val updatedPet = response.body()!!.data!!
                Result.success(updatedPet)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to update pet")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun deletePet(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deletePet(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to delete pet")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun uploadPetPhoto(id: Int, base64Image: String): Result<UploadPhotoDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.uploadPetPhoto(id, UploadPhotoRequest(imageBase64 = base64Image))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                if (id == session.getActivePetId()) {
                    session.setActivePetImageUrl(data.imageUrl)
                }
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to upload photo")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ----------------------------------------------------
    // User Profile & Settings
    // ----------------------------------------------------
    suspend fun updateProfile(name: String, email: String): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateProfile(UpdateProfileRequest(name = name.trim(), email = email.trim()))
            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()!!.data!!
                session.saveAuthSession(
                    token = session.getToken() ?: "",
                    userId = user.id,
                    userName = user.name,
                    email = user.email,
                    activePetId = session.getActivePetId()
                )
                if (!user.avatarUrl.isNullOrBlank()) {
                    session.setUserAvatarUrl(user.avatarUrl)
                }
                Result.success(user)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to update profile")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun uploadUserAvatar(base64Image: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.uploadUserAvatar(UploadPhotoRequest(imageBase64 = base64Image))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data
                val avatarUrl = data?.get("avatar_url") as? String ?: ""
                session.setUserAvatarUrl(avatarUrl)
                Result.success(avatarUrl)
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to upload avatar")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun changePassword(currentPass: String, newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.changePassword(ChangePasswordRequest(currentPassword = currentPass, newPassword = newPass))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to update password")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getPreferences(): Result<PreferencesDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPreferences()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to load preferences"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun updatePreferences(preferences: UpdatePreferencesRequest): Result<PreferencesDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.updatePreferences(preferences)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to update preferences")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun submitSupportReport(category: String, subject: String, description: String): Result<SupportTicketDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.submitSupportReport(SupportReportRequest(
                category = category.trim(),
                subject = subject.trim(),
                description = description.trim()
            ))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to submit report")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }
}




























                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     