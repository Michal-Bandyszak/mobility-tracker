package com.mobilitytracker.domain.model

import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.data.model.Completion
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val MOBILITY_CHALLENGE_SESSIONS = 84

data class MobilityChallengeState(
    val completedSessionNumbers: Set<Int>,
    val completedSessions: Int,
    val activeSessionNumber: Int,
    val todaySessionNumber: Int,
    val completedToday: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val completedThisWeek: Int
) {
    val isComplete: Boolean = completedSessions >= MOBILITY_CHALLENGE_SESSIONS
    val progress: Float = completedSessions.toFloat() / MOBILITY_CHALLENGE_SESSIONS
    val activeCycleNumber: Int = sessionToCycleAndVideo(activeSessionNumber).first
    val activeVideoOrder: Int = sessionToCycleAndVideo(activeSessionNumber).second
    val todayCycleNumber: Int = sessionToCycleAndVideo(todaySessionNumber).first
    val todayVideoOrder: Int = sessionToCycleAndVideo(todaySessionNumber).second
}

fun buildMobilityChallengeState(
    completions: List<Completion>,
    today: LocalDate = LocalDate.now()
): MobilityChallengeState {
    val positions = completions.mapNotNull(Completion::sessionNumber).toSet()
    val activeSession = generateSequence(1) { it + 1 }.first { it !in positions }
    val todayCompletion = completions
        .filter { it.date == today.toString() }
        .maxByOrNull { it.completedAt }
    val todaySession = todayCompletion?.sessionNumber() ?: activeSession
    val completedDates = completions.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())

    return MobilityChallengeState(
        completedSessionNumbers = positions,
        completedSessions = positions.count { it <= MOBILITY_CHALLENGE_SESSIONS }
            .coerceAtMost(MOBILITY_CHALLENGE_SESSIONS),
        activeSessionNumber = activeSession,
        todaySessionNumber = todaySession,
        completedToday = todayCompletion != null,
        currentStreak = currentStreak(completedDates, today),
        longestStreak = longestStreak(completedDates),
        completedThisWeek = completedDates.count { it in weekStart..today }.coerceAtMost(7)
    )
}

fun canRecordMobilityCompletion(completions: List<Completion>, date: LocalDate): Boolean =
    completions.none { it.date == date.toString() }

fun sessionToCycleAndVideo(sessionNumber: Int): Pair<Int, Int> {
    val zeroBased = (sessionNumber - 1).coerceAtLeast(0)
    return (zeroBased / CYCLE_LENGTH + 1) to (zeroBased % CYCLE_LENGTH + 1)
}

private fun Completion.sessionNumber(): Int? {
    if (cycleNumber < 1 || videoOrder !in 1..CYCLE_LENGTH) return null
    return (cycleNumber - 1) * CYCLE_LENGTH + videoOrder
}

private fun currentStreak(dates: Set<LocalDate>, today: LocalDate): Int {
    var date = when {
        today in dates -> today
        today.minusDays(1) in dates -> today.minusDays(1)
        else -> return 0
    }
    var streak = 0
    while (date in dates) {
        streak++
        date = date.minusDays(1)
    }
    return streak
}

private fun longestStreak(dates: Set<LocalDate>): Int {
    if (dates.isEmpty()) return 0
    val sorted = dates.sorted()
    var longest = 1
    var current = 1
    for (index in 1 until sorted.size) {
        current = if (ChronoUnit.DAYS.between(sorted[index - 1], sorted[index]) == 1L) current + 1 else 1
        longest = maxOf(longest, current)
    }
    return longest
}
