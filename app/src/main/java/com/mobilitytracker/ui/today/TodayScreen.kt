package com.mobilitytracker.ui.today

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilitytracker.data.model.CYCLE_LENGTH
import com.mobilitytracker.data.model.STRENGTH_RESOURCE_LINKS
import com.mobilitytracker.data.model.StrengthExerciseDefinition
import com.mobilitytracker.data.model.StrengthSetLog
import com.mobilitytracker.data.model.TrackingType
import com.mobilitytracker.data.model.WARMUP_ROUTINE
import com.mobilitytracker.data.model.WARMUP_SET_NUMBER
import com.mobilitytracker.domain.model.DayWorkout
import com.mobilitytracker.domain.model.MOBILITY_CHALLENGE_SESSIONS
import com.mobilitytracker.domain.model.MobilityChallengeState
import com.mobilitytracker.domain.model.MorningWarmup
import com.mobilitytracker.domain.model.ScheduledStrengthWorkout
import com.mobilitytracker.ui.components.AppCard
import com.mobilitytracker.ui.components.CardHeader
import com.mobilitytracker.ui.components.DoneMark
import com.mobilitytracker.ui.components.ExpandChevron
import com.mobilitytracker.ui.components.ExpressiveHeader
import com.mobilitytracker.ui.components.ExpressiveScreen
import com.mobilitytracker.ui.components.InsetBlock
import com.mobilitytracker.ui.components.ScreenColumn
import com.mobilitytracker.ui.components.StatValue
import com.mobilitytracker.ui.components.ThinProgress
import com.mobilitytracker.ui.components.daysLabel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class FootExercise(
    val title: String,
    val dosage: String,
    val instruction: String
)

private val FOOT_ROUTINE = listOf(
    FootExercise(
        title = "Short foot / aktywne podbicie",
        dosage = "3 × 8 powt. po 5–8 s • lewa 4 serie, prawa 3",
        instruction = "Boso oprzyj stopę na pięcie i podstawach palców. Unieś łuk bez podwijania palców."
    ),
    FootExercise(
        title = "Wspięcia na palce na jednej nodze",
        dosage = "3 × 10–15 na nogę • 2 s w górę i 2 s w dół, gdy jest łatwo",
        instruction = "Duży palec cały czas dociska podłoże, a ciężar pozostaje nad przodem stopy."
    ),
    FootExercise(
        title = "Piszczelowy tylny z gumą",
        dosage = "3 × 12–15 • lewa 4 serie",
        instruction = "Guma ciągnie stopę na zewnątrz; kieruj przód stopy do środka bez obracania całej nogi."
    ),
    FootExercise(
        title = "Stanie na jednej nodze z aktywnym łukiem",
        dosage = "3 × 30–45 s • potem 2 × 8–10 małych przysiadów",
        instruction = "Najpierw zrób short foot. Nie pozwalaj, żeby kolano ani łuk mocno uciekały do środka."
    ),
    FootExercise(
        title = "Mobilność kostki i łydki",
        dosage = "2 × 10–15 ruchów kolana nad palce + 2 × 30–45 s rozciągania",
        instruction = "Pięta zostaje na podłożu. Ruch spokojny, bez agresywnego rozciągania."
    )
)

@Composable
fun TodayScreen(
    scrollState: ScrollState,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadToday(showLoading = false)
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
                onSaveStrengthSet = { exerciseId, setNumber, weight, reps, time, bandColor ->
                    viewModel.saveStrengthSet(exerciseId, setNumber, weight, reps, time, bandColor)
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
                onRefresh = { viewModel.loadToday() },
                onReroll = viewModel::rerollMorningWarmup,
                scrollState = scrollState
            )
        }
    }
}

