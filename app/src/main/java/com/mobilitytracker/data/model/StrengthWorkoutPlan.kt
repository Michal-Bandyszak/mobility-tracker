package com.mobilitytracker.data.model

import java.net.URLEncoder

enum class TrackingType {
    WEIGHT_AND_REPS,
    REPS_ONLY,
    TIME_ONLY,
    NONE
}

data class StrengthWorkoutDefinition(
    val day: Int,
    val title: String,
    val focus: String,
    val exercises: List<StrengthExerciseDefinition>
)

data class ExerciseCatalogEntry(
    val id: String,
    val name: String,
    val section: String,
    val notes: String = "",
    val videoUrl: String? = null,
    val trackingType: TrackingType,
    val selectable: Boolean = true
)

data class StrengthExerciseSlot(
    val id: String,
    val defaultExerciseId: String,
    val availableExerciseIds: List<String>,
    val section: String,
    val sets: Int,
    val reps: String,
    val tempo: String = "",
    val rir: String = "",
    val restSeconds: Int? = null
)

data class StrengthExerciseDefinition(
    val id: String,
    val slotId: String = id,
    val defaultExerciseId: String = id,
    val section: String,
    val name: String,
    val sets: Int,
    val reps: String,
    val tempo: String = "",
    val rir: String = "",
    val restSeconds: Int? = null,
    val notes: String = "",
    val videoUrl: String? = null,
    val trackingType: TrackingType = TrackingType.WEIGHT_AND_REPS,
    val choices: List<ExerciseCatalogEntry> = emptyList()
) {
    val isTodayOverride: Boolean get() = id != defaultExerciseId
}

data class WarmupStep(
    val id: String,
    val name: String,
    val prescription: String,
    val notes: String = ""
)

data class StrengthResourceLink(
    val title: String,
    val url: String
)

val STRENGTH_RESOURCE_LINKS = listOf(
    StrengthResourceLink(
        "Rozciąganie opcjonalne w dni wolne",
        "https://www.youtube.com/watch?v=qzdYP2OZt5s"
    ),
    StrengthResourceLink(
        "Omówienie tempa ćwiczeń",
        "https://www.youtube.com/watch?v=ZsHyiin-qMY"
    )
)

val WARMUP_ROUTINE = listOf(
    WarmupStep("warmup_90_90", "90/90", "8 powtórzeń na stronę"),
    WarmupStep("warmup_bodyweight_hip_thrust", "Unoszenie bioder obunóż bez obciążenia", "10 powtórzeń"),
    WarmupStep("warmup_cat_cow", "Koci grzbiet", "8 powtórzeń"),
    WarmupStep("warmup_up_down_dog", "Pies z głową w górę / w dół", "6 płynnych przejść"),
    WarmupStep("warmup_lunge_thoracic_rotation", "Rotacje tułowia w wykroku", "10 na stronę"),
    WarmupStep("warmup_wrist_mobility", "Mobilność nadgarstków", "30 sekund na stronę"),
    WarmupStep("warmup_band_narrow_row", "Wiosłowanie gumą wąskim chwytem", "12 powtórzeń"),
    WarmupStep("warmup_cuban_rotation", "Rotacje kubańskie z gumą", "10 powtórzeń")
)

private fun exercise(
    id: String,
    name: String,
    section: String,
    trackingType: TrackingType,
    notes: String = "",
    selectable: Boolean = true
) = ExerciseCatalogEntry(
    id = id,
    name = name,
    section = section,
    notes = notes,
    videoUrl = "https://www.youtube.com/results?search_query=" +
        URLEncoder.encode("$name technika ćwiczenia", "UTF-8"),
    trackingType = trackingType,
    selectable = selectable
)

