package com.mobilitytracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "completions",
    indices = [Index(value = ["cycleNumber", "videoOrder"], unique = true)]
)
@Serializable
data class Completion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cycleNumber: Int,
    val videoOrder: Int,
    val completedAt: Long,
    val date: String
)
