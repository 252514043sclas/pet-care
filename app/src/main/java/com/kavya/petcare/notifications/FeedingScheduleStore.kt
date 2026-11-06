package com.kavya.petcare.notifications

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kavya.petcare.data.models.MealScheduleDto

object FeedingScheduleStore {
    private const val PREF_NAME = "petcare_feeding_schedules_cache"
    private const val KEY_SCHEDULE_PREFIX = "pet_schedules_"
    private const val KEY_PET_NAME_PREFIX = "pet_name_"
    private const val KEY_ALL_PET_IDS = "all_registered_pet_ids"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun savePetName(context: Context, petId: Int, petName: String) {
        if (petName.isNotBlank()) {
            getPrefs(context).edit().putString(KEY_PET_NAME_PREFIX + petId, petName).apply()
        }
    }

    fun getPetName(context: Context, petId: Int): String {
        return getPrefs(context).getString(KEY_PET_NAME_PREFIX + petId, null) ?: "Your Pet"
    }

    fun saveSchedules(context: Context, petId: Int, schedules: List<MealScheduleDto>, petName: String? = null) {
        val prefs = getPrefs(context)
        val json = gson.toJson(schedules)
        val allIds = getAllStoredPetIds(context).toMutableSet().apply { add(petId) }

        val editor = prefs.edit()
            .putString(KEY_SCHEDULE_PREFIX + petId, json)
            .putStringSet(KEY_ALL_PET_IDS, allIds.map { it.toString() }.toSet())
        if (!petName.isNullOrBlank()) {
            editor.putString(KEY_PET_NAME_PREFIX + petId, petName)
        }
        editor.apply()
    }

    fun getSchedules(context: Context, petId: Int): List<MealScheduleDto> {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_SCHEDULE_PREFIX + petId, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MealScheduleDto>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun updateScheduleInCache(context: Context, petId: Int, updated: MealScheduleDto) {
        val existing = getSchedules(context, petId).toMutableList()
        val index = existing.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            existing[index] = updated
        } else {
            existing.add(updated)
        }
        saveSchedules(context, petId, existing)
    }

    fun deleteScheduleFromCache(context: Context, petId: Int, scheduleId: Int) {
        val existing = getSchedules(context, petId).filter { it.id != scheduleId }
        saveSchedules(context, petId, existing)
    }

    fun getAllStoredPetIds(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        val set = prefs.getStringSet(KEY_ALL_PET_IDS, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
