package com.notbraker.tracker.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notbraker.tracker.data.repository.HabitRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HeatmapCellUi(
    val epochDay: Long,
    val intensity: Float
)

data class WeeklyBarUi(
    val label: String,
    val value: Int
)

data class HabitDrilldownUi(
    val id: Long,
    val name: String,
    val icon: String,
    val completionRate: Float
)

data class InsightsUiState(
    val rangeDays: Int = 7,
    val isPremium: Boolean = false,
    val weeklyCompletionPercent: Float = 0f,
    val heatmap: List<HeatmapCellUi> = emptyList(),
    val weeklyBars: List<WeeklyBarUi> = emptyList(),
    val drilldowns: List<HabitDrilldownUi> = emptyList(),
    val isPremiumLocked: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    private val repository: HabitRepository,
    private val isPremiumProvider: () -> Boolean = { false }
) : ViewModel() {

    private val selectedRangeDays = MutableStateFlow(7)

    val uiState: StateFlow<InsightsUiState> = selectedRangeDays
        .flatMapLatest { requestedRange ->
            val isPremium = isPremiumProvider()
            val effectiveRange = if (isPremium) requestedRange else 7
            val today = LocalDate.now().toEpochDay()
            val from = today - effectiveRange + 1
            combine(
                repository.observeActiveHabits(),
                repository.observeCompletionCountsBetween(from, today),
                repository.observeCompletionTotalsByHabit(from, today)
            ) { habits, dayCounts, totals ->
                val countByDay = dayCounts.associateBy({ it.epochDay }, { it.completedCount })
                val maxDayCount = (countByDay.values.maxOrNull() ?: 1).toFloat()
                val heatmapWindow = if (isPremium) 30 else 7
                val heatmap = (0 until heatmapWindow).map { offset ->
                    val day = today - (heatmapWindow - 1 - offset)
                    val value = countByDay[day] ?: 0
                    HeatmapCellUi(epochDay = day, intensity = (value / maxDayCount).coerceIn(0f, 1f))
                }

                val weeklyBars = (0..6).map { index ->
                    val day = today - (6 - index)
                    WeeklyBarUi(
                        label = LocalDate.ofEpochDay(day).dayOfWeek.name.take(3),
                        value = countByDay[day] ?: 0
                    )
                }

                val completionByHabit = totals.associateBy({ it.habitId }, { it.completedCount })
                val drilldowns = habits.map { habit ->
                    val total = completionByHabit[habit.id] ?: 0
                    HabitDrilldownUi(
                        id = habit.id,
                        name = habit.name,
                        icon = habit.icon,
                        completionRate = (total.toFloat() / effectiveRange.toFloat()).coerceIn(0f, 1f)
                    )
                }.sortedByDescending { it.completionRate }

                val weeklyCompleted = weeklyBars.sumOf { it.value }
                val weeklyTarget = (habits.size * 7).coerceAtLeast(1)
                InsightsUiState(
                    rangeDays = effectiveRange,
                    isPremium = isPremium,
                    weeklyCompletionPercent = (weeklyCompleted.toFloat() / weeklyTarget.toFloat()).coerceIn(0f, 1f),
                    heatmap = heatmap,
                    weeklyBars = weeklyBars,
                    drilldowns = drilldowns,
                    isPremiumLocked = !isPremium && requestedRange > 7
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InsightsUiState()
        )

    fun selectRange(days: Int) {
        selectedRangeDays.value = days
    }
}

class InsightsViewModelFactory(
    private val repository: HabitRepository,
    private val isPremiumProvider: () -> Boolean = { false }
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InsightsViewModel(repository, isPremiumProvider) as T
    }
}
