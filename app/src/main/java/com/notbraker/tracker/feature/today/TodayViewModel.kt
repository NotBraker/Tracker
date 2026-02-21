package com.notbraker.tracker.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notbraker.tracker.data.model.HabitDayState
import com.notbraker.tracker.data.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class TodayUiFlags(
    val showAddDialog: Boolean = false,
    val pendingDeleteHabitId: Long? = null,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: HabitRepository,
    private val isPremiumProvider: () -> Boolean = { false },
    private val onHabitDeleted: (Long, String?) -> Unit = { _, _ -> },
    private val onHabitArchived: (Long) -> Unit = {},
    private val onDataChanged: () -> Unit = {}
) : ViewModel() {

    private val selectedEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    private val uiFlags = MutableStateFlow(TodayUiFlags())

    private val dayHabitsFlow = selectedEpochDay
        .flatMapLatest { day ->
            repository.observeTodayHabits(day).map { states -> day to states }
        }

    private val weeklyCompletionsFlow = selectedEpochDay.flatMapLatest { day ->
        val date = LocalDate.ofEpochDay(day)
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
        val weekEnd = weekStart + 6
        repository.observeCompletionCountsBetween(weekStart, weekEnd).map { counts -> weekStart to counts }
    }
    private val routinesFlow = repository.observeRoutineSummaries()

    val uiState: StateFlow<TodayUiState> = combine(dayHabitsFlow, weeklyCompletionsFlow, routinesFlow, uiFlags) { dayAndStates, weekData, routines, flags ->
        val selectedDay = dayAndStates.first
        val states = dayAndStates.second.distinctBy { it.habit.id }
        val selectedDate = LocalDate.ofEpochDay(selectedDay)
        val sorted = states.sortedWith(compareBy<HabitDayState> { it.habit.sortOrder }.thenBy { it.habit.id })
        val cards = sorted.map { state ->
            val target = repository.run { state.habit.targetForDate(selectedDate) }
            HabitCardUi(
                id = state.habit.id,
                name = state.habit.name,
                subtitle = state.habit.frequencyLabel.ifBlank { "Daily" },
                icon = state.habit.icon,
                streak = state.habit.streakCurrent,
                targetCount = target,
                isCompleted = state.isCompleted
            )
        }
        val distinctCards = cards.distinctBy { it.id }
        val dailyHabits = distinctCards.filter { card ->
            val habit = states.firstOrNull { it.habit.id == card.id }?.habit ?: return@filter false
            habit.frequencyType.equals("DAILY", ignoreCase = true) || habit.frequencyLabel.equals("Daily", ignoreCase = true)
        }
        val totalTarget = dailyHabits.size
        val completedCount = dailyHabits.count { it.isCompleted }
        val weekStart = weekData.first
        val completionByDay = weekData.second.associateBy({ it.epochDay }, { it.completedCount })
        val weeklyDone = (0..6).sumOf { index -> completionByDay[weekStart + index] ?: 0 }
        val dailyStates = states.filter { it.habit.frequencyType.equals("DAILY", ignoreCase = true) || it.habit.frequencyLabel.equals("Daily", ignoreCase = true) }.distinctBy { it.habit.id }
        val weeklyTarget = (0..6).sumOf { index ->
            val day = LocalDate.ofEpochDay(weekStart + index)
            dailyStates.count { state -> repository.run { state.habit.targetForDate(day) > 0 } }
        }
        TodayUiState(
            weekDays = buildWeekDays(selectedDay),
            selectedEpochDay = selectedDay,
            cards = cards,
            routineSummaries = routines,
            completedCount = completedCount,
            totalCount = totalTarget,
            weeklyDoneCount = weeklyDone,
            weeklyTargetCount = weeklyTarget,
            isPremium = isPremiumProvider(),
            showAddDialog = flags.showAddDialog,
            pendingDeleteHabitId = flags.pendingDeleteHabitId,
            message = flags.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState()
    )

    fun selectDay(epochDay: Long) {
        selectedEpochDay.value = epochDay
    }

    fun onToggleHabit(id: Long, checked: Boolean) {
        viewModelScope.launch {
            repository.setHabitCompletion(id, selectedEpochDay.value, checked)
            onDataChanged()
        }
    }

    fun onArchiveHabit(id: Long) {
        viewModelScope.launch {
            repository.archiveHabit(id)
            onHabitArchived(id)
            onDataChanged()
            uiFlags.update { it.copy(message = "Habit archived") }
        }
    }

    fun onDeleteRequested(id: Long) {
        uiFlags.update { it.copy(pendingDeleteHabitId = id) }
    }

    fun onDismissDelete() {
        uiFlags.update { it.copy(pendingDeleteHabitId = null) }
    }

    fun onConfirmDelete() {
        val habitId = uiFlags.value.pendingDeleteHabitId ?: return
        viewModelScope.launch {
            val templateId = repository.deleteHabitAndGetTemplateId(habitId)
            onHabitDeleted(habitId, templateId)
            onDataChanged()
            uiFlags.update { it.copy(pendingDeleteHabitId = null, message = "Habit deleted") }
        }
    }

    fun onOpenAddHabit() {
        uiFlags.update { it.copy(showAddDialog = true) }
    }

    fun onDismissAddHabit() {
        uiFlags.update { it.copy(showAddDialog = false) }
    }

    fun onCreateHabit(
        name: String,
        description: String,
        icon: String,
        frequency: String
    ) {
        viewModelScope.launch {
            val result = repository.createHabit(
                name = name,
                description = description,
                icon = icon,
                frequencyLabel = frequency,
                reminderHour = null,
                reminderMinute = null,
                templateId = null,
                templateTag = null,
                isPremium = isPremiumProvider()
            )
            uiFlags.update {
                it.copy(
                    showAddDialog = !result.success,
                    message = result.reason ?: "Habit created"
                )
            }
            if (result.success) {
                onDataChanged()
            }
        }
    }

    fun consumeMessage() {
        uiFlags.update { it.copy(message = null) }
    }

    fun moveHabit(fromIndex: Int, toIndex: Int) {
        val cards = uiState.value.cards
        if (fromIndex !in cards.indices || toIndex !in cards.indices || fromIndex == toIndex) return
        viewModelScope.launch {
            val reordered = cards.map { it.id }.toMutableList()
            val moved = reordered.removeAt(fromIndex)
            reordered.add(toIndex, moved)
            repository.reorderHabits(reordered)
            onDataChanged()
        }
    }

    private fun buildWeekDays(selectedDay: Long): List<WeekDayUi> {
        val selectedDate = LocalDate.ofEpochDay(selectedDay)
        val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0..6).map { index ->
            val date = weekStart.plusDays(index.toLong())
            val dayLabel = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            WeekDayUi(
                label = dayLabel,
                epochDay = date.toEpochDay(),
                isSelected = date.toEpochDay() == selectedDay,
                dayOfMonth = date.dayOfMonth
            )
        }
    }
}

class TodayViewModelFactory(
    private val repository: HabitRepository,
    private val isPremiumProvider: () -> Boolean = { false },
    private val onHabitDeleted: (Long, String?) -> Unit = { _, _ -> },
    private val onHabitArchived: (Long) -> Unit = {},
    private val onDataChanged: () -> Unit = {}
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TodayViewModel(
            repository = repository,
            isPremiumProvider = isPremiumProvider,
            onHabitDeleted = onHabitDeleted,
            onHabitArchived = onHabitArchived,
            onDataChanged = onDataChanged
        ) as T
    }
}
