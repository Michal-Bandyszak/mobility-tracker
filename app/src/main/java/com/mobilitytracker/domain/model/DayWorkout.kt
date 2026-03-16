package com.mobilitytracker.domain.model

import com.mobilitytracker.data.model.WorkoutDefinition

data class DayWorkout(
    val cycleNumber: Int,
    val videoOrder: Int,
    val definition: WorkoutDefinition,
    val isCompleted: Boolean,
    val videoUri: String?
)
