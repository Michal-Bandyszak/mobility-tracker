package com.mobilitytracker.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.data.model.TrackingType
import com.mobilitytracker.domain.model.CycleStats
import com.mobilitytracker.domain.model.MOBILITY_CHALLENGE_SESSIONS
import com.mobilitytracker.domain.model.MobilityChallengeState
import com.mobilitytracker.domain.model.StrengthExerciseProgress
import com.mobilitytracker.domain.model.StrengthPerformance
import com.mobilitytracker.domain.model.StrengthProgressHighlight
import com.mobilitytracker.domain.model.StrengthSessionSummary
import com.mobilitytracker.domain.model.StrengthStats
import com.mobilitytracker.domain.model.StrengthTrend
import com.mobilitytracker.ui.components.AppCard
import com.mobilitytracker.ui.components.CardHeader
import com.mobilitytracker.ui.components.ExpandChevron
import com.mobilitytracker.ui.components.ExpressiveHeader
import com.mobilitytracker.ui.components.ExpressiveScreen
import com.mobilitytracker.ui.components.ScreenColumn
import com.mobilitytracker.ui.components.SectionTitle
import com.mobilitytracker.ui.components.StatValue
import com.mobilitytracker.ui.components.ThinProgress
import com.mobilitytracker.ui.components.daysLabel
import com.mobilitytracker.ui.components.planName
import com.mobilitytracker.ui.components.setsLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(
    scrollState: ScrollState,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val strengthStats by viewModel.strengthStats.collectAsStateWithLifecycle()

    ExpressiveScreen {
        ScreenColumn(scrollState) {
            ExpressiveHeader(title = "Statystyki")

            SectionTitle("Mobilność")
            val mobilityStats = stats
            if (mobilityStats == null) {
                AppCard {
                    CardHeader(
                        title = "Brak danych",
                        meta = "Zacznij program na ekranie Dzisiaj, a tu pojawi się postęp mobilności."
                    )
                }
            } else {
                MobilityOverviewCard(mobilityStats)
                ChallengeJourneyCard(mobilityStats.challenge)
                if (mobilityStats.cycleBreakdown.size > 1) {
                    CycleBreakdownCard(mobilityStats)
                }
            }

            SectionTitle("Siła")
            if (!strengthStats.hasLogs) {
                AppCard {
                    CardHeader(
                        title = "Brak zapisanych treningów",
                        meta = "Po pierwszym treningu zobaczysz tu ostatnie sesje i progres ćwiczeń."
                    )
                }
            } else {
                StrengthSummaryCard(strengthStats)
                if (strengthStats.exerciseProgress.isNotEmpty()) {
                    ExerciseProgressCard(strengthStats.exerciseProgress)
                }
                if (strengthStats.recentSessions.isNotEmpty()) {
                    RecentSessionsCard(strengthStats.recentSessions.take(3))
                }
            }
        }
    }
}

