package com.mobilitytracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.data.video.VideoResolver
import com.mobilitytracker.domain.model.DayWorkout
import com.mobilitytracker.domain.usecase.GetTodayWorkoutUseCase
import com.mobilitytracker.domain.usecase.ToggleCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TodayUiState(
    val isLoading: Boolean = true,
    val programNotStarted: Boolean = false,
    val workout: DayWorkout? = null,
    val completedCycles: Int = 0,
    val currentStreak: Int = 0
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val getTodayWorkout: GetTodayWorkoutUseCase,
    private val toggleCompletion: ToggleCompletionUseCase,
    private val workoutRepository: WorkoutRepository,
    private val videoResolver: VideoResolver
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    init {
        loadToday()
    }

    fun loadToday() {
        viewModelScope.launch {
            videoResolver.refreshCache()
            _state.value = TodayUiState(isLoading = true)
            try {
                when (val result = getTodayWorkout()) {
                    is GetTodayWorkoutUseCase.Result.ProgramNotStarted -> {
                        _state.value = TodayUiState(isLoading = false, programNotStarted = true)
                    }
                    is GetTodayWorkoutUseCase.Result.Success -> {
                        _state.value = TodayUiState(
                            isLoading = false,
                            programNotStarted = false,
                            workout = result.workout
                        )
                    }
                }
            } catch (_: Exception) {
                _state.value = TodayUiState(isLoading = false, programNotStarted = true)
            }
        }
    }

    fun toggleCompletion() {
        val workout = _state.value.workout ?: return
        viewModelScope.launch {
            toggleCompletion(workout.cycleNumber, workout.videoOrder)
            loadToday()
        }
    }

    fun startProgram(startDate: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            workoutRepository.setProgramStartDate(startDate)
            loadToday()
        }
    }
}
