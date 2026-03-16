package com.mobilitytracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobilitytracker.data.repository.WorkoutRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            NotificationHelper.createChannel(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val (enabled, hour, minute) = workoutRepository.getReminderSettings()
                    if (enabled) {
                        alarmScheduler.scheduleDaily(hour, minute)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
