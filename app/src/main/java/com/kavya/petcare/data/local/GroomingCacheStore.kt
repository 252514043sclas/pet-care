package com.kavya.petcare.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kavya.petcare.data.models.GroomingReminderDto

object GroomingCacheStore {
    private const val PREF_NAME = "petcare_grooming_cache"
    private const val KEY_REMINDERS = "grooming_reminders_all"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveReminders(context: Context, reminders: List<GroomingReminderDto>) {
        val json = gson.toJson(reminders)
        getPrefs(context).edit().putString(KEY_REMINDERS, json).apply()
    }

    fun getReminders(context: Context): List<GroomingReminderDto> {
        val json = getPrefs(context).getString(KEY_REMINDERS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<GroomingReminderDto>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun upsertReminder(context: Context, reminder: GroomingReminderDto) {
        val current = getReminders(context).toMutableList()
        val index = current.indexOfFirst { it.id == reminder.id }
        if (index >= 0) {
            current[index] = reminder
        } else {
            current.add(0, reminder)
        }
        saveReminders(context, current)
    }

    fun removeReminder(context: Context, reminderId: Int) {
        val current = getReminders(context).filter { it.id != reminderId }
        saveReminders(context, current)
    }

    fun updateStatus(context: Context, reminderId: Int, newStatus: String) {
        val current = getReminders(context).map {
            if (it.id == reminderId) it.copy(status = newStatus) else it
        }
        saveReminders(context, current)
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
