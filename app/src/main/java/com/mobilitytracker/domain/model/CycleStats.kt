package com.mobilitytracker.domain.model

import com.mobilitytracker.data.model.CYCLE_LENGTH

data class CycleStats(
    val currentCycleNumber: Int,
    val currentVideoOrder: Int,
    val currentCycleCompletedCount: Int,
    val currentCycleProgress: Float,
    val completedCycles: Int,
    val longestStreak: Int,
    val currentStreak: Int,
    val cycleBreakdown: List<CycleBreakdown>,
    val categoryStats: Map<String, Int>
)

data class CycleBreakdown(
    val cycleNumber: Int,
    val completedCount: Int,
    val totalCount: Int = CYCLE_LENGTH,
    val isComplete: Boolean = completedCount == totalCount
)
