package com.mobilitytracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "settings")
@Serializable
data class UserSettings(
    @PrimaryKey val key: String,
    val value: String
)

object SettingsKeys {
    const val PROGRAM_START_DATE = "program_start_date"
    const val REMINDER_HOUR = "reminder_hour"
    const val REMINDER_MINUTE = "reminder_minute"
    const val REMINDER_ENABLED = "reminder_enabled"
    const val MOBILITY_DAYS = "mobility_days"
    const val STRENGTH_DAYS = "strength_days"
    const val STRENGTH_DAY_PLAN_ASSIGNMENTS = "strength_day_plan_assignments"
    const val STRENGTH_PLAN_OVERRIDE_PREFIX = "strength_plan_override_"
    const val STRENGTH_EXERCISE_OVERRIDE_PREFIX = "strength_exercise_override_"
    const val MOBILITY_ANCHOR_DATE = "mobility_anchor_date"
    const val MOBILITY_SESSIONS_AT_ANCHOR = "mobility_sessions_at_anchor"
    const val STRENGTH_ANCHOR_DATE = "strength_anchor_date"
    const val STRENGTH_SESSIONS_AT_ANCHOR = "strength_sessions_at_anchor"
    const val THEME_MODE = "theme_mode"
    const val MORNING_WARMUP_DATE = "morning_warmup_date"
    const val MORNING_WARMUP_FILE = "morning_warmup_file"
    const val MORNING_WARMUP_HISTORY = "morning_warmup_history"
}
