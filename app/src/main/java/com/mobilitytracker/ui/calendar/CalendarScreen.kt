package com.mobilitytracker.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilitytracker.domain.model.DayWorkout
import com.mobilitytracker.ui.components.ExpressiveHeader
import com.mobilitytracker.ui.components.ExpressiveScreen
import com.mobilitytracker.ui.theme.CompletedGreen
import com.mobilitytracker.ui.theme.TodayAmber

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.workouts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Uruchom program na ekranie Dzisiaj",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    ExpressiveScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ExpressiveHeader(
                    eyebrow = "Kalendarz",
                    title = "Historia ruchu",
                    subtitle = "Cykle mobilności i faktyczne dni treningów siłowych w jednym miejscu."
                )
            }

            // Nawigacja między cyklami
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousCycle() },
                        enabled = state.currentCycle > 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Poprzedni cykl")
                    }

                    Text(
                        text = "CYKL ${state.currentCycle}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { viewModel.nextCycle() },
                        enabled = state.currentCycle < state.maxCycle
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Następny cykl")
                    }
                }
            }

            // Siatka dni 1-17
            item {
                CycleGrid(
                    workouts = state.workouts,
                    todayCycle = state.todayCycle,
                    todayVideoOrder = state.todayVideoOrder,
                    currentCycle = state.currentCycle
                )
            }

            item {
                StrengthHistoryCard(entries = state.strengthHistory)
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Lista filmów:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(state.workouts) { workout ->
                WorkoutListItem(
                    workout = workout,
                    isToday = state.currentCycle == state.todayCycle && workout.videoOrder == state.todayVideoOrder,
                    onToggle = { viewModel.toggleCompletion(workout.cycleNumber, workout.videoOrder) },
                    onPlay = workout.videoUri
                )
            }
        }
    }
}

@Composable
private fun StrengthHistoryCard(entries: List<StrengthCalendarEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Historia siłowni",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Prawdziwe dni wykonania treningu z zapisanych serii.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entries.isEmpty()) {
                Text(
                    text = "Brak zapisanych treningów siłowych.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }
            entries.take(12).forEach { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "T${entry.planDay}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = entry.dayLabel.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = entry.date.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "${entry.loggedSets} serii",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CycleGrid(
    workouts: List<DayWorkout>,
    todayCycle: Int,
    todayVideoOrder: Int,
    currentCycle: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 3 rzędy: 7 + 7 + 3
            val rows = listOf(workouts.take(7), workouts.drop(7).take(7), workouts.drop(14))
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { workout ->
                        val isToday = currentCycle == todayCycle && workout.videoOrder == todayVideoOrder
                        DayCell(
                            number = workout.videoOrder,
                            isCompleted = workout.isCompleted,
                            isToday = isToday
                        )
                    }
                    // Pad last row
                    repeat(7 - row.size) {
                        Box(modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Legenda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("✅ zrobione", style = MaterialTheme.typography.labelSmall)
                Text("🔵 dzisiaj", style = MaterialTheme.typography.labelSmall)
                Text("○ brak", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DayCell(number: Int, isCompleted: Boolean, isToday: Boolean) {
    val bgColor = when {
        isCompleted -> CompletedGreen
        isToday -> TodayAmber
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isCompleted || isToday -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WorkoutListItem(
    workout: DayWorkout,
    isToday: Boolean,
    onToggle: () -> Unit,
    onPlay: String?
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isToday -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${workout.videoOrder}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.definition.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
                if (isToday) {
                    Text(
                        text = "Dzisiaj",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (onPlay != null) {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(onPlay), "video/*")
                    }
                    context.startActivity(intent)
                }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Odtwórz",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Checkbox(
                checked = workout.isCompleted,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
