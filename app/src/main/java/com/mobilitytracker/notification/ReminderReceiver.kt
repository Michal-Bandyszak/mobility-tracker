package com.mobilitytracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobilitytracker.data.model.WORKOUT_PLAN
import com.mobilitytracker.data.repository.CompletionRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "com.mobilitytracker.ACTION_REMINDER"
        const val ACTION_MARK_DONE = "com.mobilitytracker.ACTION_MARK_DONE"
        const val EXTRA_CYCLE = "extra_cycle"
        const val EXTRA_VIDEO_ORDER = "extra_video_order"
        const val REQUEST_CODE = 2001
    }

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var completionRepository: CompletionRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        when (intent.action) {
            ACTION_REMINDER -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        handleReminder(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_MARK_DONE -> {
                val cycle = intent.getIntExtra(EXTRA_CYCLE, 0)
                val videoOrder = intent.getIntExtra(EXTRA_VIDEO_ORDER, 0)
                if (cycle > 0 && videoOrder > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            completionRepository.markCompleted(cycle, videoOrder)
                            NotificationHelper.cancel(context)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    pendingResult.finish()
                }
            }
            else -> pendingResult.finish()
        }
    }

    private suspend fun handleReminder(context: Context) {
        NotificationHelper.createChannel(context)

        val startDate = workoutRepository.getProgramStartDate() ?: return
        val totalDays = workoutRepository.calculateCurrentDay(startDate, LocalDate.now())
            .coerceAtLeast(0)
        val (cycleNumber, videoOrder) = workoutRepository.totalDaysToCycleAndVideo(totalDays)

        if (!completionRepository.isCompleted(cycleNumber, videoOrder)) {
            val definition = WORKOUT_PLAN.find { it.order == videoOrder } ?: return
            NotificationHelper.showReminder(
                context = context,
                title = "Czas na trening mobilności!",
                workoutTitle = "Dziś: ${definition.title}",
                cycleNumber = cycleNumber,
                videoOrder = videoOrder
            )
        }

        val (enabled, hour, minute) = workoutRepository.getReminderSettings()
        if (enabled) {
            alarmScheduler.scheduleDaily(hour, minute)
        }
    }
}
