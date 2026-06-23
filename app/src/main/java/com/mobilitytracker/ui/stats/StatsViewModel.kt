package com.mobilitytracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilitytracker.domain.model.CycleStats
import com.mobilitytracker.domain.model.StrengthStats
import com.mobilitytracker.domain.usecase.GetStrengthStatsUseCase
import com.mobilitytracker.domain.usecase.GetStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    getStats: GetStatsUseCase,
    getStrengthStats: GetStrengthStatsUseCase
) : ViewModel() {

    val stats: StateFlow<CycleStats?> = getStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val strengthStats: StateFlow<StrengthStats> = getStrengthStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StrengthStats.EMPTY)
}
