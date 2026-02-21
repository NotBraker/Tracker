package com.notbraker.tracker.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.notbraker.tracker.core.designsystem.HabitColors

@Composable
fun PulseLine(
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth(0.35f)
            .height(3.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        HabitColors.PrimaryElectricBlue,
                        HabitColors.HighlightCyan
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
    )
}
