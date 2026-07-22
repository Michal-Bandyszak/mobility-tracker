package com.mobilitytracker.data.repository

import com.mobilitytracker.data.local.CompletionDao
import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.data.model.Completion
import com.mobilitytracker.domain.model.canRecordMobilityCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompletionRepository @Inject constructor(
    private val completionDao: CompletionDao
) {
    private val writeMutex = Mutex()

    fun getCompletionsForCycle(cycleNumber: Int): Flow<List<Completion>> =
        completionDao.getCompletionsForCycle(cycleNumber)

    fun getAllCompletions(): Flow<List<Completion>> =
        completionDao.getAllCompletions()

    suspend fun isCompleted(cycleNumber: Int, videoOrder: Int): Boolean =
        completionDao.getCompletion(cycleNumber, videoOrder) != null

    suspend fun getAllCompletionsSync(): List<Completion> =
        completionDao.getAllCompletionsSync()

    suspend fun markCompleted(
        cycleNumber: Int,
        videoOrder: Int,
        date: LocalDate = LocalDate.now()
    ): Boolean = writeMutex.withLock {
        if (cycleNumber < 1 || videoOrder !in 1..CYCLE_LENGTH) return@withLock false
        if (completionDao.getCompletion(cycleNumber, videoOrder) != null) return@withLock false
        if (!canRecordMobilityCompletion(completionDao.getAllCompletionsSync(), date)) return@withLock false
        val completion = Completion(
            cycleNumber = cycleNumber,
            videoOrder = videoOrder,
            completedAt = System.currentTimeMillis(),
            date = date.toString()
        )
        completionDao.insert(completion)
        true
    }

    suspend fun markUncompleted(cycleNumber: Int, videoOrder: Int) {
        completionDao.delete(cycleNumber, videoOrder)
    }

    suspend fun getCompletedDates(): List<String> =
        completionDao.getCompletedDates()

    suspend fun deleteAllCompletions() {
        completionDao.deleteAll()
    }
}
