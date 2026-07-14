package com.mobilitytracker.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mobilitytracker.data.model.STRENGTH_WORKOUT_PLAN
import com.mobilitytracker.data.model.WARMUP_ROUTINE
import com.mobilitytracker.data.model.StrengthWorkoutDefinition
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
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
                Toast.makeText(context, "Wyeksportowano dane pomyślnie", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val json = InputStreamReader(stream).readText()
                viewModel.importData(json) { success ->
                    if (success) {
                        Toast.makeText(context, "Zaimporowano pomyślnie. Zrestartuj aplikację.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Błąd importu. Zły format pliku.", Toast.LENGTH_SHORT).show()
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
                text = "Ustawienia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            val isDark = state.themeMode == "dark" || (state.themeMode == "system" && androidx.compose.foundation.isSystemInDarkTheme())
            androidx.compose.material3.IconButton(
                onClick = { viewModel.setThemeMode(if (isDark) "light" else "dark") }
            ) {
                val icon = if (isDark) {
                    Icons.Outlined.LightMode
                } else {
                    Icons.Outlined.DarkMode
                }
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = if (isDark) "Zmień na jasny" else "Zmień na ciemny"
                )
            }
        }

        TrainingDaysCard(
            title = "Dni mobilności",
            description = "W te dni aplikacja pokaże kolejny film z 17-dniowego cyklu.",
            selectedDays = state.mobilityDays,
            onToggle = { viewModel.toggleMobilityDay(it) }
        )

        TrainingDaysCard(
            title = "Dni siłowni",
            description = "Klikaj dzień, żeby przełączać: brak, A, B. Aplikacja pokaże wybrany trening w danym dniu.",
            selectedDays = state.strengthDays,
            strengthAssignments = state.strengthDayPlanAssignments,
            onToggle = { viewModel.toggleStrengthDay(it) }
        )

        StrengthPlansPreviewCard()

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Codzienne przypomnienie",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Powiadomienie o ${state.reminderHour}:${state.reminderMinute.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.reminderEnabled,
                        onCheckedChange = { viewModel.setReminderEnabled(it) }
                    )
                }

                if (state.reminderEnabled) {
                    Button(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Zmień godzinę przypomnienia")
                    }
                }

                if (!state.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Text(
                        text = "Brak uprawnień do wysyłania powiadomień! Włącz je w ustawieniach aplikacji.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Włącz powiadomienia")
                    }
                }

                if (!state.canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Text(
                        text = "Powiadomienia mogą być opóźnione. Włącz dokładne alarmy, aby dostawać je punktualnie.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Włącz dokładne alarmy")
                    }
                }

                if (state.reminderEnabled) {
                    Text(
                        text = "Jeśli powiadomienia nie przychodzą — wyłącz optymalizację baterii dla tej aplikacji w ustawieniach Samsunga/systemu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Wyłącz optymalizację baterii")
                    }

                    OutlinedButton(
                        onClick = { viewModel.sendTestNotification() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Wyślij testowe powiadomienie")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Reset flow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Reset usuwa datę startu, ukończenia mobilności, logi siłowe i jednorazowe wybory A/B oraz ćwiczeń. Harmonogram dni oraz przypomnienia zostają.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resetuj flow")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Kopia zapasowa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Eksportuj lub importuj historię treningową z pliku. Przydatne przy zmianie telefonu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { exportLauncher.launch("mobility_tracker_backup.json") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Eksportuj dane do pliku")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Importuj dane z pliku")
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
            title = { Text("Wybierz godzinę przypomnienia") },
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
            title = { Text("Resetować flow?") },
            text = {
                Text("Po resecie aplikacja wróci do ekranu startu programu. Historia ukończeń, logi serii i jednorazowe wybory A/B oraz ćwiczeń zostaną usunięte.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetFlow()
                    showResetDialog = false
                }) {
                    Text("Resetuj")
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
private fun TrainingDaysCard(
    title: String,
    description: String,
    selectedDays: Set<DayOfWeek>,
    strengthAssignments: Map<DayOfWeek, Int> = emptyMap(),
    onToggle: (DayOfWeek) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(DayOfWeek.values().take(4), DayOfWeek.values().drop(4)).forEach { rowDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowDays.forEach { day ->
                            DayToggleButton(
                                day = day,
                                selected = day in selectedDays,
                                strengthPlanDay = strengthAssignments[day],
                                onToggle = onToggle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(4 - rowDays.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayToggleButton(
    day: DayOfWeek,
    selected: Boolean,
    strengthPlanDay: Int? = null,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    val label = if (strengthPlanDay != null) {
        "${day.shortPolishLabel()} T$strengthPlanDay"
    } else {
        day.shortPolishLabel()
    }
    if (selected) {
        Button(
            onClick = { onToggle(day) },
            modifier = modifier
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = { onToggle(day) },
            modifier = modifier
        ) {
            Text(label)
        }
    }
}

@Composable
private fun StrengthPlansPreviewCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
            text = "Plan bazowy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            STRENGTH_WORKOUT_PLAN.forEach { workout ->
                StrengthPlanPreview(workout = workout)
            }
            Text(
                text = "Ćwiczenia możesz podmieniać na jeden dzień na ekranie Dzisiaj.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Wspólna rozgrzewka: ${WARMUP_ROUTINE.joinToString { it.name }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StrengthPlanPreview(workout: StrengthWorkoutDefinition) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = workout.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = workout.focus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            workout.exercises.forEach { exercise ->
                Text(
                    text = "${exercise.section}: ${exercise.name} · ${exercise.sets} x ${exercise.reps}",
                    style = MaterialTheme.typography.bodySmall
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
