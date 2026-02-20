package com.notbraker.tracker.feature.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notbraker.tracker.core.components.AnimatedCheckbox
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.components.GradientRing
import com.notbraker.tracker.core.components.SectionHeader
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

@Composable
fun TodayRoute(
    viewModel: TodayViewModel = viewModel(),
    onHabitClick: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        state = state,
        onDaySelected = viewModel::selectDay,
        onToggleHabit = viewModel::onToggleHabit,
        onArchive = viewModel::onArchiveHabit,
        onDelete = viewModel::onDeleteRequested,
        onConfirmDelete = viewModel::onConfirmDelete,
        onDismissDelete = viewModel::onDismissDelete,
        onOpenAdd = viewModel::onOpenAddHabit,
        onDismissAdd = viewModel::onDismissAddHabit,
        onCreateHabit = viewModel::onCreateHabit,
        onMessageShown = viewModel::consumeMessage,
        onHabitClick = onHabitClick
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodayScreen(
    state: TodayUiState,
    onDaySelected: (Long) -> Unit,
    onToggleHabit: (Long, Boolean) -> Unit,
    onArchive: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onOpenAdd: () -> Unit,
    onDismissAdd: () -> Unit,
    onCreateHabit: (String, String, String, String) -> Unit,
    onMessageShown: () -> Unit,
    onHabitClick: (Long) -> Unit
) {
    val spacing = TrackerTheme.spacing
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.Background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = spacing.md)
                )
            }
            item {
                WeekSelector(
                    weekDays = state.weekDays,
                    onSelected = onDaySelected
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GradientRing(
                        progress = state.progress,
                        completed = state.completedCount,
                        total = state.totalCount
                    )
                    FloatingActionButton(
                        onClick = onOpenAdd,
                        containerColor = HabitColors.PrimaryAccent,
                        contentColor = HabitColors.OnBackground,
                        modifier = Modifier.size(58.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            item {
                SectionHeader(
                    title = "My Productivity Boosts",
                    subtitle = "${state.goalsLeft} goals left today",
                    actionLabel = "+ Add",
                    onActionClick = onOpenAdd
                )
            }
            items(
                items = state.cards,
                key = { card -> card.id }
            ) { card ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.StartToEnd -> {
                                onArchive(card.id)
                                false
                            }

                            SwipeToDismissBoxValue.EndToStart -> {
                                onDelete(card.id)
                                false
                            }

                            SwipeToDismissBoxValue.Settled -> false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color by animateColorAsState(
                            targetValue = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.StartToEnd -> HabitColors.SecondaryAccent.copy(alpha = 0.32f)
                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.32f)
                                SwipeToDismissBoxValue.Settled -> HabitColors.Surface
                            },
                            label = "dismissColor"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color, shape = MaterialTheme.shapes.large)
                                .padding(horizontal = spacing.lg),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) "Delete" else "Archive",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    content = {
                        HabitCard(
                            card = card,
                            modifier = Modifier.animateItem(),
                            onToggle = onToggleHabit,
                            onClick = onHabitClick
                        )
                    }
                )
            }
            item {
                AnimatedVisibility(
                    visible = state.cards.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "No habits yet. Tap + to create your first productivity boost.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = spacing.xl)
                    )
                }
            }
        }
    }

    if (state.pendingDeleteHabitId != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete habit?") },
            text = { Text("This removes all completion history for this habit.") },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) { Text("Delete") }
            }
        )
    }

    if (state.showAddDialog) {
        AddHabitDialog(
            onDismiss = onDismissAdd,
            onConfirm = onCreateHabit
        )
    }
}

@Composable
private fun WeekSelector(
    weekDays: List<WeekDayUi>,
    onSelected: (Long) -> Unit
) {
    val spacing = TrackerTheme.spacing
    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
        items(weekDays, key = { it.epochDay }) { day ->
            val backgroundColor by animateColorAsState(
                targetValue = if (day.isSelected) HabitColors.PrimaryAccent.copy(alpha = 0.24f) else HabitColors.Surface,
                label = "weekdayBackground"
            )
            Surface(
                color = backgroundColor,
                shape = MaterialTheme.shapes.medium,
                onClick = { onSelected(day.epochDay) }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitCard(
    card: HabitCardUi,
    modifier: Modifier,
    onToggle: (Long, Boolean) -> Unit,
    onClick: (Long) -> Unit
) {
    val spacing = TrackerTheme.spacing
    val completionAlpha by animateFloatAsState(
        targetValue = if (card.isCompleted) 0.65f else 1f,
        label = "completionAlpha"
    )
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(card.id) },
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                HabitColors.PrimaryAccent.copy(alpha = 0.36f),
                                HabitColors.SecondaryAccent.copy(alpha = 0.3f)
                            )
                        ),
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(card.icon, style = MaterialTheme.typography.titleLarge)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = completionAlpha)
                )
                Text(
                    text = "${card.subtitle}  ·  🔥 ${card.streak}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f * completionAlpha)
                )
            }
            AnimatedCheckbox(
                checked = card.isCompleted,
                onCheckedChange = { onToggle(card.id, it) }
            )
        }
    }
}

@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf("✨") }
    var frequency by rememberSaveable { mutableStateOf("Daily") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name") },
                    singleLine = true
                )
                androidx.compose.material3.OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                androidx.compose.material3.OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it.take(2) },
                    label = { Text("Emoji/icon") },
                    singleLine = true
                )
                androidx.compose.material3.OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Frequency") },
                    singleLine = true
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description, icon.ifBlank { "✨" }, frequency.ifBlank { "Daily" }) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        }
    )
}
