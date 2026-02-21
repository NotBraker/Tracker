package com.notbraker.tracker.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notbraker.tracker.data.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class HeatmapCellUi(
    val epochDay: Long,
    val intensity: Float
)

data class WeeklyBarUi(
    val label: String,
    val valuePercent: Int,
    val done: Int,
    val target: Int
)

data class HabitDrilldownUi(
    val id: Long,
    val name: String,
    val icon: String,
    val completionRate: Float,
    val completionCount: Int,
    val missedDays: Int
)

data class LeaderboardItemUi(
    val name: String,
    val icon: String,
    val metric: String
)

data class InsightsLeaderboardUi(
    val topStreak: LeaderboardItemUi? = null,
    val mostConsistent: LeaderboardItemUi? = null,
    val mostMissed: LeaderboardItemUi? = null
)

data class InsightsUiState(
    val rangeDays: Int = 7,
    val isPremium: Boolean = false,
    val consistencyIndex: Float = 0f,
    val doneCount: Int = 0,
    val targetCount: Int = 0,
    val heatmap: List<HeatmapCellUi> = emptyList(),
    val weeklyBars: List<WeeklyBarUi> = emptyList(),
    val drilldowns: List<HabitDrilldownUi> = emptyList(),
    val leaderboard: InsightsLeaderboardUi = InsightsLeaderboardUi(),
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
            val from = today - (effectiveRange - 1)
            combine(
                repository.observeActiveHabits(),
                repository.observeCompletionCountsBetween(from, today),
                repository.observeCompletionTotalsByHabit(from, today)
            ) { habits, dayCounts, totals ->
                val countByDay = dayCounts.associateBy({ it.epochDay }, { it.completedCount })
                val targetByDay = buildMap {
                    for (offset in 0 until effectiveRange) {
                        val dayEpoch = from + offset
                        val dayDate = LocalDate.ofEpochDay(dayEpoch)
                        val target = habits.sumOf { habit ->
                            if (dayEpoch < habit.createdAtEpochDay) {
                                0
                            } else {
                                repository.run { habit.targetForDate(dayDate) }
                            }
                        }
                        put(dayEpoch, target)
                    }
                }
                val habitCountByDay = buildMap {
                    for (offset in 0 until effectiveRange) {
                        val dayEpoch = from + offset
                        val dayDate = LocalDate.ofEpochDay(dayEpoch)
                        val count = habits.count { habit ->
                            dayEpoch >= habit.createdAtEpochDay &&
                                repository.run { habit.targetForDate(dayDate) } > 0
                        }
                        put(dayEpoch, count)
                    }
                }
                val heatmap = (0 until effectiveRange).map { offset ->
                    val day = from + offset
                    val done = countByDay[day] ?: 0
                    val totalHabits = habitCountByDay[day] ?: 0
                    val intensity = if (totalHabits == 0) 0f else (done.toFloat() / totalHabits.toFloat()).coerceIn(0f, 1f)
                    HeatmapCellUi(epochDay = day, intensity = intensity)
                }
                val todayDate = LocalDate.now()
                val weekStart = todayDate.with(DayOfWeek.MONDAY)
                val weeklyBars = listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
                ).map { dayOfWeek ->
                    val dayDate = weekStart.with(dayOfWeek)
                    val dayEpoch = dayDate.toEpochDay()
                    val done = countByDay[dayEpoch] ?: 0
                    val target = habits.sumOf { habit ->
                        if (dayEpoch < habit.createdAtEpochDay) {
                            0
                        } else {
                            repository.run { habit.targetForDate(dayDate) }
                        }
                    }
                    WeeklyBarUi(
                        label = dayOfWeek.name.take(3),
                        valuePercent = if (target == 0) 0 else ((done.toFloat() / target.toFloat()) * 100f).toInt().coerceIn(0, 100),
                        done = done,
                        target = target
                    )
                }

                val completionByHabit = totals.associateBy({ it.habitId }, { it.completedCount })
                val drilldowns = habits.map { habit ->
                    val completedCount = completionByHabit[habit.id] ?: 0
                    val activeDays = (today - maxOf(from, habit.createdAtEpochDay) + 1).coerceAtLeast(0).toInt()
                    val targetCount = (0 until activeDays).sumOf { index ->
                        val day = LocalDate.ofEpochDay(maxOf(from, habit.createdAtEpochDay) + index)
                        repository.run { habit.targetForDate(day) }
                    }
                    val safeTargetCount = targetCount.coerceAtLeast(1)
                    val missedDays = (targetCount - completedCount).coerceAtLeast(0)
                    HabitDrilldownUi(
                        id = habit.id,
                        name = habit.name,
                        icon = habit.icon,
                        completionRate = (completedCount.toFloat() / safeTargetCount.toFloat()).coerceIn(0f, 1f),
                        completionCount = completedCount,
                        missedDays = missedDays
                    )
                }.sortedByDescending { it.completionRate }

                val totalCompletedInRange = heatmap.sumOf { cell ->
                    val day = cell.epochDay
                    countByDay[day] ?: 0
                }
                val totalPossibleInRange = targetByDay.values.sum().coerceAtLeast(1)

                val topStreakHabit = habits.maxByOrNull { it.streakCurrent }
                val mostConsistentHabit = drilldowns.maxByOrNull { it.completionRate }
                val mostMissedHabit = drilldowns.maxByOrNull { it.missedDays }

                InsightsUiState(
                    rangeDays = effectiveRange,
                    isPremium = isPremium,
                    consistencyIndex = (totalCompletedInRange.toFloat() / totalPossibleInRange.toFloat()).coerceIn(0f, 1f),
                    doneCount = totalCompletedInRange,
                    targetCount = totalPossibleInRange,
                    heatmap = heatmap,
                    weeklyBars = weeklyBars,
                    drilldowns = drilldowns,
                    leaderboard = InsightsLeaderboardUi(
                        topStreak = topStreakHabit?.let {
                            LeaderboardItemUi(it.name, it.icon, "🔥 ${it.streakCurrent} days")
                        },
                        mostConsistent = mostConsistentHabit?.let {
                            LeaderboardItemUi(it.name, it.icon, "${(it.completionRate * 100).toInt()}% consistency")
                        },
                        mostMissed = mostMissedHabit?.let {
                            LeaderboardItemUi(it.name, it.icon, "${it.missedDays} missed")
                        }
                    ),
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
