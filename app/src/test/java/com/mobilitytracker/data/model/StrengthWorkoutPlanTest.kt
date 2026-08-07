package com.mobilitytracker.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthWorkoutPlanTest {

    @Test
    fun `base plan replaces RDL and removes activation`() {
        val plan = getStrengthWorkoutByDay(1)!!

        assertTrue(plan.exercises.any { it.id == "single_leg_hip_thrust" })
        assertFalse(plan.exercises.any { it.id == "day1_dumbbell_rdl" || it.section == "Aktywacja" })
        assertTrue(getStrengthExerciseCatalog().any { it.id == "day1_dumbbell_rdl" && !it.selectable })
    }

    @Test
    fun `daily override affects only matching slot`() {
        val plan = resolveStrengthWorkout(1, mapOf("day1_hip_extension" to "day1_glute_bridge"))!!

        assertEquals("day1_glute_bridge", plan.exercises.single { it.slotId == "day1_hip_extension" }.id)
        assertEquals("day1_dumbbell_bench_press", plan.exercises.single { it.slotId == "day1_horizontal_push" }.id)
    }

    @Test
    fun `unknown override falls back to base exercise`() {
        val plan = resolveStrengthWorkout(1, mapOf("day1_hip_extension" to "unknown"))

        assertNotNull(plan)
        assertEquals("single_leg_hip_thrust", plan!!.exercises.single { it.slotId == "day1_hip_extension" }.id)
    }

    @Test
    fun `vertical pull supports band assisted pull up and weighted pulldown`() {
        val base = getStrengthWorkoutByDay(1)!!
            .exercises.single { it.slotId == "day1_vertical_pull" }
        val pulldown = resolveStrengthWorkout(1, mapOf("day1_vertical_pull" to "day1_lat_pulldown"))!!
            .exercises.single { it.slotId == "day1_vertical_pull" }

        assertEquals("day1_band_pull_up", base.id)
        assertEquals(TrackingType.BAND_AND_REPS, base.trackingType)
        assertEquals(TrackingType.WEIGHT_AND_REPS, pulldown.trackingType)
    }

    @Test
    fun `loaded exercises include a warmup set but core does not`() {
        val plan = getStrengthWorkoutByDay(1)!!

        assertTrue(plan.exercises.single { it.slotId == "day1_squat" }.hasWarmupSet)
        assertTrue(plan.exercises.single { it.slotId == "day1_vertical_pull" }.hasWarmupSet)
        assertFalse(plan.exercises.single { it.slotId == "day1_core" }.hasWarmupSet)
    }

    @Test
    fun `main movements have at most two same-day alternatives including floor press for a busy bench`() {
        val plans = listOf(getStrengthWorkoutByDay(1)!!, getStrengthWorkoutByDay(2)!!)

        assertTrue(plans.flatMap { it.exercises }.filter { it.section == "Główne" }.all { it.choices.size <= 3 })
        assertTrue(
            getStrengthWorkoutByDay(1)!!.exercises
                .single { it.slotId == "day1_horizontal_push" }
                .choices
                .any { it.id == "day1_dumbbell_floor_press" }
        )
    }

    @Test
    fun `every exercise has a YouTube technique link`() {
        assertTrue(
            getStrengthExerciseCatalog().all { entry ->
                entry.videoUrl?.startsWith("https://www.youtube.com/results?") == true
            }
        )
    }
}
