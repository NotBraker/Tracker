package com.notbraker.tracker.feature.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notbraker.tracker.core.components.AdPlaceholderCard
import com.notbraker.tracker.core.components.AnimatedCheckbox
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.components.GradientRing
import com.notbraker.tracker.core.components.SectionHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme
import com.notbraker.tracker.data.model.RoutineSummary

@Composable
fun TodayRoute(
    viewModel: TodayViewModel = viewModel(),
    onHabitClick: (Long) -> Unit = {},
    onOpenTemplates: () -> Unit = {},
    onRoutineClick: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        state = state,
        onDaySelected = viewModel::selectDay,
        onToggleHabit = viewModel::onToggleHabit,
        onOpenAdd = viewModel::onOpenAddHabit,
        onDismissAdd = viewModel::onDismissAddHabit,
        onCreateHabit = viewModel::onCreateHabit,
        onMoveHabit = viewModel::moveHabit,
        onMessageShown = viewModel::consumeMessage,
        onHabitClick = onHabitClick,
        onOpenTemplates = onOpenTemplates,
        onRoutineClick = onRoutineClick
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodayScreen(
    state: TodayUiState,
    onDaySelected: (Long) -> Unit,
    onToggleHabit: (Long, Boolean) -> Unit,
    onOpenAdd: () -> Unit,
    onDismissAdd: () -> Unit,
    onCreateHabit: (String, String, String, String) -> Unit,
    onMoveHabit: (Int, Int) -> Unit,
    onMessageShown: () -> Unit,
    onHabitClick: (Long) -> Unit,
    onOpenTemplates: () -> Unit = {},
    onRoutineClick: (Long) -> Unit = {}
) {
    val spacing = TrackerTheme.spacing
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var accumulatedY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val slotHeightPx = with(density) { 56.dp.toPx() }

    fun onDragStart(idx: Int) {
        dragStartIndex = idx
        accumulatedY = 0f
    }
    fun onDragDelta(idx: Int, dy: Float) {
        if (dragStartIndex < 0) return
        accumulatedY += dy
        val n = state.cards.size
        while (accumulatedY > slotHeightPx && dragStartIndex < n - 1) {
            val to = dragStartIndex + 1
            onMoveHabit(dragStartIndex, to)
            dragStartIndex = to
            accumulatedY -= slotHeightPx
        }
        while (accumulatedY < -slotHeightPx && dragStartIndex > 0) {
            val to = dragStartIndex - 1
            onMoveHabit(dragStartIndex, to)
            dragStartIndex = to
            accumulatedY += slotHeightPx
        }
    }
    fun onDragEnd() {
        dragStartIndex = -1
        accumulatedY = 0f
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.BackgroundPrimary,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAdd,
                containerColor = HabitColors.SurfaceAccentGlow,
                contentColor = HabitColors.OnPrimary,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            contentPadding = PaddingValues(bottom = spacing.xl + spacing.lg)
        ) {
            if (!state.isPremium) {
                item {
                    AdPlaceholderCard()
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .padding(top = spacing.md)
                        .fillMaxWidth()
                ) {
                    Text(text = state.title, style = MaterialTheme.typography.displayLarge)
                    Text(
                        text = "Consistency ${((state.progress) * 100).toInt()}% · ${state.goalsLeft} goals remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = HabitColors.OnSurfaceMuted
                    )
                }
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
                        total = state.totalCount.coerceAtLeast(1),
                        size = 230.dp
                    )
                }
            }
            item {
                SectionHeader(
                    title = "My Habit Stack",
                    subtitle = if (state.isSelectedDayToday) "${state.goalsLeft} goals left today" else "View only",
                    actionLabel = "+ Add",
                    onActionClick = onOpenAdd
                )
            }
            if (state.routineSummaries.isNotEmpty()) {
                item {
                    SectionHeader(title = "My Routines", subtitle = "Tap to start or view")
                }
                items(state.routineSummaries, key = { it.id }) { routine ->
                    Surface(
                        onClick = { onRoutineClick(routine.id) },
                        shape = MaterialTheme.shapes.medium,
                        color = HabitColors.Surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.md, vertical = spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(routine.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${routine.habitCount} habits",
                                style = MaterialTheme.typography.labelMedium,
                                color = HabitColors.OnSurfaceMuted
                            )
                        }
                    }
                }
            }
            itemsIndexed(
                items = state.cards,
                key = { _, card -> card.id }
            ) { index, card ->
                HabitCard(
                    card = card,
                    index = index,
                    modifier = Modifier.animateItem(),
                    isToday = state.isSelectedDayToday,
                    onToggle = onToggleHabit,
                    onClick = onHabitClick,
                    onDragStart = { onDragStart(index) },
                    onDragDelta = { dy -> onDragDelta(index, dy) },
                    onDragEnd = { onDragEnd() }
                )
            }
            item {
                Column(
                    modifier = Modifier.padding(vertical = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    AnimatedVisibility(visible = state.cards.isEmpty()) {
                        Text(
                            text = "No habits yet. Use + to build your first precision streak.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = HabitColors.OnSurfaceMuted
                        )
                    }
                    Surface(
                        onClick = onOpenTemplates,
                        shape = MaterialTheme.shapes.medium,
                        color = HabitColors.SurfaceAccentGlow
                    ) {
                        Text(
                            text = "Popular habits",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md)
                        )
                    }
                }
            }
        }
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
                targetValue = if (day.isSelected) HabitColors.SurfaceAccentGlow else HabitColors.Surface,
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
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.size(spacing.xxs))
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (day.isSelected) 22.dp else 10.dp,
                                height = 3.dp
                            )
                            .background(
                                color = if (day.isSelected) HabitColors.HighlightCyan else HabitColors.Surface,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitCard(
    card: HabitCardUi,
    index: Int,
    modifier: Modifier,
    isToday: Boolean,
    onToggle: (Long, Boolean) -> Unit,
    onClick: (Long) -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val spacing = TrackerTheme.spacing
    val contentAlpha = when {
        !isToday -> 0.5f
        card.isCompleted -> 0.65f
        else -> 1f
    }
    val completionAlpha by animateFloatAsState(
        targetValue = contentAlpha,
        animationSpec = tween(durationMillis = TrackerTheme.motion.medium, easing = FastOutSlowInEasing),
        label = "completionAlpha"
    )
    val accentColor = when ((card.id % 4).toInt()) {
        0 -> HabitColors.PrimaryElectricBlue
        1 -> HabitColors.SecondaryViolet
        2 -> HabitColors.HighlightCyan
        else -> HabitColors.SuccessGreen
    }
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isToday) Modifier.clickable { onClick(card.id) }
                    else Modifier
                ),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isToday) {
                Box(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onDragStart() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragDelta(dragAmount.y)
                                },
                                onDragEnd = { onDragEnd() }
                            )
                        }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = "Reorder",
                        tint = HabitColors.OnSurfaceMuted
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(4.dp, 52.dp)
                    .background(accentColor, shape = MaterialTheme.shapes.small)
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.4f),
                                HabitColors.SurfaceAccentGlow.copy(alpha = 0.7f)
                            )
                        ),
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(card.icon.takeIf { it.isNotBlank() } ?: "•", style = MaterialTheme.typography.titleMedium)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
            ) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = completionAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${card.subtitle} · target ${card.targetCount} · streak ${card.streak}d",
                    style = MaterialTheme.typography.labelMedium,
                    color = HabitColors.OnSurfaceMuted.copy(alpha = completionAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AnimatedCheckbox(
                checked = card.isCompleted,
                onCheckedChange = { if (isToday) onToggle(card.id, it) }
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
    var icon by rememberSaveable { mutableStateOf("T") }
    var frequency by rememberSaveable { mutableStateOf("Daily") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TrackerTheme.spacing.sm)) {
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
                    onValueChange = { icon = it.take(1) },
                    label = { Text("Icon letter") },
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
                onClick = { onConfirm(name, description, icon.ifBlank { "T" }, frequency.ifBlank { "Daily" }) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        }
    )
}
