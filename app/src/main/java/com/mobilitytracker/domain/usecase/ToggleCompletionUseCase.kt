package com.mobilitytracker.domain.usecase

import com.mobilitytracker.data.repository.CompletionRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class ToggleCompletionUseCase @Inject constructor(
    private val completionRepository: CompletionRepository
) {
    private val mutex = Mutex()

    suspend operator fun invoke(cycleNumber: Int, videoOrder: Int) {
        mutex.withLock {
            if (completionRepository.isCompleted(cycleNumber, videoOrder)) {
                completionRepository.markUncompleted(cycleNumber, videoOrder)
            } else {
                completionRepository.markCompleted(cycleNumber, videoOrder)
            }
        }
    }
}
