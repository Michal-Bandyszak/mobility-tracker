package com.mobilitytracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "strength_set_logs",
    indices = [
        Index(
            value = ["date", "planDay", "exerciseId", "setNumber"],
            unique = true
        )
    ]
)
@Serializable
data class StrengthSetLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val planDay: Int,
    val exerciseId: String,
    val setNumber: Int,
    val weightKg: Double?,
    val reps: Int?,
    val timeSeconds: Int?,
    val bandColor: String? = null,
    val updatedAt: Long
)
