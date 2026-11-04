package com.kavya.petcare

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kavya.petcare.data.models.*
import com.kavya.petcare.data.repository.PetCareRepository
import com.kavya.petcare.data.session.SessionManager
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

    // Meals StateFlow
    private val _meals = MutableStateFlow<List<MealEntry>>(
        listOf(
            MealEntry("1", "Fresh Timothy Hay", 150, "8:00 AM"),
            MealEntry("2", "Romaine Lettuce & Herbs", 45, "12:30 PM"),
            MealEntry("3", "Pellet Portion", 80, "6:00 PM")
        )
    )
    val meals: StateFlow<List<MealEntry>> = _meals.asStateFlow()

    // Weight History StateFlow
    private val _weightHistory = MutableStateFlow<List<WeightEntry>>(
        listOf(
            WeightEntry("Jan 1", 1.6f),
            WeightEntry("Feb 1", 1.7f),
            WeightEntry("Mar 1", 1.8f)
        )
    )
    val weightHistory: StateFlow<List<WeightEntry>> = _weightHistory.asStateFlow()

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
    fun createPetProfile(name: String, breed: String, age: String, weight: String, type: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val weightKg = weight.toDoubleOrNull() ?: 1.8
            val result = repository.createPet(name, type, breed, age, weightKg)
            _isLoading.value = false
            result.onSuccess { pet ->
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
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.uploadPetPhoto(petId, base64)
            _isLoading.value = false
            result.onSuccess { data ->
                _activePet.value = _activePet.value.copy(imageUrl = data.imageUrl)
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
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.uploadUserAvatar(base64)
            _isLoading.value = false
            result.onSuccess { avatarUrl ->
                _userAvatarUrl.value = avatarUrl
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
    // Meal Operations
    // ----------------------------------------------------
    fun addMeal(mealName: String, calories: Int, time: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val newMeal = MealEntry(
                id = java.util.UUID.randomUUID().toString(),
                mealName = mealName,
                calories = calories,
                time = time
            )
            _meals.value = _meals.value + newMeal

            repository.logMeal(mealName, "1.0 Cup", calories).onSuccess {
                refreshDashboard()
            }
            onDone?.invoke()
        }
    }

    // ----------------------------------------------------
    // Weight Operations
    // ----------------------------------------------------
    fun addWeightEntry(date: String, weightKg: Float, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val newEntry = WeightEntry(date, weightKg)
            _weightHistory.value = _weightHistory.value + newEntry
            _activePet.value = _activePet.value.copy(weight = "$weightKg kg")

            val activeId = _activePet.value.id.toIntOrNull() ?: 1
            repository.addWeight(activeId, weightKg.toDouble(), date)
            onDone?.invoke()
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
    }
}
