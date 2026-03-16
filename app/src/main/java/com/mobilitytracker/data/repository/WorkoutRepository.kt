package com.mobilitytracker.data.repository

import com.mobilitytracker.data.local.SettingsDao
import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.data.model.SettingsKeys
import com.mobilitytracker.data.model.UserSettings
import com.mobilitytracker.data.model.WorkoutDefinition
import com.mobilitytracker.data.model.WORKOUT_PLAN
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val settingsDao: SettingsDao
) {
    fun getAllWorkouts(): List<WorkoutDefinition> = WORKOUT_PLAN

    fun getWorkoutByOrder(order: Int): WorkoutDefinition? =
        WORKOUT_PLAN.find { it.order == order }

    suspend fun getProgramStartDate(): LocalDate? {
        val raw = settingsDao.getValue(SettingsKeys.PROGRAM_START_DATE) ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    suspend fun setProgramStartDate(date: LocalDate) {
        settingsDao.upsert(UserSettings(SettingsKeys.PROGRAM_START_DATE, date.toString()))
    }

    fun observeProgramStartDate(): Flow<String?> =
        settingsDao.observeValue(SettingsKeys.PROGRAM_START_DATE)

    fun calculateCurrentDay(startDate: LocalDate, today: LocalDate = LocalDate.now()): Int {
        return ChronoUnit.DAYS.between(startDate, today).toInt()
    }

    fun totalDaysToCycleAndVideo(totalDays: Int): Pair<Int, Int> {
        val cycleNumber = (totalDays / CYCLE_LENGTH) + 1
        val videoOrder = (totalDays % CYCLE_LENGTH) + 1
        return Pair(cycleNumber, videoOrder)
    }

    suspend fun getReminderSettings(): Triple<Boolean, Int, Int> {
        val enabled = settingsDao.getValue(SettingsKeys.REMINDER_ENABLED)?.toBoolean() ?: false
        val hour = settingsDao.getValue(SettingsKeys.REMINDER_HOUR)?.toIntOrNull() ?: 8
        val minute = settingsDao.getValue(SettingsKeys.REMINDER_MINUTE)?.toIntOrNull() ?: 0
        return Triple(enabled, hour, minute)
    }

    suspend fun saveReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        settingsDao.upsert(UserSettings(SettingsKeys.REMINDER_ENABLED, enabled.toString()))
        settingsDao.upsert(UserSettings(SettingsKeys.REMINDER_HOUR, hour.toString()))
        settingsDao.upsert(UserSettings(SettingsKeys.REMINDER_MINUTE, minute.toString()))
    }
}
