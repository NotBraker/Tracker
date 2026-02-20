package com.notbraker.tracker.feature.today

data class WeekDayUi(
    val label: String,
    val epochDay: Long,
    val isSelected: Boolean,
    val dayOfMonth: Int
)

data class HabitCardUi(
    val id: Long,
    val name: String,
    val subtitle: String,
    val icon: String,
    val streak: Int,
    val isCompleted: Boolean
)

data class TodayUiState(
    val title: String = "Today",
    val weekDays: List<WeekDayUi> = emptyList(),
    val selectedEpochDay: Long = 0L,
    val cards: List<HabitCardUi> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val showAddDialog: Boolean = false,
    val pendingDeleteHabitId: Long? = null,
    val message: String? = null
) {
    val goalsLeft: Int = (totalCount - completedCount).coerceAtLeast(0)
    val progress: Float = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
}
