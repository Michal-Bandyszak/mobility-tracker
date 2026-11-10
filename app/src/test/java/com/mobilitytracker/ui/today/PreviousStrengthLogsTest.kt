package com.mobilitytracker.ui.today

import com.mobilitytracker.data.model.StrengthSetLog
import com.mobilitytracker.data.model.WARMUP_SET_NUMBER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviousStrengthLogsTest {

    private fun log(date: String, exerciseId: String, setNumber: Int, weight: Double) =
        StrengthSetLog(date = date, planDay = 1, exerciseId = exerciseId, setNumber = setNumber, weightKg = weight, reps = 8, timeSeconds = null, updatedAt = 0)

    @Test
    fun `takes the most recent earlier session per exercise and falls back to its last working set`() {
        val logs = listOf(
            log("2026-11-01", "bench", 1, 16.0),
            log("2026-11-05", "bench", 1, 18.0),
            log("2026-11-05", "bench", 2, 20.0),
            log("2026-11-09", "bench", 1, 22.0), // dzisiejszy zapis nie jest „poprzednim”
            log("2026-11-02", "curl", WARMUP_SET_NUMBER, 6.0),
            log("2026-11-02", "curl", 1, 10.0)
        )

        val previous = previousStrengthLogs(logs, "2026-11-09")

        assertEquals(18.0, previousSetLog(previous, "bench", 1)?.weightKg)
        assertEquals(20.0, previousSetLog(previous, "bench", 4)?.weightKg)
        assertNull(previousSetLog(previous, "bench", WARMUP_SET_NUMBER))
        assertEquals(6.0, previousSetLog(previous, "curl", WARMUP_SET_NUMBER)?.weightKg)
        assertNull(previousSetLog(previous, "squat", 1))
    }
}
