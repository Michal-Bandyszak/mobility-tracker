package com.mobilitytracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import com.mobilitytracker.data.local.AppDatabase
import com.mobilitytracker.data.local.CompletionDao
import com.mobilitytracker.data.local.SettingsDao
import com.mobilitytracker.data.local.StrengthLogDao
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `strength_set_logs` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `date` TEXT NOT NULL,
                    `planDay` INTEGER NOT NULL,
                    `exerciseId` TEXT NOT NULL,
                    `setNumber` INTEGER NOT NULL,
                    `weightKg` REAL,
                    `reps` INTEGER,
                    `updatedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                `index_strength_set_logs_date_planDay_exerciseId_setNumber`
                ON `strength_set_logs` (`date`, `planDay`, `exerciseId`, `setNumber`)
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE strength_set_logs ADD COLUMN timeSeconds INTEGER DEFAULT NULL")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE strength_set_logs ADD COLUMN bandColor TEXT DEFAULT NULL")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mobility_tracker.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

    @Provides
    fun provideCompletionDao(db: AppDatabase): CompletionDao = db.completionDao()

    @Provides
    fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideStrengthLogDao(db: AppDatabase): StrengthLogDao = db.strengthLogDao()
}
