package com.mobilitytracker.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthWorkoutPlanTest {

    @Test
    fun `base plan trains hamstrings with lying leg curl instead of RDL`() {
        val plan = getStrengthWorkoutByDay(1)!!

        assertTrue(plan.exercises.any { it.id == "lying_leg_curl" })
        assertFalse(plan.exercises.any { it.id == "day1_dumbbell_rdl" || it.section == "Aktywacja" })
        assertTrue(getStrengthExerciseCatalog().any { it.id == "day1_dumbbell_rdl" && !it.selectable })
    }

    @Test
    fun `daily override affects only matching slot`() {
        val plan = resolveStrengthWorkout(1, mapOf("day1_hamstrings" to "ball_leg_curl"))!!

        assertEquals("ball_leg_curl", plan.exercises.single { it.slotId == "day1_hamstrings" }.id)
        assertEquals("day1_dumbbell_bench_press", plan.exercises.single { it.slotId == "day1_horizontal_push" }.id)
    }

    @Test
    fun `unknown override falls back to base exercise`() {
        val plan = resolveStrengthWorkout(1, mapOf("day1_hamstrings" to "unknown"))

        assertNotNull(plan)
        assertEquals("lying_leg_curl", plan!!.exercises.single { it.slotId == "day1_hamstrings" }.id)
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
    fun `loaded exercises include a warmup set but bodyweight leg curl does not`() {
        val plan = getStrengthWorkoutByDay(1)!!

        assertTrue(plan.exercises.single { it.slotId == "day1_squat" }.hasWarmupSet)
        assertTrue(plan.exercises.single { it.slotId == "day1_vertical_pull" }.hasWarmupSet)

        val hamstrings = resolveStrengthWorkout(1, mapOf("day1_hamstrings" to "ball_leg_curl"))!!
            .exercises.single { it.slotId == "day1_hamstrings" }
        assertFalse(hamstrings.hasWarmupSet)
    }

    @Test
    fun `main movements have at most two same-day alternatives including floor press for a busy bench`() {
        val plans = listOf(getStrengthWorkoutByDay(1)!!, getStrengthWorkoutByDay(2)!!)

        assertTrue(plans.flatMap { it.exercises }.all { it.choices.size <= 3 })
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

    @Test
    fun `chest is trained first and gets at least ten weekly sets`() {
        val day1 = getStrengthWorkoutByDay(1)!!
        val day2 = getStrengthWorkoutByDay(2)!!

        assertEquals("day1_horizontal_push", day1.exercises.first().slotId)
        assertEquals("day2_incline_push", day2.exercises.first().slotId)

        val chestSets = day1.exercises.single { it.slotId == "day1_horizontal_push" }.sets +
            day2.exercises.single { it.slotId == "day2_incline_push" }.sets +
            day2.exercises.single { it.slotId == "day2_chest_fly" }.sets
        assertTrue(chestSets >= 10)
    }

    @Test
    fun `warmup includes foot work for flat foot`() {
        val ids = WARMUP_ROUTINE.map { it.id }

        assertTrue(ids.contains("warmup_short_foot"))
        assertTrue(ids.contains("warmup_single_leg_calf_raise"))
    }

    @Test
    fun `both workouts train core`() {
        assertEquals("day1_deadbug", getStrengthWorkoutByDay(1)!!.exercises.single { it.slotId == "day1_core" }.id)
        assertEquals("pallof_press", getStrengthWorkoutByDay(2)!!.exercises.single { it.slotId == "day2_core" }.id)
    }

    @Test
    fun `squat is not limited by grip and upper back is trained in both workouts`() {
        val day1 = getStrengthWorkoutByDay(1)!!

        assertEquals("day1_goblet_squat_pause", day1.exercises.single { it.slotId == "day1_squat" }.id)
        assertEquals("reverse_pec_deck", day1.exercises.single { it.slotId == "day1_upper_back" }.id)
        assertEquals("day2_face_pull", getStrengthWorkoutByDay(2)!!.exercises.single { it.slotId == "day2_rear_delts" }.id)
    }

    @Test
    fun `retired exercises are not offered in any slot`() {
        val retired = setOf("hack_squat_machine", "cable_overhead_triceps_extension", "cable_crunch", "day2_split_squat")
        val offered = listOf(getStrengthWorkoutByDay(1)!!, getStrengthWorkoutByDay(2)!!)
            .flatMap { it.exercises }
            .flatMap { listOf(it.id) + it.choices.map { choice -> choice.id } }

        assertTrue(offered.none { it in retired })
        assertEquals("reverse_lunge", getStrengthWorkoutByDay(2)!!.exercises.single { it.slotId == "day2_unilateral_legs" }.id)
    }
}
