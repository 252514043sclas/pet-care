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
                if (authData.activePet != null && authData.activePet.id > 0 && authData.activePet.name.isNotBlank()) {
                    val p = authData.activePet
                    session.saveActivePetDetails(
                        id = p.id,
                        name = p.name,
                        type = p.species,
                        breed = p.breed,
                        age = p.ageYears,
                        weight = "${p.weightKg} kg",
                        imageUrl = p.imageUrl,
                        gender = p.gender ?: "Female",
                        microchipId = p.microchipId,
                        healthScore = p.healthScore,
                        vaccinationStatus = p.vaccinationStatus
                    )
                } else {
                    session.clearActivePet()
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
                session.clearActivePet()
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

    suspend fun addWeight(petId: Int, weightKg: Double, date: String? = null, notes: String? = null): Result<WeightEntryDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.addPetWeight(petId, AddWeightRequest(weightKg = weightKg, recordedDate = date, notes = notes))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response, response.body()?.message ?: "Failed to record weight")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun updateWeight(petId: Int, weightId: Int, weightKg: Double, date: String? = null, notes: String? = null): Result<WeightEntryDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.updatePetWeight(petId, weightId, UpdateWeightRequest(weightKg = weightKg, recordedDate = date, notes = notes))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response, response.body()?.message ?: "Failed to update weight entry")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun deleteWeight(petId: Int, weightId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deletePetWeight(petId, weightId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response, "Failed to delete weight entry")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun updateTargetWeight(petId: Int, targetWeightKg: Double?): Result<Double?> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateTargetWeight(petId, UpdateTargetWeightRequest(targetWeightKg = targetWeightKg))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data?.targetWeight)
            } else {
                Result.failure(Exception(parseErrorMessage(response, response.body()?.message ?: "Failed to update target weight")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ----------------------------------------------------
    // Diet & Nutrition
    // ----------------------------------------------------
    suspend fun getDietSummary(petId: Int? = null): Result<DietSummaryDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDietSummary(petId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load diet summary"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getMealSchedule(petId: Int? = null): Result<List<MealScheduleDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMealSchedule(petId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load meal schedule"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun createMealSchedule(request: CreateMealScheduleRequest): Result<MealScheduleDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.createMealSchedule(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to create meal schedule"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun updateMealSchedule(id: Int, request: UpdateMealScheduleRequest): Result<MealScheduleDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateMealSchedule(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to update meal schedule"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun deleteMealSchedule(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteMealSchedule(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete meal schedule"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun toggleMealReminder(id: Int): Result<MealScheduleDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.toggleMealReminder(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to toggle meal reminder"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun analyzeFood(
        foodName: String,
        quantity: Double,
        unit: String,
        species: String? = null,
        petId: Int? = null
    ): Result<FoodAnalysisDto> = withContext(Dispatchers.IO) {
        try {
            val req = AnalyzeFoodRequest(
                foodName = foodName,
                portionQuantity = quantity,
                portionUnit = unit,
                species = species,
                petId = petId
            )
            val response = api.analyzeFood(req)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response, response.body()?.message ?: "Failed to analyze food")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getLoggedMeals(petId: Int? = null, date: String? = null): Result<List<MealEntryDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLoggedMeals(petId, date)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(parseErrorMessage(response, response.body()?.message ?: "Failed to load meal history")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun logMeal(request: LogMealRequest): Result<MealEntryDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.logMeal(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response, response.body()?.message ?: "Failed to save meal entry")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun updateLoggedMeal(id: Int, request: UpdateMealRequest): Result<MealEntryDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateLoggedMeal(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response, response.body()?.message ?: "Failed to update meal entry")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun deleteLoggedMeal(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteLoggedMeal(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response, "Failed to delete meal entry")))
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

    // ----------------------------------------------------
    // Dynamic Grooming Calendar Reminders
    // ----------------------------------------------------
    suspend fun getGroomingReminders(petId: Int? = null, status: String? = null): Result<List<GroomingReminderDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getGroomingReminders(petId = petId, status = status)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data ?: emptyList()
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to load grooming reminders")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun createGroomingReminder(request: CreateGroomingReminderRequest): Result<GroomingReminderDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.createGroomingReminder(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to create grooming reminder")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getGroomingReminderById(id: Int): Result<GroomingReminderDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getGroomingReminderById(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to fetch grooming reminder")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun updateGroomingReminder(id: Int, request: UpdateGroomingReminderRequest): Result<GroomingReminderDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateGroomingReminder(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to update grooming reminder")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun updateGroomingReminderStatus(id: Int, status: String): Result<GroomingReminderDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateGroomingReminderStatus(id, UpdateGroomingStatusRequest(status = status))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to update status")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun deleteGroomingReminder(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteGroomingReminder(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to delete grooming reminder")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ----------------------------------------------------
    // Dynamic Pet Health Score & Health Records
    // ----------------------------------------------------
    suspend fun getPetHealthScore(petId: Int): Result<PetHealthScoreOverviewDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPetHealthScore(petId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: PetHealthScoreOverviewDto())
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to fetch health score")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getPetHealthRecords(petId: Int): Result<List<PetHealthRecordDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPetHealthRecords(petId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to fetch health records")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun addPetHealthRecord(petId: Int, request: CreateHealthRecordRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.addPetHealthRecord(petId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to add health record")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun updatePetHealthRecord(petId: Int, recordId: Int, request: CreateHealthRecordRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.updatePetHealthRecord(petId, recordId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to update health record")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun deletePetHealthRecord(petId: Int, recordId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deletePetHealthRecord(petId, recordId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to delete health record")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ----------------------------------------------------
    // Dynamic Pet Vaccinations
    // ----------------------------------------------------
    suspend fun getPetVaccinations(petId: Int): Result<VaccinationOverviewDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPetVaccinations(petId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: VaccinationOverviewDto())
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to fetch vaccination records")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun addPetVaccination(petId: Int, request: CreateVaccinationRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.addPetVaccination(petId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to add vaccination")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun updatePetVaccination(petId: Int, vaccineId: Int, request: CreateVaccinationRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.updatePetVaccination(petId, vaccineId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Failed to update vaccination")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun deletePetVaccination(petId: Int, vaccineId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deletePetVaccination(petId, vaccineId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to delete vaccination")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ----------------------------------------------------
    // AI Chatbot & Multimodal Vision (Qwen3-VL 2B)
    // ----------------------------------------------------
    suspend fun sendChatMessage(
        message: String,
        petId: Int?,
        enableSearch: Boolean = true
    ): Result<ChatResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.sendChatMessage(ChatRequest(message = message, petId = petId, enableSearch = enableSearch))
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to generate AI response")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun sendChatImage(
        imageBase64: String,
        prompt: String,
        petId: Int?
    ): Result<ChatImageResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.sendChatImageAnalysis(ChatImageRequest(imageBase64 = imageBase64, prompt = prompt, petId = petId))
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, "AI image analysis failed")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getNutritionGuidance(
        query: String,
        petId: Int?
    ): Result<NutritionResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getNutritionGuidance(NutritionRequest(query = query, petId = petId))
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, "Failed to retrieve nutrition plan")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun searchVeterinary(
        query: String,
        maxResults: Int = 4
    ): Result<SearchResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchVeterinary(SearchRequest(query = query, maxResults = maxResults))
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, "Search failed")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getChatHistory(
        petId: Int?,
        limit: Int = 50
    ): Result<List<ChatMessageDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getChatHistory(petId = petId, limit = limit)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, "Could not load chat history")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun clearChatHistory(
        petId: Int?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.clearChatHistory(petId = petId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response, "Could not clear chat history")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    suspend fun getSystemHealth(): Result<SystemHealthDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSystemHealth()
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response, "Could not check system health")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatNetworkError(e)))
        }
    }
}




























                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     