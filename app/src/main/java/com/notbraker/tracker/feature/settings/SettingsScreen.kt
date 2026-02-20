package com.notbraker.tracker.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.components.SectionHeader
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

@Composable
fun SettingsScreen(
    isPremium: Boolean,
    onPremiumToggle: (Boolean) -> Unit,
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
                text = "Settings",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Premium access", color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            if (isPremium) "Unlimited habits, reminders, insights, widgets" else "Free plan",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(checked = isPremium, onCheckedChange = onPremiumToggle)
                }
            }
            AppCard {
                SectionHeader(
                    title = "Upgrade",
                    subtitle = "Unlock full analytics, unlimited reminders, and expanded widgets.",
                    actionLabel = "View Paywall",
                    onActionClick = onOpenPaywall
                )
            }
        }
    }
}

@Composable
fun PaywallScreen(
    isPremium: Boolean,
    onRestore: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val spacing = TrackerTheme.spacing
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                HabitColors.PrimaryAccent.copy(alpha = 0.5f),
                                HabitColors.SecondaryAccent.copy(alpha = 0.4f),
                                HabitColors.Background
                            )
                        )
                    )
                    .padding(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Tracker Premium", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
                Text("Build unstoppable momentum with full control.", style = MaterialTheme.typography.bodyLarge)
            }

            Column(
                modifier = Modifier.padding(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text("What you get", style = MaterialTheme.typography.titleLarge)
                        Text("• Unlimited habits")
                        Text("• Unlimited reminders")
                        Text("• Advanced 30/90 day insights")
                        Text("• Premium widgets")
                        Text("• No ads")
                    }
                }
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text("Price", style = MaterialTheme.typography.titleLarge)
                        Text("$4.99 / month (placeholder)", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Surface(
                        onClick = onRestore,
                        shape = MaterialTheme.shapes.medium,
                        color = HabitColors.SurfaceElevated
                    ) {
                        Text("Restore", modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm))
                    }
                    Surface(
                        onClick = onContinue,
                        shape = MaterialTheme.shapes.medium,
                        color = HabitColors.PrimaryAccent.copy(alpha = 0.25f)
                    ) {
                        Text(
                            if (isPremium) "Premium Active" else "Continue",
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}
