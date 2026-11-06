package com.kavya.petcare.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MealBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MealBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast action: $action")

        val validActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )

        if (action in validActions) {
            val petIds = FeedingScheduleStore.getAllStoredPetIds(context)
            Log.d(TAG, "Restoring feeding schedule alarms for pets: $petIds")

            petIds.forEach { petId ->
                val schedules = FeedingScheduleStore.getSchedules(context, petId)
                val petName = FeedingScheduleStore.getPetName(context, petId)

                if (schedules.isNotEmpty()) {
                    Log.d(TAG, "Re-scheduling ${schedules.size} meals for pet $petName (ID: $petId)")
                    MealNotificationScheduler.rescheduleAllForPet(
                        context = context,
                        petId = petId,
                        petName = petName,
                        schedules = schedules
                    )
                }
            }
        }
    }
}