@Composable
private fun TodayContent(
    state: TodayUiState,
    onToggle: () -> Unit,
    onSaveStrengthSet: (String, Int, Double?, Int?, Int?, String?) -> Unit,
    onChooseStrengthWorkoutToday: (Int) -> Unit,
    onChangeStrengthExerciseToday: (String, String) -> Unit,
    onRestoreStrengthExerciseToday: (String) -> Unit,
    onMessageShown: () -> Unit,
    onRefresh: () -> Unit,
    onReroll: () -> Unit,
    scrollState: ScrollState
) {
    val mobilityWorkout = state.mobilityWorkout
    val strengthWorkout = state.strengthWorkout
    val challenge = state.mobilityChallenge
    val title = when {
        challenge?.completedToday == true -> "Mobilność zrobiona"
        mobilityWorkout != null && strengthWorkout != null -> "Dzisiaj pełny zestaw"
        mobilityWorkout != null -> "Dzisiaj mobilność"
        strengthWorkout != null -> "Dzisiaj siła"
        else -> "Dzisiaj regeneracja"
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
            ScreenColumn(scrollState) {
                ExpressiveHeader(eyebrow = todayLabel(), title = title)

                if (challenge != null) {
                    MobilityChallengeCard(challenge)
                }

                MorningWarmupCard(state.morningWarmup, onReroll)

                if (mobilityWorkout != null) {
                    MobilityWorkoutCard(
                        workout = mobilityWorkout,
                        next = state.nextMobilityWorkout.takeIf { challenge?.completedToday == true },
                        onToggle = onToggle,
                        onRefresh = onRefresh
                    )
                } else {
                    NoTrainingCard(
                        title = "Mobilność",
                        description = "Nie udało się wczytać bieżącej sesji. Odśwież ekran."
                    )
                }

                FootRoutineCard()

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
                    StrengthTodayPickerCard(onChooseStrengthWorkoutToday = onChooseStrengthWorkoutToday)
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

private fun todayLabel(): String =
    LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl")))
        .replaceFirstChar { it.uppercase() }

private const val REVANCED_PACKAGE = "app.revanced.android.youtube"

@Composable
private fun MorningWarmupCard(warmup: MorningWarmup?, onReroll: () -> Unit) {
    val context = LocalContext.current
    if (warmup == null) return

    AppCard {
        CardHeader(
            title = "Rozruch poranny",
            meta = if (warmup.isLocal) "Film z telefonu" else "Losowy film z playlisty"
        )
        Text(
            text = warmup.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = {
                    if (warmup.isLocal) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(warmup.videoUri), "video/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        )
                    } else {
                        val view = Intent(Intent.ACTION_VIEW, Uri.parse(warmup.videoUri))
                        // Najpierw YouTube ReVanced (bez reklam); gdy go brak, domyślna aplikacja.
                        try {
                            context.startActivity(Intent(view).setPackage(REVANCED_PACKAGE))
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(view)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                ButtonIcon(Icons.Filled.PlayArrow)
                Text("Odtwórz")
            }
            TextButton(onClick = onReroll) {
                ButtonIcon(Icons.Filled.Shuffle)
                Text("Losuj inny")
            }
        }
    }
}

@Composable
private fun FootRoutineCard() {
    var expanded by rememberSaveable { mutableStateOf(false) }

    AppCard {
        CardHeader(
            title = "Stopy",
            meta = "Płaskostopie · ${FOOT_ROUTINE.size} ćwiczeń · 10–12 min",
            onClick = { expanded = !expanded },
            trailing = { ExpandChevron(expanded) }
        )

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Cel: 4× w tygodniu, lekki priorytet lewej stopy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FOOT_ROUTINE.forEachIndexed { index, exercise ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${index + 1}. ${exercise.title}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = exercise.dosage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = exercise.instruction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Ból lub obrzęk po wewnętrznej stronie lewej kostki albo łuku: przerwij i skonsultuj stopę z fizjoterapeutą.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MobilityChallengeCard(challenge: MobilityChallengeState) {
    val title = when {
        challenge.isComplete -> "12 tygodni ukończone"
        challenge.completedToday -> "Zrobione na dziś"
        else -> "Sesja ${challenge.activeSessionNumber.coerceAtMost(MOBILITY_CHALLENGE_SESSIONS)} z $MOBILITY_CHALLENGE_SESSIONS"
    }
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer

    AppCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Column {
            Text(
                text = "Wyzwanie 12 tygodni",
                style = MaterialTheme.typography.bodySmall,
                color = onContainer.copy(alpha = 0.75f)
            )
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        }
        ThinProgress(
            progress = challenge.progress,
            color = MaterialTheme.colorScheme.primary,
            trackColor = onContainer.copy(alpha = 0.12f)
        )
        Row {
            StatValue(value = daysLabel(challenge.currentStreak), label = "seria", modifier = Modifier.weight(1f))
            StatValue(value = "${challenge.completedThisWeek}/7", label = "ten tydzień", modifier = Modifier.weight(1f))
            StatValue(value = daysLabel(challenge.longestStreak), label = "rekord serii", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MobilityWorkoutCard(
    workout: DayWorkout,
    next: DayWorkout?,
    onToggle: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var expanded by rememberSaveable(workout.cycleNumber, workout.videoOrder, workout.isCompleted) {
        mutableStateOf(!workout.isCompleted)
    }

    AppCard {
        CardHeader(
            title = "Mobilność",
            meta = "Cykl ${workout.cycleNumber} · film ${workout.videoOrder} z $CYCLE_LENGTH",
            onClick = { expanded = !expanded },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (workout.isCompleted) DoneMark(done = true)
                    ExpandChevron(expanded)
                }
            }
        )

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(text = workout.definition.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = listOfNotNull(
                            workout.definition.category.replaceFirstChar { it.uppercase() },
                            workout.definition.durationMinutes.takeIf { it > 0 }?.let { "$it min" }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (workout.videoUri == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Brak pliku „${workout.definition.videoFileName}” w telefonie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRefresh) { Text("Odśwież") }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (workout.videoUri != null) {
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(workout.videoUri), "video/*")
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            ButtonIcon(Icons.Filled.PlayArrow)
                            Text("Odtwórz")
                        }
                    }
                    when {
                        workout.isCompleted -> OutlinedButton(onClick = onToggle, modifier = Modifier.weight(1f)) {
                            Text("Cofnij zrobione")
                        }
                        workout.videoUri == null -> Button(onClick = onToggle, modifier = Modifier.weight(1f)) {
                            ButtonIcon(Icons.Filled.Check)
                            Text("Zrobione")
                        }
                        else -> FilledTonalButton(onClick = onToggle, modifier = Modifier.weight(1f)) {
                            ButtonIcon(Icons.Filled.Check)
                            Text("Zrobione")
                        }
                    }
                }
            }
        }

        if (next != null) {
            Text(
                text = "Jutro: ${next.definition.title}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StrengthWorkoutCard(
    workout: ScheduledStrengthWorkout,
    logs: Map<String, StrengthSetLog>,
    onSaveSet: (String, Int, Double?, Int?, Int?, String?) -> Unit,
    onChooseStrengthWorkoutToday: (Int) -> Unit,
    onChangeExercise: (String, String) -> Unit,
    onRestoreExercise: (String) -> Unit
) {
    val context = LocalContext.current
    var pickerExercise by remember { mutableStateOf<StrengthExerciseDefinition?>(null) }
    var resourcesExpanded by rememberSaveable { mutableStateOf(false) }
    val exercises = workout.definition.exercises
    val plannedSets = exercises.sumOf { it.sets }
    val loggedSets = exercises.sumOf { exercise ->
        (1..exercise.sets).count { logs.containsKey(strengthLogKey(exercise.id, it)) }
    }

    AppCard {
        CardHeader(
            title = "Siła",
            meta = "Sesja ${workout.sessionNumber} · ${workout.definition.focus}"
        )

        StrengthPlanSwitch(
            selectedPlanDay = workout.planDay,
            onChooseStrengthWorkoutToday = onChooseStrengthWorkoutToday
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "$loggedSets z $plannedSets serii",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ThinProgress(progress = if (plannedSets > 0) loggedSets.toFloat() / plannedSets else 0f)
        }

        WarmupBlock()

        exercises.forEach { exercise ->
            StrengthExerciseBlock(
                exercise = exercise,
                logs = logs,
                onSaveSet = onSaveSet,
                onChange = { pickerExercise = exercise },
                onRestore = { onRestoreExercise(exercise.slotId) }
            )
        }

        TextButton(onClick = { resourcesExpanded = !resourcesExpanded }) {
            Text(if (resourcesExpanded) "Ukryj materiały dodatkowe" else "Materiały dodatkowe")
        }
        AnimatedVisibility(visible = resourcesExpanded) {
            Column {
                STRENGTH_RESOURCE_LINKS.forEach { link ->
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url))) }) {
                        ButtonIcon(Icons.AutoMirrored.Filled.OpenInNew)
                        Text(link.title)
                    }
                }
            }
        }
    }

    pickerExercise?.let { exercise ->
        ModalBottomSheet(onDismissRequest = { pickerExercise = null }) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Zamień tylko na dziś", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Przy następnym treningu wróci ćwiczenie z planu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
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
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WarmupBlock() {
    var completed by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(true) }

    InsetBlock {
        CardHeader(
            title = "Rozgrzewka",
            meta = if (completed) "Zrobiona" else "${WARMUP_ROUTINE.size} ćwiczeń",
            onClick = { expanded = !expanded },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (completed) DoneMark(done = true)
                    ExpandChevron(expanded)
                }
            }
        )

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WARMUP_ROUTINE.forEach { step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(step.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            step.prescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = completed,
                        onCheckedChange = { checked ->
                            completed = checked
                            if (checked) expanded = false
                        }
                    )
                    Text(if (completed) "Rozgrzewka zrobiona" else "Oznacz rozgrzewkę jako zrobioną")
                }
            }
        }
    }
}

