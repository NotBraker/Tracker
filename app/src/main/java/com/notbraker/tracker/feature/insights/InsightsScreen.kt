package com.notbraker.tracker.feature.insights

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

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
                            color = HabitColors.PrimaryAccent.copy(alpha = 0.25f)
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

            GradientRing(
                progress = state.weeklyCompletionPercent,
                completed = (state.weeklyCompletionPercent * 100).toInt(),
                total = 100,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                size = 200.dp
            )

            SectionHeader(title = "Calendar heatmap")
            HeatmapGrid(cells = state.heatmap)

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
                                Text("${item.icon} ${item.name}", color = MaterialTheme.colorScheme.onBackground)
                                Text(
                                    "${(item.completionRate * 100).toInt()}%",
                                    color = HabitColors.Success
                                )
                            }
                        }
                    }
                }
            }
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
                color = if (selectedRange == range) HabitColors.PrimaryAccent.copy(alpha = 0.25f) else HabitColors.Surface
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
private fun HeatmapGrid(cells: List<HeatmapCellUi>) {
    val spacing = TrackerTheme.spacing
    val rows = cells.chunked(7)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                row.forEach { cell ->
                    val alpha = 0.18f + (cell.intensity * 0.82f)
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(HabitColors.PrimaryAccent.copy(alpha = alpha), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyBars(bars: List<WeeklyBarUi>) {
    val spacing = TrackerTheme.spacing
    val maxValue = (bars.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            val heightRatio = bar.value.toFloat() / maxValue.toFloat()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height((120.dp * heightRatio).coerceAtLeast(12.dp))
                        .background(HabitColors.SecondaryAccent.copy(alpha = 0.8f), MaterialTheme.shapes.small)
                )
                Text(bar.label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}
