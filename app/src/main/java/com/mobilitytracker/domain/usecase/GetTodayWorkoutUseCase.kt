package com.mobilitytracker.domain.usecase

import com.mobilitytracker.data.repository.CompletionRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.data.video.VideoResolver
import com.mobilitytracker.domain.model.DayWorkout
import java.time.LocalDate
import javax.inject.Inject

class GetTodayWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val completionRepository: CompletionRepository,
    private val videoResolver: VideoResolver
) {
    sealed class Result {
        object ProgramNotStarted : Result()
        data class Success(val workout: DayWorkout) : Result()
    }

    suspend operator fun invoke(): Result {
        val startDate = workoutRepository.getProgramStartDate()
            ?: return Result.ProgramNotStarted

        val today = LocalDate.now()
        val totalDays = workoutRepository.calculateCurrentDay(startDate, today)

        if (totalDays < 0) return Result.ProgramNotStarted

        val (cycleNumber, videoOrder) = workoutRepository.totalDaysToCycleAndVideo(totalDays)
        val definition = workoutRepository.getWorkoutByOrder(videoOrder)
            ?: return Result.ProgramNotStarted

        val isCompleted = completionRepository.isCompleted(cycleNumber, videoOrder)
        val videoUri = videoResolver.findVideoUri(definition.videoFileName)?.toString()

        return Result.Success(
            DayWorkout(
                cycleNumber = cycleNumber,
                videoOrder = videoOrder,
                definition = definition,
                isCompleted = isCompleted,
                videoUri = videoUri
            )
        )
    }
}
