package com.mobilitytracker.domain.usecase

import com.mobilitytracker.data.model.StrengthExerciseDefinition
import com.mobilitytracker.data.model.StrengthWorkoutDefinition
import com.mobilitytracker.data.model.StrengthSetLog
import com.mobilitytracker.data.model.TrackingType
import com.mobilitytracker.data.repository.StrengthRepository
import com.mobilitytracker.domain.model.StrengthExerciseProgress
import com.mobilitytracker.domain.model.StrengthPerformance
import com.mobilitytracker.domain.model.StrengthSessionExerciseSummary
import com.mobilitytracker.domain.model.StrengthSessionSummary
import com.mobilitytracker.domain.model.StrengthStats
import com.mobilitytracker.domain.model.StrengthStatsOverview
import com.mobilitytracker.domain.model.StrengthTrend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetStrengthStatsUseCase @Inject constructor(
    private val strengthRepository: StrengthRepository
) {
    operator fun invoke(): Flow<StrengthStats> =
        strengthRepository.observeAllLogs().map { logs ->
            buildStats(logs)
        }

    private fun buildStats(logs: List<StrengthSetLog>): StrengthStats {
        val workouts = strengthRepository.getAllWorkouts()
        val exercises = workouts
            .flatMap { it.exercises }
            .distinctBy { it.id }
            .associateBy { it.id }

        val trackedLogs = logs.filter { log ->
            val trackingType = exercises[log.exerciseId]?.trackingType ?: TrackingType.NONE
            trackingType != TrackingType.NONE
        }

        val recentSessions = buildSessionSummaries(trackedLogs, workouts, exercises)
        val exerciseProgress = buildExerciseProgress(trackedLogs, exercises)
        val overview = buildOverview(trackedLogs, recentSessions, exerciseProgress)

        return StrengthStats(
            overview = overview,
            exerciseProgress = exerciseProgress,
            recentSessions = recentSessions.take(6)
        )
    }

    private fun buildOverview(
        logs: List<StrengthSetLog>,
        sessions: List<StrengthSessionSummary>,
        exerciseProgress: List<StrengthExerciseProgress>
    ): StrengthStatsOverview {
        val latestSession = sessions.firstOrNull()
        return StrengthStatsOverview(
            totalSessions = sessions.size,
            latestSessionDate = latestSession?.date,
            latestSessionPlanDay = latestSession?.planDay,
            trackedExercises = exerciseProgress.size,
            totalVolumeKg = logs.sumOf { it.volumeKg() },
            totalLoggedSets = logs.size
        )
    }

    private fun buildExerciseProgress(
        logs: List<StrengthSetLog>,
        exercises: Map<String, StrengthExerciseDefinition>
    ): List<StrengthExerciseProgress> {
        return logs.groupBy { it.exerciseId }
            .mapNotNull { (exerciseId, exerciseLogs) ->
                val exercise = exercises[exerciseId] ?: return@mapNotNull null
                val performances = exerciseLogs
                    .groupBy { it.date to it.planDay }
                    .map { (_, dailyLogs) -> dailyLogs.toPerformance() }
                    .sortedByDescending { it.date }

                StrengthExerciseProgress(
                    exerciseId = exerciseId,
                    name = exercise.name,
                    section = exercise.section,
                    trackingType = exercise.trackingType,
                    loggedSessions = performances.size,
                    latest = performances.firstOrNull(),
                    best = performances.maxWithOrNull(
                        compareBy<StrengthPerformance> { it.scoreFor(exercise.trackingType) }
                    ),
                    trend = calculateTrend(performances, exercise.trackingType),
                    recentPerformances = performances.take(5)
                )
            }
            .sortedWith(
                compareByDescending<StrengthExerciseProgress> { it.latest?.date ?: "" }
                    .thenBy { it.name }
            )
    }

    private fun buildSessionSummaries(
        logs: List<StrengthSetLog>,
        workouts: List<StrengthWorkoutDefinition>,
        exercises: Map<String, StrengthExerciseDefinition>
    ): List<StrengthSessionSummary> {
        return logs.groupBy { it.date to it.planDay }
            .map { (sessionKey, sessionLogs) ->
                val (date, planDay) = sessionKey
                val plannedTrackedSets = plannedTrackedSets(planDay, workouts)
                val exerciseSummaries = sessionLogs
                    .groupBy { it.exerciseId }
                    .mapNotNull { (exerciseId, exerciseLogs) ->
                        val exercise = exercises[exerciseId] ?: return@mapNotNull null
                        StrengthSessionExerciseSummary(
                            exerciseName = exercise.name,
                            trackingType = exercise.trackingType,
                            bestPerformance = exerciseLogs.toPerformance(),
                            loggedSets = exerciseLogs.size
                        )
                    }
                    .sortedBy { it.exerciseName }

                StrengthSessionSummary(
                    date = date,
                    planDay = planDay,
                    completedTrackedSets = sessionLogs.size,
                    plannedTrackedSets = plannedTrackedSets,
                    totalVolumeKg = sessionLogs.sumOf { it.volumeKg() },
                    exerciseSummaries = exerciseSummaries
                )
            }
            .sortedWith(compareByDescending<StrengthSessionSummary> { it.date }.thenByDescending { it.planDay })
    }

    private fun plannedTrackedSets(planDay: Int, workouts: List<StrengthWorkoutDefinition>): Int =
        workouts
            .firstOrNull { it.day == planDay }
            ?.exercises
            ?.filter { it.trackingType != TrackingType.NONE }
            ?.sumOf { it.sets }
            ?: 0

    private fun List<StrengthSetLog>.toPerformance(): StrengthPerformance {
        val bestLog = maxWithOrNull(
            compareBy<StrengthSetLog> { it.volumeKg() }
                .thenBy { it.timeSeconds ?: 0 }
                .thenBy { it.reps ?: 0 }
                .thenBy { it.weightKg ?: 0.0 }
        ) ?: first()

        return StrengthPerformance(
            date = bestLog.date,
            planDay = bestLog.planDay,
            weightKg = bestLog.weightKg,
            reps = bestLog.reps,
            timeSeconds = bestLog.timeSeconds,
            volumeKg = bestLog.volumeKg().takeIf { it > 0.0 }
        )
    }

    private fun calculateTrend(
        performances: List<StrengthPerformance>,
        trackingType: TrackingType
    ): StrengthTrend {
        if (performances.size < 2) return StrengthTrend.UNKNOWN

        val latest = performances[0].scoreFor(trackingType)
        val previous = performances[1].scoreFor(trackingType)
        if (latest <= 0.0 || previous <= 0.0) return StrengthTrend.UNKNOWN

        return when {
            latest > previous -> StrengthTrend.UP
            latest < previous -> StrengthTrend.DOWN
            else -> StrengthTrend.FLAT
        }
    }

    private fun StrengthSetLog.volumeKg(): Double {
        val weight = weightKg ?: return 0.0
        val repetitions = reps ?: return 0.0
        return weight * repetitions
    }

    private fun StrengthPerformance.scoreFor(trackingType: TrackingType): Double =
        when (trackingType) {
            TrackingType.WEIGHT_AND_REPS -> volumeKg ?: 0.0
            TrackingType.REPS_ONLY -> reps?.toDouble() ?: 0.0
            TrackingType.TIME_ONLY -> timeSeconds?.toDouble() ?: 0.0
            TrackingType.NONE -> 0.0
        }
}
