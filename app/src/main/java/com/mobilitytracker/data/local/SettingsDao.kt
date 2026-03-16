package com.mobilitytracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobilitytracker.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: UserSettings)

    @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
    fun observeValue(key: String): Flow<String?>
}
