package com.notbraker.tracker.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

@Composable
fun AdPlaceholderCard() {
    val spacing = TrackerTheme.spacing
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text("Sponsored", style = MaterialTheme.typography.labelMedium, color = HabitColors.OnSurfaceMuted)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .background(HabitColors.Surface, shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("Ad slot: Upgrade to Tracker Pro to remove ads", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
