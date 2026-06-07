package com.mobilitytracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilitytracker.domain.model.CycleStats
import com.mobilitytracker.domain.usecase.GetStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import com.mobilitytracker.data.model.StrengthExerciseDefinition
import com.mobilitytracker.data.model.StrengthSetLog
import com.mobilitytracker.data.repository.StrengthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@HiltViewModel
class StatsViewModel @Inject constructor(
    getStats: GetStatsUseCase,
    strengthRepository: StrengthRepository
) : ViewModel() {

    val stats: StateFlow<CycleStats?> = getStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val strengthLogs: StateFlow<List<StrengthSetLog>> = strengthRepository.observeAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: List<StrengthExerciseDefinition> = strengthRepository.getAllWorkouts()
        .flatMap { it.exercises }
        .distinctBy { it.id }
}
