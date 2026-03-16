package com.mobilitytracker.data.repository

import com.mobilitytracker.data.local.CompletionDao
import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.data.model.Completion
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompletionRepository @Inject constructor(
    private val completionDao: CompletionDao
) {
    fun getCompletionsForCycle(cycleNumber: Int): Flow<List<Completion>> =
        completionDao.getCompletionsForCycle(cycleNumber)

    fun getAllCompletions(): Flow<List<Completion>> =
        completionDao.getAllCompletions()

    fun getCompletedCyclesCount(): Flow<Int> =
        completionDao.getCompletedCyclesCount(CYCLE_LENGTH)

    suspend fun isCompleted(cycleNumber: Int, videoOrder: Int): Boolean =
        completionDao.getCompletion(cycleNumber, videoOrder) != null

    suspend fun markCompleted(cycleNumber: Int, videoOrder: Int) {
        val today = LocalDate.now().toString()
        val completion = Completion(
            cycleNumber = cycleNumber,
            videoOrder = videoOrder,
            completedAt = System.currentTimeMillis(),
            date = today
        )
        completionDao.insert(completion)
    }

    suspend fun markUncompleted(cycleNumber: Int, videoOrder: Int) {
        completionDao.delete(cycleNumber, videoOrder)
    }

    suspend fun getCompletedDates(): List<String> =
        completionDao.getCompletedDates()
}
