package com.mobilitytracker.domain.model

data class TodayTrainingPlan(
    val mobilityWorkout: DayWorkout?,
    val nextMobilityWorkout: DayWorkout?,
    val mobilityChallenge: MobilityChallengeState,
    val strengthWorkout: ScheduledStrengthWorkout?,
    val morningWarmup: MorningWarmup? = null
)
