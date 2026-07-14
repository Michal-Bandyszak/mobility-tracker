package com.mobilitytracker.ui.today

import android.content.Intent
import com.mobilitytracker.data.model.TrackingType
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.data.model.STRENGTH_RESOURCE_LINKS
import com.mobilitytracker.data.model.StrengthExerciseDefinition
import com.mobilitytracker.data.model.StrengthSetLog
import com.mobilitytracker.data.model.WARMUP_ROUTINE
import com.mobilitytracker.domain.model.DayWorkout
import com.mobilitytracker.domain.model.ScheduledStrengthWorkout
import com.mobilitytracker.ui.components.ExpressiveHeader
import com.mobilitytracker.ui.components.ExpressivePanel
import com.mobilitytracker.ui.components.ExpressiveScreen
import com.mobilitytracker.ui.components.StatusPill
import com.mobilitytracker.ui.theme.StreakOrange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadToday()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                onSaveStrengthSet = { exerciseId, setNumber, weight, reps, time ->
                    viewModel.saveStrengthSet(exerciseId, setNumber, weight, reps, time)
                },
                onChooseStrengthWorkoutToday = { planDay ->
                    viewModel.chooseStrengthWorkoutToday(planDay)
                },
                onChangeStrengthExerciseToday = { slotId, exerciseId ->
                    viewModel.changeStrengthExerciseToday(slotId, exerciseId)
                },
                onRestoreStrengthExerciseToday = { slotId ->
                    viewModel.restoreStrengthExerciseToday(slotId)
                },
                onMessageShown = viewModel::consumeMessage,
                onRefresh = { viewModel.loadToday() }
            )
        }
    }
}

