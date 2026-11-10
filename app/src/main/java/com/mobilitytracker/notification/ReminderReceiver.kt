package com.mobilitytracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobilitytracker.data.model.WORKOUT_PLAN
import com.mobilitytracker.data.model.getStrengthWorkoutByDay
import com.mobilitytracker.data.repository.CompletionRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.domain.model.buildMobilityChallengeState
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
        const val EXTRA_REMINDER_SLOT = "extra_reminder_slot"
        const val REQUEST_CODE = 2001
    }

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var completionRepository: CompletionRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        when (intent.action) {
            ACTION_REMINDER -> {
                val reminderSlot = intent.getIntExtra(EXTRA_REMINDER_SLOT, 0)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        handleReminder(context, reminderSlot)
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

    private suspend fun handleReminder(context: Context, reminderSlot: Int) {
        NotificationHelper.createChannel(context)

        val (enabled, hour, minute) = workoutRepository.getReminderSettings()
        if (!enabled) return
        alarmScheduler.scheduleDaily(hour, minute)

        val startDate = workoutRepository.getProgramStartDate() ?: return
        val today = LocalDate.now()
        if (today.isBefore(startDate)) return

        val mobilityReminder = buildMobilityReminder(today)
        val strengthReminder = buildStrengthReminder(today)

        if (mobilityReminder != null || strengthReminder != null) {
            val title = when {
                mobilityReminder != null && strengthReminder != null -> "Najpierw mobilność, potem siła"
                mobilityReminder != null -> "Nie odkładaj mobilności"
                else -> "Nie odkładaj treningu siłowego"
            }
            val workoutTitle = listOfNotNull(
                mobilityReminder?.title,
                strengthReminder
            ).joinToString(" + ")

            NotificationHelper.showReminder(
                context = context,
                title = title,
                workoutTitle = workoutTitle,
                cycleNumber = mobilityReminder?.cycleNumber,
                videoOrder = mobilityReminder?.videoOrder,
                reminderSlot = reminderSlot
            )
        }
    }

    private suspend fun buildMobilityReminder(today: LocalDate): MobilityReminder? {
        val challenge = buildMobilityChallengeState(completionRepository.getAllCompletionsSync(), today)
        if (challenge.completedToday) return null

        val cycleNumber = challenge.activeCycleNumber
        val videoOrder = challenge.activeVideoOrder

        val definition = WORKOUT_PLAN.find { it.order == videoOrder } ?: return null
        return MobilityReminder(
            title = "Sesja ${challenge.completedSessions + 1}/84: ${definition.title} • seria ${challenge.currentStreak} dni",
            cycleNumber = cycleNumber,
            videoOrder = videoOrder
        )
    }

    private suspend fun buildStrengthReminder(today: LocalDate): String? {
        val planDay = workoutRepository.getStrengthPlanDayForDate(today) ?: return null

        val definition = getStrengthWorkoutByDay(planDay) ?: return null
        return "Siłownia: ${definition.title}"
    }

    private data class MobilityReminder(
        val title: String,
        val cycleNumber: Int,
        val videoOrder: Int
    )
}
