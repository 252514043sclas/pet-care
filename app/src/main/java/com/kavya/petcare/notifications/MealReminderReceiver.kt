package com.kavya.petcare.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kavya.petcare.DietPlanActivity
import com.kavya.petcare.R

class MealReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MealReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MealNotificationScheduler.ACTION_MEAL_REMINDER) {
            return
        }

        val scheduleId = intent.getIntExtra(MealNotificationScheduler.EXTRA_SCHEDULE_ID, -1)
        val petId = intent.getIntExtra(MealNotificationScheduler.EXTRA_PET_ID, -1)
        val petName = intent.getStringExtra(MealNotificationScheduler.EXTRA_PET_NAME) ?: "Your Pet"
        val mealName = intent.getStringExtra(MealNotificationScheduler.EXTRA_MEAL_NAME) ?: "Meal"
        val foodName = intent.getStringExtra(MealNotificationScheduler.EXTRA_FOOD_NAME) ?: ""
        val foodQuantity = intent.getStringExtra(MealNotificationScheduler.EXTRA_FOOD_QUANTITY) ?: ""
        val scheduledTime = intent.getStringExtra(MealNotificationScheduler.EXTRA_SCHEDULED_TIME) ?: "8:00 AM"

        Log.d(TAG, "Meal reminder received: $mealName for pet $petName (ID: $petId, Schedule: $scheduleId)")

        if (scheduleId == -1 || petId == -1) {
            return
        }

        // Verify if reminder is still enabled in local store
        val cachedSchedules = FeedingScheduleStore.getSchedules(context, petId)
        val matchingSchedule = cachedSchedules.find { it.id == scheduleId }
        if (matchingSchedule != null && matchingSchedule.isReminderEnabled != 1) {
            Log.d(TAG, "Reminder is disabled for schedule $scheduleId, skipping notification and re-arm")
            return
        }

        // Ensure channel exists
        MealNotificationScheduler.createNotificationChannel(context)

        // Notification content
        val (title, body) = MealNotificationScheduler.getNotificationContent(
            mealName = mealName,
            petName = petName,
            foodName = foodName,
            foodQuantity = foodQuantity
        )

        // PendingIntent to launch DietPlanActivity
        val contentIntent = Intent(context, DietPlanActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("pet_id", petId)
            putExtra("pet_name", petName)
        }

        val requestCode = MealNotificationScheduler.getRequestCode(petId, scheduleId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Small icon fallback
        val iconRes = R.drawable.ic_pet_default

        val notification = NotificationCompat.Builder(context, MealNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                NotificationManagerCompat.from(context).notify(requestCode, notification)
                Log.d(TAG, "Notification successfully posted for $mealName")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException while posting notification: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; notification suppressed")
        }

        // Re-arm for the next day
        MealNotificationScheduler.scheduleNextDailyAlarm(
            context = context,
            scheduleId = scheduleId,
            petId = petId,
            petName = petName,
            mealName = mealName,
            foodName = foodName,
            foodQuantity = foodQuantity,
            scheduledTime = scheduledTime
        )
    }
}

