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
import com.kavya.petcare.GroomingCalendarActivity
import com.kavya.petcare.R
import com.kavya.petcare.data.local.GroomingCacheStore

class GroomingReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GroomingReminderRcvr"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GroomingNotificationScheduler.ACTION_GROOMING_REMINDER) {
            return
        }

        val reminderId = intent.getIntExtra(GroomingNotificationScheduler.EXTRA_REMINDER_ID, -1)
        val petId = intent.getIntExtra(GroomingNotificationScheduler.EXTRA_PET_ID, -1)
        val petName = intent.getStringExtra(GroomingNotificationScheduler.EXTRA_PET_NAME) ?: "Your Pet"
        val activity = intent.getStringExtra(GroomingNotificationScheduler.EXTRA_ACTIVITY_NAME) ?: "Care Activity"
        val reminderDate = intent.getStringExtra(GroomingNotificationScheduler.EXTRA_REMINDER_DATE) ?: ""
        val reminderTime = intent.getStringExtra(GroomingNotificationScheduler.EXTRA_REMINDER_TIME) ?: ""

        Log.d(TAG, "Grooming reminder received: $activity for pet $petName (ID: $reminderId)")

        if (reminderId == -1) {
            return
        }

        // Verify if reminder is still enabled and not completed in local cache
        val cached = GroomingCacheStore.getReminders(context)
        val currentReminder = cached.find { it.id == reminderId }
        if (currentReminder != null) {
            if (!currentReminder.notificationEnabled || currentReminder.isCompleted) {
                Log.d(TAG, "Reminder $reminderId is disabled or completed, skipping notification")
                return
            }
        }

        // Ensure notification channel exists
        GroomingNotificationScheduler.createNotificationChannel(context)

        val title = "Grooming Care: $activity"
        val body = "Time for $petName's $activity routine! ($reminderTime)"

        val contentIntent = Intent(context, GroomingCalendarActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("reminder_id", reminderId)
            putExtra("pet_id", petId)
        }

        val requestCode = GroomingNotificationScheduler.getRequestCode(reminderId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GroomingNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pet_default)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                NotificationManagerCompat.from(context).notify(requestCode, notification)
                Log.d(TAG, "Posted notification for grooming activity '$activity'")
            } catch (se: SecurityException) {
                Log.e(TAG, "SecurityException while posting notification: ${se.message}")
            }
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping alert display")
        }
    }
}
