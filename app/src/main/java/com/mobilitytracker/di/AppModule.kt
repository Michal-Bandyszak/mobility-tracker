package com.mobilitytracker.di

import android.content.Context
import androidx.room.Room
import com.mobilitytracker.data.local.AppDatabase
import com.mobilitytracker.data.local.CompletionDao
import com.mobilitytracker.data.local.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mobility_tracker.db"
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideCompletionDao(db: AppDatabase): CompletionDao = db.completionDao()

    @Provides
    fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()
}
