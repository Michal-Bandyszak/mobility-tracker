package com.mobilitytracker.data.model

data class WorkoutDefinition(
    val order: Int,
    val title: String,
    val videoFileName: String,
    val durationMinutes: Int,
    val category: String
)
