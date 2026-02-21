package com.notbraker.tracker.feature.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notbraker.tracker.core.components.AdPlaceholderCard
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.components.GradientRing
import com.notbraker.tracker.core.components.SectionHeader
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

@Composable
fun InsightsRoute(
    viewModel: InsightsViewModel = viewModel(),
    onOpenPaywall: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(
        state = state,
        onRangeSelected = viewModel::selectRange,
        onOpenPaywall = onOpenPaywall
    )
}

@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onRangeSelected: (Int) -> Unit,
    onOpenPaywall: () -> Unit
) {
    val spacing = TrackerTheme.spacing
    var selectedEpochDay by remember { mutableLongStateOf(0L) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.BackgroundPrimary,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            if (state.isPremiumLocked) {
                AdPlaceholderCard()
            }
            Text(
                text = "Insights",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GradientRing(
                    progress = state.consistencyIndex,
                    completed = state.doneCount,
                    total = state.targetCount.coerceAtLeast(1),
                    size = 200.dp
                )
            }
            RangeSelector(
                selectedRange = state.rangeDays,
                onSelected = onRangeSelected
            )

            if (state.isPremiumLocked) {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text("Premium analytics", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Unlock 30 and 90-day trends, full heatmaps, and expanded widget analytics.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            onClick = onOpenPaywall,
                            shape = MaterialTheme.shapes.medium,
                            color = HabitColors.SurfaceAccentGlow
                        ) {
                            Text(
                                text = "Upgrade to Premium",
                                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            SectionHeader(title = "Calendar heatmap")
            HeatmapGrid(
                cells = state.heatmap,
                selectedEpochDay = selectedEpochDay,
                onCellClick = { selectedEpochDay = it }
            )
            if (selectedEpochDay != 0L) {
                val cell = state.heatmap.firstOrNull { it.epochDay == selectedEpochDay }
                Text(
                    text = "Day score: ${((cell?.intensity ?: 0f) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = HabitColors.OnSurfaceMuted
                )
            }

            SectionHeader(title = "Weekly activity")
            WeeklyBars(bars = state.weeklyBars)

            SectionHeader(title = "Habit drilldown")
            Crossfade(targetState = state.drilldowns, label = "drilldownTransition") { list ->
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    list.take(6).forEach { item ->
                        AppCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.name, color = MaterialTheme.colorScheme.onBackground)
                                    Text(
                                        "${item.completionCount} done · ${item.missedDays} missed",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = HabitColors.OnSurfaceMuted
                                    )
                                }
                                Text("${(item.completionRate * 100).toInt()}%", color = HabitColors.SuccessGreen)
                            }
                        }
                    }
                }
            }

            SectionHeader(title = "Habit leaderboard")
            LeaderboardCard(state.leaderboard)
            Box(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun RangeSelector(
    selectedRange: Int,
    onSelected: (Int) -> Unit
) {
    val spacing = TrackerTheme.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
        listOf(7, 30, 90).forEach { range ->
            Surface(
                onClick = { onSelected(range) },
                shape = MaterialTheme.shapes.medium,
                color = if (selectedRange == range) HabitColors.SurfaceAccentGlow else HabitColors.Surface
            ) {
                Text(
                    text = "${range}d",
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun HeatmapGrid(
    cells: List<HeatmapCellUi>,
    selectedEpochDay: Long,
    onCellClick: (Long) -> Unit
) {
    val spacing = TrackerTheme.spacing
    val dayHeaders = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
    val cellByDay = cells.associateBy { it.epochDay }
    val firstDay = cells.minOfOrNull { it.epochDay } ?: 0L
    val lastDay = cells.maxOfOrNull { it.epochDay } ?: 0L
    val numRows = if (cells.isNotEmpty()) ((lastDay - firstDay) / 7 + 1).toInt() else 0

    fun colorForIntensity(intensity: Float): Color {
        return when {
            intensity <= 0f -> HabitColors.HeatmapPastelRed
            intensity <= 0.5f -> lerp(
                HabitColors.HeatmapPastelRed,
                HabitColors.HeatmapPastelYellow,
                intensity * 2f
            )
            else -> lerp(
                HabitColors.HeatmapPastelYellow,
                HabitColors.HeatmapPastelGreen,
                (intensity - 0.5f) * 2f
            )
        }
    }

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
        for (row in 0 until numRows) {
            val rowStartDay = firstDay + row * 7
            val gridCells = (0..6).map { col ->
                val epochDay = rowStartDay + col
                cellByDay[epochDay] to epochDay
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                for ((cell, epochDay) in gridCells) {
                    val dayNum = if (cell != null) java.time.LocalDate.ofEpochDay(epochDay).dayOfMonth else 0
                    val color = if (cell != null) colorForIntensity(cell.intensity) else HabitColors.SurfaceElevated
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                color = color,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .then(
                                if (cell != null) Modifier.clickable { onCellClick(epochDay) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell != null) {
                            Text(
                                text = dayNum.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyBars(bars: List<WeeklyBarUi>) {
    val spacing = TrackerTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            val heightRatio = bar.valuePercent.toFloat() / 100f
            val animatedHeight by animateFloatAsState(
                targetValue = heightRatio,
                animationSpec = tween(durationMillis = TrackerTheme.motion.medium),
                label = "barHeight"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(120.dp)
                        .background(HabitColors.SurfaceElevated, MaterialTheme.shapes.small)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(24.dp)
                            .height((120.dp * animatedHeight).coerceAtLeast(10.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        HabitColors.SecondaryViolet,
                                        HabitColors.PrimaryElectricBlue
                                    )
                                ),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
                Text(bar.label, style = MaterialTheme.typography.labelMedium, color = HabitColors.OnSurface)
                Text("${bar.valuePercent}%", style = MaterialTheme.typography.labelMedium, color = HabitColors.OnSurfaceMuted)
            }
        }
    }
}

@Composable
private fun LeaderboardCard(leaderboard: InsightsLeaderboardUi) {
    val spacing = TrackerTheme.spacing
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            LeaderItem(label = "Top streak", item = leaderboard.topStreak)
            LeaderItem(label = "Most consistent", item = leaderboard.mostConsistent)
            LeaderItem(label = "Most missed", item = leaderboard.mostMissed)
        }
    }
}

@Composable
private fun LeaderItem(label: String, item: LeaderboardItemUi?) {
    AnimatedContent(targetState = item, label = "leaderItem") { content ->
        if (content == null) {
            Text("$label: --", style = MaterialTheme.typography.labelMedium, color = HabitColors.OnSurfaceMuted)
        } else {
            Text(
                "$label: ${content.name} · ${content.metric}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
