package com.mobilitytracker.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilitytracker.data.model.STRENGTH_WORKOUT_PLAN
import com.mobilitytracker.data.model.StrengthWorkoutDefinition
import com.mobilitytracker.data.model.WARMUP_ROUTINE
import com.mobilitytracker.notification.REMINDER_INTERVAL_HOURS
import com.mobilitytracker.ui.components.AppCard
import com.mobilitytracker.ui.components.CardHeader
import com.mobilitytracker.ui.components.ExpandChevron
import com.mobilitytracker.ui.components.ExpressiveHeader
import com.mobilitytracker.ui.components.ExpressiveScreen
import com.mobilitytracker.ui.components.ScreenColumn
import com.mobilitytracker.ui.components.planName
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    scrollState: ScrollState,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            viewModel.exportData { json ->
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    OutputStreamWriter(stream).use { writer ->
                        writer.write(json)
                    }
                }
                Toast.makeText(context, "Wyeksportowano dane", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val json = InputStreamReader(stream).readText()
                viewModel.importData(json) { success ->
                    if (success) {
                        Toast.makeText(context, "Zaimportowano dane. Uruchom aplikację ponownie.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Nie udało się zaimportować: plik ma zły format.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    ExpressiveScreen {
        ScreenColumn(scrollState) {
            val isDark = state.themeMode == "dark" || (state.themeMode == "system" && isSystemInDarkTheme())
            ExpressiveHeader(
                title = "Ustawienia",
                trailing = {
                    IconButton(onClick = { viewModel.setThemeMode(if (isDark) "light" else "dark") }) {
                        Icon(
                            imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (isDark) "Zmień na jasny motyw" else "Zmień na ciemny motyw"
                        )
                    }
                }
            )

            TrainingDaysCard(
                selectedDays = state.strengthDays,
                strengthAssignments = state.strengthDayPlanAssignments,
                onToggle = { viewModel.toggleStrengthDay(it) }
            )

            StrengthPlansPreviewCard()

            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardHeader(
                        title = "Przypomnienia o mobilności",
                        meta = "Pierwsze o ${state.reminderHour}:${state.reminderMinute.toString().padStart(2, '0')}, potem co $REMINDER_INTERVAL_HOURS godziny",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.reminderEnabled,
                        onCheckedChange = { viewModel.setReminderEnabled(it) }
                    )
                }

                if (state.reminderEnabled) {
                    FilledTonalButton(onClick = { showTimePicker = true }) {
                        Text("Zmień godzinę")
                    }
                }

                if (!state.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    SettingsNotice(
                        text = "Aplikacja nie ma zgody na powiadomienia, więc przypomnienia nie przyjdą.",
                        isError = true,
                        action = "Włącz powiadomienia",
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            )
                        }
                    )
                }

                if (!state.canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsNotice(
                        text = "Bez dokładnych alarmów przypomnienia mogą przychodzić z opóźnieniem.",
                        action = "Włącz dokładne alarmy",
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    )
                }

                if (state.reminderEnabled) {
                    SettingsNotice(
                        text = "Jeśli przypomnienia nie przychodzą, wyłącz optymalizację baterii dla tej aplikacji.",
                        action = "Wyłącz optymalizację baterii",
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    )
                    TextButton(onClick = { viewModel.sendTestNotification() }) {
                        Text("Wyślij testowe powiadomienie")
                    }
                }
            }

            AppCard {
                CardHeader(
                    title = "Wyzwanie 12 tygodni",
                    meta = "Zaczyna 84 sesje mobilności od nowa. Historia i plan siłowy zostają."
                )
                OutlinedButton(onClick = { showResetDialog = true }) {
                    Text("Zacznij wyzwanie od nowa")
                }
            }

            AppCard {
                CardHeader(
                    title = "Kopia zapasowa",
                    meta = "Zapis całej historii do pliku, np. przy zmianie telefonu."
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("mobility_tracker_backup.json") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Eksportuj")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Importuj")
                    }
                }
            }
        }
    }

    // Dialog z TimePickerem — przyciski zawsze widoczne
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.reminderHour,
            initialMinute = state.reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Godzina pierwszego przypomnienia") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setReminderTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Zacząć wyzwanie od nowa?") },
            text = {
                Text("Postęp mobilności wróci do zera i dziś pojawi się pierwsza z 84 sesji. Dane siłowe zostaną.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetFlow()
                    showResetDialog = false
                }) {
                    Text("Zacznij od nowa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
private fun SettingsNotice(
    text: String,
    action: String,
    onAction: () -> Unit,
    isError: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(onClick = onAction) {
            Text(action)
        }
    }
}

@Composable
private fun TrainingDaysCard(
    selectedDays: Set<DayOfWeek>,
    strengthAssignments: Map<DayOfWeek, Int>,
    onToggle: (DayOfWeek) -> Unit
) {
    AppCard {
        CardHeader(title = "Dni siłowni", meta = "Dotknij dzień, żeby przełączyć: brak, trening A, trening B.")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.values().forEach { day ->
                DayToggle(
                    day = day,
                    selected = day in selectedDays,
                    strengthPlanDay = strengthAssignments[day],
                    onToggle = onToggle,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DayToggle(
    day: DayOfWeek,
    selected: Boolean,
    strengthPlanDay: Int?,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onToggle(day) },
        modifier = modifier.height(56.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = day.shortPolishLabel(), style = MaterialTheme.typography.labelLarge)
            Text(
                text = if (selected) strengthPlanDay?.let(::planName) ?: "✓" else "–",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun StrengthPlansPreviewCard() {
    var expanded by rememberSaveable { mutableStateOf(false) }

    AppCard {
        CardHeader(
            title = "Plan bazowy",
            meta = "Treningi A i B. Ćwiczenie możesz zamienić na jeden dzień na ekranie Dzisiaj.",
            onClick = { expanded = !expanded },
            trailing = { ExpandChevron(expanded) }
        )
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                STRENGTH_WORKOUT_PLAN.forEach { workout ->
                    StrengthPlanPreview(workout = workout)
                }
                Text(
                    text = "Wspólna rozgrzewka: ${WARMUP_ROUTINE.joinToString { it.name }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StrengthPlanPreview(workout: StrengthWorkoutDefinition) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = workout.title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = workout.focus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        workout.exercises.forEach { exercise ->
            val warmup = if (exercise.hasWarmupSet) "1 rozgrz. + " else ""
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = exercise.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(
                    text = "$warmup${exercise.sets} × ${exercise.reps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun DayOfWeek.shortPolishLabel(): String =
    when (this) {
        DayOfWeek.MONDAY -> "Pn"
        DayOfWeek.TUESDAY -> "Wt"
        DayOfWeek.WEDNESDAY -> "Śr"
        DayOfWeek.THURSDAY -> "Cz"
        DayOfWeek.FRIDAY -> "Pt"
        DayOfWeek.SATURDAY -> "So"
        DayOfWeek.SUNDAY -> "Nd"
    }
