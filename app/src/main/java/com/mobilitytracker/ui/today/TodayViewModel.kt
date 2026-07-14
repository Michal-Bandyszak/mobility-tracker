package com.mobilitytracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilitytracker.data.model.StrengthSetLog
import com.mobilitytracker.data.repository.StrengthRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.data.video.VideoResolver
import com.mobilitytracker.domain.model.DayWorkout
import com.mobilitytracker.domain.model.ScheduledStrengthWorkout
import com.mobilitytracker.domain.usecase.GetTodayWorkoutUseCase
import com.mobilitytracker.domain.usecase.ToggleCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TodayUiState(
    val isLoading: Boolean = true,
    val programNotStarted: Boolean = false,
    val mobilityWorkout: DayWorkout? = null,
    val strengthWorkout: ScheduledStrengthWorkout? = null,
    val strengthLogs: Map<String, StrengthSetLog> = emptyMap(),
    val completedCycles: Int = 0,
    val currentStreak: Int = 0,
    val message: String? = null
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val getTodayWorkout: GetTodayWorkoutUseCase,
    private val toggleCompletion: ToggleCompletionUseCase,
    private val workoutRepository: WorkoutRepository,
    private val strengthRepository: StrengthRepository,
    private val videoResolver: VideoResolver
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    private var strengthLogsJob: Job? = null

    init {
        loadToday()
    }

    fun loadToday(showLoading: Boolean = true) {
        viewModelScope.launch {
            videoResolver.refreshCache()
            if (showLoading) _state.value = _state.value.copy(isLoading = true)
            try {
                when (val result = getTodayWorkout()) {
                    is GetTodayWorkoutUseCase.Result.ProgramNotStarted -> {
                        strengthLogsJob?.cancel()
                        _state.value = TodayUiState(isLoading = false, programNotStarted = true)
                    }
                    is GetTodayWorkoutUseCase.Result.Success -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            programNotStarted = false,
                            mobilityWorkout = result.plan.mobilityWorkout,
                            strengthWorkout = result.plan.strengthWorkout
                        )
                        observeStrengthLogs(result.plan.strengthWorkout)
                    }
                }
            } catch (_: Exception) {
                strengthLogsJob?.cancel()
                _state.value = _state.value.copy(isLoading = false, message = "Nie udało się odświeżyć planu. Spróbuj ponownie.")
            }
        }
    }

    fun toggleCompletion() {
        val workout = _state.value.mobilityWorkout ?: return
        viewModelScope.launch {
            toggleCompletion(workout.cycleNumber, workout.videoOrder)
            loadToday()
        }
    }

    fun saveStrengthSet(
        exerciseId: String,
        setNumber: Int,
        weightKg: Double?,
        reps: Int?,
        timeSeconds: Int?
    ) {
        val strengthWorkout = _state.value.strengthWorkout ?: return

        viewModelScope.launch {
            strengthRepository.saveSetLog(
                date = strengthWorkout.date,
                planDay = strengthWorkout.planDay,
                exerciseId = exerciseId,
                setNumber = setNumber,
                weightKg = weightKg,
                reps = reps,
                timeSeconds = timeSeconds
            )
        }
    }

    fun startProgram(startDate: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            workoutRepository.setProgramStartDate(startDate)
            loadToday()
        }
    }

    fun chooseStrengthWorkoutToday(planDay: Int) {
        viewModelScope.launch {
            workoutRepository.saveStrengthPlanOverrideForDate(LocalDate.now(), planDay)
            loadToday(showLoading = false)
        }
    }

    fun changeStrengthExerciseToday(slotId: String, exerciseId: String) {
        val workout = _state.value.strengthWorkout ?: return
        val exercise = workout.definition.exercises.find { it.slotId == slotId } ?: return
        if (_state.value.strengthLogs.keys.any { it.startsWith("${exercise.id}:") }) {
            _state.value = _state.value.copy(message = "Nie można zmienić ćwiczenia po zapisaniu serii.")
            return
        }

        viewModelScope.launch {
            val saved = strengthRepository.saveExerciseOverrideForDate(
                workout.date,
                workout.planDay,
                slotId,
                exerciseId
            )
            _state.value = _state.value.copy(
                message = if (saved) "Zmieniono tylko na dziś." else "Nie można wybrać tego ćwiczenia w tym miejscu planu."
            )
            if (saved) loadToday(showLoading = false)
        }
    }

    fun restoreStrengthExerciseToday(slotId: String) {
        val workout = _state.value.strengthWorkout ?: return
        val exercise = workout.definition.exercises.find { it.slotId == slotId } ?: return
        if (_state.value.strengthLogs.keys.any { it.startsWith("${exercise.id}:") }) {
            _state.value = _state.value.copy(message = "Nie można zmienić ćwiczenia po zapisaniu serii.")
            return
        }

        viewModelScope.launch {
            strengthRepository.clearExerciseOverrideForDate(workout.date, workout.planDay, slotId)
            _state.value = _state.value.copy(message = "Przywrócono ćwiczenie z planu bazowego.")
            loadToday(showLoading = false)
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun observeStrengthLogs(strengthWorkout: ScheduledStrengthWorkout?) {
        strengthLogsJob?.cancel()
        if (strengthWorkout == null) {
            _state.value = _state.value.copy(strengthLogs = emptyMap())
            return
        }

        strengthLogsJob = viewModelScope.launch {
            strengthRepository.observeLogsForDate(strengthWorkout.date).collectLatest { logs ->
                _state.value = _state.value.copy(
                    strengthLogs = logs
                        .filter { it.planDay == strengthWorkout.planDay }
                        .associateBy { strengthLogKey(it.exerciseId, it.setNumber) }
                )
            }
        }
    }
}

fun strengthLogKey(exerciseId: String, setNumber: Int): String = "$exerciseId:$setNumber"
