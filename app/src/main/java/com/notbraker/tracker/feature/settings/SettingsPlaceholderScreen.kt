package com.notbraker.tracker.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

@Composable
fun SettingsPlaceholderScreen(
    onClose: () -> Unit,
    onResetApp: () -> Unit
) {
    val spacing = TrackerTheme.spacing
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
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.padding(spacing.sm)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                onClick = onResetApp,
                shape = MaterialTheme.shapes.medium,
                color = HabitColors.Surface
            ) {
                Text(
                    text = "Reset app (temporary)",
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
