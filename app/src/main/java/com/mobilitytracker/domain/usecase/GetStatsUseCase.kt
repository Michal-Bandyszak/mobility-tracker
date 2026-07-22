package com.mobilitytracker.domain.usecase

import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.data.model.Completion
import com.mobilitytracker.data.repository.CompletionRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.domain.model.CycleBreakdown
import com.mobilitytracker.domain.model.CycleStats
import com.mobilitytracker.domain.model.buildMobilityChallengeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class GetStatsUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val completionRepository: CompletionRepository
) {
    operator fun invoke(): Flow<CycleStats?> {
        return completionRepository.getAllCompletions().map { allCompletions ->
            workoutRepository.getProgramStartDate() ?: return@map null

            val today = LocalDate.now()
            val challenge = buildMobilityChallengeState(allCompletions, today)
            val currentCycle = challenge.activeCycleNumber

            val currentCycleCompletions = allCompletions.filter { it.cycleNumber == currentCycle }
            val currentCycleCompletedCount = currentCycleCompletions.size
            val currentCycleProgress = currentCycleCompletedCount.toFloat() / CYCLE_LENGTH

            val cycleBreakdown = buildCycleBreakdown(allCompletions, currentCycle)
            val completedCyclesCount = cycleBreakdown.count { it.isComplete }

            CycleStats(
                challenge = challenge,
                currentCycleNumber = currentCycle,
                currentCycleCompletedCount = currentCycleCompletedCount,
                currentCycleProgress = currentCycleProgress,
                completedCycles = completedCyclesCount,
                longestStreak = challenge.longestStreak,
                currentStreak = challenge.currentStreak,
                cycleBreakdown = cycleBreakdown
            )
        }
    }

    private fun buildCycleBreakdown(
        completions: List<Completion>,
        currentCycle: Int
    ): List<CycleBreakdown> {
        val byCycle = completions.groupBy { it.cycleNumber }
        val maxCycle = maxOf(currentCycle, completions.maxOfOrNull { it.cycleNumber } ?: 1)
        return (1..maxCycle).map { cycle ->
            val count = byCycle[cycle]?.map { it.videoOrder }?.distinct()?.size ?: 0
            CycleBreakdown(cycleNumber = cycle, completedCount = count)
        }
    }

}
