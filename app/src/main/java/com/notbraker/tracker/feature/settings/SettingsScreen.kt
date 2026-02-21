package com.notbraker.tracker.feature.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.components.SectionHeader
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

data class SettingsUiState(
    val isPremium: Boolean = false,
    val appVersion: String = "1.0.0",
    val reminderEnabled: Boolean = true,
    val defaultReminderTime: String = "20:00",
    val selectedAccent: String = "Electric Blue",
    val themeMode: String = "system"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenSubscription: () -> Unit,
    onRestorePurchase: () -> Unit,
    onToggleReminderMaster: () -> Unit,
    onSelectAccent: (String) -> Unit,
    onThemeModeChanged: (String) -> Unit = {},
    onExportCsv: () -> Unit,
    onResetData: () -> Unit
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
            contentPadding = PaddingValues(bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                AppCard {
                    SectionHeader(title = "Account", subtitle = if (state.isPremium) "Tracker Pro Active" else "Free Plan")
                    SettingsActionRow("Subscription status", if (state.isPremium) "Active" else "Inactive")
                    SettingsButtonRow("Manage subscription", onOpenSubscription)
                    SettingsButtonRow("Restore purchase", onRestorePurchase)
                }
            }

            item {
                AppCard {
                    SectionHeader(title = "Appearance")
                    SettingsActionRow("Theme", when (state.themeMode) {
                        "light" -> "Light"
                        "dark" -> "Dark"
                        else -> "System"
                    })
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (mode, label) ->
                            Surface(
                                onClick = { onThemeModeChanged(mode) },
                                shape = MaterialTheme.shapes.medium,
                                color = if (state.themeMode == mode) HabitColors.SurfaceAccentGlow else HabitColors.Surface
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    SettingsActionRow("Accent", state.selectedAccent)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        listOf("Electric Blue", "Violet", "Cyan").forEach { accent ->
                            Surface(
                                onClick = { onSelectAccent(accent) },
                                shape = MaterialTheme.shapes.medium,
                                color = if (state.selectedAccent == accent) HabitColors.SurfaceAccentGlow else HabitColors.Surface
                            ) {
                                Text(
                                    accent,
                                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item {
                AppCard {
                    SectionHeader(title = "Notifications")
                    SettingsActionRow("Master reminder", if (state.reminderEnabled) "Enabled" else "Disabled")
                    SettingsButtonRow(
                        label = if (state.reminderEnabled) "Disable reminders" else "Enable reminders",
                        onClick = onToggleReminderMaster
                    )
                    SettingsActionRow("Default reminder time", state.defaultReminderTime)
                }
            }

            item {
                AppCard {
                    SectionHeader(title = "Data")
                    SettingsButtonRow("Export CSV", onExportCsv)
                    SettingsButtonRow("Reset data", onResetData)
                    SettingsActionRow("Backup", "Coming soon")
                }
            }

            item {
                AppCard {
                    SectionHeader(title = "About")
                    SettingsActionRow("Version", state.appVersion)
                    SettingsActionRow("Privacy", "View policy")
                    SettingsActionRow("Terms", "View terms")
                }
            }
            item {
                Row(modifier = Modifier.navigationBarsPadding()) {
                    Text("")
                }
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TrackerTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = HabitColors.OnSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsButtonRow(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = HabitColors.Surface
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = TrackerTheme.spacing.md,
                vertical = TrackerTheme.spacing.sm
            ),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
