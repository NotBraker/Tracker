package com.notbraker.tracker.feature.habitdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notbraker.tracker.data.model.HabitFrequencyType
import com.notbraker.tracker.data.repository.HabitRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitDetailUiState(
    val habitId: Long = 0L,
    val name: String = "",
    val icon: String = "✨",
    val description: String = "",
    val frequencyLabel: String = "Daily",
    val frequencyType: String = "DAILY",
    val streakCurrent: Int = 0,
    val streakLongest: Int = 0,
    val completionRate: Float = 0f,
    val miniGrid: List<Boolean> = emptyList(),
    val reminderEnabled: Boolean = false,
    val reminderTimeText: String = "No reminder",
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val isPremium: Boolean = false,
    val message: String? = null
)

class HabitDetailViewModel(
    private val habitId: Long,
    private val repository: HabitRepository,
    private val isPremiumProvider: () -> Boolean = { false },
    private val onHabitDeleted: (Long, String?) -> Unit = { _, _ -> },
    private val onHabitArchived: (Long) -> Unit = {},
    private val onReminderUpdated: (Long, String, Boolean, Int?, Int?) -> Unit = { _, _, _, _, _ -> },
    private val onDataChanged: () -> Unit = {}
) : ViewModel() {

    val uiState: StateFlow<HabitDetailUiState> = combine(
        repository.observeHabit(habitId),
        repository.observeCompletionsForHabit(habitId)
    ) { habit, completions ->
        if (habit == null) {
            HabitDetailUiState(message = "Habit not found")
        } else {
            val today = LocalDate.now().toEpochDay()
            val created = habit.createdAtEpochDay
            val activeDays = (today - created + 1).coerceAtLeast(1)
            val completionRate = (completions.size.toFloat() / activeDays.toFloat()).coerceIn(0f, 1f)
            val completionSet = completions.map { it.epochDay }.toSet()
            val miniGrid = (0 until 28).map { offset ->
                completionSet.contains(today - (27 - offset))
            }
            val reminderHour = (habit.reminderHour ?: 8).coerceIn(0, 23)
            val reminderMinute = (habit.reminderMinute ?: 0).coerceIn(0, 59)
            HabitDetailUiState(
                habitId = habit.id,
                name = habit.name,
                icon = habit.icon,
                description = habit.description,
                frequencyLabel = habit.frequencyLabel.ifBlank { "Daily" },
                frequencyType = habit.frequencyType.ifBlank { HabitFrequencyType.DAILY.name },
                streakCurrent = habit.streakCurrent,
                streakLongest = habit.streakLongest,
                completionRate = completionRate,
                miniGrid = miniGrid,
                reminderEnabled = habit.reminderEnabled,
                reminderTimeText = if (habit.reminderEnabled) {
                    "${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')}"
                } else {
                    "No reminder"
                },
                reminderHour = if (habit.reminderEnabled) reminderHour else null,
                reminderMinute = if (habit.reminderEnabled) reminderMinute else null,
                isPremium = isPremiumProvider()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitDetailUiState()
    )

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            val result = repository.updateReminder(
                habitId = habitId,
                enabled = enabled,
                hour = if (enabled) hour else null,
                minute = if (enabled) minute else null,
                isPremium = isPremiumProvider()
            )
            if (result.success) {
                onReminderUpdated(
                    habitId,
                    uiState.value.name,
                    enabled,
                    if (enabled) hour else null,
                    if (enabled) minute else null
                )
                onDataChanged()
            }
        }
    }

    fun updateHabit(
        name: String,
        description: String,
        icon: String,
        frequencyLabel: String,
        frequencyType: String
    ) {
        viewModelScope.launch {
            val current = repository.getHabit(habitId) ?: return@launch
            val updated = current.copy(
                name = name.trim(),
                description = description.trim(),
                icon = icon.ifBlank { current.icon }.take(1),
                frequencyLabel = frequencyLabel.ifBlank { "Daily" },
                frequencyType = frequencyType.ifBlank { HabitFrequencyType.DAILY.name }
            )
            repository.updateHabit(updated)
            onDataChanged()
        }
    }

    fun archiveHabit() {
        viewModelScope.launch {
            repository.archiveHabit(habitId)
            onHabitArchived(habitId)
            onDataChanged()
        }
    }

    fun deleteHabit() {
        viewModelScope.launch {
            val templateId = repository.deleteHabitAndGetTemplateId(habitId)
            onHabitDeleted(habitId, templateId)
            onDataChanged()
        }
    }
}

class HabitDetailViewModelFactory(
    private val habitId: Long,
    private val repository: HabitRepository,
    private val isPremiumProvider: () -> Boolean = { false },
    private val onHabitDeleted: (Long, String?) -> Unit = { _, _ -> },
    private val onHabitArchived: (Long) -> Unit = {},
    private val onReminderUpdated: (Long, String, Boolean, Int?, Int?) -> Unit = { _, _, _, _, _ -> },
    private val onDataChanged: () -> Unit = {}
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HabitDetailViewModel(
            habitId = habitId,
            repository = repository,
            isPremiumProvider = isPremiumProvider,
            onHabitDeleted = onHabitDeleted,
            onHabitArchived = onHabitArchived,
            onReminderUpdated = onReminderUpdated,
            onDataChanged = onDataChanged
        ) as T
    }
}
