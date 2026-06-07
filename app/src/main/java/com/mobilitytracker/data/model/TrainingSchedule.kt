package com.mobilitytracker.data.model

import java.time.DayOfWeek
import java.time.LocalDate

val DEFAULT_MOBILITY_DAYS: Set<DayOfWeek> = DayOfWeek.values().toSet()
val DEFAULT_STRENGTH_DAYS: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)

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
