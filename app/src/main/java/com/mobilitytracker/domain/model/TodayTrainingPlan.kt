package com.mobilitytracker.domain.model

data class TodayTrainingPlan(
    val mobilityWorkout: DayWorkout?,
    val strengthWorkout: ScheduledStrengthWorkout?
)