@Composable
private fun TodayContent(
    state: TodayUiState,
    onToggle: () -> Unit,
    onSaveStrengthSet: (String, Int, Double?, Int?, Int?) -> Unit,
    onChooseStrengthWorkoutToday: (Int) -> Unit,
    onChangeStrengthExerciseToday: (String, String) -> Unit,
    onRestoreStrengthExerciseToday: (String) -> Unit,
    onMessageShown: () -> Unit,
    onRefresh: () -> Unit
) {
    val mobilityWorkout = state.mobilityWorkout
    val strengthWorkout = state.strengthWorkout
    val title = when {
        mobilityWorkout != null && strengthWorkout != null -> "Dzisiaj pełny zestaw"
        mobilityWorkout != null -> "Dzisiaj mobilność"
        strengthWorkout != null -> "Dzisiaj siła"
        else -> "Dzisiaj regeneracja"
    }
    val subtitle = when {
        mobilityWorkout != null && strengthWorkout != null -> "Najpierw rozruszaj ciało, potem przejdź do planu siłowego."
        mobilityWorkout != null -> "Jeden konkretny blok ruchu wystarczy, żeby utrzymać rytm."
        strengthWorkout != null -> "Masz aktywny trening siłowy i możesz zmienić A/B jednym tapnięciem."
        else -> "Dzień bez planu. Możesz odpocząć albo odpalić siłownię ręcznie."
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    ExpressiveScreen {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            ExpressiveHeader(
                eyebrow = LocalDate.now().toString(),
                title = title,
                subtitle = subtitle
            )

            if (mobilityWorkout != null) {
                MobilityWorkoutCard(
                    workout = mobilityWorkout,
                    onToggle = onToggle,
                    onRefresh = onRefresh
                )
            } else {
                NoTrainingCard(
                    title = "Mobilność",
                    description = "Dziś nie ma zaplanowanej mobilności według ustawionego harmonogramu."
                )
            }

            if (strengthWorkout != null) {
                StrengthWorkoutCard(
                    workout = strengthWorkout,
                    logs = state.strengthLogs,
                    onSaveSet = onSaveStrengthSet,
                    onChooseStrengthWorkoutToday = onChooseStrengthWorkoutToday,
                    onChangeExercise = onChangeStrengthExerciseToday,
                    onRestoreExercise = onRestoreStrengthExerciseToday
                )
            } else {
                StrengthTodayPickerCard(
                    title = "Siłownia",
                    description = "Dziś nie ma zaplanowanego treningu siłowego, ale możesz zrobić go jednorazowo bez zmiany tygodniowego planu.",
                    selectedPlanDay = null,
                    onChooseStrengthWorkoutToday = onChooseStrengthWorkoutToday
                )
            }

            if (mobilityWorkout == null && strengthWorkout == null) {
                NoTrainingCard(
                    title = "Dzień wolny",
                    description = "Możesz odpocząć albo zrobić opcjonalne rozciąganie z planu."
                )
            }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun TodayPlanPanel(
    mobilityWorkout: DayWorkout?,
    strengthWorkout: ScheduledStrengthWorkout?,
    completedCycles: Int,
    currentStreak: Int
) {
    ExpressivePanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(
                    label = "Mobilność",
                    value = mobilityWorkout?.let { "C${it.cycleNumber}.${it.videoOrder}" } ?: "wolne",
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "Siła",
                    value = strengthWorkout?.let { "T${it.planDay}" } ?: "wybierz",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (completedCycles > 0 || currentStreak > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (completedCycles > 0) {
                        StatusPill(
                            label = "Cykle",
                            value = completedCycles.toString(),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (currentStreak > 1) {
                        StatusPill(
                            label = "Seria",
                            value = "$currentStreak dni",
                            modifier = Modifier.weight(1f),
                            color = StreakOrange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobilityWorkoutCard(workout: DayWorkout, onToggle: () -> Unit, onRefresh: () -> Unit) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StrengthWorkoutCard(
    workout: ScheduledStrengthWorkout,
    logs: Map<String, StrengthSetLog>,
    onSaveSet: (String, Int, Double?, Int?, Int?) -> Unit,
    onChooseStrengthWorkoutToday: (Int) -> Unit,
    onChangeExercise: (String, String) -> Unit,
    onRestoreExercise: (String) -> Unit
) {
    val context = LocalContext.current
    var pickerExercise by remember { mutableStateOf<StrengthExerciseDefinition?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = workout.definition.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sesja siłowa ${workout.sessionNumber} • ${workout.definition.focus}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            StrengthPlanSwitch(
                selectedPlanDay = workout.planDay,
                onChooseStrengthWorkoutToday = onChooseStrengthWorkoutToday
            )

            HorizontalDivider()

            WarmupCard()

            STRENGTH_RESOURCE_LINKS.forEach { link ->
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(link.title)
                }
            }

            workout.definition.exercises.forEach { exercise ->
                StrengthExerciseCard(
                    exercise = exercise,
                    logs = logs,
                    onSaveSet = onSaveSet,
                    onChange = { pickerExercise = exercise },
                    onRestore = { onRestoreExercise(exercise.slotId) }
                )
            }
        }
    }

    pickerExercise?.let { exercise ->
        ModalBottomSheet(onDismissRequest = { pickerExercise = null }) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Zmień tylko dzisiaj", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Jutro wróci ćwiczenie z planu bazowego.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                exercise.choices.forEach { choice ->
                    OutlinedButton(
                        onClick = {
                            onChangeExercise(exercise.slotId, choice.id)
                            pickerExercise = null
                        },
                        enabled = choice.id != exercise.id,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(choice.name)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun WarmupCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Rozgrzewka", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            WARMUP_ROUTINE.forEachIndexed { index, step ->
                Text(
                    text = "${index + 1}. ${step.name} — ${step.prescription}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StrengthTodayPickerCard(
    title: String,
    description: String,
    selectedPlanDay: Int?,
    onChooseStrengthWorkoutToday: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StrengthPlanSwitch(
                selectedPlanDay = selectedPlanDay,
                onChooseStrengthWorkoutToday = onChooseStrengthWorkoutToday
            )
        }
    }
}

@Composable
private fun StrengthPlanSwitch(
    selectedPlanDay: Int?,
    onChooseStrengthWorkoutToday: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StrengthPlanButton(
            label = "Zrób dziś A",
            selected = selectedPlanDay == 1,
            onClick = { onChooseStrengthWorkoutToday(1) },
            modifier = Modifier.weight(1f)
        )
        StrengthPlanButton(
            label = "Zrób dziś B",
            selected = selectedPlanDay == 2,
            onClick = { onChooseStrengthWorkoutToday(2) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StrengthPlanButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}

@Composable
private fun StrengthExerciseCard(
    exercise: StrengthExerciseDefinition,
    logs: Map<String, StrengthSetLog>,
    onSaveSet: (String, Int, Double?, Int?, Int?) -> Unit,
    onChange: () -> Unit,
    onRestore: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = exercise.section.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (exercise.isTodayOverride) {
                Text(
                    text = "Zmienione na dziś",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = buildExercisePrescription(exercise),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (exercise.notes.isNotBlank()) {
                Text(
                    text = exercise.notes,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (exercise.videoUrl != null) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(exercise.videoUrl)))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zobacz technikę")
                }
            }

            if (exercise.isTodayOverride) {
                TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                    Text("Przywróć z planu")
                }
            }
            TextButton(onClick = onChange, modifier = Modifier.fillMaxWidth()) {
                Text(if (exercise.isTodayOverride) "Wybierz inne" else "Zmień tylko dzisiaj")
            }

            HorizontalDivider()

            (1..exercise.sets).forEach { setNumber ->
                val log = logs[strengthLogKey(exercise.id, setNumber)]
                StrengthSetLogRow(
                    exercise = exercise,
                    setNumber = setNumber,
                    log = log,
                    onSaveSet = onSaveSet
                )
            }
        }
    }
}
@Composable
private fun StrengthSetLogRow(
    exercise: StrengthExerciseDefinition,
    setNumber: Int,
    log: StrengthSetLog?,
    onSaveSet: (String, Int, Double?, Int?, Int?) -> Unit
) {
    val exerciseId = exercise.id
    val trackingType = exercise.trackingType
    
    var weightText by remember(log?.weightKg, exerciseId, setNumber) {
        mutableStateOf(log?.weightKg?.let { formatWeight(it) } ?: "")
    }
    var repsText by remember(log?.reps, exerciseId, setNumber) {
        mutableStateOf(log?.reps?.toString() ?: "")
    }
    var timeText by remember(log?.timeSeconds, exerciseId, setNumber) {
        mutableStateOf(log?.timeSeconds?.toString() ?: "")
    }
    
    val isCompleted = log != null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Seria $setNumber",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (trackingType) {
                TrackingType.WEIGHT_AND_REPS -> {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("kg", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("powt.", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                TrackingType.REPS_ONLY -> {
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("powt.", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                TrackingType.TIME_ONLY -> {
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("czas (s)", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                TrackingType.NONE -> {
                    // Żadnych dodatkowych pól tekstowych
                    Spacer(Modifier.weight(1f))
                }
            }

            if (trackingType == TrackingType.NONE) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onSaveSet(exerciseId, setNumber, 0.0, 0, 0)
                        } else {
                            onSaveSet(exerciseId, setNumber, null, null, null)
                        }
                    }
                )
                Text(
                    text = if (isCompleted) "Zrobione!" else "Zaznacz",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            } else {
                OutlinedButton(
                    onClick = { 
                        val weight = weightText.replace(',', '.').trim().toDoubleOrNull()
                        val reps = repsText.trim().toIntOrNull()
                        val time = timeText.trim().toIntOrNull()
                        // Ensure we always save *something* (like 0) if nothing was parsed, or just save nulls. 
                        // But if we pass all nulls, it will DELETE the log.
                        // So if user leaves fields empty but clicks "Zapisz", maybe we want to save them as 0 if they want to explicitly save an empty set.
                        // For now, let's just pass parsed values.
                        if (weight != null || reps != null || time != null) {
                            onSaveSet(exerciseId, setNumber, weight, reps, time)
                        }
                    }
                ) {
                    Text(if (isCompleted) "Aktualizuj" else "Zapisz")
                }
            }
        }
    }
}

@Composable
private fun NoTrainingCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildExercisePrescription(exercise: StrengthExerciseDefinition): String {
    val parts = mutableListOf("${exercise.sets} x ${exercise.reps}")
    if (exercise.tempo.isNotBlank()) parts += "tempo ${exercise.tempo}"
    if (exercise.rir.isNotBlank()) parts += exercise.rir
    exercise.restSeconds?.let { parts += "przerwa ${it}s" }
    return parts.joinToString(" • ")
}

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toInt().toString() else weight.toString()

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
            text = "17-dniowy program mobilności i plan siłowy 2x w tygodniu. Dni treningowe ustawisz w Ustawieniach.",
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
