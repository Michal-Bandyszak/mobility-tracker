package com.mobilitytracker.domain.usecase

import com.mobilitytracker.data.model.Completion
import com.mobilitytracker.data.model.WorkoutDefinition
import com.mobilitytracker.data.repository.CompletionRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.data.video.VideoResolver
import com.mobilitytracker.domain.model.DayWorkout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCycleWorkoutsUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val completionRepository: CompletionRepository,
    private val videoResolver: VideoResolver
) {
    operator fun invoke(cycleNumber: Int): Flow<List<DayWorkout>> {
        val allWorkouts: List<WorkoutDefinition> = workoutRepository.getAllWorkouts()
        return completionRepository.getCompletionsForCycle(cycleNumber).map { completions ->
            val completedOrders = completions.map { it.videoOrder }.toSet()
            allWorkouts.map { definition ->
                DayWorkout(
                    cycleNumber = cycleNumber,
                    videoOrder = definition.order,
                    definition = definition,
                    isCompleted = definition.order in completedOrders,
                    videoUri = videoResolver.findVideoUri(definition.videoFileName)?.toString()
                )
            }
        }
    }
}
