package com.mobilitytracker.data.repository

import com.mobilitytracker.data.local.CompletionDao
import com.mobilitytracker.data.local.SettingsDao
import com.mobilitytracker.data.local.StrengthLogDao
import com.mobilitytracker.data.model.BackupData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val completionDao: CompletionDao,
    private val settingsDao: SettingsDao,
    private val strengthLogDao: StrengthLogDao
) {

    suspend fun exportDataToJson(): String {
        val completions = completionDao.getAllCompletionsSync()
        val settings = settingsDao.getAllSettings()
        val logs = strengthLogDao.getAllLogs()

        val backupData = BackupData(
            completions = completions,
            settings = settings,
            strengthLogs = logs
        )

        return Json.encodeToString(backupData)
    }

    suspend fun importDataFromJson(jsonString: String): Result<Unit> {
        return try {
            val backupData = Json.decodeFromString<BackupData>(jsonString)

            // Clear existing data
            completionDao.deleteAll()
            strengthLogDao.deleteAll()
            // Wait, we probably want to clear all settings, or maybe not delete them but overwrite?
            // Safer to clear them so obsolete settings disappear
            settingsDao.getAllSettings().forEach { settingsDao.delete(it.key) }

            // Insert new data
            backupData.completions.forEach { completionDao.insert(it) }
            backupData.settings.forEach { settingsDao.upsert(it) }
            backupData.strengthLogs.forEach { strengthLogDao.upsert(it) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
