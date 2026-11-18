package com.kavya.petcare.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kavya.petcare.data.models.MealEntryDto
import com.kavya.petcare.data.models.WeightOverviewDto

object WeightMealCacheStore {
    private const val PREF_NAME = "petcare_weight_meal_cache"
    private const val KEY_WEIGHT_PREFIX = "weight_overview_"
    private const val KEY_MEALS_PREFIX = "meal_history_"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveWeightOverview(context: Context, petId: Int, overview: WeightOverviewDto) {
        val json = gson.toJson(overview)
        getPrefs(context).edit().putString(KEY_WEIGHT_PREFIX + petId, json).apply()
    }

    fun getWeightOverview(context: Context, petId: Int): WeightOverviewDto? {
        val json = getPrefs(context).getString(KEY_WEIGHT_PREFIX + petId, null) ?: return null
        return try {
            gson.fromJson(json, WeightOverviewDto::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun saveMealHistory(context: Context, petId: Int, meals: List<MealEntryDto>) {
        val json = gson.toJson(meals)
        getPrefs(context).edit().putString(KEY_MEALS_PREFIX + petId, json).apply()
    }

    fun getMealHistory(context: Context, petId: Int): List<MealEntryDto> {
        val json = getPrefs(context).getString(KEY_MEALS_PREFIX + petId, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MealEntryDto>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
