package com.mobilitytracker.data.model

import java.time.DayOfWeek
import java.time.LocalDate

val DEFAULT_MOBILITY_DAYS: Set<DayOfWeek> = DayOfWeek.values().toSet()
val DEFAULT_STRENGTH_DAYS: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
val DEFAULT_STRENGTH_DAY_PLAN_ASSIGNMENTS: Map<DayOfWeek, Int> = mapOf(
    DayOfWeek.MONDAY to 1,
    DayOfWeek.THURSDAY to 2
)

fun encodeTrainingDays(days: Set<DayOfWeek>): String =
    days.map { it.value }.sorted().joinToString(",")

fun decodeTrainingDays(raw: String?, defaultDays: Set<DayOfWeek>): Set<DayOfWeek> {
    if (raw.isNullOrBlank()) return defaultDays

    return raw.split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .map { DayOfWeek.of(it) }
        .toSet()
}

fun encodeStrengthDayPlanAssignments(assignments: Map<DayOfWeek, Int>): String =
    assignments.entries
        .filter { it.value in 1..2 }
        .sortedBy { it.key.value }
        .joinToString(",") { "${it.key.value}:${it.value}" }

fun decodeStrengthDayPlanAssignments(raw: String?): Map<DayOfWeek, Int> {
    if (raw == null) return DEFAULT_STRENGTH_DAY_PLAN_ASSIGNMENTS
    if (raw.isBlank()) return emptyMap()

    return raw.split(",")
        .mapNotNull { token ->
            val parts = token.split(":")
            val dayValue = parts.getOrNull(0)?.trim()?.toIntOrNull()
            val planDay = parts.getOrNull(1)?.trim()?.toIntOrNull()
            if (dayValue in 1..7 && planDay in 1..2) {
                DayOfWeek.of(dayValue!!) to planDay!!
            } else {
                null
            }
        }
        .toMap()
}

fun isTrainingScheduled(date: LocalDate, days: Set<DayOfWeek>): Boolean =
    date.dayOfWeek in days

fun countScheduledSessions(
    startDate: LocalDate,
    targetDate: LocalDate,
    days: Set<DayOfWeek>
): Int {
    if (targetDate.isBefore(startDate) || days.isEmpty()) return 0

    var count = 0
    var date = startDate
    while (!date.isAfter(targetDate)) {
        if (date.dayOfWeek in days) count++
        date = date.plusDays(1)
    }
    return count
}
