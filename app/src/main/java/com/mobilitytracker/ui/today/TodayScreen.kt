package com.mobilitytracker.ui.today

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.ui.theme.StreakOrange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AnimatedContent(targetState = state.isLoading, label = "loading") { isLoading ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.programNotStarted) {
            OnboardingContent(onStart = { date -> viewModel.startProgram(date) })
        } else {
            TodayContent(
                state = state,
                onToggle = { viewModel.toggleCompletion() },
                onRefresh = { viewModel.loadToday() }
            )
        }
    }
}

@Composable
private fun TodayContent(state: TodayUiState, onToggle: () -> Unit, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val workout = state.workout ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CYKL ${workout.cycleNumber}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Film ${workout.videoOrder} / $CYCLE_LENGTH",
                style = MaterialTheme.typography.titleMedium
            )
        }

        LinearProgressIndicator(
            progress = { workout.videoOrder.toFloat() / CYCLE_LENGTH },
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = workout.definition.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = workout.definition.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (workout.definition.durationMinutes > 0) {
                    Text(
                        text = "${workout.definition.durationMinutes} min",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider()

                AnimatedVisibility(visible = workout.videoUri != null) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(workout.videoUri), "video/*")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("▶  Odtwórz filmik")
                    }
                }

                if (workout.videoUri == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Filmik nie znaleziony na urządzeniu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Oczekiwana nazwa pliku: \"${workout.definition.videoFileName}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { onRefresh() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Odśwież")
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = workout.isCompleted,
                        onCheckedChange = { onToggle() }
                    )
                    Text(
                        text = if (workout.isCompleted) "Trening zrobiony!" else "Oznacz jako zrobione",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (workout.isCompleted) FontWeight.Bold else FontWeight.Normal,
                        color = if (workout.isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (state.completedCycles > 0) {
                    Text(
                        text = "Ukończone cykle: ${state.completedCycles}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (state.currentStreak > 1) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Seria: ${state.currentStreak} dni z rzędu",
                        style = MaterialTheme.typography.bodyLarge,
                        color = StreakOrange,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingContent(onStart: (LocalDate) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Witaj w Mobility Tracker!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "17-dniowy program mobilności, który powtarza się w nieskończoność. Jeden filmik dziennie.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = { onStart(LocalDate.now()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zacznij program od dzisiaj")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Wybierz inną datę startu")
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onStart(date)
                    }
                    showDatePicker = false
                }) {
                    Text("Wybierz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Anuluj")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
