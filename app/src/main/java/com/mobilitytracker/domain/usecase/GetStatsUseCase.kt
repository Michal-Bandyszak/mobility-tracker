package com.mobilitytracker.domain.usecase

import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.data.model.Completion
import com.mobilitytracker.data.model.WORKOUT_PLAN
import com.mobilitytracker.data.repository.CompletionRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.domain.model.CycleBreakdown
import com.mobilitytracker.domain.model.CycleStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetStatsUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val completionRepository: CompletionRepository
) {
    operator fun invoke(): Flow<CycleStats?> {
        return combine(
            completionRepository.getAllCompletions(),
            completionRepository.getCompletedCyclesCount()
        ) { allCompletions, completedCyclesCount ->
            workoutRepository.getProgramStartDate() ?: return@combine null

            val today = LocalDate.now()
            val (currentCycle, currentVideo) = workoutRepository.getCurrentMobilityPosition(today)
                ?: Pair(1, 1)

            val currentCycleCompletions = allCompletions.filter { it.cycleNumber == currentCycle }
            val currentCycleCompletedCount = currentCycleCompletions.size
            val currentCycleProgress = currentCycleCompletedCount.toFloat() / CYCLE_LENGTH

            val (longestStreak, currentStreak) = calculateStreaks(allCompletions, today)

            val cycleBreakdown = buildCycleBreakdown(allCompletions, currentCycle)

            val categoryStats = buildCategoryStats(allCompletions)

            CycleStats(
                currentCycleNumber = currentCycle,
                currentVideoOrder = currentVideo,
                currentCycleCompletedCount = currentCycleCompletedCount,
                currentCycleProgress = currentCycleProgress,
                completedCycles = completedCyclesCount,
                longestStreak = longestStreak,
                currentStreak = currentStreak,
                cycleBreakdown = cycleBreakdown,
                categoryStats = categoryStats
            )
        }
    }

    private fun calculateStreaks(completions: List<Completion>, today: LocalDate): Pair<Int, Int> {
        if (completions.isEmpty()) return Pair(0, 0)

        val completedDates = completions
            .map { it.date }
            .toSortedSet()
            .map { LocalDate.parse(it) }
            .sortedDescending()

        if (completedDates.isEmpty()) return Pair(0, 0)

        // Current streak: consecutive days ending today or yesterday
        var currentStreak = 0
        var checkDate = today
        if (completedDates.first() < today.minusDays(1)) {
            currentStreak = 0
        } else {
            for (date in completedDates) {
                if (date == checkDate || date == checkDate.minusDays(1)) {
                    currentStreak++
                    checkDate = date
                } else if (date < checkDate.minusDays(1)) {
                    break
                }
            }
        }

        // Longest streak: scan all dates
        var longestStreak = 0
        var streak = 1
        val sortedAsc = completedDates.sortedAscending()
        for (i in 1 until sortedAsc.size) {
            val daysBetween = ChronoUnit.DAYS.between(sortedAsc[i - 1], sortedAsc[i])
            if (daysBetween == 1L) {
                streak++
                if (streak > longestStreak) longestStreak = streak
            } else {
                if (streak > longestStreak) longestStreak = streak
                streak = 1
            }
        }
        if (streak > longestStreak) longestStreak = streak
        if (longestStreak == 0 && sortedAsc.isNotEmpty()) longestStreak = 1

        return Pair(longestStreak, currentStreak)
    }

    private fun buildCycleBreakdown(
        completions: List<Completion>,
        currentCycle: Int
    ): List<CycleBreakdown> {
        val byCycle = completions.groupBy { it.cycleNumber }
        return (1..currentCycle).map { cycle ->
            val count = byCycle[cycle]?.size ?: 0
            CycleBreakdown(cycleNumber = cycle, completedCount = count)
        }
    }

    private fun buildCategoryStats(completions: List<Completion>): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        for (completion in completions) {
            val definition = WORKOUT_PLAN.find { it.order == completion.videoOrder } ?: continue
            stats[definition.category] = (stats[definition.category] ?: 0) + 1
        }
        return stats
    }

    private fun <T : Comparable<T>> Iterable<T>.sortedAscending(): List<T> = sortedWith(compareBy { it })
}
