package com.mobilitytracker.data.repository

import com.mobilitytracker.data.local.SettingsDao
import com.mobilitytracker.data.local.StrengthLogDao
import com.mobilitytracker.data.model.ExerciseCatalogEntry
import com.mobilitytracker.data.model.SettingsKeys
import com.mobilitytracker.data.model.StrengthSetLog
import com.mobilitytracker.data.model.StrengthWorkoutDefinition
import com.mobilitytracker.data.model.getStrengthWorkoutByDay
import com.mobilitytracker.data.model.getStrengthExerciseCatalog
import com.mobilitytracker.data.model.resolveStrengthWorkout
import com.mobilitytracker.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrengthRepository @Inject constructor(
    private val strengthLogDao: StrengthLogDao,
    private val settingsDao: SettingsDao
) {
    fun getAllWorkouts(): List<StrengthWorkoutDefinition> =
        listOfNotNull(getStrengthWorkoutByDay(1), getStrengthWorkoutByDay(2))

    fun getWorkoutByDay(day: Int): StrengthWorkoutDefinition? =
        getStrengthWorkoutByDay(day)

    fun getExerciseCatalog(): Collection<ExerciseCatalogEntry> = getStrengthExerciseCatalog()

    suspend fun getWorkoutForDate(planDay: Int, date: LocalDate): StrengthWorkoutDefinition? =
        resolveStrengthWorkout(planDay, getExerciseOverridesForDate(date, planDay))

    suspend fun saveExerciseOverrideForDate(
        date: LocalDate,
        planDay: Int,
        slotId: String,
        exerciseId: String
    ): Boolean {
        val current = getWorkoutForDate(planDay, date) ?: return false
        val slot = current.exercises.find { it.slotId == slotId } ?: return false
        if (exerciseId !in slot.choices.map { it.id }) return false
        if (current.exercises.any { it.slotId != slotId && it.id == exerciseId }) return false

        val key = exerciseOverrideKey(date, planDay, slotId)
        if (exerciseId == slot.defaultExerciseId) {
            settingsDao.delete(key)
        } else {
            settingsDao.upsert(UserSettings(key, exerciseId))
        }
        return true
    }

    suspend fun clearExerciseOverrideForDate(date: LocalDate, planDay: Int, slotId: String) {
        settingsDao.delete(exerciseOverrideKey(date, planDay, slotId))
    }

    fun observeLogsForDate(date: LocalDate): Flow<List<StrengthSetLog>> =
        strengthLogDao.observeLogsForDate(date.toString())

    fun observeAllLogs(): Flow<List<StrengthSetLog>> =
        strengthLogDao.observeAllLogs()

    suspend fun saveSetLog(
        date: LocalDate,
        planDay: Int,
        exerciseId: String,
        setNumber: Int,
        weightKg: Double?,
        reps: Int?,
        timeSeconds: Int?
    ) {
        if (weightKg == null && reps == null && timeSeconds == null) {
            strengthLogDao.deleteSetLog(
                date = date.toString(),
                planDay = planDay,
                exerciseId = exerciseId,
                setNumber = setNumber
            )
            return
        }

        strengthLogDao.upsert(
            StrengthSetLog(
                date = date.toString(),
                planDay = planDay,
                exerciseId = exerciseId,
                setNumber = setNumber,
                weightKg = weightKg,
                reps = reps,
                timeSeconds = timeSeconds,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun getExerciseOverridesForDate(date: LocalDate, planDay: Int): Map<String, String> {
        val workout = getWorkoutByDay(planDay) ?: return emptyMap()
        return workout.exercises.mapNotNull { exercise ->
            settingsDao.getValue(exerciseOverrideKey(date, planDay, exercise.slotId))
                ?.let { exercise.slotId to it }
        }.toMap()
    }

    private fun exerciseOverrideKey(date: LocalDate, planDay: Int, slotId: String): String =
        "${SettingsKeys.STRENGTH_EXERCISE_OVERRIDE_PREFIX}${date}_${planDay}_$slotId"
}