@Composable
private fun StrengthTodayPickerCard(onChooseStrengthWorkoutToday: (Int) -> Unit) {
    AppCard {
        CardHeader(
            title = "Siła",
            meta = "Dziś bez treningu w planie. Możesz zrobić jeden jednorazowo, plan tygodnia się nie zmieni."
        )
        StrengthPlanSwitch(
            selectedPlanDay = null,
            onChooseStrengthWorkoutToday = onChooseStrengthWorkoutToday
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StrengthPlanSwitch(
    selectedPlanDay: Int?,
    onChooseStrengthWorkoutToday: (Int) -> Unit
) {
    val options = listOf(1 to "Trening A", 2 to "Trening B")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (planDay, label) ->
            SegmentedButton(
                selected = selectedPlanDay == planDay,
                onClick = { onChooseStrengthWorkoutToday(planDay) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun StrengthExerciseBlock(
    exercise: StrengthExerciseDefinition,
    logs: Map<String, StrengthSetLog>,
    onSaveSet: (String, Int, Double?, Int?, Int?, String?) -> Unit,
    onChange: () -> Unit,
    onRestore: () -> Unit
) {
    val context = LocalContext.current
    val completedWorkingSets = (1..exercise.sets).count { setNumber ->
        logs.containsKey(strengthLogKey(exercise.id, setNumber))
    }
    val isComplete = completedWorkingSets == exercise.sets
    var expanded by rememberSaveable(exercise.id, isComplete) { mutableStateOf(!isComplete) }

    InsetBlock {
        CardHeader(
            title = exercise.name,
            meta = if (exercise.isTodayOverride) "${exercise.section} · zmienione na dziś" else exercise.section,
            onClick = if (isComplete) {
                { expanded = !expanded }
            } else null,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isComplete) {
                        DoneMark(done = true)
                        ExpandChevron(expanded)
                    } else {
                        Text(
                            text = "$completedWorkingSets/${exercise.sets}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = buildExercisePrescription(exercise),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (exercise.notes.isNotBlank()) {
                    Text(text = exercise.notes, style = MaterialTheme.typography.bodySmall)
                }

                Row {
                    if (exercise.videoUrl != null) {
                        TextButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(exercise.videoUrl)))
                        }) {
                            Text("Technika")
                        }
                    }
                    TextButton(onClick = onChange) {
                        Text(if (exercise.isTodayOverride) "Wybierz inne" else "Zamień")
                    }
                    if (exercise.isTodayOverride) {
                        TextButton(onClick = onRestore) {
                            Text("Przywróć")
                        }
                    }
                }

                if (exercise.hasWarmupSet) {
                    StrengthSetLogRow(
                        exercise = exercise,
                        setNumber = WARMUP_SET_NUMBER,
                        log = logs[strengthLogKey(exercise.id, WARMUP_SET_NUMBER)],
                        onSaveSet = onSaveSet
                    )
                }
                (1..exercise.sets).forEach { setNumber ->
                    StrengthSetLogRow(
                        exercise = exercise,
                        setNumber = setNumber,
                        log = logs[strengthLogKey(exercise.id, setNumber)],
                        onSaveSet = onSaveSet
                    )
                }
            }
        }
    }
}

@Composable
private fun StrengthSetLogRow(
    exercise: StrengthExerciseDefinition,
    setNumber: Int,
    log: StrengthSetLog?,
    onSaveSet: (String, Int, Double?, Int?, Int?, String?) -> Unit
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
    var bandText by remember(log?.bandColor, exerciseId, setNumber) {
        mutableStateOf(log?.bandColor ?: "")
    }

    val isCompleted = log != null
    val isWarmup = setNumber == WARMUP_SET_NUMBER
    var confirmDelete by remember(log) { mutableStateOf(false) }
    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            delay(3_000)
            confirmDelete = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            if (isWarmup) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = "Seria rozgrzewkowa",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "$setNumber",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (trackingType) {
            TrackingType.WEIGHT_AND_REPS -> {
                SetField(weightText, { weightText = it }, "kg", KeyboardType.Decimal, Modifier.weight(1f))
                SetField(repsText, { repsText = it }, "powt.", KeyboardType.Number, Modifier.weight(1f))
            }
            TrackingType.BAND_AND_REPS -> {
                SetField(bandText, { bandText = it }, "guma", KeyboardType.Text, Modifier.weight(1f))
                SetField(repsText, { repsText = it }, "powt.", KeyboardType.Number, Modifier.weight(1f))
            }
            TrackingType.REPS_ONLY -> {
                SetField(repsText, { repsText = it }, "powt.", KeyboardType.Number, Modifier.weight(1f))
            }
            TrackingType.TIME_ONLY -> {
                SetField(timeText, { timeText = it }, "czas (s)", KeyboardType.Number, Modifier.weight(1f))
            }
            TrackingType.NONE -> {
                Text(
                    text = if (isCompleted) "Zrobione" else "Do zrobienia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (trackingType == TrackingType.NONE) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { checked ->
                    if (checked) {
                        onSaveSet(exerciseId, setNumber, 0.0, 0, 0, null)
                    } else {
                        onSaveSet(exerciseId, setNumber, null, null, null, null)
                    }
                }
            )
        } else {
            val save = {
                val weight = weightText.replace(',', '.').trim().toDoubleOrNull()
                val reps = repsText.trim().toIntOrNull()
                val time = timeText.trim().toIntOrNull()
                val bandColor = bandText.trim().takeIf { it.isNotEmpty() }
                if (weight != null || reps != null || time != null || bandColor != null) {
                    onSaveSet(exerciseId, setNumber, weight, reps, time, bandColor)
                }
            }
            if (isCompleted) {
                FilledIconButton(onClick = save) {
                    Icon(Icons.Filled.Check, contentDescription = "Aktualizuj serię")
                }
                // Usuwanie na dwa tapnięcia: × → czerwony kosz → usuń. Kosz wraca do × po 3 s.
                IconButton(
                    onClick = {
                        if (confirmDelete) onSaveSet(exerciseId, setNumber, null, null, null, null)
                        else confirmDelete = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (confirmDelete) Icons.Filled.Delete else Icons.Filled.Close,
                        contentDescription = if (confirmDelete) "Potwierdź usunięcie serii" else "Usuń zapis serii",
                        tint = if (confirmDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (confirmDelete) 22.dp else 18.dp)
                    )
                }
            } else {
                OutlinedIconButton(onClick = save) {
                    Icon(Icons.Filled.Check, contentDescription = "Zapisz serię")
                }
                // Miejsce na „usuń”, żeby pola nie skakały po zapisie serii.
                Spacer(Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun SetField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

@Composable
private fun NoTrainingCard(title: String, description: String) {
    AppCard {
        CardHeader(title = title, meta = description)
    }
}

@Composable
private fun ButtonIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
}

private fun buildExercisePrescription(exercise: StrengthExerciseDefinition): String {
    val workingSets = "${exercise.sets} x ${exercise.reps}"
    val parts = mutableListOf(if (exercise.hasWarmupSet) "1 rozgrzewkowa + $workingSets" else workingSets)
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
            text = "Mobility Tracker",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "84 sesje mobilności po kolei, najwyżej jedna dziennie, i trening siłowy A/B dwa razy w tygodniu.",
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
