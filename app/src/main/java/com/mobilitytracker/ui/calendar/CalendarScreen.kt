package com.mobilitytracker.ui.calendar

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.domain.model.DayWorkout
import com.mobilitytracker.ui.components.AppCard
import com.mobilitytracker.ui.components.CardHeader
import com.mobilitytracker.ui.components.DoneMark
import com.mobilitytracker.ui.components.ExpressiveHeader
import com.mobilitytracker.ui.components.ExpressiveScreen

@Composable
fun CalendarScreen(
    listState: LazyListState,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ExpressiveScreen {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@ExpressiveScreen
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ExpressiveHeader(title = "Kalendarz") }

            if (state.workouts.isEmpty()) {
                item {
                    AppCard {
                        CardHeader(title = "Brak programu", meta = "Zacznij program na ekranie Dzisiaj.")
                    }
                }
                return@LazyColumn
            }

            val isTodayCycle = state.currentCycle == state.todayCycle
            item {
                CycleCard(
                    cycleNumber = state.currentCycle,
                    workouts = state.workouts,
                    todayVideoOrder = state.todayVideoOrder.takeIf { isTodayCycle },
                    canGoBack = state.currentCycle > 1,
                    canGoForward = state.currentCycle < state.maxCycle,
                    onPrevious = viewModel::previousCycle,
                    onNext = viewModel::nextCycle
                )
            }
            item {
                WorkoutListCard(
                    workouts = state.workouts,
                    todayVideoOrder = state.todayVideoOrder.takeIf { isTodayCycle }
                )
            }
        }
    }
}

@Composable
private fun CycleCard(
    cycleNumber: Int,
    workouts: List<DayWorkout>,
    todayVideoOrder: Int?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, enabled = canGoBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Poprzedni cykl")
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Cykl $cycleNumber", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${workouts.count { it.isCompleted }} z $CYCLE_LENGTH filmów",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onNext, enabled = canGoForward) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Następny cykl")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            workouts.chunked(7).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { workout ->
                        DayCell(
                            number = workout.videoOrder,
                            isCompleted = workout.isCompleted,
                            isToday = workout.videoOrder == todayVideoOrder,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendItem("zrobione", fill = MaterialTheme.colorScheme.primary)
            LegendItem("dziś", outline = MaterialTheme.colorScheme.primary)
            LegendItem("czeka", fill = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun DayCell(number: Int, isCompleted: Boolean, isToday: Boolean, modifier: Modifier = Modifier) {
    val base = modifier
        .aspectRatio(1f)
        .clip(CircleShape)
    Box(
        modifier = when {
            isCompleted -> base.background(MaterialTheme.colorScheme.primary)
            isToday -> base.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            else -> base.background(MaterialTheme.colorScheme.surfaceVariant)
        },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.onPrimary
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LegendItem(label: String, fill: Color? = null, outline: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val dot = Modifier
            .size(12.dp)
            .clip(CircleShape)
        Box(
            modifier = when {
                fill != null -> dot.background(fill)
                outline != null -> dot.border(2.dp, outline, CircleShape)
                else -> dot
            }
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WorkoutListCard(workouts: List<DayWorkout>, todayVideoOrder: Int?) {
    val context = LocalContext.current

    AppCard {
        CardHeader(title = "Filmy w cyklu")
        workouts.forEachIndexed { index, workout ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val isToday = workout.videoOrder == todayVideoOrder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${workout.videoOrder}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = workout.definition.title, style = MaterialTheme.typography.bodyLarge)
                    if (isToday) {
                        Text(
                            text = "Dzisiaj",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                val videoUri = workout.videoUri
                if (videoUri != null) {
                    IconButton(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(videoUri), "video/*") }
                        )
                    }) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Odtwórz ${workout.definition.title}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                DoneMark(done = workout.isCompleted, active = isToday)
            }
        }
    }
}
