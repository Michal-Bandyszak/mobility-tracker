package com.mobilitytracker.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.mobilitytracker.domain.model.StrengthStatsOverview
import com.mobilitytracker.domain.model.StrengthTrend
import com.mobilitytracker.ui.components.ExpressiveHeader
import com.mobilitytracker.ui.components.ExpressiveScreen
import com.mobilitytracker.ui.theme.CompletedGreen
import com.mobilitytracker.ui.theme.StreakOrange
import java.util.Locale

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val strengthStats by viewModel.strengthStats.collectAsStateWithLifecycle()

    ExpressiveScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ExpressiveHeader(
                eyebrow = "Statystyki",
                title = "Panel treningu",
                subtitle = "Najważniejsze sygnały: ostatnia sesja, rytm i ćwiczenia, które realnie idą do przodu."
            )

            if (stats == null) {
                EmptyStatsCard()
                StatsHeroCard(strengthStats = strengthStats)
                StrengthStatsSection(strengthStats = strengthStats)
                return@Column
            }

            val mobilityStats = stats!!
            ChallengeJourneyCard(mobilityStats.challenge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    label = "Aktualna seria",
                    value = "${mobilityStats.currentStreak} dni",
                    valueColor = StreakOrange,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Ten tydzień",
                    value = "${mobilityStats.challenge.completedThisWeek}/7",
                    modifier = Modifier.weight(1f)
                )
            }

            ProgressRingCard(stats = mobilityStats)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    label = "Ukończone cykle",
                    value = mobilityStats.completedCycles.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Najdłuższa seria",
                    value = "${mobilityStats.longestStreak} dni",
                    modifier = Modifier.weight(1f)
                )
            }

            StatsHeroCard(strengthStats = strengthStats)
            StrengthStatsSection(strengthStats = strengthStats)

            if (mobilityStats.cycleBreakdown.isNotEmpty()) {
                CycleBreakdownCard(stats = mobilityStats)
            }

        }
    }
}

