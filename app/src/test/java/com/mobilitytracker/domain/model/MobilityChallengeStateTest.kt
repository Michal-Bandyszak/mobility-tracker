package com.mobilitytracker.domain.model

import com.mobilitytracker.data.model.Completion
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilityChallengeStateTest {

    private val today: LocalDate = LocalDate.of(2026, 7, 17)

    @Test
    fun `missed day keeps first unfinished session`() {
        val state = buildMobilityChallengeState(
            completions = listOf(completion(1, today.minusDays(2))),
            today = today
        )

        assertEquals(2, state.activeSessionNumber)
        assertEquals(2, state.todaySessionNumber)
        assertEquals(0, state.currentStreak)
        assertFalse(state.completedToday)
    }

    @Test
    fun `completion today stays visible while next session waits`() {
        val state = buildMobilityChallengeState(
            completions = listOf(
                completion(1, today.minusDays(1)),
                completion(2, today)
            ),
            today = today
        )

        assertEquals(2, state.todaySessionNumber)
        assertEquals(3, state.activeSessionNumber)
        assertEquals(2, state.currentStreak)
        assertTrue(state.completedToday)
    }

    @Test
    fun `old gap becomes active without deleting later completions`() {
        val state = buildMobilityChallengeState(
            completions = listOf(completion(1, today.minusDays(2)), completion(3, today.minusDays(1))),
            today = today
        )

        assertEquals(2, state.activeSessionNumber)
        assertEquals(2, state.completedSessions)
    }

    @Test
    fun `challenge caps at 84 and sequence continues`() {
        val state = buildMobilityChallengeState(
            completions = (1..85).map { completion(it, today.minusDays((85 - it).toLong())) },
            today = today
        )

        assertEquals(MOBILITY_CHALLENGE_SESSIONS, state.completedSessions)
        assertEquals(86, state.activeSessionNumber)
        assertTrue(state.isComplete)
    }

    @Test
    fun `only one completion can be recorded per date`() {
        val completions = listOf(completion(1, today))

        assertFalse(canRecordMobilityCompletion(completions, today))
        assertTrue(canRecordMobilityCompletion(completions, today.plusDays(1)))
    }

    @Test
    fun `streak and weekly goal count distinct dates`() {
        val state = buildMobilityChallengeState(
            completions = listOf(
                completion(1, today.minusDays(1)),
                completion(2, today),
                completion(3, today)
            ),
            today = today
        )

        assertEquals(2, state.currentStreak)
        assertEquals(2, state.completedThisWeek)
    }

    private fun completion(sessionNumber: Int, date: LocalDate): Completion {
        val (cycle, video) = sessionToCycleAndVideo(sessionNumber)
        return Completion(
            id = sessionNumber,
            cycleNumber = cycle,
            videoOrder = video,
            completedAt = date.toEpochDay(),
            date = date.toString()
        )
    }
}
