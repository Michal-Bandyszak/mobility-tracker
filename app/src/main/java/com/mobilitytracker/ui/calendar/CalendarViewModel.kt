package com.mobilitytracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilitytracker.data.model.StrengthSetLog
import com.mobilitytracker.data.model.TrackingType
import com.mobilitytracker.data.repository.StrengthRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.domain.model.DayWorkout
import com.mobilitytracker.domain.usecase.GetCycleWorkoutsUseCase
import com.mobilitytracker.domain.usecase.ToggleCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = true,
    val currentCycle: Int = 1,
    val maxCycle: Int = 1,
    val workouts: List<DayWorkout> = emptyList(),
    val todayCycle: Int = 1,
    val todayVideoOrder: Int = 1,
    val strengthHistory: List<StrengthCalendarEntry> = emptyList()
)

data class StrengthCalendarEntry(
    val date: LocalDate,
    val dayLabel: String,
    val planDay: Int,
    val loggedSets: Int
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCycleWorkouts: GetCycleWorkoutsUseCase,
    private val toggleCompletion: ToggleCompletionUseCase,
    private val workoutRepository: WorkoutRepository,
    private val strengthRepository: StrengthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var strengthHistoryJob: Job? = null

    init {
        viewModelScope.launch {
            if (workoutRepository.getProgramStartDate() != null) {
                val (todayCycle, todayVideo) = workoutRepository.getCurrentMobilityPosition(LocalDate.now())
                    ?: Pair(1, 1)
                _state.value = _state.value.copy(
                    currentCycle = todayCycle,
                    maxCycle = todayCycle,
                    todayCycle = todayCycle,
                    todayVideoOrder = todayVideo
                )
                loadCycle(todayCycle)
                observeStrengthHistory()
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

    fun toggleCompletion(cycleNumber: Int, videoOrder: Int) {
        viewModelScope.launch {
            toggleCompletion.invoke(cycleNumber, videoOrder)
        }
    }

    private fun observeStrengthHistory() {
        if (strengthHistoryJob != null) return

        strengthHistoryJob = viewModelScope.launch {
            strengthRepository.observeAllLogs().collectLatest { logs ->
                _state.value = _state.value.copy(
                    strengthHistory = logs.toStrengthCalendarEntries(
                        strengthRepository.getExerciseCatalog().associateBy { it.id }
                    )
                )
            }
        }
    }

    private fun List<StrengthSetLog>.toStrengthCalendarEntries(
        exercises: Map<String, com.mobilitytracker.data.model.ExerciseCatalogEntry>
    ): List<StrengthCalendarEntry> =
        asSequence()
            .filter { exercises[it.exerciseId]?.trackingType != TrackingType.NONE }
            .mapNotNull { log ->
                val date = runCatching { LocalDate.parse(log.date) }.getOrNull()
                if (date != null) (date to log.planDay) to log else null
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .map { (key, sessionLogs) ->
                val (date, planDay) = key
                StrengthCalendarEntry(
                    date = date,
                    dayLabel = date.toPolishDayLabel(),
                    planDay = planDay,
                    loggedSets = sessionLogs.size
                )
            }
            .sortedWith(compareByDescending<StrengthCalendarEntry> { it.date }.thenByDescending { it.planDay })

    private fun LocalDate.toPolishDayLabel(): String =
        when (dayOfWeek) {
            DayOfWeek.MONDAY -> "poniedziałek"
            DayOfWeek.TUESDAY -> "wtorek"
            DayOfWeek.WEDNESDAY -> "środa"
            DayOfWeek.THURSDAY -> "czwartek"
            DayOfWeek.FRIDAY -> "piątek"
            DayOfWeek.SATURDAY -> "sobota"
            DayOfWeek.SUNDAY -> "niedziela"
        }
}
