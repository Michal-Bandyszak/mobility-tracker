package com.mobilitytracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilitytracker.data.repository.CompletionRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.domain.model.DayWorkout
import com.mobilitytracker.domain.model.buildMobilityChallengeState
import com.mobilitytracker.domain.usecase.GetCycleWorkoutsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = true,
    val currentCycle: Int = 1,
    val maxCycle: Int = 1,
    val workouts: List<DayWorkout> = emptyList(),
    val todayCycle: Int = 1,
    val todayVideoOrder: Int = 1
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCycleWorkouts: GetCycleWorkoutsUseCase,
    private val workoutRepository: WorkoutRepository,
    private val completionRepository: CompletionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            if (workoutRepository.getProgramStartDate() != null) {
                val completions = completionRepository.getAllCompletionsSync()
                val challenge = buildMobilityChallengeState(completions)
                val todayCycle = challenge.todayCycleNumber
                val todayVideo = challenge.todayVideoOrder
                val maxCycle = maxOf(challenge.activeCycleNumber, completions.maxOfOrNull { it.cycleNumber } ?: 1)
                _state.value = _state.value.copy(
                    currentCycle = todayCycle,
                    maxCycle = maxCycle,
                    todayCycle = todayCycle,
                    todayVideoOrder = todayVideo
                )
                loadCycle(todayCycle)
            } else {
                _state.value = CalendarUiState(isLoading = false)
            }
        }
    }

    private fun loadCycle(cycle: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getCycleWorkouts(cycle).collectLatest { workouts ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    currentCycle = cycle,
                    workouts = workouts
                )
            }
        }
    }

    fun previousCycle() {
        val current = _state.value.currentCycle
        if (current > 1) loadCycle(current - 1)
    }

    fun nextCycle() {
        val current = _state.value.currentCycle
        if (current < _state.value.maxCycle) loadCycle(current + 1)
    }
}
