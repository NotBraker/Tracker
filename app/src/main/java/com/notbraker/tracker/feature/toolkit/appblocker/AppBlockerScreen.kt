package com.notbraker.tracker.feature.toolkit.appblocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.components.SectionHeader
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

// Future: AccessibilityService can be used here to enforce app blocking.
// Placeholder only; no actual blocking or unsafe behavior.

@Composable
fun AppBlockerScreen() {
    val spacing = TrackerTheme.spacing
    var blockNotifications by remember { mutableStateOf(true) }
    var blockApps by remember { mutableStateOf(true) }
    val categories = listOf("Games", "Entertainment", "Social", "Other")
    val placeholderApps = listOf("Instagram", "YouTube", "TikTok", "X (Twitter)")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.BackgroundPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                "Focus Shield",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Block distracting apps and notifications during focus sessions. Requires Accessibility and Usage Stats (future implementation).",
                style = MaterialTheme.typography.bodyMedium,
                color = HabitColors.OnSurfaceMuted
            )

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Block notifications", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = blockNotifications, onCheckedChange = { blockNotifications = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Block apps", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = blockApps, onCheckedChange = { blockApps = it })
                    }
                }
            }

            SectionHeader(title = "Categories")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                for (cat in categories) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = HabitColors.Surface
                    ) {
                        Text(
                            cat,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            SectionHeader(title = "Apps (placeholder)")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    placeholderApps.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app, style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = false, onCheckedChange = { })
                        }
                    }
                }
            }

            Column(modifier = Modifier.navigationBarsPadding()) { Text("") }
        }
    }
}
