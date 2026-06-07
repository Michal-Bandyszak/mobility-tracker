package com.mobilitytracker.data.model

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

data class StrengthExerciseDefinition(
    val id: String,
    val section: String,
    val name: String,
    val sets: Int,
    val reps: String,
    val tempo: String = "",
    val rir: String = "",
    val restSeconds: Int? = null,
    val notes: String = "",
    val videoUrl: String? = null,
    val trackingType: TrackingType = TrackingType.WEIGHT_AND_REPS
)

data class StrengthResourceLink(
    val title: String,
    val url: String
)

val STRENGTH_RESOURCE_LINKS = listOf(
    StrengthResourceLink(
        "Rozgrzewka dynamiczna z planu",
        "https://www.youtube.com/watch?v=Oz_NQCflcgk&t=346s"
    ),
    StrengthResourceLink(
        "Rozciąganie opcjonalne w dni wolne",
        "https://www.youtube.com/watch?v=qzdYP2OZt5s"
    ),
    StrengthResourceLink(
        "Omówienie tempa ćwiczeń",
        "https://www.youtube.com/watch?v=ZsHyiin-qMY"
    )
)

val STRENGTH_WORKOUT_PLAN = listOf(
    StrengthWorkoutDefinition(
        day = 1,
        title = "Trening siłowy - Dzień 1",
        focus = "Full body: przysiad, hinge, poziome pchanie i pionowe przyciąganie",
        exercises = listOf(
            StrengthExerciseDefinition(
                id = "day1_cuban_rotation",
                section = "Aktywacja",
                name = "Cuban rotation z gumą lub hantlami",
                sets = 2,
                reps = "15",
                restSeconds = 30,
                notes = "Ruch pod kontrolą, lekki opór.",
                videoUrl = "https://www.zing.coach/exercises/dumbbell-cuban-rotations",
                trackingType = TrackingType.NONE
            ),
            StrengthExerciseDefinition(
                id = "day1_monster_walk",
                section = "Aktywacja",
                name = "Monster walk z gumą",
                sets = 2,
                reps = "12/str",
                restSeconds = 30,
                notes = "Aktywacja pośladka, kolana prowadź na zewnątrz.",
                videoUrl = "https://elite-performance-institute.com/exercise-library/glute-exercises/monster-walks/",
                trackingType = TrackingType.NONE
            ),
            StrengthExerciseDefinition(
                id = "day1_goblet_squat_pause",
                section = "Główne",
                name = "Przysiad goblet z pauzą",
                sets = 3,
                reps = "10",
                tempo = "31x1",
                rir = "RIR2",
                restSeconds = 90,
                notes = "Pauza w dole ruchu, pełna kontrola pozycji.",
                videoUrl = "https://www.youtube.com/watch?v=gm4ln6PO4rc",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day1_dumbbell_rdl",
                section = "Główne",
                name = "RDL z hantlami",
                sets = 3,
                reps = "10",
                tempo = "3010",
                rir = "RIR2",
                restSeconds = 90,
                notes = "Biodrowy wzorzec ruchu, napięty tułów, neutralny kręgosłup.",
                videoUrl = "https://www.youtube.com/shorts/5rIqP63yWFg",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day1_dumbbell_bench_press",
                section = "Główne",
                name = "Wyciskanie hantli na ławce poziomej",
                sets = 3,
                reps = "8-10",
                tempo = "3010",
                rir = "RIR2",
                restSeconds = 120,
                notes = "Łokcie około 45 stopni względem tułowia.",
                videoUrl = "https://barbend.com/dumbbell-bench-press/",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day1_lat_pulldown",
                section = "Główne",
                name = "Podciąganie nachwytem / lat pulldown",
                sets = 3,
                reps = "8-10",
                tempo = "3010",
                rir = "RIR2",
                restSeconds = 120,
                notes = "Pełen zakres ruchu, kontroluj łopatki.",
                videoUrl = "https://gymkee.com/exercises/lats/lat-pulldown/",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day1_lateral_raise",
                section = "Dodatkowe",
                name = "Wznosy ramion bokiem",
                sets = 3,
                reps = "12-15",
                tempo = "2011",
                rir = "RIR1",
                restSeconds = 60,
                notes = "Lekkie ciężary, bez bujania tułowiem.",
                videoUrl = "https://grabgains.com/exercises/dumbbell-lateral-raise",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day1_supinated_curl",
                section = "Dodatkowe",
                name = "Uginanie ramion z hantlami z supinacją",
                sets = 2,
                reps = "10-12",
                tempo = "3010",
                rir = "RIR1",
                restSeconds = 60,
                notes = "Bez bujania, łokcie stabilnie przy tułowiu.",
                videoUrl = "https://www.drugs.com/video/video-biceps-curl-with-dumbbell-285/",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day1_deadbug",
                section = "Dodatkowe",
                name = "Crunching skywalkers / deadbug",
                sets = 3,
                reps = "10-15",
                restSeconds = 60,
                notes = "Kontrola tułowia i oddechu.",
                videoUrl = "https://www.nasm.org/resource-center/exercise-library/dead-bug",
                trackingType = TrackingType.REPS_ONLY
            )
        )
    ),
    StrengthWorkoutDefinition(
        day = 2,
        title = "Trening siłowy - Dzień 2",
        focus = "Full body: hip thrust, unilateralne nogi, skos dodatni i wiosłowanie",
        exercises = listOf(
            StrengthExerciseDefinition(
                id = "day2_banded_glute_bridge",
                section = "Aktywacja",
                name = "Glute bridge z gumą",
                sets = 2,
                reps = "15",
                restSeconds = 30,
                notes = "Napięcie pośladków, nie przeprostowuj lędźwi.",
                videoUrl = "https://redefiningstrength.com/mini-band-glute-bridges/",
                trackingType = TrackingType.NONE
            ),
            StrengthExerciseDefinition(
                id = "day2_9090_hip_lift",
                section = "Aktywacja",
                name = "90/90 hip lift",
                sets = 2,
                reps = "5 oddechów",
                restSeconds = 30,
                notes = "Oddychanie i ustawienie miednicy.",
                videoUrl = "https://www.movecheck.net/articles/90-90-hip-lift-explained",
                trackingType = TrackingType.NONE
            ),
            StrengthExerciseDefinition(
                id = "day2_hip_thrust",
                section = "Główne",
                name = "Hip thrust ze sztangą / gumą",
                sets = 3,
                reps = "10-12",
                tempo = "3010",
                rir = "RIR2",
                restSeconds = 90,
                notes = "Pauza w górze, żebra schowane, mocne spięcie pośladków.",
                videoUrl = "https://gravitus.com/guides/exercises/barbell-hip-thrust/",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day2_split_squat",
                section = "Główne",
                name = "Split squat z hantlami",
                sets = 3,
                reps = "8/str",
                tempo = "3010",
                rir = "RIR2",
                restSeconds = 90,
                notes = "Kontrola pozycji, stabilny tułów i kolano.",
                videoUrl = "https://weighttraining.guide/exercises/dumbbell-split-squat/",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day2_incline_db_press",
                section = "Główne",
                name = "Incline DB Press",
                sets = 3,
                reps = "10",
                tempo = "3010",
                rir = "RIR2",
                restSeconds = 120,
                notes = "Skos 30-45 stopni, pełen kontrolowany zakres.",
                videoUrl = "https://gravitus.com/guides/exercises/incline-dumbbell-press/",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day2_seated_row",
                section = "Główne",
                name = "Seated row / low cable row",
                sets = 3,
                reps = "10",
                tempo = "3011",
                rir = "RIR2",
                restSeconds = 90,
                notes = "Jedna sekunda spięcia w końcówce ruchu.",
                videoUrl = "https://gym.com/exercises/seated-cable-row",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day2_face_pull",
                section = "Dodatkowe",
                name = "Face pull",
                sets = 2,
                reps = "12-15",
                tempo = "2011",
                rir = "RIR1",
                restSeconds = 60,
                notes = "Z zatrzymaniem, łokcie wysoko, bez szarpania.",
                videoUrl = "https://www.nasm.org/resource-center/exercise-library/face-pull",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day2_triceps_rope_extension",
                section = "Dodatkowe",
                name = "Triceps rope extension",
                sets = 2,
                reps = "12-15",
                tempo = "3010",
                rir = "RIR1",
                restSeconds = 60,
                notes = "Bez machania tułowiem, pełny wyprost łokcia.",
                videoUrl = "https://trainest.com/exercise/triceps-pushdown",
                trackingType = TrackingType.WEIGHT_AND_REPS
            ),
            StrengthExerciseDefinition(
                id = "day2_plank",
                section = "Dodatkowe",
                name = "Plank",
                sets = 3,
                reps = "30-45s",
                restSeconds = 60,
                notes = "Napięcie brzucha i pośladków, neutralny kręgosłup.",
                videoUrl = "https://en.wikipedia.org/wiki/Plank_(exercise)",
                trackingType = TrackingType.TIME_ONLY
            )
        )
    )
)

fun getStrengthWorkoutByDay(day: Int): StrengthWorkoutDefinition? =
    STRENGTH_WORKOUT_PLAN.find { it.day == day }
