package com.mobilitytracker.data.model

const val CYCLE_LENGTH = 17

val WORKOUT_PLAN = listOf(
    // Dzień 1-6
    WorkoutDefinition(1,  "Biodra z sprzętem",                "biodra_z_sprzetem.mp4",                0, "biodra"),
    WorkoutDefinition(2,  "Kręgosłup ze sprzętem",            "kregoslup_ze_sprzetem.mp4",            0, "kregoslup"),
    WorkoutDefinition(3,  "Obręcz barkowa z sprzętem",        "obrecz_barkowa_z_sprzetem.mp4",        0, "barki"),
    WorkoutDefinition(4,  "Tylna taśma z sprzętem",           "tylna_tasma_z_sprzetem.mp4",           0, "tylna_tasma"),
    WorkoutDefinition(5,  "Przednia taśma",                   "przednia_tasma.mp4",                   0, "przednia_tasma"),
    WorkoutDefinition(6,  "Biodra bez sprzętu",               "biodra_bez_sprzetu.mp4",               0, "biodra"),
    // Dzień 7-12
    WorkoutDefinition(7,  "Staw skokowy",                     "staw_skokowy.mp4",                     0, "inne"),
    WorkoutDefinition(8,  "Kręgosłup bez sprzętu",            "kregoslup_bez_sprzetu.mp4",            0, "kregoslup"),
    WorkoutDefinition(9,  "Obręcz barkowa bez sprzętu",       "obrecz_barkowa_bez_sprzetu.mp4",       0, "barki"),
    WorkoutDefinition(10, "Tylna taśma bez sprzętu",          "tylna_tasma_bez_sprzetu.mp4",          0, "tylna_tasma"),
    WorkoutDefinition(11, "Nadgarstki",                       "nadgarstki.mp4",                       0, "inne"),
    WorkoutDefinition(12, "Biodra bez sprzętu zestaw 2",      "biodra_bez_sprzetu_2.mp4",             0, "biodra"),
    // Dzień 13-17
    WorkoutDefinition(13, "Przednia taśma zestaw 2",          "przednia_tasma_2.mp4",                 0, "przednia_tasma"),
    WorkoutDefinition(14, "Kręgosłup bez sprzętu zestaw 2",   "kregoslup_bez_sprzetu_2.mp4",          0, "kregoslup"),
    WorkoutDefinition(15, "Obręcz barkowa sprzęt zestaw 2",   "obrecz_barkowa_sprzet_2.mp4",          0, "barki"),
    WorkoutDefinition(16, "Tylna taśma bez sprzętu zestaw 2", "tylna_tasma_bez_sprzetu_2.mp4",        0, "tylna_tasma"),
    WorkoutDefinition(17, "Odcinek szyjny",                   "odcinek_szyjny.mp4",                   0, "kregoslup"),
)
// Odstępy między tą samą kategorią:
// biodra:          1 → 6 → 12  (5, 6 dni)
// kręgosłup:       2 → 8 → 14  (6, 6 dni) + odcinek szyjny na 17
// barki:           3 → 9 → 15  (6, 6 dni)
// tylna taśma:     4 → 10 → 16 (6, 6 dni)
// przednia taśma:  5 → 13      (8 dni)
