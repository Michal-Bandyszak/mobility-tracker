package com.mobilitytracker.data.model

import java.net.URLEncoder

enum class TrackingType {
    WEIGHT_AND_REPS,
    BAND_AND_REPS,
    REPS_ONLY,
    TIME_ONLY,
    NONE
}

const val WARMUP_SET_NUMBER = 0

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
    val hasWarmupSet: Boolean
        get() = trackingType == TrackingType.WEIGHT_AND_REPS ||
            trackingType == TrackingType.BAND_AND_REPS
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
    WarmupStep("warmup_short_foot", "Krótka stopa", "10 × 5 s na stronę", "Skróć stopę bez podwijania palców."),
    WarmupStep("warmup_single_leg_calf_raise", "Wspięcia na palce jednonóż", "12–15 na stronę"),
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
    exercise("day1_goblet_squat_pause", "Przysiad kielichowy z kettlem", "Główne", TrackingType.WEIGHT_AND_REPS, "Pauza w dole ruchu, pełna kontrola pozycji. Lewe kolano prowadź nad palcami."),
    exercise("day1_box_squat", "Przysiad do podwyższenia z hantlem", "Główne", TrackingType.WEIGHT_AND_REPS, "Kontroluj zejście i wygodny zakres ruchu."),
    exercise("single_leg_hip_thrust", "Unoszenie bioder jednonóż", "Główne", TrackingType.WEIGHT_AND_REPS, "Zacznij bez obciążenia; przerwij przy bólu biodra."),
    exercise("day1_glute_bridge", "Most biodrowy obunóż", "Główne", TrackingType.WEIGHT_AND_REPS, "Pauza w górze, żebra schowane."),
    exercise("day1_dumbbell_bench_press", "Wyciskanie hantli na ławce poziomej", "Główne", TrackingType.WEIGHT_AND_REPS, "Łokcie około 45 stopni względem tułowia."),
    exercise("day1_dumbbell_floor_press", "Wyciskanie hantli na podłodze", "Główne", TrackingType.WEIGHT_AND_REPS, "Zatrzymaj łokcie miękko na podłodze."),
    exercise("day1_band_pull_up", "Podciąganie nachwytem z gumą", "Główne", TrackingType.BAND_AND_REPS, "Wpisz kolor użytej gumy. Pełen zakres ruchu, kontroluj łopatki."),
    exercise("day1_lat_pulldown", "Ściąganie drążka wyciągu górnego", "Główne", TrackingType.WEIGHT_AND_REPS, "Pełen zakres ruchu, kontroluj łopatki."),
    exercise("day1_one_arm_row", "Wiosłowanie hantlem jednorącz", "Główne", TrackingType.WEIGHT_AND_REPS, "Stabilny tułów, prowadź łokieć do biodra."),
    exercise("day1_lateral_raise", "Wznosy ramion bokiem", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Lekkie ciężary, bez bujania tułowiem."),
    exercise("day1_rear_delt_raise", "Odwrotne rozpiętki z hantlami", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Prowadź ruch łopatkami, bez szarpania."),
    exercise("day1_supinated_curl", "Uginanie ramion z hantlami", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Bez bujania, łokcie stabilnie przy tułowiu."),
    exercise("day1_hammer_curl", "Uginanie młotkowe z hantlami", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Nadgarstki neutralnie, łokcie blisko tułowia."),
    exercise("day1_deadbug", "Martwy robak", "Dodatkowe", TrackingType.REPS_ONLY, "Lędźwie dociśnięte do podłogi, kontrola oddechu."),
    exercise("day1_bird_dog", "Naprzemienny wyprost ręki i nogi w klęku", "Dodatkowe", TrackingType.REPS_ONLY, "Powoli, bez rotacji tułowia."),
    exercise("day2_hip_thrust", "Unoszenie bioder ze sztangą / gumą", "Główne", TrackingType.WEIGHT_AND_REPS, "Pauza w górze, żebra schowane, mocne spięcie pośladków."),
    exercise("day2_split_squat", "Przysiad wykroczny z hantlami", "Główne", TrackingType.WEIGHT_AND_REPS, "Wycofane z planu.", selectable = false),
    exercise("day2_step_up", "Wejścia na podwyższenie z hantlami", "Główne", TrackingType.WEIGHT_AND_REPS, "Wchodź kontrolowanie, bez odbijania tylną nogą."),
    exercise("day2_incline_db_press", "Wyciskanie hantli na ławce skośnej", "Główne", TrackingType.WEIGHT_AND_REPS, "Skos 30-45 stopni, pełen kontrolowany zakres."),
    exercise("day2_seated_row", "Wiosłowanie siedząc na wyciągu dolnym", "Główne", TrackingType.WEIGHT_AND_REPS, "Jedna sekunda spięcia w końcówce ruchu."),
    exercise("day2_face_pull", "Przyciąganie liny do twarzy", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Z zatrzymaniem, łokcie wysoko, bez szarpania."),
    exercise("day2_triceps_rope_extension", "Prostowanie ramion na wyciągu z liną", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Bez machania tułowiem, pełny wyprost łokcia."),
    exercise("day2_overhead_triceps_extension", "Francuskie wyciskanie hantla", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Łokcie stabilnie, bez pogłębiania bólu nadgarstka."),
    exercise("day2_plank", "Deska", "Dodatkowe", TrackingType.TIME_ONLY, "Napięcie brzucha i pośladków, neutralny kręgosłup."),
    exercise("day2_side_plank", "Deska bokiem", "Dodatkowe", TrackingType.TIME_ONLY, "Utrzymuj linię bark-biodro-kolano lub stopa."),
    exercise("lying_leg_curl", "Uginanie nóg leżąc na maszynie", "Główne", TrackingType.WEIGHT_AND_REPS, "Biodra dociśnięte do ławki, wolne opuszczanie. Przy bólu z tyłu biodra zmniejsz ciężar."),
    exercise("ball_leg_curl", "Uginanie nóg na piłce leżąc", "Główne", TrackingType.REPS_ONLY, "Biodra uniesione, pięty przyciągaj do pośladków."),
    exercise("cable_fly", "Rozpiętki na wyciągu", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Lekko zgięte łokcie, ruch tylko w barkach."),
    exercise("dumbbell_fly", "Rozpiętki z hantlami na ławce", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Kontrolowane rozciągnięcie, bez prostowania łokci."),
    exercise("hack_squat_machine", "Przysiad na maszynie hack", "Główne", TrackingType.WEIGHT_AND_REPS, "Wycofane: maszyna za trudna.", selectable = false),
    exercise("leg_press", "Wypychanie nóg na suwnicy", "Główne", TrackingType.WEIGHT_AND_REPS, "Lędźwie przy oparciu, w dole miednica nie odkleja się od siedziska."),
    exercise("reverse_pec_deck", "Odwrotne rozpiętki na maszynie", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Barki w dół, ściągaj łopatki do siebie, pauza w końcówce."),
    exercise("assisted_pull_up_machine", "Podciąganie na maszynie z odciążeniem", "Główne", TrackingType.BAND_AND_REPS, "W polu gumy wpisz odciążenie, np. 40 kg. Mniej odciążenia to postęp."),
    exercise("cable_overhead_triceps_extension", "Prostowanie ramion nad głową na wyciągu z liną", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Tyłem do bramy, łokcie przy głowie, pełne rozciągnięcie w dole.", selectable = false),
    exercise("cable_bar_pushdown", "Prostowanie ramion na wyciągu z drążkiem", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Łokcie przy tułowiu, pełny wyprost."),
    exercise("cable_crunch", "Spięcia brzucha na wyciągu klęcząc", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Zwijaj tułów w stronę bioder, biodra nieruchomo.", selectable = false),
    exercise("pallof_press", "Pallof press na wyciągu (antyrotacja)", "Dodatkowe", TrackingType.WEIGHT_AND_REPS, "Bokiem do bramy, wypchnij uchwyt przed klatkę i nie daj się obrócić."),
    exercise("sandbag_squat", "Przysiad z workiem", "Główne", TrackingType.WEIGHT_AND_REPS, "Worek przy klatce, stopy na szerokość bioder, lewe kolano nad palcami."),
    exercise("reverse_lunge", "Zakroki z hantlami", "Główne", TrackingType.WEIGHT_AND_REPS, "Krok w tył, tułów prosto, zacznij lekko. Przerwij przy bólu biodra.")
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
        focus = "Priorytet klatka i plecy w superserii, tył uda i góra pleców, przysiad z kettlem, ramiona i martwy robak",
        slots = listOf(
            slot("day1_horizontal_push", "day1_dumbbell_bench_press", listOf("day1_dumbbell_floor_press"), "Superseria 1", 4, "6-10", "3010", "RIR2", 60),
            slot("day1_vertical_pull", "day1_band_pull_up", listOf("day1_lat_pulldown", "assisted_pull_up_machine"), "Superseria 1", 4, "6-10", "3010", "RIR2", 60),
            slot("day1_hamstrings", "lying_leg_curl", listOf("ball_leg_curl"), "Superseria 2", 3, "10-15", "3010", "RIR2", 60),
            slot("day1_upper_back", "reverse_pec_deck", listOf("day1_rear_delt_raise"), "Superseria 2", 3, "12-15", "2011", "RIR1", 45),
            slot("day1_squat", "day1_goblet_squat_pause", listOf("sandbag_squat", "leg_press"), "Główne", 2, "8-12", "31x1", "RIR2", 90),
            slot("day1_lateral_delts", "day1_lateral_raise", listOf("day1_rear_delt_raise"), "Superseria 3", 3, "12-20", "2011", "RIR1", 45),
            slot("day1_biceps", "day1_supinated_curl", listOf("day1_hammer_curl"), "Superseria 3", 3, "8-12", "3010", "RIR1", 45),
            slot("day1_triceps", "cable_bar_pushdown", listOf("day2_triceps_rope_extension"), "Superseria 4", 3, "10-15", "3010", "RIR1", 45),
            slot("day1_core", "day1_deadbug", listOf("day1_bird_dog"), "Superseria 4", 3, "10/str", restSeconds = 45)
        )
    ),
    StrengthWorkoutTemplate(
        day = 2,
        title = "Trening B",
        focus = "Priorytet skos i wiosłowanie w superserii, pośladki z brzuchem, klatka, tył barku i ramiona",
        slots = listOf(
            slot("day2_incline_push", "day2_incline_db_press", listOf("day1_dumbbell_floor_press"), "Superseria 1", 4, "8-12", "3010", "RIR2", 60),
            slot("day2_horizontal_pull", "day2_seated_row", listOf("day1_one_arm_row"), "Superseria 1", 4, "8-12", "3011", "RIR2", 60),
            slot("day2_hip_extension", "day2_hip_thrust", listOf("day1_glute_bridge"), "Superseria 2", 3, "8-12", "3010", "RIR2", 75),
            slot("day2_core", "pallof_press", listOf("day1_bird_dog"), "Superseria 2", 3, "10/str", "2011", "RIR2", 45),
            slot("day2_unilateral_legs", "reverse_lunge", listOf("day2_step_up"), "Główne", 2, "8-10/str", "3010", "RIR2", 90),
            slot("day2_chest_fly", "cable_fly", listOf("dumbbell_fly"), "Superseria 3", 3, "12-15", "3010", "RIR1", 45),
            slot("day2_rear_delts", "day2_face_pull", listOf("day1_rear_delt_raise"), "Superseria 3", 3, "12-15", "2011", "RIR1", 45),
            slot("day2_biceps", "day1_hammer_curl", listOf("day1_supinated_curl"), "Superseria 4", 3, "10-12", "3010", "RIR1", 45),
            slot("day2_triceps", "day2_triceps_rope_extension", listOf("cable_bar_pushdown"), "Superseria 4", 3, "10-15", "3010", "RIR1", 45)
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
            ?.takeIf { it.selectable && it.id !in selectedIds }
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
