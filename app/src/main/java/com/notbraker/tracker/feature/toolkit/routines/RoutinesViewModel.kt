package com.notbraker.tracker.feature.toolkit.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notbraker.tracker.data.model.Habit
import com.notbraker.tracker.data.model.HabitDayState
import com.notbraker.tracker.data.model.RoutineSummary
import com.notbraker.tracker.data.repository.HabitRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutinesUiState(
    val routines: List<RoutineSummary> = emptyList(),
    val availableHabits: List<Habit> = emptyList(),
    val selectedHabitIds: Set<Long> = emptySet(),
    val draftName: String = "",
    val draftDescription: String = "",
    val message: String? = null
)

class RoutinesViewModel(
    private val repository: HabitRepository
) : ViewModel() {
    private val draft = MutableStateFlow(RoutinesUiState())

    val uiState: StateFlow<RoutinesUiState> = combine(
        repository.observeRoutineSummaries(),
        repository.observeActiveHabits(),
        draft
    ) { routines, habits, current ->
        current.copy(routines = routines, availableHabits = habits)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoutinesUiState()
    )

    fun onNameChanged(value: String) {
        draft.update { it.copy(draftName = value.take(40)) }
    }

    fun onDescriptionChanged(value: String) {
        draft.update { it.copy(draftDescription = value.take(80)) }
    }

    fun toggleHabitSelection(habitId: Long) {
        draft.update { state ->
            val next = state.selectedHabitIds.toMutableSet()
            if (!next.add(habitId)) {
                next.remove(habitId)
            }
            state.copy(selectedHabitIds = next)
        }
    }

    fun createRoutine() {
        val snapshot = uiState.value
        if (snapshot.draftName.isBlank()) {
            draft.update { it.copy(message = "Routine name is required.") }
            return
        }
        viewModelScope.launch {
            repository.createRoutine(
                name = snapshot.draftName,
                description = snapshot.draftDescription.ifBlank { "Routine session" },
                habitIds = snapshot.selectedHabitIds.toList()
            )
            draft.update {
                it.copy(
                    selectedHabitIds = emptySet(),
                    draftName = "",
                    draftDescription = "",
                    message = "Routine created"
                )
            }
        }
    }

    fun consumeMessage() {
        draft.update { it.copy(message = null) }
    }
}

data class RoutineDetailUiState(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val habits: List<Habit> = emptyList()
)

class RoutineDetailViewModel(
    routineId: Long,
    repository: HabitRepository
) : ViewModel() {
    val uiState: StateFlow<RoutineDetailUiState> = repository.observeRoutineWithHabits(routineId)
        .map { routine ->
            if (routine == null) {
                RoutineDetailUiState()
            } else {
                RoutineDetailUiState(
                    id = routine.routine.id,
                    title = routine.routine.name,
                    description = routine.routine.description,
                    habits = routine.habits
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RoutineDetailUiState()
        )
}

data class RoutineSessionUiState(
    val routineName: String = "",
    val steps: List<HabitDayState> = emptyList(),
    val currentIndex: Int = 0
) {
    val currentStep: HabitDayState? = steps.getOrNull(currentIndex)
    val isComplete: Boolean = steps.isNotEmpty() && steps.all { it.isCompleted }
}

class RoutineSessionViewModel(
    private val routineId: Long,
    private val repository: HabitRepository
) : ViewModel() {
    private val currentIndex = MutableStateFlow(0)
    private val today = LocalDate.now().toEpochDay()

    val uiState: StateFlow<RoutineSessionUiState> = combine(
        repository.observeRoutineWithHabits(routineId),
        repository.observeRoutineHabitsForDay(routineId, today),
        currentIndex
    ) { routine, steps, index ->
        RoutineSessionUiState(
            routineName = routine?.routine?.name.orEmpty(),
            steps = steps,
            currentIndex = index.coerceIn(0, (steps.lastIndex).coerceAtLeast(0))
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoutineSessionUiState()
    )

    fun markCurrentDone() {
        val step = uiState.value.currentStep ?: return
        viewModelScope.launch {
            repository.setHabitCompletion(step.habit.id, today, true)
            next()
        }
    }

    fun next() {
        currentIndex.update { it + 1 }
    }

    fun previous() {
        currentIndex.update { (it - 1).coerceAtLeast(0) }
    }
}

class RoutinesViewModelFactory(
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RoutinesViewModel(repository) as T
    }
}

class RoutineDetailViewModelFactory(
    private val routineId: Long,
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RoutineDetailViewModel(routineId, repository) as T
    }
}

class RoutineSessionViewModelFactory(
    private val routineId: Long,
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RoutineSessionViewModel(routineId, repository) as T
    }
}
