package com.notbraker.tracker.feature.toolkit.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.components.SectionHeader
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

@Composable
fun RoutinesRoute(
    viewModel: RoutinesViewModel = viewModel(),
    onOpenRoutine: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) {
        if (state.message != null) {
            viewModel.consumeMessage()
        }
    }
    RoutinesScreen(
        state = state,
        onNameChanged = viewModel::onNameChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onToggleHabit = viewModel::toggleHabitSelection,
        onCreateRoutine = viewModel::createRoutine,
        onOpenRoutine = onOpenRoutine
    )
}

@Composable
fun RoutinesScreen(
    state: RoutinesUiState,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onToggleHabit: (Long) -> Unit,
    onCreateRoutine: () -> Unit,
    onOpenRoutine: (Long) -> Unit
) {
    val spacing = TrackerTheme.spacing
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.BackgroundPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item {
                Text("Routines", style = MaterialTheme.typography.displayLarge)
            }
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        SectionHeader(title = "Create routine", subtitle = "Bundle habits into one run")
                        OutlinedTextField(
                            value = state.draftName,
                            onValueChange = onNameChanged,
                            label = { Text("Routine name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.draftDescription,
                            onValueChange = onDescriptionChanged,
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Add habits", style = MaterialTheme.typography.titleMedium)
                        state.availableHabits.forEach { habit ->
                            Surface(
                                onClick = { onToggleHabit(habit.id) },
                                shape = MaterialTheme.shapes.medium,
                                color = if (state.selectedHabitIds.contains(habit.id)) HabitColors.SurfaceAccentGlow else HabitColors.Surface
                            ) {
                                Text(
                                    habit.name,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xs),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Surface(
                            onClick = onCreateRoutine,
                            shape = MaterialTheme.shapes.medium,
                            color = HabitColors.PrimaryElectricBlue
                        ) {
                            Text(
                                "Create routine",
                                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
                            )
                        }
                    }
                }
            }
            item {
                SectionHeader(title = "Your routines")
            }
            items(state.routines, key = { it.id }) { routine ->
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                            Text(routine.name, style = MaterialTheme.typography.titleMedium)
                            Text("${routine.habitCount} habits", style = MaterialTheme.typography.labelMedium, color = HabitColors.OnSurfaceMuted)
                        }
                        Surface(onClick = { onOpenRoutine(routine.id) }, shape = MaterialTheme.shapes.medium, color = HabitColors.Surface) {
                            Text("Open", modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs))
                        }
                    }
                }
            }
            item { Row(modifier = Modifier.navigationBarsPadding()) { Text("") } }
        }
    }
}

@Composable
fun RoutineDetailRoute(
    viewModel: RoutineDetailViewModel = viewModel(),
    onStartSession: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = TrackerTheme.spacing
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.BackgroundPrimary
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding().padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(state.title.ifBlank { "Routine" }, style = MaterialTheme.typography.displayLarge)
            Text(state.description, style = MaterialTheme.typography.bodyLarge, color = HabitColors.OnSurfaceMuted)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    state.habits.forEach { habit ->
                        Text("• ${habit.name}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Surface(onClick = { onStartSession(state.id) }, color = HabitColors.SurfaceAccentGlow, shape = MaterialTheme.shapes.medium) {
                Text("Start routine", modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm))
            }
        }
    }
}

@Composable
fun RoutineSessionRoute(
    viewModel: RoutineSessionViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = TrackerTheme.spacing
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.BackgroundPrimary
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding().padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(state.routineName.ifBlank { "Routine session" }, style = MaterialTheme.typography.displayLarge)
            Text("Step ${state.currentIndex + 1} of ${state.steps.size}", style = MaterialTheme.typography.labelMedium, color = HabitColors.OnSurfaceMuted)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    val step = state.currentStep
                    if (step == null) {
                        Text("No steps available.")
                    } else {
                        Text(step.habit.name, style = MaterialTheme.typography.titleLarge)
                        Text(step.habit.description, style = MaterialTheme.typography.bodyMedium, color = HabitColors.OnSurfaceMuted)
                        Surface(
                            onClick = viewModel::markCurrentDone,
                            shape = MaterialTheme.shapes.medium,
                            color = HabitColors.SuccessGreen
                        ) {
                            Text(
                                if (step.isCompleted) "Completed" else "Mark done",
                                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Surface(onClick = viewModel::previous, shape = MaterialTheme.shapes.medium, color = HabitColors.Surface) {
                    Text("Previous", modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm))
                }
                Surface(onClick = viewModel::next, shape = MaterialTheme.shapes.medium, color = HabitColors.Surface) {
                    Text("Next", modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm))
                }
            }
            if (state.isComplete) {
                Text("Routine complete.", color = HabitColors.SuccessGreen, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
