package com.mobilitytracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class UserSettings(
    @PrimaryKey val key: String,
    val value: String
)

object SettingsKeys {
    const val PROGRAM_START_DATE = "program_start_date"
    const val REMINDER_HOUR = "reminder_hour"
    const val REMINDER_MINUTE = "reminder_minute"
    const val REMINDER_ENABLED = "reminder_enabled"
}
