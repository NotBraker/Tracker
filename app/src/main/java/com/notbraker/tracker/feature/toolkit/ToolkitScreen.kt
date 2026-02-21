package com.notbraker.tracker.feature.toolkit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notbraker.tracker.core.components.AdPlaceholderCard
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.components.SectionHeader
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

data class ToolkitModuleUi(
    val title: String,
    val subtitle: String,
    val icon: String,
    val isSoon: Boolean = false
)

@Composable
fun ToolkitScreen(
    onOpenToday: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenFocusTimer: () -> Unit,
    onOpenAppBlocker: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSubscription: () -> Unit,
    isPremium: Boolean
) {
    val spacing = TrackerTheme.spacing
    val modules = listOf(
        ToolkitModuleUi("Settings", "Account, appearance, notifications, data", "⚙"),
        ToolkitModuleUi("Habits", "Manage your active list and schedule order", "H"),
        ToolkitModuleUi("Routines", "Group habits into repeatable sessions", "R"),
        ToolkitModuleUi("Focus Timer", "Pomodoro presets and deep-focus tracking", "F"),
        ToolkitModuleUi("App Blocker", "Focus Shield and app limits", "S", isSoon = true),
        ToolkitModuleUi(
            if (isPremium) "Manage Subscription" else "Upgrade to Premium",
            if (isPremium) "View or change your plan" else "Unlock unlimited habits and insights",
            "★"
        )
    )
    val actions = listOf(
        onOpenSettings,
        onOpenToday,
        onOpenRoutines,
        onOpenFocusTimer,
        onOpenAppBlocker,
        onOpenSubscription
    )
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.BackgroundPrimary
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
            if (!isPremium) {
                AdPlaceholderCard()
            }
            Text(
                text = "Toolkit",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            SectionHeader(title = "Performance Modules", subtitle = "Precision tools for execution")
            modules.forEachIndexed { index, module ->
                ModuleCard(
                    module = module,
                    onClick = actions[index]
                )
            }
            Row(modifier = Modifier.navigationBarsPadding()) { Text("") }
        }
    }
}

@Composable
private fun ModuleCard(module: ToolkitModuleUi, onClick: () -> Unit) {
    val spacing = TrackerTheme.spacing
    AppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            onClick = onClick,
            color = androidx.compose.ui.graphics.Color.Transparent,
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = HabitColors.SurfaceAccentGlow
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text(module.icon, modifier = Modifier.padding(TrackerTheme.spacing.sm))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        Text(module.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (module.isSoon) {
                            Text(
                                "Soon",
                                style = MaterialTheme.typography.labelMedium,
                                color = HabitColors.WarningOrange,
                                modifier = Modifier
                                    .background(HabitColors.Surface, shape = MaterialTheme.shapes.small)
                                    .padding(horizontal = spacing.xs, vertical = spacing.xxs)
                            )
                        }
                    }
                    Text(
                        module.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = HabitColors.OnSurfaceMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("›", style = MaterialTheme.typography.titleLarge, color = HabitColors.OnSurfaceMuted)
            }
        }
    }
}
