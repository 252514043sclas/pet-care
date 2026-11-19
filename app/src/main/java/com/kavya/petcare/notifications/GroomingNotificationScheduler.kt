package com.kavya.petcare.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.kavya.petcare.data.models.GroomingReminderDto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object GroomingNotificationScheduler {
    private const val TAG = "GroomingNotifScheduler"
    const val CHANNEL_ID = "grooming_schedule_channel"
    const val CHANNEL_NAME = "Grooming & Hygiene Reminders"
    const val CHANNEL_DESC = "Notifications for pet baths, haircuts, nail trimming, and ear cleaning"

    const val ACTION_GROOMING_REMINDER = "com.kavya.petcare.ACTION_GROOMING_REMINDER"

    const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
    const val EXTRA_PET_ID = "EXTRA_PET_ID"
    const val EXTRA_PET_NAME = "EXTRA_PET_NAME"
    const val EXTRA_ACTIVITY_NAME = "EXTRA_ACTIVITY_NAME"
    const val EXTRA_REMINDER_DATE = "EXTRA_REMINDER_DATE"
    const val EXTRA_REMINDER_TIME = "EXTRA_REMINDER_TIME"

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

    fun getRequestCode(reminderId: Int): Int {
        val safeId = if (reminderId > 0) reminderId else 1
        return 300000 + safeId
    }

    /**
     * Parses combined date and time strings into a timestamp.
     * Supports formats:
     * Date: "yyyy-MM-dd", "MMM dd, yyyy", "MMM d, yyyy", "dd MMM yyyy", "yyyy/MM/dd"
     * Time: "h:mm a", "hh:mm a", "HH:mm", "H:mm"
     */
    fun parseDateTimeToMillis(dateStr: String, timeStr: String): Long? {
        val datePatterns = listOf("yyyy-MM-dd", "MMM d, yyyy", "MMM dd, yyyy", "dd MMM yyyy", "yyyy/MM/dd")
        val timePatterns = listOf("h:mm a", "hh:mm a", "HH:mm", "H:mm")

        val cleanDate = dateStr.trim()
        val cleanTime = timeStr.trim()

        for (dPat in datePatterns) {
            for (tPat in timePatterns) {
                val fullPattern = "$dPat $tPat"
                try {
                    val sdf = SimpleDateFormat(fullPattern, Locale.US).apply {
                        isLenient = false
                    }
                    val date = sdf.parse("$cleanDate $cleanTime")
                    if (date != null) {
                        return date.time
                    }
                } catch (_: Exception) {
                    // Try next pattern
                }
            }
        }

        // Fallback: Try regex matching time into today's or specified calendar
        val timeRegex = Regex("(?i)(\\d{1,2}):(\\d{2})\\s*(AM|PM)?")
        val match = timeRegex.find(cleanTime)
        if (match != null) {
            val calendar = Calendar.getInstance()
            var hour = match.groupValues[1].toIntOrNull() ?: 9
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val amPm = match.groupValues[3].uppercase()

            if (amPm == "PM" && hour < 12) hour += 12
            else if (amPm == "AM" && hour == 12) hour = 0

            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        return null
    }

    fun scheduleReminder(context: Context, reminder: GroomingReminderDto) {
        if (!reminder.notificationEnabled || reminder.isCompleted) {
            cancelReminder(context, reminder.id)
            return
        }

        val targetMillis = parseDateTimeToMillis(reminder.reminderDate, reminder.reminderTime)
        val now = System.currentTimeMillis()

        if (targetMillis == null || targetMillis <= now) {
            Log.d(TAG, "Reminder date/time in the past or invalid (${reminder.reminderDate} ${reminder.reminderTime}), skipping exact alarm")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.w(TAG, "AlarmManager not available")
            return
        }

        createNotificationChannel(context)

        val intent = Intent(context, GroomingReminderReceiver::class.java).apply {
            action = ACTION_GROOMING_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_PET_ID, reminder.petId)
            putExtra(EXTRA_PET_NAME, reminder.petName)
            putExtra(EXTRA_ACTIVITY_NAME, reminder.activity)
            putExtra(EXTRA_REMINDER_DATE, reminder.reminderDate)
            putExtra(EXTRA_REMINDER_TIME, reminder.reminderTime)
        }

        val requestCode = getRequestCode(reminder.id)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    targetMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled grooming reminder for '${reminder.activity}' at $targetMillis")
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException: SCHEDULE_EXACT_ALARM permission missing: ${se.message}")
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    targetMillis,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.e(TAG, "Fallback alarm scheduling failed: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for reminder ID ${reminder.id}: ${e.message}")
        }
    }

    fun cancelReminder(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, GroomingReminderReceiver::class.java).apply {
            action = ACTION_GROOMING_REMINDER
        }
        val requestCode = getRequestCode(reminderId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for reminder ID: $reminderId")
        }
    }

    fun rescheduleAll(context: Context, reminders: List<GroomingReminderDto>) {
        reminders.forEach { reminder ->
            if (reminder.notificationEnabled && !reminder.isCompleted) {
                scheduleReminder(context, reminder)
            } else {
                cancelReminder(context, reminder.id)
            }
        }
    }
}
