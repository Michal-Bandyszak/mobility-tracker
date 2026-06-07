package com.mobilitytracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobilitytracker.data.model.StrengthSetLog
import kotlinx.coroutines.flow.Flow

@Dao
interface StrengthLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: StrengthSetLog)

    @Query(
        """
        SELECT * FROM strength_set_logs
        WHERE date = :date
        ORDER BY planDay ASC, exerciseId ASC, setNumber ASC
        """
    )
    fun observeLogsForDate(date: String): Flow<List<StrengthSetLog>>

    @Query(
        """
        DELETE FROM strength_set_logs
        WHERE date = :date
            AND planDay = :planDay
            AND exerciseId = :exerciseId
            AND setNumber = :setNumber
        """
    )
    suspend fun deleteSetLog(
        date: String,
        planDay: Int,
        exerciseId: String,
        setNumber: Int
    )

    @Query("SELECT * FROM strength_set_logs ORDER BY date DESC, planDay ASC, exerciseId ASC, setNumber ASC")
    fun observeAllLogs(): Flow<List<StrengthSetLog>>

    @Query("DELETE FROM strength_set_logs")
    suspend fun deleteAll()

    @Query("SELECT * FROM strength_set_logs")
    suspend fun getAllLogs(): List<StrengthSetLog>
}
