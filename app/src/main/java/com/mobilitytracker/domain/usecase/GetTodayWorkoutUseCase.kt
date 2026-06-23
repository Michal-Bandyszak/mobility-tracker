package com.mobilitytracker.domain.usecase

import com.mobilitytracker.data.repository.CompletionRepository
import com.mobilitytracker.data.repository.StrengthRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.data.video.VideoResolver
import com.mobilitytracker.domain.model.DayWorkout
import com.mobilitytracker.domain.model.ScheduledStrengthWorkout
import com.mobilitytracker.domain.model.TodayTrainingPlan
import com.mobilitytracker.data.model.isTrainingScheduled
import java.time.LocalDate
import javax.inject.Inject

class GetTodayWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val completionRepository: CompletionRepository,
    private val strengthRepository: StrengthRepository,
    private val videoResolver: VideoResolver
) {
    sealed class Result {
        object ProgramNotStarted : Result()
        data class Success(val plan: TodayTrainingPlan) : Result()
    }

    suspend operator fun invoke(): Result {
        val startDate = workoutRepository.getProgramStartDate()
            ?: return Result.ProgramNotStarted

        val today = LocalDate.now()

        if (today.isBefore(startDate)) return Result.ProgramNotStarted

        val mobilityWorkout = buildMobilityWorkout(startDate, today)
        val strengthWorkout = buildStrengthWorkout(today)

        return Result.Success(
            TodayTrainingPlan(
                mobilityWorkout = mobilityWorkout,
                strengthWorkout = strengthWorkout
            )
        )
    }

    private suspend fun buildMobilityWorkout(startDate: LocalDate, today: LocalDate): DayWorkout? {
        val mobilityDays = workoutRepository.getMobilityDays()
        if (!isTrainingScheduled(today, mobilityDays)) return null

        val sessionNumber = workoutRepository.getTotalMobilitySessions(today)
        if (sessionNumber <= 0) return null

        val (cycleNumber, videoOrder) = workoutRepository.sessionNumberToCycleAndVideo(sessionNumber)
        val definition = workoutRepository.getWorkoutByOrder(videoOrder) ?: return null

        return DayWorkout(
            cycleNumber = cycleNumber,
            videoOrder = videoOrder,
            definition = definition,
            isCompleted = completionRepository.isCompleted(cycleNumber, videoOrder),
            videoUri = videoResolver.findVideoUri(definition.videoFileName)?.toString()
        )
    }

    private suspend fun buildStrengthWorkout(today: LocalDate): ScheduledStrengthWorkout? {
        val planDay = workoutRepository.getStrengthPlanDayForDate(today) ?: return null

        val sessionNumber = workoutRepository.getTotalStrengthSessions(today)
        if (sessionNumber <= 0) return null

        val definition = strengthRepository.getWorkoutByDay(planDay) ?: return null

        return ScheduledStrengthWorkout(
            date = today,
            sessionNumber = sessionNumber,
            planDay = planDay,
            definition = definition
        )
    }
}
