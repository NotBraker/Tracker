package com.notbraker.tracker.core.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.notbraker.tracker.core.designsystem.HabitColors

@Composable
fun AnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.88f,
        animationSpec = spring(stiffness = 360f),
        label = "checkboxScale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (checked) HabitColors.SuccessGreen else Color.Transparent,
        label = "checkboxBackground"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) HabitColors.SuccessGreen else HabitColors.OutlineSubtle,
        label = "checkboxBorder"
    )
    Box(
        modifier = modifier
            .size(28.dp)
            .scale(animatedScale)
            .border(2.dp, borderColor, shape = MaterialTheme.shapes.small)
            .background(
                brush = if (checked) {
                    Brush.linearGradient(
                        listOf(
                            HabitColors.SuccessGreen.copy(alpha = 0.95f),
                            HabitColors.HighlightCyan.copy(alpha = 0.7f)
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(containerColor, containerColor))
                },
                shape = MaterialTheme.shapes.small
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Text(
                text = "✓",
                color = HabitColors.BackgroundPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
