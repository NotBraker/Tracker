package com.notbraker.tracker.feature.habitdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.components.SectionHeader
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

@Composable
fun HabitDetailRoute(
    viewModel: HabitDetailViewModel = viewModel(),
    onClose: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HabitDetailScreen(
        state = state,
        onBack = onClose,
        onArchive = viewModel::archiveHabit,
        onDelete = viewModel::deleteHabit,
        onReminderToggled = viewModel::updateReminder
    )
}

@Composable
fun HabitDetailScreen(
    state: HabitDetailUiState,
    onBack: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onReminderToggled: (Boolean, Int, Int) -> Unit
) {
    val spacing = TrackerTheme.spacing
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                HabitColors.SecondaryAccent.copy(alpha = 0.38f),
                                HabitColors.PrimaryAccent.copy(alpha = 0.22f),
                                HabitColors.Background
                            )
                        )
                    )
                    .padding(spacing.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Text(
                        text = "${state.icon} ${state.name}",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Current streak: 🔥 ${state.streakCurrent}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                SectionHeader(title = "Calendar mini grid")
                MiniGrid(state.miniGrid)

                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        SectionHeader(title = "Reminder settings")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Daily reminder", color = MaterialTheme.colorScheme.onBackground)
                                Text(
                                    text = state.reminderTimeText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = state.reminderEnabled,
                                onCheckedChange = { onReminderToggled(it, 20, 0) }
                            )
                        }
                    }
                }

                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text("Completion rate: ${(state.completionRate * 100).toInt()}%")
                        Text("Longest streak: ${state.streakLongest} days")
                        Text(state.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Surface(
                        onClick = onArchive,
                        color = HabitColors.SecondaryAccent.copy(alpha = 0.22f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Archive", modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm))
                    }
                    Surface(
                        onClick = { showDeleteDialog = true },
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.22f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Delete", modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete habit?") },
            text = { Text("This action cannot be undone.") },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                        onBack()
                    }
                ) {
                    Text("Delete")
                }
            }
        )
    }
}

@Composable
private fun MiniGrid(values: List<Boolean>) {
    val spacing = TrackerTheme.spacing
    val rows = values.chunked(7)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                row.forEach { completed ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (completed) HabitColors.Success else HabitColors.SurfaceElevated,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
