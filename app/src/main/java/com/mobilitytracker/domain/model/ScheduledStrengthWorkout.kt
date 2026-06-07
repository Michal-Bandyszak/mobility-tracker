package com.mobilitytracker.domain.model

import com.mobilitytracker.data.model.StrengthWorkoutDefinition
import java.time.LocalDate

data class ScheduledStrengthWorkout(
    val date: LocalDate,
    val sessionNumber: Int,
    val planDay: Int,
    val definition: StrengthWorkoutDefinition
)
