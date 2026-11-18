package com.kavya.petcare

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kavya.petcare.data.local.WeightMealCacheStore
import com.kavya.petcare.data.models.*
import com.kavya.petcare.data.repository.PetCareRepository
import com.kavya.petcare.data.session.SessionManager
import com.kavya.petcare.notifications.FeedingScheduleStore
import com.kavya.petcare.notifications.MealNotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ----------------------------------------------------
// UI Models (Retained for 100% UI Compatibility)
// ----------------------------------------------------
data class PetProfile(
    val id: String = "1",
    val name: String = "Clover",
    val type: String = "Rabbit",
    val breed: String = "Holland Lop",
    val age: String = "1.2 yrs",
    val weight: String = "1.8 kg",
    val healthScore: Int = 95,
    val gender: String = "Female",
    val microchipId: String = "#985142091",
    val vaccinationStatus: String = "Up to Date",
    val imageUrl: String? = null
)

data class CareReminder(
    val id: String,
    val title: String,
    val time: String,
    val isCompleted: Boolean = false
)

data class MealEntry(
    val id: String,
    val mealName: String,
    val calories: Int,
    val time: String
)

data class WeightEntry(
    val date: String,
    val weightKg: Float
)

// ----------------------------------------------------
// Production-Ready MVVM ViewModel
// ----------------------------------------------------
class PetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PetCareRepository(application.applicationContext)
    private val session = SessionManager.getInstance(application.applicationContext)

    // UI Loading & Error StateFlows
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Active Pet Profile StateFlow initialized from persistent SessionManager
    private val _activePet = MutableStateFlow(
        if (session.getSavedActivePetName() != null) {
            PetProfile(
                id = session.getActivePetId().toString(),
                name = session.getSavedActivePetName() ?: "Clover",
                type = session.getSavedActivePetType() ?: "Rabbit",
                breed = session.getSavedActivePetBreed() ?: "Holland Lop",
                age = session.getSavedActivePetAge() ?: "1.2 yrs",
                weight = session.getSavedActivePetWeight() ?: "1.8 kg",
                healthScore = session.getSavedActivePetHealthScore(),
                gender = session.getSavedActivePetGender() ?: "Female",
                microchipId = session.getSavedActivePetMicrochip() ?: "#985142091",
                vaccinationStatus = session.getSavedActivePetVaccination(),
                imageUrl = session.getSavedActivePetImageUrl()
            )
        } else {
            PetProfile()
        }
    )
    val activePet: StateFlow<PetProfile> = _activePet.asStateFlow()

    // User Avatar StateFlow
    private val _userAvatarUrl = MutableStateFlow<String?>(session.getUserAvatarUrl())
    val userAvatarUrl: StateFlow<String?> = _userAvatarUrl.asStateFlow()

    // Reminders StateFlow
    private val _reminders = MutableStateFlow<List<CareReminder>>(
        listOf(
            CareReminder("1", "Morning Timothy Hay & Greens", "8:00 AM"),
            CareReminder("2", "Fur Brushing & Nail Check", "5:30 PM"),
            CareReminder("3", "Evening Fresh Water Refill", "8:00 PM")
        )
    )
    val reminders: StateFlow<List<CareReminder>> = _reminders.asStateFlow()

    // Meals StateFlow (NO hardcoded dummy data)
    private val _meals = MutableStateFlow<List<MealEntry>>(emptyList())
    val meals: StateFlow<List<MealEntry>> = _meals.asStateFlow()

    // Persistent Meal History StateFlow (from MySQL + local cache)
    private val _mealHistory = MutableStateFlow<List<MealEntryDto>>(emptyList())
    val mealHistory: StateFlow<List<MealEntryDto>> = _mealHistory.asStateFlow()

    private val _isLoadingMeals = MutableStateFlow(false)
    val isLoadingMeals: StateFlow<Boolean> = _isLoadingMeals.asStateFlow()

    private val _mealHistoryError = MutableStateFlow<String?>(null)
    val mealHistoryError: StateFlow<String?> = _mealHistoryError.asStateFlow()

    // Dynamic Food Analysis StateFlows (Calories + Species Safety)
    private val _foodAnalysis = MutableStateFlow<FoodAnalysisDto?>(null)
    val foodAnalysis: StateFlow<FoodAnalysisDto?> = _foodAnalysis.asStateFlow()

    private val _isAnalyzingFood = MutableStateFlow(false)
    val isAnalyzingFood: StateFlow<Boolean> = _isAnalyzingFood.asStateFlow()

    private val _foodAnalysisError = MutableStateFlow<String?>(null)
    val foodAnalysisError: StateFlow<String?> = _foodAnalysisError.asStateFlow()

    // Feeding Schedule / Meal Schedules StateFlow
    private val _mealSchedules = MutableStateFlow<List<MealScheduleDto>>(emptyList())
    val mealSchedules: StateFlow<List<MealScheduleDto>> = _mealSchedules.asStateFlow()

    // Diet Summary StateFlow
    private val _dietSummary = MutableStateFlow<DietSummaryDto?>(null)
    val dietSummary: StateFlow<DietSummaryDto?> = _dietSummary.asStateFlow()

    // Weight History StateFlow (NO hardcoded dummy data)
    private val _weightHistory = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weightHistory: StateFlow<List<WeightEntry>> = _weightHistory.asStateFlow()

    // Persistent Weight Tracker StateFlows (from MySQL + local cache)
    private val _weightOverview = MutableStateFlow<WeightOverviewDto?>(null)
    val weightOverview: StateFlow<WeightOverviewDto?> = _weightOverview.asStateFlow()

    private val _weightEntries = MutableStateFlow<List<WeightEntryDto>>(emptyList())
    val weightEntries: StateFlow<List<WeightEntryDto>> = _weightEntries.asStateFlow()

    // Safe vs Toxic Foods
    private val _safeToxicFoods = MutableStateFlow<List<SafeToxicFoodDto>>(emptyList())
    val safeToxicFoods: StateFlow<List<SafeToxicFoodDto>> = _safeToxicFoods.asStateFlow()

    // Care Calendar Tasks
    private val _calendarTasks = MutableStateFlow<List<CalendarTaskDto>>(emptyList())
    val calendarTasks: StateFlow<List<CalendarTaskDto>> = _calendarTasks.asStateFlow()

    // Grooming Guides
    private val _groomingGuides = MutableStateFlow<List<GroomingGuideDto>>(emptyList())
    val groomingGuides: StateFlow<List<GroomingGuideDto>> = _groomingGuides.asStateFlow()

    // Health Symptoms & Diagnostics
    private val _symptomsList = MutableStateFlow<List<String>>(emptyList())
    val symptomsList: StateFlow<List<String>> = _symptomsList.asStateFlow()

    private val _diagnosticResult = MutableStateFlow<HealthScanResultDto?>(null)
    val diagnosticResult: StateFlow<HealthScanResultDto?> = _diagnosticResult.asStateFlow()

    private val _conditionDetail = MutableStateFlow<ConditionGuideDto?>(null)
    val conditionDetail: StateFlow<ConditionGuideDto?> = _conditionDetail.asStateFlow()

    // All User Pets (for Pet Management)
    private val _allPets = MutableStateFlow<List<PetDto>>(emptyList())
    val allPets: StateFlow<List<PetDto>> = _allPets.asStateFlow()

    // User Preferences (Notifications & Theme)
    private val _preferences = MutableStateFlow(PreferencesDto())
    val preferences: StateFlow<PreferencesDto> = _preferences.asStateFlow()

    init {
        // If user already has a session or on launch, fetch live data from backend
        refreshDashboard()
        loadAllPets()
        loadPreferences()
        loadSafeToxicFoods()
        loadCalendarTasks()
        loadSymptoms()
        loadConditionDetail(1)
        loadMealSchedules()
        loadDietSummary()
        loadWeightHistory()
        loadMealHistory()
    }

    // ----------------------------------------------------
    // Authentication Operations
    // ----------------------------------------------------
    fun login(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.login(email, pass)
            _isLoading.value = false
            result.onSuccess {
                refreshDashboard()
                onSuccess()
            }.onFailure { err ->
                _errorMessage.value = err.message
                onError(err.message ?: "Login failed")
            }
        }
    }

    fun register(name: String, email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.register(name, email, pass)
            _isLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                _errorMessage.value = err.message
                onError(err.message ?: "Registration failed")
            }
        }
    }

    fun forgotPassword(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.forgotPassword(email)
            _isLoading.value = false
            result.onSuccess { msg ->
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, err.message ?: "Could not send reset link")
            }
        }
    }

    fun validateSession(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val valid = repository.validateSession()
            onResult(valid)
        }
    }

    // ----------------------------------------------------
    // Dashboard & Data Synchronization
    // ----------------------------------------------------
    fun refreshDashboard() {
        viewModelScope.launch {
            repository.getDashboard().onSuccess { dash ->
                dash.activePet?.let { p ->
                    val profile = PetProfile(
                        id = p.id.toString(),
                        name = p.name,
                        type = p.species,
                        breed = p.breed,
                        age = p.ageYears,
                        weight = "${p.weightKg} kg",
                        healthScore = p.healthScore,
                        gender = p.gender ?: "Female",
                        microchipId = p.microchipId ?: "#985142091",
                        vaccinationStatus = p.vaccinationStatus,
                        imageUrl = p.imageUrl
                    )
                    _activePet.value = profile
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
                    loadWeightHistory(p.id)
                    loadMealHistory(p.id)
                }
                _userAvatarUrl.value = session.getUserAvatarUrl()

                if (dash.todaySchedule.isNotEmpty()) {
                    _reminders.value = dash.todaySchedule.map { item ->
                        CareReminder(
                            id = item.id,
                            title = item.title,
                            time = item.time,
                            isCompleted = item.isCompleted
                        )
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // Pet Operations
    // ----------------------------------------------------
    fun createPetProfile(
        name: String,
        breed: String,
        age: String,
        weight: String,
        type: String,
        onComplete: () -> Unit
    ) {
        createPetProfile(name, breed, age, weight, type, null, null, onComplete)
    }

    fun createPetProfile(
        name: String,
        breed: String,
        age: String,
        weight: String,
        type: String,
        imageBase64: String?,
        bitmap: android.graphics.Bitmap?,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val weightKg = weight.toDoubleOrNull() ?: 1.8
            val result = repository.createPet(
                name = name,
                species = type,
                breed = breed,
                ageYears = age,
                weightKg = weightKg,
                imageBase64 = imageBase64
            )
            _isLoading.value = false
            result.onSuccess { pet ->
                if (bitmap != null && !pet.imageUrl.isNullOrBlank()) {
                    com.kavya.petcare.ui.components.PetImageLoader.cacheBitmap(getApplication(), pet.imageUrl, bitmap)
                    val full = com.kavya.petcare.ui.components.PetImageLoader.resolveFullUrl(getApplication(), pet.imageUrl)
                    com.kavya.petcare.ui.components.PetImageLoader.cacheBitmap(getApplication(), full, bitmap)
                }
                val profile = PetProfile(
                    id = pet.id.toString(),
                    name = pet.name,
                    type = pet.species,
                    breed = pet.breed,
                    age = pet.ageYears,
                    weight = "${pet.weightKg} kg",
                    healthScore = pet.healthScore,
                    gender = pet.gender ?: "Female",
                    microchipId = pet.microchipId ?: "#985142091",
                    vaccinationStatus = pet.vaccinationStatus,
                    imageUrl = pet.imageUrl
                )
                _activePet.value = profile
                session.saveActivePetDetails(
                    id = pet.id,
                    name = pet.name,
                    type = pet.species,
                    breed = pet.breed,
                    age = pet.ageYears,
                    weight = "${pet.weightKg} kg",
                    imageUrl = pet.imageUrl,
                    gender = pet.gender ?: "Female",
                    microchipId = pet.microchipId,
                    healthScore = pet.healthScore,
                    vaccinationStatus = pet.vaccinationStatus
                )
                refreshDashboard()
                loadAllPets()
                onComplete()
            }.onFailure {
                // Keep local state update for seamless UX even if offline
                updatePetProfile(name, breed, age, weight, type)
                session.saveActivePetDetails(
                    id = session.getActivePetId(),
                    name = name,
                    type = type,
                    breed = breed,
                    age = age,
                    weight = weight,
                    imageUrl = null
                )
                onComplete()
            }
        }
    }

    fun updatePetProfile(name: String, breed: String, age: String, weight: String, type: String) {
        _activePet.value = _activePet.value.copy(
            name = name,
            breed = breed,
            age = age,
            weight = weight,
            type = type
        )
    }

    fun loadAllPets() {
        viewModelScope.launch {
            repository.getPets().onSuccess { list ->
                _allPets.value = list
            }
        }
    }

    fun selectActivePet(p: PetDto) {
        val profile = PetProfile(
            id = p.id.toString(),
            name = p.name,
            type = p.species,
            breed = p.breed,
            age = p.ageYears,
            weight = "${p.weightKg} kg",
            healthScore = p.healthScore,
            gender = p.gender ?: "Female",
            microchipId = p.microchipId ?: "#985142091",
            vaccinationStatus = p.vaccinationStatus,
            imageUrl = p.imageUrl
        )
        _activePet.value = profile
        session.setActivePetId(p.id)
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
        // Permanently persist active pet selection to MySQL
        viewModelScope.launch {
            repository.updatePetDetails(p.id, UpdatePetRequest(isActive = 1))
            refreshDashboard()
            loadAllPets()
            loadWeightHistory(p.id)
            loadMealHistory(p.id)
        }
    }

    fun editPetProfile(
        petId: Int,
        name: String,
        breed: String,
        age: String,
        weight: String,
        type: String,
        gender: String,
        microchipId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val cleanedWeight = weight.replace(Regex("(?i)kg"), "").trim()
            val weightKg = cleanedWeight.toDoubleOrNull() ?: 1.8
            val trimmedAge = age.trim()
            val formattedAge = if (trimmedAge.isNotBlank() && !trimmedAge.contains(Regex("[a-zA-Z]"))) {
                "$trimmedAge yrs"
            } else {
                trimmedAge
            }
            val request = UpdatePetRequest(
                name = name.trim(),
                species = type.trim(),
                breed = breed.trim(),
                ageYears = formattedAge,
                weightKg = weightKg,
                gender = gender.trim().ifBlank { "Female" },
                microchipId = microchipId.trim().ifBlank { null }
            )
            val result = repository.updatePetDetails(petId, request)
            _isLoading.value = false
            result.onSuccess { updated ->
                _activePet.value = PetProfile(
                    id = updated.id.toString(),
                    name = updated.name,
                    type = updated.species,
                    breed = updated.breed,
                    age = updated.ageYears,
                    weight = "${updated.weightKg} kg",
                    healthScore = updated.healthScore,
                    gender = updated.gender,
                    microchipId = updated.microchipId ?: _activePet.value.microchipId,
                    vaccinationStatus = updated.vaccinationStatus,
                    imageUrl = updated.imageUrl ?: _activePet.value.imageUrl
                )
                refreshDashboard()
                loadAllPets()
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Failed to update pet profile")
            }
        }
    }

    fun uploadPetPhoto(petId: Int, base64: String, onDone: (Boolean, String) -> Unit) {
        uploadPetPhoto(petId, base64, null, onDone)
    }

    fun uploadPetPhoto(petId: Int, base64: String, bitmap: android.graphics.Bitmap?, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            if (bitmap != null) {
                com.kavya.petcare.ui.components.PetImageLoader.cacheBitmap(getApplication(), base64, bitmap)
            }
            val result = repository.uploadPetPhoto(petId, base64)
            _isLoading.value = false
            result.onSuccess { data ->
                if (bitmap != null) {
                    com.kavya.petcare.ui.components.PetImageLoader.cacheBitmap(getApplication(), data.imageUrl, bitmap)
                    val fullUrl = com.kavya.petcare.ui.components.PetImageLoader.resolveFullUrl(getApplication(), data.imageUrl)
                    com.kavya.petcare.ui.components.PetImageLoader.cacheBitmap(getApplication(), fullUrl, bitmap)
                }
                if (_activePet.value.id == petId.toString()) {
                    _activePet.value = _activePet.value.copy(imageUrl = data.imageUrl)
                    session.setActivePetImageUrl(data.imageUrl)
                }
                refreshDashboard()
                loadAllPets()
                onDone(true, "Pet photo updated successfully")
            }.onFailure { err ->
                onDone(false, err.message ?: "Failed to upload photo")
            }
        }
    }

    fun deletePet(petId: Int, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deletePet(petId)
            _isLoading.value = false
            result.onSuccess {
                loadAllPets()
                refreshDashboard()
                onDone(true, "Pet deleted successfully")
            }.onFailure { err ->
                onDone(false, err.message ?: "Failed to delete pet")
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.logout()
            com.kavya.petcare.ui.components.PetImageLoader.clearMemoryCache()
            WeightMealCacheStore.clearAll(getApplication())
            _activePet.value = PetProfile()
            _userAvatarUrl.value = null
            _allPets.value = emptyList()
            _weightOverview.value = null
            _weightEntries.value = emptyList()
            _weightHistory.value = emptyList()
            _mealHistory.value = emptyList()
            _meals.value = emptyList()
            _foodAnalysis.value = null
            resetShared()
            _isLoading.value = false
            onComplete()
        }
    }

    fun updateUserProfile(name: String, email: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateProfile(name, email)
            _isLoading.value = false
            result.onSuccess { user ->
                onDone(true, "Profile updated successfully")
            }.onFailure { err ->
                onDone(false, err.message ?: "Failed to update profile")
            }
        }
    }

    fun uploadUserAvatar(base64: String, onDone: (Boolean, String) -> Unit) {
        uploadUserAvatar(base64, null, onDone)
    }

    fun uploadUserAvatar(base64: String, bitmap: android.graphics.Bitmap?, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            if (bitmap != null) {
                com.kavya.petcare.ui.components.PetImageLoader.cacheBitmap(getApplication(), base64, bitmap)
            }
            val result = repository.uploadUserAvatar(base64)
            _isLoading.value = false
            result.onSuccess { avatarUrl ->
                if (bitmap != null && avatarUrl.isNotBlank()) {
                    com.kavya.petcare.ui.components.PetImageLoader.cacheBitmap(getApplication(), avatarUrl, bitmap)
                    val fullUrl = com.kavya.petcare.ui.components.PetImageLoader.resolveFullUrl(getApplication(), avatarUrl)
                    com.kavya.petcare.ui.components.PetImageLoader.cacheBitmap(getApplication(), fullUrl, bitmap)
                }
                _userAvatarUrl.value = avatarUrl
                session.setUserAvatarUrl(avatarUrl)
                refreshDashboard()
                onDone(true, "Avatar updated successfully")
            }.onFailure { err ->
                onDone(false, err.message ?: "Failed to upload avatar")
            }
        }
    }

    fun changePassword(currentPass: String, newPass: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.changePassword(currentPass, newPass)
            _isLoading.value = false
            result.onSuccess {
                onDone(true, "Password changed successfully")
            }.onFailure { err ->
                onDone(false, err.message ?: "Failed to change password")
            }
        }
    }

    fun loadPreferences() {
        viewModelScope.launch {
            repository.getPreferences().onSuccess { prefs ->
                _preferences.value = prefs
            }
        }
    }

    fun updatePreferences(map: Map<String, Any>, onDone: ((Boolean, String) -> Unit)? = null) {
        val req = UpdatePreferencesRequest(
            notifVaccination = map["notif_vaccination"] as? Boolean,
            notifMedication = map["notif_medication"] as? Boolean,
            notifAppointments = map["notif_appointments"] as? Boolean,
            themeMode = map["theme_mode"] as? String,
            fontSize = map["font_size"] as? String
        )
        updatePreferences(req, onDone)
    }

    fun updatePreferences(request: UpdatePreferencesRequest, onDone: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.updatePreferences(request)
            result.onSuccess { updated ->
                _preferences.value = updated
                onDone?.invoke(true, "Preferences updated")
            }.onFailure { err ->
                onDone?.invoke(false, err.message ?: "Failed to update preferences")
            }
        }
    }

    fun submitSupportReport(category: String, subject: String, description: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.submitSupportReport(category, subject, description)
            _isLoading.value = false
            result.onSuccess { ticket ->
                onDone(true, "Report submitted successfully! Ticket #${ticket.ticketId}")
            }.onFailure { err ->
                onDone(false, err.message ?: "Failed to submit report")
            }
        }
    }

    // ----------------------------------------------------
    // Reminder Operations
    // ----------------------------------------------------
    fun addReminder(title: String, time: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val localReminder = CareReminder(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                time = time
            )
            _reminders.value = _reminders.value + localReminder

            repository.createReminder(title, time).onSuccess {
                refreshDashboard()
                loadCalendarTasks()
            }
            onDone?.invoke()
        }
    }

    fun toggleReminder(id: String) {
        _reminders.value = _reminders.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }

        val numericId = id.toIntOrNull()
        if (numericId != null) {
            viewModelScope.launch {
                repository.toggleReminder(numericId)
            }
        }
    }

    // ----------------------------------------------------
    // Meal Operations & Feeding Schedules
    // ----------------------------------------------------
    fun loadMealSchedules(petId: Int? = null) {
        val targetPetId = petId ?: _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        val petName = _activePet.value.name

        // Instant local cache load
        val cached = FeedingScheduleStore.getSchedules(getApplication(), targetPetId)
        if (cached.isNotEmpty()) {
            _mealSchedules.value = cached
        }

        viewModelScope.launch {
            repository.getMealSchedule(targetPetId).onSuccess { list ->
                _mealSchedules.value = list
                FeedingScheduleStore.saveSchedules(getApplication(), targetPetId, list, petName)
                MealNotificationScheduler.rescheduleAllForPet(getApplication(), targetPetId, petName, list)
            }
            loadDietSummary()
        }
    }

    fun loadDietSummary() {
        viewModelScope.launch {
            repository.getDietSummary().onSuccess { summary ->
                _dietSummary.value = summary
            }
        }
    }

    fun createMealSchedule(
        mealName: String,
        scheduledTime: String,
        foodName: String?,
        foodQuantity: String?,
        instructions: String?,
        calories: Int,
        isReminderEnabled: Int = 1,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val targetPetId = _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        val petName = _activePet.value.name
        viewModelScope.launch {
            _isLoading.value = true
            val request = CreateMealScheduleRequest(
                petId = targetPetId,
                mealName = mealName,
                scheduledTime = scheduledTime,
                foodName = foodName,
                foodQuantity = foodQuantity,
                mealDescription = foodName,
                instructions = instructions,
                calories = calories,
                isReminderEnabled = isReminderEnabled
            )
            val result = repository.createMealSchedule(request)
            _isLoading.value = false
            result.onSuccess { created ->
                val updatedList = _mealSchedules.value + created
                _mealSchedules.value = updatedList
                FeedingScheduleStore.updateScheduleInCache(getApplication(), targetPetId, created)
                if (created.isReminderEnabled == 1) {
                    MealNotificationScheduler.scheduleMealReminder(getApplication(), created, petName, targetPetId)
                }
                loadDietSummary()
                onSuccess?.invoke()
            }.onFailure { err ->
                onError?.invoke(err.message ?: "Failed to add meal schedule")
            }
        }
    }

    fun updateMealSchedule(
        id: Int,
        mealName: String?,
        scheduledTime: String?,
        foodName: String?,
        foodQuantity: String?,
        instructions: String?,
        calories: Int?,
        isReminderEnabled: Int?,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val targetPetId = _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        val petName = _activePet.value.name
        viewModelScope.launch {
            _isLoading.value = true
            val request = UpdateMealScheduleRequest(
                mealName = mealName,
                scheduledTime = scheduledTime,
                foodName = foodName,
                foodQuantity = foodQuantity,
                mealDescription = foodName,
                instructions = instructions,
                calories = calories,
                isReminderEnabled = isReminderEnabled
            )
            val result = repository.updateMealSchedule(id, request)
            _isLoading.value = false
            result.onSuccess { updated ->
                val updatedList = _mealSchedules.value.map { if (it.id == id) updated else it }
                _mealSchedules.value = updatedList
                FeedingScheduleStore.updateScheduleInCache(getApplication(), targetPetId, updated)
                if (updated.isReminderEnabled == 1) {
                    MealNotificationScheduler.scheduleMealReminder(getApplication(), updated, petName, targetPetId)
                } else {
                    MealNotificationScheduler.cancelMealReminder(getApplication(), targetPetId, updated.id)
                }
                loadDietSummary()
                onSuccess?.invoke()
            }.onFailure { err ->
                onError?.invoke(err.message ?: "Failed to update meal schedule")
            }
        }
    }

    fun deleteMealSchedule(id: Int, onSuccess: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        val targetPetId = _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteMealSchedule(id)
            _isLoading.value = false
            result.onSuccess {
                _mealSchedules.value = _mealSchedules.value.filter { it.id != id }
                FeedingScheduleStore.deleteScheduleFromCache(getApplication(), targetPetId, id)
                MealNotificationScheduler.cancelMealReminder(getApplication(), targetPetId, id)
                loadDietSummary()
                onSuccess?.invoke()
            }.onFailure { err ->
                onError?.invoke(err.message ?: "Failed to delete meal schedule")
            }
        }
    }

    fun toggleMealReminder(id: Int, onDone: ((Boolean) -> Unit)? = null) {
        val targetPetId = _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        val petName = _activePet.value.name
        val current = _mealSchedules.value.find { it.id == id } ?: return
        val newStatus = if (current.isReminderEnabled == 1) 0 else 1

        // Optimistic UI update
        val optimistic = current.copy(isReminderEnabled = newStatus)
        _mealSchedules.value = _mealSchedules.value.map { if (it.id == id) optimistic else it }
        FeedingScheduleStore.updateScheduleInCache(getApplication(), targetPetId, optimistic)

        if (newStatus == 1) {
            MealNotificationScheduler.scheduleMealReminder(getApplication(), optimistic, petName, targetPetId)
        } else {
            MealNotificationScheduler.cancelMealReminder(getApplication(), targetPetId, id)
        }

        viewModelScope.launch {
            val result = repository.toggleMealReminder(id)
            result.onSuccess { updated ->
                _mealSchedules.value = _mealSchedules.value.map { if (it.id == id) updated else it }
                FeedingScheduleStore.updateScheduleInCache(getApplication(), targetPetId, updated)
                onDone?.invoke(true)
            }.onFailure {
                // Revert on failure
                _mealSchedules.value = _mealSchedules.value.map { if (it.id == id) current else it }
                FeedingScheduleStore.updateScheduleInCache(getApplication(), targetPetId, current)
                if (current.isReminderEnabled == 1) {
                    MealNotificationScheduler.scheduleMealReminder(getApplication(), current, petName, targetPetId)
                } else {
                    MealNotificationScheduler.cancelMealReminder(getApplication(), targetPetId, id)
                }
                onDone?.invoke(false)
            }
        }
    }

    // ----------------------------------------------------
    // Dynamic Food Analysis & Persistent Meal History CRUD
    // ----------------------------------------------------
    fun clearFoodAnalysis() {
        _foodAnalysis.value = null
        _foodAnalysisError.value = null
        _isAnalyzingFood.value = false
    }

    fun analyzeFood(foodName: String, quantity: Double, unit: String, species: String? = null) {
        val trimmed = foodName.trim()
        if (trimmed.isBlank() || quantity <= 0.0) {
            _foodAnalysis.value = null
            _foodAnalysisError.value = null
            _isAnalyzingFood.value = false
            return
        }
        val petId = _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        val targetSpecies = species ?: _activePet.value.type

        viewModelScope.launch {
            _isAnalyzingFood.value = true
            _foodAnalysisError.value = null
            val result = repository.analyzeFood(
                foodName = trimmed,
                quantity = quantity,
                unit = unit,
                species = targetSpecies,
                petId = petId
            )
            _isAnalyzingFood.value = false
            result.onSuccess { analysis ->
                _foodAnalysis.value = analysis
            }.onFailure { err ->
                _foodAnalysisError.value = err.message ?: "Unable to check food nutrition & safety"
            }
        }
    }

    suspend fun analyzeFoodDirect(foodName: String, quantity: Double, unit: String, species: String? = null): Result<FoodAnalysisDto> {
        val petId = _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        val targetSpecies = species ?: _activePet.value.type
        return repository.analyzeFood(
            foodName = foodName.trim(),
            quantity = quantity,
            unit = unit,
            species = targetSpecies,
            petId = petId
        )
    }

    fun loadMealHistory(petId: Int? = null, dateFilter: String? = null) {
        val targetPetId = petId ?: _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        if (targetPetId <= 0) return

        if (dateFilter.isNullOrBlank()) {
            val cached = WeightMealCacheStore.getMealHistory(getApplication(), targetPetId)
            _mealHistory.value = cached
            _meals.value = cached.map {
                MealEntry(
                    id = it.id.toString(),
                    mealName = it.foodItem,
                    calories = it.calories ?: 0,
                    time = it.mealTime
                )
            }
        }

        viewModelScope.launch {
            _isLoadingMeals.value = true
            _mealHistoryError.value = null
            repository.getLoggedMeals(targetPetId, dateFilter).onSuccess { list ->
                _isLoadingMeals.value = false
                _mealHistoryError.value = null
                _mealHistory.value = list
                _meals.value = list.map {
                    MealEntry(
                        id = it.id.toString(),
                        mealName = it.foodItem,
                        calories = it.calories ?: 0,
                        time = it.mealTime
                    )
                }
                if (dateFilter.isNullOrBlank()) {
                    WeightMealCacheStore.saveMealHistory(getApplication(), targetPetId, list)
                }
            }.onFailure { err ->
                _isLoadingMeals.value = false
                _mealHistoryError.value = err.message ?: "Unable to load food safety history. Please check your connection."
            }
        }
    }

    fun saveMealEntry(
        foodItem: String,
        portionQuantity: Double,
        portionUnit: String,
        calories: Int?,
        safetyStatus: String,
        safetyWarning: String?,
        mealDate: String,
        mealTime: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val pet = _activePet.value
        val targetPetId = pet.id.toIntOrNull() ?: session.getActivePetId()
        val qtyStr = if (portionQuantity % 1.0 == 0.0) {
            portionQuantity.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", portionQuantity).trimEnd('0').trimEnd('.')
        }
        val portionDisplay = "$qtyStr $portionUnit"

        viewModelScope.launch {
            val req = LogMealRequest(
                petId = targetPetId,
                petName = pet.name,
                petSpecies = pet.type,
                foodItem = foodItem.trim(),
                portionSize = portionDisplay,
                portionQuantity = portionQuantity,
                portionUnit = portionUnit,
                calories = calories,
                safetyStatus = safetyStatus,
                safetyWarning = safetyWarning,
                mealDate = mealDate,
                mealTime = mealTime
            )
            val result = repository.logMeal(req)
            result.onSuccess { saved ->
                val updated = listOf(saved) + _mealHistory.value.filter { it.id != saved.id }
                _mealHistory.value = updated
                _meals.value = updated.map {
                    MealEntry(it.id.toString(), it.foodItem, it.calories ?: 0, it.mealTime)
                }
                WeightMealCacheStore.saveMealHistory(getApplication(), targetPetId, updated)
                loadDietSummary()
                onResult(true, "Meal entry saved successfully")
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to save meal entry")
            }
        }
    }

    fun updateMealEntry(
        id: Int,
        foodItem: String,
        portionQuantity: Double,
        portionUnit: String,
        calories: Int?,
        safetyStatus: String,
        safetyWarning: String?,
        mealDate: String,
        mealTime: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val pet = _activePet.value
        val targetPetId = pet.id.toIntOrNull() ?: session.getActivePetId()
        val qtyStr = if (portionQuantity % 1.0 == 0.0) {
            portionQuantity.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", portionQuantity).trimEnd('0').trimEnd('.')
        }
        val portionDisplay = "$qtyStr $portionUnit"

        viewModelScope.launch {
            val req = UpdateMealRequest(
                foodItem = foodItem.trim(),
                portionSize = portionDisplay,
                portionQuantity = portionQuantity,
                portionUnit = portionUnit,
                petSpecies = pet.type,
                calories = calories,
                safetyStatus = safetyStatus,
                safetyWarning = safetyWarning,
                mealDate = mealDate,
                mealTime = mealTime
            )
            val result = repository.updateLoggedMeal(id, req)
            result.onSuccess { updatedItem ->
                val updatedList = _mealHistory.value.map { if (it.id == id) updatedItem else it }
                _mealHistory.value = updatedList
                _meals.value = updatedList.map {
                    MealEntry(it.id.toString(), it.foodItem, it.calories ?: 0, it.mealTime)
                }
                WeightMealCacheStore.saveMealHistory(getApplication(), targetPetId, updatedList)
                loadDietSummary()
                onResult(true, "Meal entry updated")
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to update meal entry")
            }
        }
    }

    fun deleteMealEntry(id: Int, onResult: (Boolean, String) -> Unit) {
        val targetPetId = _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        viewModelScope.launch {
            val result = repository.deleteLoggedMeal(id)
            result.onSuccess {
                val remaining = _mealHistory.value.filter { it.id != id }
                _mealHistory.value = remaining
                _meals.value = remaining.map {
                    MealEntry(it.id.toString(), it.foodItem, it.calories ?: 0, it.mealTime)
                }
                WeightMealCacheStore.saveMealHistory(getApplication(), targetPetId, remaining)
                loadDietSummary()
                onResult(true, "Meal entry deleted")
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to delete meal entry")
            }
        }
    }

    // ----------------------------------------------------
    // Persistent Weight Tracker Operations (CRUD + Target)
    // ----------------------------------------------------
    fun loadWeightHistory(petId: Int? = null) {
        val targetPetId = petId ?: _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        if (targetPetId <= 0) return

        val cached = WeightMealCacheStore.getWeightOverview(getApplication(), targetPetId)
        if (cached != null) {
            _weightOverview.value = cached
            _weightEntries.value = cached.history
            _weightHistory.value = cached.history.map { WeightEntry(it.recordedDate, it.weightKg.toFloat()) }
        } else {
            _weightOverview.value = WeightOverviewDto(currentWeight = null, targetWeight = null, status = "No Records", history = emptyList())
            _weightEntries.value = emptyList()
            _weightHistory.value = emptyList()
        }

        viewModelScope.launch {
            repository.getPetWeights(targetPetId).onSuccess { overview ->
                _weightOverview.value = overview
                _weightEntries.value = overview.history
                _weightHistory.value = overview.history.map { WeightEntry(it.recordedDate, it.weightKg.toFloat()) }
                WeightMealCacheStore.saveWeightOverview(getApplication(), targetPetId, overview)
                overview.currentWeight?.let { latest ->
                    _activePet.value = _activePet.value.copy(weight = "$latest kg")
                }
            }
        }
    }

    fun addWeightEntry(
        date: String,
        weightKg: Float,
        notes: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        val targetPetId = _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        viewModelScope.launch {
            val result = repository.addWeight(targetPetId, weightKg.toDouble(), date, notes)
            result.onSuccess {
                repository.getPetWeights(targetPetId).onSuccess { overview ->
                    _weightOverview.value = overview
                    _weightEntries.value = overview.history
                    _weightHistory.value = overview.history.map { WeightEntry(it.recordedDate, it.weightKg.toFloat()) }
                    WeightMealCacheStore.saveWeightOverview(getApplication(), targetPetId, overview)
                    overview.currentWeight?.let { latest ->
                        _activePet.value = _activePet.value.copy(weight = "$latest kg")
                    }
                }
                onResult(true, "Weight recorded successfully")
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to save weight record")
            }
        }
    }

    fun updateWeightEntry(
        weightId: Int,
        weightKg: Float,
        date: String,
        notes: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        val targetPetId = _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        viewModelScope.launch {
            val result = repository.updateWeight(targetPetId, weightId, weightKg.toDouble(), date, notes)
            result.onSuccess {
                repository.getPetWeights(targetPetId).onSuccess { overview ->
                    _weightOverview.value = overview
                    _weightEntries.value = overview.history
                    _weightHistory.value = overview.history.map { WeightEntry(it.recordedDate, it.weightKg.toFloat()) }
                    WeightMealCacheStore.saveWeightOverview(getApplication(), targetPetId, overview)
                    overview.currentWeight?.let { latest ->
                        _activePet.value = _activePet.value.copy(weight = "$latest kg")
                    }
                }
                onResult(true, "Weight record updated")
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to update weight record")
            }
        }
    }

    fun deleteWeightEntry(weightId: Int, onResult: (Boolean, String) -> Unit) {
        val targetPetId = _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        viewModelScope.launch {
            val result = repository.deleteWeight(targetPetId, weightId)
            result.onSuccess {
                repository.getPetWeights(targetPetId).onSuccess { overview ->
                    _weightOverview.value = overview
                    _weightEntries.value = overview.history
                    _weightHistory.value = overview.history.map { WeightEntry(it.recordedDate, it.weightKg.toFloat()) }
                    WeightMealCacheStore.saveWeightOverview(getApplication(), targetPetId, overview)
                    val latest = overview.currentWeight
                    if (latest != null) {
                        _activePet.value = _activePet.value.copy(weight = "$latest kg")
                    }
                }
                onResult(true, "Weight record deleted")
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to delete weight record")
            }
        }
    }

    fun updateTargetWeight(targetKg: Double?, onResult: (Boolean, String) -> Unit) {
        val targetPetId = _activePet.value.id.toIntOrNull() ?: session.getActivePetId()
        viewModelScope.launch {
            val result = repository.updateTargetWeight(targetPetId, targetKg)
            result.onSuccess {
                repository.getPetWeights(targetPetId).onSuccess { overview ->
                    _weightOverview.value = overview
                    _weightEntries.value = overview.history
                    WeightMealCacheStore.saveWeightOverview(getApplication(), targetPetId, overview)
                }
                onResult(true, if (targetKg != null) "Target weight updated" else "Target weight cleared")
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to update target weight")
            }
        }
    }

    // ----------------------------------------------------
    // Health & Diagnostics
    // ----------------------------------------------------
    fun loadSymptoms() {
        viewModelScope.launch {
            repository.getSymptoms().onSuccess { list ->
                if (list.isNotEmpty()) {
                    _symptomsList.value = list
                }
            }
        }
    }

    fun runDiagnosticScan(symptoms: List<String>, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.runHealthScan(symptoms)
            _isLoading.value = false
            result.onSuccess { scanResult ->
                _diagnosticResult.value = scanResult
                onComplete()
            }.onFailure {
                onComplete()
            }
        }
    }

    fun loadConditionDetail(id: Int = 1) {
        viewModelScope.launch {
            repository.getConditionDetail(id).onSuccess { guide ->
                _conditionDetail.value = guide
            }
        }
    }

    fun saveReport(title: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveHealthReport(title)
            onSaved()
        }
    }

    // ----------------------------------------------------
    // Guides & Safe Foods
    // ----------------------------------------------------
    fun loadSafeToxicFoods() {
        viewModelScope.launch {
            repository.getSafeToxicFoods().onSuccess { foods ->
                if (foods.isNotEmpty()) {
                    _safeToxicFoods.value = foods
                }
            }
        }
    }

    fun loadCalendarTasks() {
        viewModelScope.launch {
            repository.getCalendarSchedule().onSuccess { tasks ->
                if (tasks.isNotEmpty()) {
                    _calendarTasks.value = tasks
                }
            }
        }
    }

    fun loadGroomingGuides() {
        viewModelScope.launch {
            repository.getGroomingGuides().onSuccess { guides ->
                if (guides.isNotEmpty()) {
                    _groomingGuides.value = guides
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Companion object for simple access across activities
    companion object {
        @Volatile
        private var sharedViewModel: PetViewModel? = null

        fun getShared(context: Context): PetViewModel {
            return sharedViewModel ?: synchronized(this) {
                sharedViewModel ?: PetViewModel(context.applicationContext as Application).also {
                    sharedViewModel = it
                }
            }
        }

        fun resetShared() {
            sharedViewModel = null
        }
    }
}