@Composable
private fun ChallengeJourneyCard(challenge: MobilityChallengeState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (challenge.isComplete) "WYZWANIE UKOŃCZONE" else "12 TYGODNI MOBILNOŚCI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${challenge.completedSessions}/$MOBILITY_CHALLENGE_SESSIONS sesji • film czeka, dopóki go nie wykonasz",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            repeat(12) { weekIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "T${weekIndex + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(26.dp)
                    )
                    repeat(7) { dayIndex ->
                        val session = weekIndex * 7 + dayIndex + 1
                        val completed = session in challenge.completedSessionNumbers
                        val active = !challenge.isComplete && session == challenge.activeSessionNumber
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    when {
                                        completed -> CompletedGreen
                                        active -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (completed) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHeroCard(strengthStats: StrengthStats) {
    val contentColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "SIŁA I MOBILNOŚĆ",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.72f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strengthStats.notableProgress?.heroTitle() ?: "Zapisuj treningi, nie szum.",
                style = MaterialTheme.typography.headlineLarge,
                color = contentColor,
                fontWeight = FontWeight.Black
            )
            Text(
                text = strengthStats.notableProgress?.heroSubtitle()
                    ?: "Po pierwszych sesjach pokażę ostatni trening, kompletność serii i realny progres ćwiczeń.",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.78f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetric(
                    label = "Ten miesiąc",
                    value = "${strengthStats.overview.sessionsThisMonth} sesje",
                    modifier = Modifier.weight(1f)
                )
                HeroMetric(
                    label = "Ostatnio",
                    value = strengthStats.overview.latestSessionPlanDay?.let { "T$it" } ?: "brak",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(contentColor.copy(alpha = 0.10f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.72f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyStatsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Uruchom program, aby zobaczyć statystyki mobilności.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StrengthStatsSection(strengthStats: StrengthStats) {
    SectionHeader(
        title = "Siła",
        subtitle = "Ostatni trening, kompletność serii i progres ćwiczeń."
    )

    if (!strengthStats.hasLogs) {
        EmptyStrengthCard()
        return
    }

    StrengthOverviewCard(overview = strengthStats.overview)

    if (strengthStats.exerciseProgress.isNotEmpty()) {
        SectionHeader(
            title = "Progres ćwiczeń",
            subtitle = "Ostatni wynik, najlepszy wynik i kierunek zmiany dla danego typu ćwiczenia."
        )
        strengthStats.exerciseProgress.forEach { progress ->
            ExerciseProgressCard(progress = progress)
        }
    }

    if (strengthStats.recentSessions.isNotEmpty()) {
        SectionHeader(
            title = "Ostatnie sesje",
            subtitle = "Czytelny zapis ostatnich treningów A/B."
        )
        strengthStats.recentSessions.forEach { session ->
            SessionSummaryCard(session = session)
        }
    }
}

@Composable
private fun EmptyStrengthCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Brak logów siłowych",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Po pierwszym zapisanym treningu zobaczysz ostatnią sesję, kompletność serii i progres ćwiczeń.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StrengthOverviewCard(overview: StrengthStatsOverview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    label = "Wszystkie sesje",
                    value = overview.totalSessions.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Ten miesiąc",
                    value = overview.sessionsThisMonth.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    label = "Śledzone ćwiczenia",
                    value = overview.trackedExercises.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Ostatnia sesja",
                    value = overview.latestSessionLabel(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExerciseProgressCard(progress: StrengthExerciseProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${progress.section} · ${trackingLabel(progress.trackingType)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TrendPill(trend = progress.trend)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PerformanceBlock(
                    label = "Ostatnio",
                    performance = progress.latest,
                    trackingType = progress.trackingType,
                    modifier = Modifier.weight(1f)
                )
                PerformanceBlock(
                    label = "Najlepiej",
                    performance = progress.best,
                    trackingType = progress.trackingType,
                    modifier = Modifier.weight(1f)
                )
            }

            if (progress.recentPerformances.size > 1) {
                Sparkline(
                    values = progress.recentPerformances
                        .reversed()
                        .map { it.scoreFor(progress.trackingType) }
                )
            }
        }
    }
}

@Composable
private fun PerformanceBlock(
    label: String,
    performance: StrengthPerformance?,
    trackingType: TrackingType,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = performance?.formatFor(trackingType) ?: "brak",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (performance != null) {
                Text(
                    text = "T${performance.planDay} · ${performance.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(session: StrengthSessionSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "T${session.planDay} · ${session.date}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${session.completedTrackedSets}/${session.plannedTrackedSets} serii · ${session.exerciseSummaries.size} ćw.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${(session.completionRatio.coerceIn(0f, 1f) * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { session.completionRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
            )

            session.exerciseSummaries.take(5).forEach { exercise ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = exercise.bestPerformance?.formatFor(exercise.trackingType) ?: "${exercise.loggedSets} serii",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 5.dp, height = 40.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.tertiary)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun TrendPill(trend: StrengthTrend) {
    val (label, color) = when (trend) {
        StrengthTrend.UP -> "rośnie" to CompletedGreen
        StrengthTrend.FLAT -> "stabilnie" to MaterialTheme.colorScheme.primary
        StrengthTrend.DOWN -> "spadek" to MaterialTheme.colorScheme.error
        StrengthTrend.UNKNOWN -> "brak trendu" to MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Sparkline(values: List<Double>) {
    val cleanValues = values.filter { it > 0.0 }
    if (cleanValues.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val min = cleanValues.minOrNull() ?: return
    val max = cleanValues.maxOrNull() ?: return
    val range = (max - min).takeIf { it > 0.0 } ?: 1.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        val xStep = size.width / (cleanValues.size - 1).coerceAtLeast(1)
        val points = cleanValues.mapIndexed { index, value ->
            val normalized = ((value - min) / range).toFloat()
            Offset(
                x = index * xStep,
                y = size.height - (normalized * size.height)
            )
        }

        for (index in 0 until points.lastIndex) {
            drawLine(
                color = lineColor,
                start = points[index],
                end = points[index + 1],
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ProgressRingCard(stats: CycleStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var animationPlayed by remember { mutableStateOf(false) }
            val progress by animateFloatAsState(
                targetValue = if (animationPlayed) stats.currentCycleProgress else 0f,
                animationSpec = tween(durationMillis = 1000),
                label = "progress"
            )
            LaunchedEffect(stats) { animationPlayed = true }

            val primaryColor = MaterialTheme.colorScheme.primary
            val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    drawArc(
                        color = surfaceVariantColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(stats.currentCycleProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stats.currentCycleCompletedCount}/$CYCLE_LENGTH",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Cykl ${stats.currentCycleNumber} - bieżący postęp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CycleBreakdownCard(stats: CycleStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ROZKŁAD CYKLI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            stats.cycleBreakdown.forEach { breakdown ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "C${breakdown.cycleNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(0.12f)
                    )
                    LinearProgressIndicator(
                        progress = { breakdown.completedCount.toFloat() / breakdown.totalCount },
                        modifier = Modifier
                            .weight(0.6f)
                            .height(8.dp),
                        color = if (breakdown.isComplete) CompletedGreen else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${breakdown.completedCount}/${breakdown.totalCount}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(0.2f)
                    )
                    if (breakdown.isComplete) {
                        Text("*", fontSize = 16.sp, modifier = Modifier.weight(0.08f))
                    }
                }
            }
        }
    }
}

private fun StrengthPerformance.formatFor(trackingType: TrackingType): String =
    when (trackingType) {
        TrackingType.WEIGHT_AND_REPS -> {
            val weight = weightKg?.let { "${formatNumber(it)} kg" }
            val repetitions = reps?.let { "$it powt." }
            listOfNotNull(weight, repetitions).joinToString(" x ").ifBlank { "brak" }
        }
        TrackingType.BAND_AND_REPS -> {
            val band = bandColor?.let { "guma $it" }
            val repetitions = reps?.let { "$it powt." }
            listOfNotNull(band, repetitions).joinToString(" • ").ifBlank { "brak" }
        }
        TrackingType.REPS_ONLY -> reps?.let { "$it powt." } ?: "brak"
        TrackingType.TIME_ONLY -> timeSeconds?.let { "${it}s" } ?: "brak"
        TrackingType.NONE -> "wykonano"
    }

private fun StrengthProgressHighlight.heroTitle(): String =
    when (trend) {
        StrengthTrend.UP -> "${exerciseName}: lepiej niż ostatnio"
        StrengthTrend.FLAT -> "${exerciseName}: stabilny wynik"
        StrengthTrend.DOWN -> "${exerciseName}: słabszy ostatni zapis"
        StrengthTrend.UNKNOWN -> "${exerciseName}: pierwszy punkt odniesienia"
    }

private fun StrengthProgressHighlight.heroSubtitle(): String {
    val latestValue = latest.formatFor(trackingType)
    val previousValue = previous?.formatFor(trackingType)
    return if (previousValue != null) {
        "Ostatnio $latestValue, poprzednio $previousValue. T${latest.planDay} · ${latest.date}"
    } else {
        "Ostatnio $latestValue. Zapisz kolejną sesję, a pokażę kierunek zmiany."
    }
}

private fun StrengthStatsOverview.latestSessionLabel(): String {
    if (latestSessionDate == null) return "brak"
    val plan = latestSessionPlanDay?.let { "T$it" } ?: "T?"
    val sets = if (latestPlannedTrackedSets > 0) {
        "$latestCompletedTrackedSets/$latestPlannedTrackedSets"
    } else {
        latestCompletedTrackedSets.toString()
    }
    return "$plan · $sets serii"
}

private fun StrengthPerformance.scoreFor(trackingType: TrackingType): Double =
    when (trackingType) {
        TrackingType.WEIGHT_AND_REPS -> weightKg ?: 0.0
        TrackingType.BAND_AND_REPS -> reps?.toDouble() ?: 0.0
        TrackingType.REPS_ONLY -> reps?.toDouble() ?: 0.0
        TrackingType.TIME_ONLY -> timeSeconds?.toDouble() ?: 0.0
        TrackingType.NONE -> 0.0
    }

private fun trackingLabel(type: TrackingType): String =
    when (type) {
        TrackingType.WEIGHT_AND_REPS -> "ciężar + powtórzenia"
        TrackingType.BAND_AND_REPS -> "guma + powtórzenia"
        TrackingType.REPS_ONLY -> "powtórzenia"
        TrackingType.TIME_ONLY -> "czas"
        TrackingType.NONE -> "bez progresu"
    }

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
