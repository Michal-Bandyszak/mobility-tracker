package com.mobilitytracker.domain.usecase

import com.mobilitytracker.data.repository.CompletionRepository
import com.mobilitytracker.data.repository.StrengthRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import javax.inject.Inject

class ResetProgramFlowUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val completionRepository: CompletionRepository,
    private val strengthRepository: StrengthRepository
) {
    suspend operator fun invoke() {
        workoutRepository.clearProgramFlowSettings()
        completionRepository.deleteAllCompletions()
        strengthRepository.deleteAllLogs()
        strengthRepository.clearExerciseOverrides()
    }
}
