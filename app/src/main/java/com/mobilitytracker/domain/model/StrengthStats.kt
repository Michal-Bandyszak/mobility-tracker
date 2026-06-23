package com.mobilitytracker.domain.model

import com.mobilitytracker.data.model.TrackingType

data class StrengthStats(
    val overview: StrengthStatsOverview,
    val exerciseProgress: List<StrengthExerciseProgress>,
    val recentSessions: List<StrengthSessionSummary>
) {
    val hasLogs: Boolean = overview.totalLoggedSets > 0

    companion object {
        val EMPTY = StrengthStats(
            overview = StrengthStatsOverview(
                totalSessions = 0,
                latestSessionDate = null,
                latestSessionPlanDay = null,
                trackedExercises = 0,
                totalVolumeKg = 0.0,
                totalLoggedSets = 0
            ),
            exerciseProgress = emptyList(),
            recentSessions = emptyList()
        )
    }
}

data class StrengthStatsOverview(
    val totalSessions: Int,
    val latestSessionDate: String?,
    val latestSessionPlanDay: Int?,
    val trackedExercises: Int,
    val totalVolumeKg: Double,
    val totalLoggedSets: Int
)

data class StrengthExerciseProgress(
    val exerciseId: String,
    val name: String,
    val section: String,
    val trackingType: TrackingType,
    val loggedSessions: Int,
    val latest: StrengthPerformance?,
    val best: StrengthPerformance?,
    val trend: StrengthTrend,
    val recentPerformances: List<StrengthPerformance>
)

data class StrengthSessionSummary(
    val date: String,
    val planDay: Int,
    val completedTrackedSets: Int,
    val plannedTrackedSets: Int,
    val totalVolumeKg: Double,
    val exerciseSummaries: List<StrengthSessionExerciseSummary>
) {
    val completionRatio: Float =
        if (plannedTrackedSets == 0) 0f else completedTrackedSets.toFloat() / plannedTrackedSets
}

data class StrengthSessionExerciseSummary(
    val exerciseName: String,
    val trackingType: TrackingType,
    val bestPerformance: StrengthPerformance?,
    val loggedSets: Int
)

data class StrengthPerformance(
    val date: String,
    val planDay: Int,
    val weightKg: Double?,
    val reps: Int?,
    val timeSeconds: Int?,
    val volumeKg: Double?
)

enum class StrengthTrend {
    UP,
    FLAT,
    DOWN,
    UNKNOWN
}