val EXERCISE_CATALOG = listOf(
    exercise("day1_cuban_rotation", "Rotacja kubańska z gumą lub hantlami", "Aktywacja", TrackingType.NONE, selectable = false),
    exercise("day1_monster_walk", "Chód bokiem z gumą", "Aktywacja", TrackingType.NONE, selectable = false),
    exercise("day2_banded_glute_bridge", "Most biodrowy z gumą", "Aktywacja", TrackingType.NONE, selectable = false),
    exercise("day2_9090_hip_lift", "Unoszenie bioder 90/90", "Aktywacja", TrackingType.NONE, selectable = false),
    exercise("day1_dumbbell_rdl", "Martwy ciąg rumuński z hantlami", "Główne", TrackingType.WEIGHT_AND_REPS, selectable = false),
    exercise("day1_goblet_squat_pause", "Przysiad kielichowy z pauzą", "Główne", TrackingType.WEIGHT_AND_REPS, "Pauza w dole ruchu, pełna kontrola pozycji."),
    exercise("day1_box_squat", "Przysiad do podwyższenia z hantlem", "Główne", TrackingType.WEIGHT_AND_REPS, "Kontroluj zejście i wygodny zakres ruchu."),
    exercise("single_leg_hip_thrust", "Unoszenie bioder jednonóż", "Główne", TrackingType.WEIGHT_AND_REPS, "Zacznij bez obciążenia; przerwij przy bólu biodra."),
    exercise("day1_glute_bridge", "Most biodrowy obunóż", "Główne", TrackingType.WEIGHT_AND_REPS, "Pauza w górze, żebra schowane."),
    exercise("day1_dumbbell_bench_press", "Wyciskanie hantli na ławce poziomej", "Główne", TrackingType.WEIGHT_AND_REPS, "Łokcie około 45 stopni względem tułowia."),
    exercise("day1_dumbbell_floor_press", "Wyciskanie hantli na podłodze", "Główne", TrackingType.WEIGHT_AND_REPS, "Zatrzymaj łokcie miękko na podłodze."),
    exercise("day1_lat_pulldown", "Podciąganie nachwytem / ściąganie drążka wyciągu górnego", "Główne", TrackingType.WEIGHT_AND_REPS, "Pełen zakres ruchu, kontroluj łopatki."),
    exercise("day1_one_arm_row", "Wiosłowanie hantlem jednorącz", "Główne", TrackingType.WEIGHT_AND_REPS, "Stabilny tułów, prowadź łokieć do biodra."),
    exercise("day1_lateral_raise", "Wznosy ramion bokiem", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Lekkie ciężary, bez bujania tułowiem."),
    exercise("day1_rear_delt_raise", "Odwrotne rozpiętki z hantlami", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Prowadź ruch łopatkami, bez szarpania."),
    exercise("day1_supinated_curl", "Uginanie ramion z hantlami z supinacją", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Bez bujania, łokcie stabilnie przy tułowiu."),
    exercise("day1_hammer_curl", "Uginanie młotkowe z hantlami", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Nadgarstki neutralnie, łokcie blisko tułowia."),
    exercise("day1_deadbug", "Naprzemienny wyprost ręki i nogi w leżeniu", "Dodatkowe", TrackingType.REPS_ONLY, "Kontrola tułowia i oddechu."),
    exercise("day1_bird_dog", "Naprzemienny wyprost ręki i nogi w klęku", "Dodatkowe", TrackingType.REPS_ONLY, "Powoli, bez rotacji tułowia."),
    exercise("day2_hip_thrust", "Unoszenie bioder ze sztangą / gumą", "Główne", TrackingType.WEIGHT_AND_REPS, "Pauza w górze, żebra schowane, mocne spięcie pośladków."),
    exercise("day2_split_squat", "Przysiad wykroczny z hantlami", "Główne", TrackingType.WEIGHT_AND_REPS, "Kontrola pozycji, stabilny tułów i kolano."),
    exercise("day2_step_up", "Wejścia na podwyższenie z hantlami", "Główne", TrackingType.WEIGHT_AND_REPS, "Wchodź kontrolowanie, bez odbijania tylną nogą."),
    exercise("day2_incline_db_press", "Wyciskanie hantli na ławce skośnej", "Główne", TrackingType.WEIGHT_AND_REPS, "Skos 30-45 stopni, pełen kontrolowany zakres."),
    exercise("day2_seated_row", "Wiosłowanie siedząc na wyciągu dolnym", "Główne", TrackingType.WEIGHT_AND_REPS, "Jedna sekunda spięcia w końcówce ruchu."),
    exercise("day2_face_pull", "Przyciąganie liny do twarzy", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Z zatrzymaniem, łokcie wysoko, bez szarpania."),
    exercise("day2_triceps_rope_extension", "Prostowanie ramion na wyciągu z liną", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Bez machania tułowiem, pełny wyprost łokcia."),
    exercise("day2_overhead_triceps_extension", "Francuskie wyciskanie hantla", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Łokcie stabilnie, bez pogłębiania bólu nadgarstka."),
    exercise("day2_plank", "Deska", "Dodatkowe", TrackingType.TIME_ONLY, "Napięcie brzucha i pośladków, neutralny kręgosłup."),
    exercise("day2_side_plank", "Deska bokiem", "Dodatkowe", TrackingType.TIME_ONLY, "Utrzymuj linię bark-biodro-kolano lub stopa.")
).associateBy { it.id }

private data class StrengthWorkoutTemplate(
    val day: Int,
    val title: String,
    val focus: String,
    val slots: List<StrengthExerciseSlot>
)

private fun slot(
    id: String,
    defaultExerciseId: String,
    alternatives: List<String>,
    section: String,
    sets: Int,
    reps: String,
    tempo: String = "",
    rir: String = "",
    restSeconds: Int? = null
) = StrengthExerciseSlot(id, defaultExerciseId, listOf(defaultExerciseId) + alternatives, section, sets, reps, tempo, rir, restSeconds)

private val STRENGTH_WORKOUT_TEMPLATES = listOf(
    StrengthWorkoutTemplate(
        day = 1,
        title = "Trening A",
        focus = "Całe ciało: przysiad, unoszenie bioder, poziome pchanie i pionowe przyciąganie",
        slots = listOf(
            slot("day1_squat", "day1_goblet_squat_pause", listOf("day1_box_squat"), "Główne", 3, "10", "31x1", "RIR2", 90),
            slot("day1_hip_extension", "single_leg_hip_thrust", listOf("day1_glute_bridge"), "Główne", 3, "10/str", "3010", "RIR2", 90),
            slot("day1_horizontal_push", "day1_dumbbell_bench_press", listOf("day1_dumbbell_floor_press"), "Główne", 3, "8-10", "3010", "RIR2", 120),
            slot("day1_vertical_pull", "day1_lat_pulldown", listOf("day1_one_arm_row"), "Główne", 3, "8-10", "3010", "RIR2", 120),
            slot("day1_lateral_delts", "day1_lateral_raise", listOf("day1_rear_delt_raise"), "Dodatkowe", 3, "12-15", "2011", "RIR1", 60),
            slot("day1_biceps", "day1_supinated_curl", listOf("day1_hammer_curl"), "Dodatkowe", 2, "10-12", "3010", "RIR1", 60),
            slot("day1_core", "day1_deadbug", listOf("day1_bird_dog"), "Dodatkowe", 3, "10-15", restSeconds = 60)
        )
    ),
    StrengthWorkoutTemplate(
        day = 2,
        title = "Trening B",
        focus = "Całe ciało: unoszenie bioder, ćwiczenie jednostronne na nogi, skos dodatni i wiosłowanie",
        slots = listOf(
            slot("day2_hip_extension", "day2_hip_thrust", listOf("day1_glute_bridge"), "Główne", 3, "10-12", "3010", "RIR2", 90),
            slot("day2_unilateral_legs", "day2_split_squat", listOf("day2_step_up"), "Główne", 3, "8/str", "3010", "RIR2", 90),
            slot("day2_incline_push", "day2_incline_db_press", listOf("day1_dumbbell_floor_press"), "Główne", 3, "10", "3010", "RIR2", 120),
            slot("day2_horizontal_pull", "day2_seated_row", listOf("day1_one_arm_row"), "Główne", 3, "10", "3011", "RIR2", 90),
            slot("day2_rear_delts", "day2_face_pull", listOf("day1_rear_delt_raise"), "Dodatkowe", 2, "12-15", "2011", "RIR1", 60),
            slot("day2_triceps", "day2_triceps_rope_extension", listOf("day2_overhead_triceps_extension"), "Dodatkowe", 2, "12-15", "3010", "RIR1", 60),
            slot("day2_core", "day2_plank", listOf("day2_side_plank"), "Dodatkowe", 3, "30-45s", restSeconds = 60)
        )
    )
)

val STRENGTH_WORKOUT_PLAN: List<StrengthWorkoutDefinition>
    get() = STRENGTH_WORKOUT_TEMPLATES.map { template -> resolveStrengthWorkout(template.day)!! }

fun getStrengthWorkoutByDay(day: Int): StrengthWorkoutDefinition? = resolveStrengthWorkout(day)

fun getStrengthExerciseCatalog(): Collection<ExerciseCatalogEntry> = EXERCISE_CATALOG.values

fun resolveStrengthWorkout(
    day: Int,
    overridesBySlot: Map<String, String> = emptyMap()
): StrengthWorkoutDefinition? {
    val template = STRENGTH_WORKOUT_TEMPLATES.find { it.day == day } ?: return null
    val selectedIds = mutableSetOf<String>()
    val exercises = template.slots.map { slot ->
        val default = EXERCISE_CATALOG.getValue(slot.defaultExerciseId)
        val requested = overridesBySlot[slot.id]
        val selected = requested
            ?.takeIf { it in slot.availableExerciseIds }
            ?.let(EXERCISE_CATALOG::get)
            ?.takeIf { it.selectable && it.trackingType == default.trackingType && it.id !in selectedIds }
            ?: default
        selectedIds += selected.id
        StrengthExerciseDefinition(
            id = selected.id,
            slotId = slot.id,
            defaultExerciseId = slot.defaultExerciseId,
            section = slot.section,
            name = selected.name,
            sets = slot.sets,
            reps = slot.reps,
            tempo = slot.tempo,
            rir = slot.rir,
            restSeconds = slot.restSeconds,
            notes = selected.notes,
            videoUrl = selected.videoUrl,
            trackingType = selected.trackingType,
            choices = slot.availableExerciseIds.mapNotNull(EXERCISE_CATALOG::get).filter { it.selectable }
        )
    }
    return StrengthWorkoutDefinition(template.day, template.title, template.focus, exercises)
}
