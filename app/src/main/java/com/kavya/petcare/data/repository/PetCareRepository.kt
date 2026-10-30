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

    // ----------------------------------------------------
    // Authentication
    // ----------------------------------------------------
    suspend fun login(email: String, pass: String): Result<AuthData> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(LoginRequest(email = email.trim(), password = pass.trim()))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                session.saveAuthSession(
                    token = authData.token,
                    userId = authData.user.id,
                    userName = authData.user.name,
                    email = authData.user.email,
                    activePetId = authData.activePet?.id
                )
                Result.success(authData)
            } else {
                val errorMsg = parseErrorMessage(response, response.body()?.message ?: "Login failed. Please verify credentials.")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Could not connect to server"}"))
        }
    }

    suspend fun register(name: String, email: String, pass: String): Result<AuthData> = withContext(Dispatchers.IO) {
        try {
            val response = api.register(RegisterRequest(name = name.trim(), email = email.trim(), password = pass.trim()))
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
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Could not connect to server"}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.logout()
            session.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            session.clearSession()
            Result.success(Unit)
        }
    }

    // ----------------------------------------------------
    // Dashboard & Active Pet
    // ----------------------------------------------------
    suspend fun getDashboard(): Result<DashboardData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDashboard()
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                data.activePet?.id?.let { session.setActivePetId(it) }
                Result.success(data)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load dashboard"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }

    suspend fun createPet(
        name: String,
        species: String,
        breed: String,
        ageYears: String,
        weightKg: Double,
        gender: String = "Female"
    ): Result<PetDto> = withContext(Dispatchers.IO) {
        try {
            val req = CreatePetRequest(
                name = name.trim(),
                species = species.trim(),
                breed = breed.trim(),
                ageYears = ageYears.trim(),
                weightKg = weightKg,
                gender = gender
            )
            val response = api.createPet(req)
            if (response.isSuccessful && response.body()?.success == true) {
                val pet = response.body()!!.data!!
                session.setActivePetId(pet.id)
                Result.success(pet)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to create pet"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }
}