@Composable
private fun MobilityOverviewCard(stats: CycleStats) {
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CycleRing(progress = stats.currentCycleProgress)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Cykl ${stats.currentCycleNumber}", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${stats.currentCycleCompletedCount} z $CYCLE_LENGTH filmów",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ukończone cykle: ${stats.completedCycles}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row {
            StatValue(value = daysLabel(stats.currentStreak), label = "seria", modifier = Modifier.weight(1f))
            StatValue(value = daysLabel(stats.longestStreak), label = "rekord serii", modifier = Modifier.weight(1f))
            StatValue(value = "${stats.challenge.completedThisWeek}/7", label = "ten tydzień", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CycleRing(progress: Float) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animated by animateFloatAsState(
        targetValue = if (animationPlayed) progress else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "cycleProgress"
    )
    LaunchedEffect(progress) { animationPlayed = true }

    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val strokeWidth = 9.dp.toPx()
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth))
            drawArc(primary, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
        }
        Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ChallengeJourneyCard(challenge: MobilityChallengeState) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    AppCard {
        CardHeader(
            title = if (challenge.isComplete) "Wyzwanie ukończone" else "Wyzwanie 12 tygodni",
            meta = "${challenge.completedSessions} z $MOBILITY_CHALLENGE_SESSIONS sesji",
            onClick = { expanded = !expanded },
            trailing = { ExpandChevron(expanded) }
        )
        ThinProgress(progress = challenge.completedSessions.toFloat() / MOBILITY_CHALLENGE_SESSIONS)

        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(12) { weekIndex ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${weekIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(20.dp)
                        )
                        repeat(7) { dayIndex ->
                            val session = weekIndex * 7 + dayIndex + 1
                            SessionDot(
                                done = session in challenge.completedSessionNumbers,
                                active = !challenge.isComplete && session == challenge.activeSessionNumber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Text(
                    text = "Wiersz to tydzień, kratka to sesja. Wypełniona: zrobiona, z obrysem: następna.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SessionDot(done: Boolean, active: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    val base = modifier
        .height(18.dp)
        .clip(shape)
    Box(
        modifier = when {
            done -> base.background(MaterialTheme.colorScheme.primary)
            active -> base.border(2.dp, MaterialTheme.colorScheme.primary, shape)
            else -> base.background(MaterialTheme.colorScheme.surfaceVariant)
        }
    )
}

@Composable
private fun CycleBreakdownCard(stats: CycleStats) {
    AppCard {
        CardHeader(title = "Cykle")
        stats.cycleBreakdown.forEach { breakdown ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cykl ${breakdown.cycleNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(64.dp)
                )
                ThinProgress(
                    progress = breakdown.completedCount.toFloat() / breakdown.totalCount,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${breakdown.completedCount}/${breakdown.totalCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StrengthSummaryCard(strengthStats: StrengthStats) {
    val overview = strengthStats.overview
    AppCard {
        strengthStats.notableProgress?.let { highlight ->
            Column {
                Text(text = highlight.heroTitle(), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = highlight.heroSubtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row {
            StatValue(value = "${overview.totalSessions}", label = "wszystkie sesje", modifier = Modifier.weight(1f))
            StatValue(value = "${overview.sessionsThisMonth}", label = "w tym miesiącu", modifier = Modifier.weight(1f))
            StatValue(value = "${overview.trackedExercises}", label = "ćwiczenia", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExerciseProgressCard(progress: List<StrengthExerciseProgress>) {
    AppCard {
        CardHeader(title = "Progres ćwiczeń", meta = "Ostatni wynik i trend ciężaru")
        progress.forEachIndexed { index, exercise ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ExerciseProgressRow(exercise)
        }
    }
}

@Composable
private fun ExerciseProgressRow(progress: StrengthExerciseProgress) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = progress.name, style = MaterialTheme.typography.titleSmall)
            Text(
                text = progress.latest?.let { "${it.formatFor(progress.trackingType)} · ${shortDate(it.date)}" } ?: "brak zapisu",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val best = progress.best
            if (best != null && best != progress.latest) {
                Text(
                    text = "Najlepiej: ${best.formatFor(progress.trackingType)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (progress.recentPerformances.size > 1) {
                Sparkline(
                    values = progress.recentPerformances.reversed().map { it.scoreFor(progress.trackingType) },
                    modifier = Modifier.size(width = 64.dp, height = 24.dp)
                )
            }
            TrendLabel(progress.trend)
        }
    }
}

@Composable
private fun RecentSessionsCard(sessions: List<StrengthSessionSummary>) {
    AppCard {
        CardHeader(title = "Ostatnie sesje")
        sessions.forEachIndexed { index, session ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Trening ${planName(session.planDay)} · ${shortDate(session.date)}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${session.completedTrackedSets}/${session.plannedTrackedSets} serii",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ThinProgress(progress = session.completionRatio)
                session.exerciseSummaries.take(5).forEach { exercise ->
                    Row {
                        Text(
                            text = exercise.exerciseName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = exercise.bestPerformance?.formatFor(exercise.trackingType) ?: setsLabel(exercise.loggedSets),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendLabel(trend: StrengthTrend) {
    val (label, color) = when (trend) {
        StrengthTrend.UP -> "↑ rośnie" to MaterialTheme.colorScheme.primary
        StrengthTrend.FLAT -> "→ stabilnie" to MaterialTheme.colorScheme.onSurfaceVariant
        StrengthTrend.DOWN -> "↓ spadek" to MaterialTheme.colorScheme.error
        StrengthTrend.UNKNOWN -> "za mało danych" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
private fun Sparkline(values: List<Double>, modifier: Modifier = Modifier) {
    val cleanValues = values.filter { it > 0.0 }
    if (cleanValues.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val min = cleanValues.min()
    val range = (cleanValues.max() - min).takeIf { it > 0.0 } ?: 1.0

    Canvas(modifier = modifier) {
        val inset = 2.dp.toPx()
        val xStep = (size.width - 2 * inset) / (cleanValues.size - 1)
        val points = cleanValues.mapIndexed { index, value ->
            Offset(
                x = inset + index * xStep,
                y = size.height - inset - ((value - min) / range).toFloat() * (size.height - 2 * inset)
            )
        }
        for (index in 0 until points.lastIndex) {
            drawLine(lineColor, points[index], points[index + 1], strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

private fun StrengthPerformance.formatFor(trackingType: TrackingType): String =
    when (trackingType) {
        TrackingType.WEIGHT_AND_REPS -> {
            val weight = weightKg?.let { "${formatNumber(it)} kg" }
            val repetitions = reps?.let { "$it powt." }
            listOfNotNull(weight, repetitions).joinToString(" × ").ifBlank { "brak" }
        }
        TrackingType.BAND_AND_REPS -> {
            val band = bandColor?.let { "guma $it" }
            val repetitions = reps?.let { "$it powt." }
            listOfNotNull(band, repetitions).joinToString(", ").ifBlank { "brak" }
        }
        TrackingType.REPS_ONLY -> reps?.let { "$it powt." } ?: "brak"
        TrackingType.TIME_ONLY -> timeSeconds?.let { "$it s" } ?: "brak"
        TrackingType.NONE -> "wykonano"
    }

private fun StrengthProgressHighlight.heroTitle(): String =
    when (trend) {
        StrengthTrend.UP -> "$exerciseName: lepiej niż ostatnio"
        StrengthTrend.FLAT -> "$exerciseName: stabilny wynik"
        StrengthTrend.DOWN -> "$exerciseName: słabszy ostatni zapis"
        StrengthTrend.UNKNOWN -> "$exerciseName: pierwszy punkt odniesienia"
    }

private fun StrengthProgressHighlight.heroSubtitle(): String {
    val latestValue = latest.formatFor(trackingType)
    val previousValue = previous?.formatFor(trackingType)
    return if (previousValue != null) {
        "Ostatnio $latestValue, wcześniej $previousValue"
    } else {
        "Ostatnio $latestValue. Po kolejnej sesji pokażę kierunek zmiany."
    }
}

private fun StrengthPerformance.scoreFor(trackingType: TrackingType): Double =
    when (trackingType) {
        TrackingType.WEIGHT_AND_REPS -> weightKg ?: 0.0
        TrackingType.BAND_AND_REPS -> reps?.toDouble() ?: 0.0
        TrackingType.REPS_ONLY -> reps?.toDouble() ?: 0.0
        TrackingType.TIME_ONLY -> timeSeconds?.toDouble() ?: 0.0
        TrackingType.NONE -> 0.0
    }

private fun shortDate(iso: String): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d.MM")) }.getOrDefault(iso)

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
