package com.notbraker.tracker.feature.habitdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
        onClose = onClose,
        onDelete = viewModel::deleteHabit,
        onReminderToggled = viewModel::updateReminder,
        onUpdateHabit = viewModel::updateHabit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    state: HabitDetailUiState,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onReminderToggled: (Boolean, Int, Int) -> Unit,
    onUpdateHabit: (String, String, String, String, String) -> Unit
) {
    val spacing = TrackerTheme.spacing
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    fun formatTime(h: Int, m: Int): String {
        val hour = h.coerceIn(0, 23)
        val min = m.coerceIn(0, 59)
        val am = hour < 12
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return "$displayHour:${min.toString().padStart(2, '0')} ${if (am) "AM" else "PM"}"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.BackgroundPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(HabitColors.BackgroundPrimary)
                    .padding(horizontal = spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                HabitColors.SecondaryViolet.copy(alpha = 0.42f),
                                HabitColors.PrimaryElectricBlue.copy(alpha = 0.3f),
                                HabitColors.BackgroundPrimary
                            )
                        )
                    )
                    .padding(spacing.lg)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = state.name,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Current streak",
                        style = MaterialTheme.typography.labelMedium,
                        color = HabitColors.OnSurfaceMuted
                    )
                    Text(
                        text = "${state.streakCurrent} days",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                StatsGrid(state = state)

                SectionHeader(title = "Calendar")
                MiniGrid(state.miniGrid)

                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (state.reminderEnabled) "Reminder: ${formatTime(state.reminderHour ?: 8, state.reminderMinute ?: 0)}" else "Reminder: Off",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.reminderEnabled) {
                                    TextButton(onClick = { showTimePicker = true }) {
                                        Text("Change time", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                Switch(
                                    checked = state.reminderEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) showTimePicker = true
                                        else onReminderToggled(false, 0, 0)
                                    }
                                )
                            }
                        }
                        if (state.reminderEnabled && !showTimePicker) {
                            Text(
                                text = "Daily at ${formatTime(state.reminderHour ?: 8, state.reminderMinute ?: 0)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = HabitColors.OnSurfaceMuted
                            )
                        }
                    }
                }

                AppCard {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Habit", color = MaterialTheme.colorScheme.error)
                    }
                }

                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text("Completion rate: ${(state.completionRate * 100).toInt()}%")
                        Text("Longest streak: ${state.streakLongest} days")
                        Text(state.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(modifier = Modifier.navigationBarsPadding()) { Text("") }
            }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = (state.reminderHour ?: 8),
            initialMinute = (state.reminderMinute ?: 0),
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReminderToggled(true, timeState.hour, timeState.minute)
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timeState)
            }
        )
    }

    if (showEditDialog) {
        EditHabitDialog(
            state = state,
            onDismiss = { showEditDialog = false },
            onSave = { name, desc, icon, freqLabel, freqType ->
                onUpdateHabit(name, desc, icon, freqLabel, freqType)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Are you sure?") },
            text = { Text("This removes the habit and all its completion history.") },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("No") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                        onClose()
                    }
                ) { Text("Yes") }
            }
        )
    }
}

@Composable
private fun EditHabitDialog(
    state: HabitDetailUiState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember(state.habitId) { mutableStateOf(state.name) }
    var description by remember(state.habitId) { mutableStateOf(state.description) }
    var icon by remember(state.habitId) { mutableStateOf(state.icon) }
    var frequency by remember(state.habitId) { mutableStateOf(state.frequencyLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TrackerTheme.spacing.sm)) {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it.take(1) },
                    label = { Text("Icon letter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Frequency (Daily/Weekly)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(
                onClick = {
                    val freqType = if (frequency.equals("Weekly", ignoreCase = true)) "WEEKLY" else "DAILY"
                    onSave(
                        name,
                        description,
                        icon.ifBlank { state.icon }.take(1),
                        frequency.ifBlank { "Daily" },
                        freqType
                    )
                }
            ) { Text("Save") }
        }
    )
}

@Composable
private fun MiniGrid(values: List<Boolean>) {
    val spacing = TrackerTheme.spacing
    val dayHeaders = listOf("S", "M", "T", "W", "Th", "F", "S")
    val rows = values.chunked(7)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            dayHeaders.forEach { label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = HabitColors.OnSurfaceMuted
                    )
                }
            }
        }
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                row.forEach { completed ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                color = if (completed) HabitColors.SuccessGreen else HabitColors.SurfaceElevated,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(state: HabitDetailUiState) {
    val spacing = TrackerTheme.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
        StatTile(
            modifier = Modifier.weight(1f),
            label = "Completion",
            value = "${(state.completionRate * 100).toInt()}%"
        )
        StatTile(
            modifier = Modifier.weight(1f),
            label = "Longest",
            value = "${state.streakLongest}d"
        )
        StatTile(
            modifier = Modifier.weight(1f),
            label = "Missed",
            value = "${(100 - (state.completionRate * 100).toInt()).coerceAtLeast(0)}%"
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = HabitColors.Surface
    ) {
        Column(modifier = Modifier.padding(TrackerTheme.spacing.sm)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = HabitColors.OnSurfaceMuted)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
