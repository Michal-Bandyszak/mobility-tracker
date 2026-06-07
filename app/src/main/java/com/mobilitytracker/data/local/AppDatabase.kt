package com.mobilitytracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mobilitytracker.data.model.Completion
import com.mobilitytracker.data.model.StrengthSetLog
import com.mobilitytracker.data.model.UserSettings

@Database(
    entities = [Completion::class, UserSettings::class, StrengthSetLog::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun completionDao(): CompletionDao
    abstract fun settingsDao(): SettingsDao
    abstract fun strengthLogDao(): StrengthLogDao
}
