package com.notbraker.tracker.core.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

@Composable
fun GradientRing(
    progress: Float,
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = TrackerTheme.motion.medium, easing = FastOutSlowInEasing),
        label = "ringProgress"
    )
    val animatedCompleted by animateIntAsState(
        targetValue = completed.coerceAtLeast(0),
        animationSpec = tween(durationMillis = TrackerTheme.motion.medium, easing = FastOutSlowInEasing),
        label = "completedCounter"
    )
    val strokeWidth = 18.dp
    val ringDiameter = size
    Box(
        modifier = modifier
            .size(ringDiameter)
            .background(HabitColors.Surface.copy(alpha = 0.2f), shape = MaterialTheme.shapes.extraLarge),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(ringDiameter)) {
            val ringSize = Size(this.size.width, this.size.height)
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = HabitColors.HighlightCyan.copy(alpha = 0.16f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = (strokeWidth + 12.dp).toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = HabitColors.RingTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        HabitColors.PrimaryElectricBlue,
                        HabitColors.SecondaryViolet,
                        HabitColors.HighlightCyan,
                        HabitColors.PrimaryElectricBlue
                    ),
                    center = Offset(ringSize.width / 2f, ringSize.height / 2f)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = stroke
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.18f),
                radius = ringDiameter.toPx() * 0.28f
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$animatedCompleted",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "habits done",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (total == 1) "1 habit total" else "$total habits total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
