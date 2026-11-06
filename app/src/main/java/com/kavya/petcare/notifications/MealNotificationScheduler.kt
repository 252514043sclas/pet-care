package com.kavya.petcare.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.kavya.petcare.data.models.MealScheduleDto
import java.util.Calendar

object MealNotificationScheduler {
    private const val TAG = "MealNotifScheduler"
    const val CHANNEL_ID = "feeding_schedule_channel"
    const val CHANNEL_NAME = "Pet Feeding Reminders"
    const val CHANNEL_DESC = "Daily notifications for your pet's feeding schedule"

    const val ACTION_MEAL_REMINDER = "com.kavya.petcare.ACTION_MEAL_REMINDER"

    const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
    const val EXTRA_PET_ID = "EXTRA_PET_ID"
    const val EXTRA_PET_NAME = "EXTRA_PET_NAME"
    const val EXTRA_MEAL_NAME = "EXTRA_MEAL_NAME"
    const val EXTRA_FOOD_NAME = "EXTRA_FOOD_NAME"
    const val EXTRA_FOOD_QUANTITY = "EXTRA_FOOD_QUANTITY"
    const val EXTRA_SCHEDULED_TIME = "EXTRA_SCHEDULED_TIME"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun getRequestCode(petId: Int, scheduleId: Int): Int {
        val safePetId = if (petId > 0) petId else 1
        return (safePetId * 1000) + scheduleId
    }

    /**
     * Parses human-readable time strings such as:
     * "8:00 AM", "1:00 PM", "4:30 PM", "7:00 PM", "08:00", "13:00", "Morning • 8:00 AM"
     * into a Calendar object set for today or tomorrow if already past.
     */
    fun parseTimeToCalendar(timeStr: String): Calendar {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        val timeRegex = Regex("(?i)(\\d{1,2}):(\\d{2})\\s*(AM|PM)?")
        val match = timeRegex.find(timeStr)

        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: 8
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val amPm = match.groupValues[3].uppercase()

            if (amPm == "PM" && hour < 12) {
                hour += 12
            } else if (amPm == "AM" && hour == 12) {
                hour = 0
            }

            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        } else {
            // Default fallback: 8:00 AM
            calendar.set(Calendar.HOUR_OF_DAY, 8)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        // If time has already passed today, advance to tomorrow
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar
    }

    fun scheduleMealReminder(
        context: Context,
        schedule: MealScheduleDto,
        petName: String,
        petId: Int
    ) {
        if (schedule.isReminderEnabled != 1) {
            cancelMealReminder(context, petId, schedule.id)
            return
        }

        createNotificationChannel(context)

        val calendar = parseTimeToCalendar(schedule.scheduledTime)
        val triggerTime = calendar.timeInMillis
        val requestCode = getRequestCode(petId, schedule.id)

        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            action = ACTION_MEAL_REMINDER
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_PET_ID, petId)
            putExtra(EXTRA_PET_NAME, petName)
            putExtra(EXTRA_MEAL_NAME, schedule.mealName)
            putExtra(EXTRA_FOOD_NAME, schedule.foodName)
            putExtra(EXTRA_FOOD_QUANTITY, schedule.foodQuantity)
            putExtra(EXTRA_SCHEDULED_TIME, schedule.scheduledTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled reminder for ${schedule.mealName} (ID: ${schedule.id}, Pet: $petName) at ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm: ${e.message}, falling back", e)
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback alarm failed: ${ex.message}", ex)
            }
        }
    }

    /**
     * Re-arms next day's alarm for recurring daily schedule.
     */
    fun scheduleNextDailyAlarm(
        context: Context,
        scheduleId: Int,
        petId: Int,
        petName: String,
        mealName: String,
        foodName: String,
        foodQuantity: String,
        scheduledTime: String
    ) {
        val calendar = parseTimeToCalendar(scheduledTime)
        // Ensure it's in the future
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val triggerTime = calendar.timeInMillis
        val requestCode = getRequestCode(petId, scheduleId)

        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            action = ACTION_MEAL_REMINDER
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_PET_ID, petId)
            putExtra(EXTRA_PET_NAME, petName)
            putExtra(EXTRA_MEAL_NAME, mealName)
            putExtra(EXTRA_FOOD_NAME, foodName)
            putExtra(EXTRA_FOOD_QUANTITY, foodQuantity)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Re-armed daily alarm for $mealName (ID: $scheduleId, Pet: $petName) at ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-arm alarm: ${e.message}", e)
        }
    }

    fun cancelMealReminder(context: Context, petId: Int, scheduleId: Int) {
        val requestCode = getRequestCode(petId, scheduleId)
        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            action = ACTION_MEAL_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Canceled reminder for schedule ID $scheduleId (Pet ID: $petId)")
        }
    }

    fun rescheduleAllForPet(
        context: Context,
        petId: Int,
        petName: String,
        schedules: List<MealScheduleDto>
    ) {
        schedules.forEach { schedule ->
            if (schedule.isReminderEnabled == 1) {
                scheduleMealReminder(context, schedule, petName, petId)
            } else {
                cancelMealReminder(context, petId, schedule.id)
            }
        }
    }

    fun getNotificationContent(mealName: String, petName: String, foodName: String, foodQuantity: String): Pair<String, String> {
        val safePetName = if (petName.isNotBlank()) petName else "your pet"
        val normalized = mealName.trim().lowercase()

        val title: String
        val body: String

        when {
            normalized.contains("breakfast") -> {
                title = "🐾 Breakfast Time!"
                body = "It's time to feed $safePetName. Don't forget their morning meal."
            }
            normalized.contains("lunch") -> {
                title = "🐾 Lunch Time!"
                body = "$safePetName's lunchtime is here."
            }
            normalized.contains("snack") -> {
                title = "🐾 Snack Time!"
                body = "It's time for $safePetName's evening snack."
            }
            normalized.contains("dinner") -> {
                title = "🐾 Dinner Time!"
                body = "$safePetName's dinner is ready. Don't forget to feed them."
            }
            else -> {
                title = "🐾 Meal Time: $mealName!"
                body = "It's time for $safePetName's $mealName."
            }
        }

        val detail = if (foodName.isNotBlank()) {
            if (foodQuantity.isNotBlank()) "\nServing: $foodQuantity $foodName" else "\nMenu: $foodName"
        } else ""

        return Pair(title, body + detail)
    }
}

