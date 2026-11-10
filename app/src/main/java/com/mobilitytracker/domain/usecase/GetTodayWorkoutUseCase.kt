package com.mobilitytracker.domain.usecase

import com.mobilitytracker.data.repository.CompletionRepository
import com.mobilitytracker.data.repository.MorningWarmupRepository
import com.mobilitytracker.data.repository.StrengthRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.data.video.VideoResolver
import com.mobilitytracker.domain.model.DayWorkout
import com.mobilitytracker.domain.model.ScheduledStrengthWorkout
import com.mobilitytracker.domain.model.TodayTrainingPlan
import com.mobilitytracker.domain.model.buildMobilityChallengeState
import java.time.LocalDate
import javax.inject.Inject

class GetTodayWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val completionRepository: CompletionRepository,
    private val strengthRepository: StrengthRepository,
    private val morningWarmupRepository: MorningWarmupRepository,
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

        val challenge = buildMobilityChallengeState(completionRepository.getAllCompletionsSync(), today)
        val mobilityWorkout = buildMobilityWorkout(
            cycleNumber = challenge.todayCycleNumber,
            videoOrder = challenge.todayVideoOrder,
            isCompleted = challenge.completedToday
        )
        val nextMobilityWorkout = if (challenge.completedToday) {
            buildMobilityWorkout(
                cycleNumber = challenge.activeCycleNumber,
                videoOrder = challenge.activeVideoOrder,
                isCompleted = false
            )
        } else null
        val strengthWorkout = buildStrengthWorkout(today)
        val morningWarmup = morningWarmupRepository.getToday()

        return Result.Success(
            TodayTrainingPlan(
                mobilityWorkout = mobilityWorkout,
                nextMobilityWorkout = nextMobilityWorkout,
                mobilityChallenge = challenge,
                strengthWorkout = strengthWorkout,
                morningWarmup = morningWarmup
            )
        )
    }

    private fun buildMobilityWorkout(
        cycleNumber: Int,
        videoOrder: Int,
        isCompleted: Boolean
    ): DayWorkout? {
        val definition = workoutRepository.getWorkoutByOrder(videoOrder) ?: return null

        return DayWorkout(
            cycleNumber = cycleNumber,
            videoOrder = videoOrder,
            definition = definition,
            isCompleted = isCompleted,
            videoUri = videoResolver.findVideoUri(definition.videoFileName)?.toString()
        )
    }

    private suspend fun buildStrengthWorkout(today: LocalDate): ScheduledStrengthWorkout? {
        val planDay = workoutRepository.getStrengthPlanDayForDate(today) ?: return null

        val sessionNumber = workoutRepository.getTotalStrengthSessions(today)
        if (sessionNumber <= 0) return null

        val definition = strengthRepository.getWorkoutForDate(planDay, today) ?: return null

        return ScheduledStrengthWorkout(
            date = today,
            sessionNumber = sessionNumber,
            planDay = planDay,
            definition = definition
        )
    }
}
