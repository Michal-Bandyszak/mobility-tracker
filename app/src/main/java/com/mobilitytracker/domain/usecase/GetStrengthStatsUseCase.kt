package com.mobilitytracker.domain.usecase

import com.mobilitytracker.data.model.StrengthExerciseDefinition
import com.mobilitytracker.data.model.StrengthWorkoutDefinition
import com.mobilitytracker.data.model.StrengthSetLog
import com.mobilitytracker.data.model.TrackingType
import com.mobilitytracker.data.repository.StrengthRepository
import com.mobilitytracker.domain.model.StrengthExerciseProgress
import com.mobilitytracker.domain.model.StrengthPerformance
import com.mobilitytracker.domain.model.StrengthProgressHighlight
import com.mobilitytracker.domain.model.StrengthSessionExerciseSummary
import com.mobilitytracker.domain.model.StrengthSessionSummary
import com.mobilitytracker.domain.model.StrengthStats
import com.mobilitytracker.domain.model.StrengthStatsOverview
import com.mobilitytracker.domain.model.StrengthTrend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
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
        val exercises = strengthRepository.getExerciseCatalog().associate { entry ->
            entry.id to StrengthExerciseDefinition(
                id = entry.id,
                section = entry.section,
                name = entry.name,
                sets = 0,
                reps = "",
                notes = entry.notes,
                videoUrl = entry.videoUrl,
                trackingType = entry.trackingType
            )
        }

        val trackedLogs = logs.filter { log ->
            val trackingType = exercises[log.exerciseId]?.trackingType ?: TrackingType.NONE
            trackingType != TrackingType.NONE
        }

        val recentSessions = buildSessionSummaries(trackedLogs, workouts, exercises)
        val exerciseProgress = buildExerciseProgress(trackedLogs, exercises)
        val overview = buildOverview(recentSessions, exerciseProgress)
        val notableProgress = buildNotableProgress(exerciseProgress)

        return StrengthStats(
            overview = overview,
            notableProgress = notableProgress,
            exerciseProgress = exerciseProgress,
            recentSessions = recentSessions.take(6)
        )
    }

    private fun buildOverview(
        sessions: List<StrengthSessionSummary>,
        exerciseProgress: List<StrengthExerciseProgress>
    ): StrengthStatsOverview {
        val latestSession = sessions.firstOrNull()
        val currentMonth = YearMonth.now()
        return StrengthStatsOverview(
            totalSessions = sessions.size,
            sessionsThisMonth = sessions.count { session ->
                session.date.toYearMonthOrNull() == currentMonth
            },
            latestSessionDate = latestSession?.date,
            latestSessionPlanDay = latestSession?.planDay,
            latestCompletedTrackedSets = latestSession?.completedTrackedSets ?: 0,
            latestPlannedTrackedSets = latestSession?.plannedTrackedSets ?: 0,
            trackedExercises = exerciseProgress.size,
            totalLoggedSets = sessions.sumOf { it.completedTrackedSets }
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
                    .map { (_, dailyLogs) -> dailyLogs.toPerformance(exercise.trackingType) }
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
                            bestPerformance = exerciseLogs.toPerformance(exercise.trackingType),
                            loggedSets = exerciseLogs.size
                        )
                    }
                    .sortedBy { it.exerciseName }

                StrengthSessionSummary(
                    date = date,
                    planDay = planDay,
                    completedTrackedSets = sessionLogs.size,
                    plannedTrackedSets = plannedTrackedSets,
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

    private fun buildNotableProgress(
        exerciseProgress: List<StrengthExerciseProgress>
    ): StrengthProgressHighlight? {
        val improved = exerciseProgress.firstOrNull { progress ->
            progress.trend == StrengthTrend.UP && progress.latest != null
        }
        val fallback = exerciseProgress.firstOrNull { it.latest != null }
        val selected = improved ?: fallback ?: return null
        val latest = selected.latest ?: return null

        return StrengthProgressHighlight(
            exerciseName = selected.name,
            trackingType = selected.trackingType,
            latest = latest,
            previous = selected.recentPerformances.getOrNull(1),
            trend = selected.trend
        )
    }

    private fun List<StrengthSetLog>.toPerformance(trackingType: TrackingType): StrengthPerformance {
        val bestLog = maxWithOrNull(
            logComparator(trackingType)
        ) ?: first()

        return StrengthPerformance(
            date = bestLog.date,
            planDay = bestLog.planDay,
            weightKg = bestLog.weightKg,
            reps = bestLog.reps,
            timeSeconds = bestLog.timeSeconds,
            bandColor = bestLog.bandColor
        )
    }

    private fun calculateTrend(
        performances: List<StrengthPerformance>,
        trackingType: TrackingType
    ): StrengthTrend {
        if (performances.size < 2) return StrengthTrend.UNKNOWN

        val latestPerformance = performances[0]
        val previousPerformance = performances[1]
        if (
            trackingType == TrackingType.BAND_AND_REPS &&
            !latestPerformance.bandColor.equals(previousPerformance.bandColor, ignoreCase = true)
        ) {
            return StrengthTrend.UNKNOWN
        }

        val latest = latestPerformance.scoreFor(trackingType)
        val previous = previousPerformance.scoreFor(trackingType)
        if (latest <= 0.0 || previous <= 0.0) return StrengthTrend.UNKNOWN

        return when {
            latest > previous -> StrengthTrend.UP
            latest < previous -> StrengthTrend.DOWN
            else -> StrengthTrend.FLAT
        }
    }

    private fun logComparator(trackingType: TrackingType): Comparator<StrengthSetLog> =
        when (trackingType) {
            TrackingType.WEIGHT_AND_REPS -> compareBy<StrengthSetLog> { it.weightKg ?: 0.0 }
                .thenBy { it.reps ?: 0 }
            TrackingType.BAND_AND_REPS -> compareBy { it.reps ?: 0 }
            TrackingType.REPS_ONLY -> compareBy { it.reps ?: 0 }
            TrackingType.TIME_ONLY -> compareBy { it.timeSeconds ?: 0 }
            TrackingType.NONE -> compareBy { it.updatedAt }
        }

    private fun StrengthPerformance.scoreFor(trackingType: TrackingType): Double =
        when (trackingType) {
            TrackingType.WEIGHT_AND_REPS -> ((weightKg ?: 0.0) * 100.0) + ((reps ?: 0) / 100.0)
            TrackingType.BAND_AND_REPS -> reps?.toDouble() ?: 0.0
            TrackingType.REPS_ONLY -> reps?.toDouble() ?: 0.0
            TrackingType.TIME_ONLY -> timeSeconds?.toDouble() ?: 0.0
            TrackingType.NONE -> 0.0
        }

    private fun String.toYearMonthOrNull(): YearMonth? =
        runCatching { YearMonth.from(LocalDate.parse(this)) }.getOrNull()
}
