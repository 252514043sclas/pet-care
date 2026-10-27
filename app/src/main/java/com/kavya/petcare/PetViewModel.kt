package com.kavya.petcare

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class PetProfile(
    val id: String = "1",
    val name: String = "Clover",
    val type: String = "Rabbit",
    val breed: String = "Holland Lop",
    val age: String = "1.2 yrs",
    val weight: String = "1.8 kg",
    val healthScore: Int = 95
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

class PetViewModel : ViewModel() {
    private val _activePet = MutableStateFlow(PetProfile())
    val activePet: StateFlow<PetProfile> = _activePet.asStateFlow()

    private val _reminders = MutableStateFlow(
        listOf(
            CareReminder("1", "Morning Timothy Hay & Greens", "8:00 AM"),
            CareReminder("2", "Fur Brushing & Nail Check", "5:30 PM"),
            CareReminder("3", "Evening Fresh Water Refill", "8:00 PM")
        )
    )
    val reminders: StateFlow<List<CareReminder>> = _reminders.asStateFlow()

    private val _meals = MutableStateFlow(
        listOf(
            MealEntry("1", "Fresh Timothy Hay", 150, "8:00 AM"),
            MealEntry("2", "Romaine Lettuce & Herbs", 45, "12:30 PM"),
            MealEntry("3", "Pellet Portion", 80, "6:00 PM")
        )
    )
    val meals: StateFlow<List<MealEntry>> = _meals.asStateFlow()

    private val _weightHistory = MutableStateFlow(
        listOf(
            WeightEntry("Jan 1", 1.6f),
            WeightEntry("Feb 1", 1.7f),
            WeightEntry("Mar 1", 1.8f)
        )
    )
    val weightHistory: StateFlow<List<WeightEntry>> = _weightHistory.asStateFlow()

    fun updatePetProfile(name: String, breed: String, age: String, weight: String, type: String) {
        _activePet.value = _activePet.value.copy(
            name = name,
            breed = breed,
            age = age,
            weight = weight,
            type = type
        )
    }

    fun addReminder(title: String, time: String) {
        val newReminder = CareReminder(
            id = UUID.randomUUID().toString(),
            title = title,
            time = time
        )
        _reminders.value = _reminders.value + newReminder
    }

    fun toggleReminder(id: String) {
        _reminders.value = _reminders.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    fun addMeal(mealName: String, calories: Int, time: String) {
        val newMeal = MealEntry(
            id = UUID.randomUUID().toString(),
            mealName = mealName,
            calories = calories,
            time = time
        )
        _meals.value = _meals.value + newMeal
    }

    fun addWeightEntry(date: String, weightKg: Float) {
        val newEntry = WeightEntry(date, weightKg)
        _weightHistory.value = _weightHistory.value + newEntry
        _activePet.value = _activePet.value.copy(weight = "$weightKg kg")
    }
}
