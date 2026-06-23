package com.mobilitytracker.domain.model

import com.mobilitytracker.data.model.TrackingType

data class StrengthStats(
    val overview: StrengthStatsOverview,
    val notableProgress: StrengthProgressHighlight?,
    val exerciseProgress: List<StrengthExerciseProgress>,
    val recentSessions: List<StrengthSessionSummary>
) {
    val hasLogs: Boolean = overview.totalLoggedSets > 0

    companion object {
        val EMPTY = StrengthStats(
            overview = StrengthStatsOverview(
                totalSessions = 0,
                sessionsThisMonth = 0,
                latestSessionDate = null,
                latestSessionPlanDay = null,
                latestCompletedTrackedSets = 0,
                latestPlannedTrackedSets = 0,
                trackedExercises = 0,
                totalLoggedSets = 0
            ),
            notableProgress = null,
            exerciseProgress = emptyList(),
            recentSessions = emptyList()
        )
    }
}

data class StrengthStatsOverview(
    val totalSessions: Int,
    val sessionsThisMonth: Int,
    val latestSessionDate: String?,
    val latestSessionPlanDay: Int?,
    val latestCompletedTrackedSets: Int,
    val latestPlannedTrackedSets: Int,
    val trackedExercises: Int,
    val totalLoggedSets: Int
)

data class StrengthProgressHighlight(
    val exerciseName: String,
    val trackingType: TrackingType,
    val latest: StrengthPerformance,
    val previous: StrengthPerformance?,
    val trend: StrengthTrend
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
    val timeSeconds: Int?
)

enum class StrengthTrend {
    UP,
    FLAT,
    DOWN,
    UNKNOWN
}
