package com.mobilitytracker.domain.usecase

import com.mobilitytracker.data.repository.CompletionRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import javax.inject.Inject

class ResetProgramFlowUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val completionRepository: CompletionRepository
) {
    suspend operator fun invoke() {
        workoutRepository.resetMobilityProgress()
        completionRepository.deleteAllCompletions()
    }
}
