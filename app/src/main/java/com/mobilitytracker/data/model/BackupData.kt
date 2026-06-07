package com.mobilitytracker.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val completions: List<Completion>,
    val settings: List<UserSettings>,
    val strengthLogs: List<StrengthSetLog>
)
