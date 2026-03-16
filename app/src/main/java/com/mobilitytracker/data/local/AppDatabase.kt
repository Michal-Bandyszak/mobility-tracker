package com.mobilitytracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mobilitytracker.data.model.Completion
import com.mobilitytracker.data.model.UserSettings

@Database(
    entities = [Completion::class, UserSettings::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun completionDao(): CompletionDao
    abstract fun settingsDao(): SettingsDao
}
