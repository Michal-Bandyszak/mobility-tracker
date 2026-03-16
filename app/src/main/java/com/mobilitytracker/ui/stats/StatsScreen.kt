package com.mobilitytracker.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.domain.model.CycleStats
import com.mobilitytracker.ui.theme.CompletedGreen
import com.mobilitytracker.ui.theme.StreakOrange

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "TWÓJ POSTĘP",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (stats == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Uruchom program aby zobaczyć statystyki",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        val s = stats!!

        // Pierścień postępu
        ProgressRingCard(stats = s)

        // Statystyki ogólne
        StatRow("Ukończone cykle", s.completedCycles.toString())
        StatRow("Najdłuższa seria", "${s.longestStreak} dni")
        if (s.currentStreak > 1) {
            StatRow("Aktualna seria", "${s.currentStreak} dni", StreakOrange)
        }

        // Rozkład cykli
        if (s.cycleBreakdown.isNotEmpty()) {
            CycleBreakdownCard(stats = s)
        }

        // Kategorie
        if (s.categoryStats.isNotEmpty()) {
            CategoryStatsCard(categoryStats = s.categoryStats)
        }
    }
}

@Composable
private fun ProgressRingCard(stats: CycleStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                text = "Cykl ${stats.currentCycleNumber} — bieżący postęp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun CycleBreakdownCard(stats: CycleStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ROZKŁAD CYKLI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            stats.cycleBreakdown.forEach { breakdown ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                        modifier = Modifier.weight(0.6f).height(8.dp),
                        color = if (breakdown.isComplete) CompletedGreen else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${breakdown.completedCount}/${breakdown.totalCount}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(0.2f)
                    )
                    if (breakdown.isComplete) {
                        Text("⭐", fontSize = 16.sp, modifier = Modifier.weight(0.08f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryStatsCard(categoryStats: Map<String, Int>) {
    val categoryNames = mapOf(
        "biodra" to "Biodra",
        "kregoslup" to "Kręgosłup",
        "barki" to "Barki",
        "tylna_tasma" to "Tylna taśma",
        "przednia_tasma" to "Przednia taśma",
        "inne" to "Inne"
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "KATEGORIE (wszystkie cykle)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            categoryStats.entries.sortedByDescending { it.value }.forEach { (key, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = categoryNames[key] ?: key,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$count razy",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
