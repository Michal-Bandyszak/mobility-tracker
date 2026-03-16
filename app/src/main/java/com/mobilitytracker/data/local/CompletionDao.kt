package com.mobilitytracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobilitytracker.data.model.Completion
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: Completion)

    @Query("DELETE FROM completions WHERE cycleNumber = :cycleNumber AND videoOrder = :videoOrder")
    suspend fun delete(cycleNumber: Int, videoOrder: Int)

    @Query("SELECT * FROM completions WHERE cycleNumber = :cycleNumber AND videoOrder = :videoOrder LIMIT 1")
    suspend fun getCompletion(cycleNumber: Int, videoOrder: Int): Completion?

    @Query("SELECT * FROM completions WHERE cycleNumber = :cycleNumber")
    fun getCompletionsForCycle(cycleNumber: Int): Flow<List<Completion>>

    @Query("SELECT * FROM completions ORDER BY completedAt DESC")
    fun getAllCompletions(): Flow<List<Completion>>

    @Query("SELECT DISTINCT date FROM completions ORDER BY date DESC")
    suspend fun getCompletedDates(): List<String>

    @Query("SELECT COUNT(DISTINCT cycleNumber) FROM completions WHERE cycleNumber IN (SELECT cycleNumber FROM completions GROUP BY cycleNumber HAVING COUNT(*) = :cycleLength)")
    fun getCompletedCyclesCount(cycleLength: Int): Flow<Int>
}
