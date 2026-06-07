package com.mobilitytracker.data.repository

import com.mobilitytracker.data.local.StrengthLogDao
import com.mobilitytracker.data.model.STRENGTH_WORKOUT_PLAN
import com.mobilitytracker.data.model.StrengthSetLog
import com.mobilitytracker.data.model.StrengthWorkoutDefinition
import com.mobilitytracker.data.model.getStrengthWorkoutByDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrengthRepository @Inject constructor(
    private val strengthLogDao: StrengthLogDao
) {
    fun getAllWorkouts(): List<StrengthWorkoutDefinition> = STRENGTH_WORKOUT_PLAN

    fun getWorkoutByDay(day: Int): StrengthWorkoutDefinition? =
        getStrengthWorkoutByDay(day)

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

    suspend fun deleteAllLogs() {
        strengthLogDao.deleteAll()
    }
}
