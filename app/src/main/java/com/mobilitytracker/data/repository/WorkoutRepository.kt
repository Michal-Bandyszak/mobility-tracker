package com.mobilitytracker.data.repository

import com.mobilitytracker.data.local.SettingsDao
import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.data.model.DEFAULT_MOBILITY_DAYS
import com.mobilitytracker.data.model.DEFAULT_STRENGTH_DAYS
import com.mobilitytracker.data.model.SettingsKeys
import com.mobilitytracker.data.model.UserSettings
import com.mobilitytracker.data.model.WorkoutDefinition
import com.mobilitytracker.data.model.WORKOUT_PLAN
import com.mobilitytracker.data.model.countScheduledSessions
import com.mobilitytracker.data.model.decodeStrengthDayPlanAssignments
import com.mobilitytracker.data.model.decodeTrainingDays
import com.mobilitytracker.data.model.encodeStrengthDayPlanAssignments
import com.mobilitytracker.data.model.encodeTrainingDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
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

    suspend fun clearProgramStartDate() {
        settingsDao.delete(SettingsKeys.PROGRAM_START_DATE)
    }

    suspend fun clearProgramFlowSettings() {
        settingsDao.delete(SettingsKeys.PROGRAM_START_DATE)
        settingsDao.delete(SettingsKeys.MOBILITY_ANCHOR_DATE)
        settingsDao.delete(SettingsKeys.MOBILITY_SESSIONS_AT_ANCHOR)
        settingsDao.delete(SettingsKeys.STRENGTH_ANCHOR_DATE)
        settingsDao.delete(SettingsKeys.STRENGTH_SESSIONS_AT_ANCHOR)
        clearStrengthPlanOverrides()
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

    fun sessionNumberToCycleAndVideo(sessionNumber: Int): Pair<Int, Int> =
        totalDaysToCycleAndVideo((sessionNumber - 1).coerceAtLeast(0))

    suspend fun getMobilityDays(): Set<DayOfWeek> =
        decodeTrainingDays(settingsDao.getValue(SettingsKeys.MOBILITY_DAYS), DEFAULT_MOBILITY_DAYS)

    suspend fun getStrengthDays(): Set<DayOfWeek> =
        getStrengthDayPlanAssignments().keys

    suspend fun getStrengthDayPlanAssignments(): Map<DayOfWeek, Int> {
        val raw = settingsDao.getValue(SettingsKeys.STRENGTH_DAY_PLAN_ASSIGNMENTS)
        if (raw != null) return decodeStrengthDayPlanAssignments(raw)

        val legacyDays = decodeTrainingDays(
            settingsDao.getValue(SettingsKeys.STRENGTH_DAYS),
            DEFAULT_STRENGTH_DAYS
        )
        if (legacyDays == DEFAULT_STRENGTH_DAYS) return decodeStrengthDayPlanAssignments(null)

        return legacyDays.sortedBy { it.value }
            .mapIndexed { index, day -> day to ((index % 2) + 1) }
            .toMap()
    }

    suspend fun getMobilityAnchor(): Pair<LocalDate, Int>? {
        val dateRaw = settingsDao.getValue(SettingsKeys.MOBILITY_ANCHOR_DATE)
        val sessionsRaw = settingsDao.getValue(SettingsKeys.MOBILITY_SESSIONS_AT_ANCHOR)
        if (dateRaw != null && sessionsRaw != null) {
            return Pair(LocalDate.parse(dateRaw), sessionsRaw.toInt())
        }
        return null
    }

    suspend fun getStrengthAnchor(): Pair<LocalDate, Int>? {
        val dateRaw = settingsDao.getValue(SettingsKeys.STRENGTH_ANCHOR_DATE)
        val sessionsRaw = settingsDao.getValue(SettingsKeys.STRENGTH_SESSIONS_AT_ANCHOR)
        if (dateRaw != null && sessionsRaw != null) {
            return Pair(LocalDate.parse(dateRaw), sessionsRaw.toInt())
        }
        return null
    }

    suspend fun getTotalMobilitySessions(targetDate: LocalDate): Int {
        val startDate = getProgramStartDate() ?: return 0
        if (targetDate.isBefore(startDate)) return 0
        
        val days = getMobilityDays()
        val anchor = getMobilityAnchor()
        
        if (anchor != null && !targetDate.isBefore(anchor.first)) {
            val sessionsSinceAnchor = countScheduledSessions(anchor.first.plusDays(1), targetDate, days)
            return anchor.second + sessionsSinceAnchor
        } else {
            return countScheduledSessions(startDate, targetDate, days)
        }
    }
    
    suspend fun getTotalStrengthSessions(targetDate: LocalDate): Int {
        val startDate = getProgramStartDate() ?: return 0
        if (targetDate.isBefore(startDate)) return 0

        val anchor = getStrengthAnchor()
        val assignments = getStrengthDayPlanAssignments()
        val overrides = getStrengthPlanOverrides()

        if (anchor != null && !targetDate.isBefore(anchor.first)) {
            val sessionsSinceAnchor = countStrengthSessions(
                startDate = anchor.first.plusDays(1),
                targetDate = targetDate,
                assignments = assignments,
                overrides = overrides
            )
            return anchor.second + sessionsSinceAnchor
        } else {
            return countStrengthSessions(
                startDate = startDate,
                targetDate = targetDate,
                assignments = assignments,
                overrides = overrides
            )
        }
    }

    suspend fun saveMobilityDays(days: Set<DayOfWeek>) {
        val currentDays = getMobilityDays()
        if (currentDays == days) return
        
        val today = LocalDate.now()
        val startDate = getProgramStartDate()
        if (startDate != null && !today.isBefore(startDate)) {
            val sessionsSoFar = getTotalMobilitySessions(today.minusDays(1))
            settingsDao.upsert(UserSettings(SettingsKeys.MOBILITY_ANCHOR_DATE, today.minusDays(1).toString()))
            settingsDao.upsert(UserSettings(SettingsKeys.MOBILITY_SESSIONS_AT_ANCHOR, sessionsSoFar.toString()))
        }
        
        settingsDao.upsert(UserSettings(SettingsKeys.MOBILITY_DAYS, encodeTrainingDays(days)))
    }

    suspend fun saveStrengthDays(days: Set<DayOfWeek>) {
        val assignments = days.sortedBy { it.value }
            .mapIndexed { index, day -> day to ((index % 2) + 1) }
            .toMap()
        saveStrengthDayPlanAssignments(assignments)
    }

    suspend fun saveStrengthDayPlanAssignments(assignments: Map<DayOfWeek, Int>) {
        val normalized = assignments
            .filterValues { it in 1..2 }
            .toSortedMap(compareBy { it.value })
        val currentDays = getStrengthDays()
        val updatedDays = normalized.keys
        if (currentDays != updatedDays) {
            val today = LocalDate.now()
            val startDate = getProgramStartDate()
            if (startDate != null && !today.isBefore(startDate)) {
                val sessionsSoFar = getTotalStrengthSessions(today.minusDays(1))
                settingsDao.upsert(UserSettings(SettingsKeys.STRENGTH_ANCHOR_DATE, today.minusDays(1).toString()))
                settingsDao.upsert(UserSettings(SettingsKeys.STRENGTH_SESSIONS_AT_ANCHOR, sessionsSoFar.toString()))
            }
        }

        settingsDao.upsert(
            UserSettings(
                SettingsKeys.STRENGTH_DAY_PLAN_ASSIGNMENTS,
                encodeStrengthDayPlanAssignments(normalized)
            )
        )
        settingsDao.upsert(UserSettings(SettingsKeys.STRENGTH_DAYS, encodeTrainingDays(updatedDays)))
    }

    fun observeMobilityDays(): Flow<Set<DayOfWeek>> =
        settingsDao.observeValue(SettingsKeys.MOBILITY_DAYS)
            .map { decodeTrainingDays(it, DEFAULT_MOBILITY_DAYS) }

    fun observeStrengthDays(): Flow<Set<DayOfWeek>> =
        settingsDao.observeValue(SettingsKeys.STRENGTH_DAY_PLAN_ASSIGNMENTS)
            .map { decodeStrengthDayPlanAssignments(it).keys }

    fun observeStrengthDayPlanAssignments(): Flow<Map<DayOfWeek, Int>> =
        settingsDao.observeValue(SettingsKeys.STRENGTH_DAY_PLAN_ASSIGNMENTS)
            .map { decodeStrengthDayPlanAssignments(it) }

    suspend fun getStrengthPlanDayForDate(date: LocalDate): Int? =
        getStrengthPlanOverrideForDate(date) ?: getStrengthDayPlanAssignments()[date.dayOfWeek]

    suspend fun saveStrengthPlanOverrideForDate(date: LocalDate, planDay: Int) {
        if (planDay !in 1..2) return
        settingsDao.upsert(
            UserSettings(
                key = strengthPlanOverrideKey(date),
                value = planDay.toString()
            )
        )
    }

    suspend fun clearStrengthPlanOverrideForDate(date: LocalDate) {
        settingsDao.delete(strengthPlanOverrideKey(date))
    }

    private suspend fun clearStrengthPlanOverrides() {
        settingsDao.getAllSettings()
            .filter { it.key.startsWith(SettingsKeys.STRENGTH_PLAN_OVERRIDE_PREFIX) }
            .forEach { settingsDao.delete(it.key) }
    }

    private suspend fun getStrengthPlanOverrideForDate(date: LocalDate): Int? =
        settingsDao.getValue(strengthPlanOverrideKey(date))?.toIntOrNull()?.takeIf { it in 1..2 }

    private suspend fun getStrengthPlanOverrides(): Map<LocalDate, Int> =
        settingsDao.getAllSettings()
            .asSequence()
            .filter { it.key.startsWith(SettingsKeys.STRENGTH_PLAN_OVERRIDE_PREFIX) }
            .mapNotNull { setting ->
                val dateRaw = setting.key.removePrefix(SettingsKeys.STRENGTH_PLAN_OVERRIDE_PREFIX)
                val date = runCatching { LocalDate.parse(dateRaw) }.getOrNull()
                val planDay = setting.value.toIntOrNull()?.takeIf { it in 1..2 }
                if (date != null && planDay != null) date to planDay else null
            }
            .toMap()

    private fun countStrengthSessions(
        startDate: LocalDate,
        targetDate: LocalDate,
        assignments: Map<DayOfWeek, Int>,
        overrides: Map<LocalDate, Int>
    ): Int {
        if (targetDate.isBefore(startDate)) return 0

        var count = 0
        var date = startDate
        while (!date.isAfter(targetDate)) {
            if (date in overrides || date.dayOfWeek in assignments) count++
            date = date.plusDays(1)
        }
        return count
    }

    private fun strengthPlanOverrideKey(date: LocalDate): String =
        SettingsKeys.STRENGTH_PLAN_OVERRIDE_PREFIX + date.toString()

    fun calculateScheduledSessions(
        startDate: LocalDate,
        targetDate: LocalDate,
        days: Set<DayOfWeek>
    ): Int = countScheduledSessions(startDate, targetDate, days)

    suspend fun getCurrentMobilityPosition(today: LocalDate = LocalDate.now()): Pair<Int, Int>? {
        val startDate = getProgramStartDate() ?: return null
        if (today.isBefore(startDate)) return null

        val sessions = getTotalMobilitySessions(today)
        if (sessions <= 0) return Pair(1, 1)

        return sessionNumberToCycleAndVideo(sessions)
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
