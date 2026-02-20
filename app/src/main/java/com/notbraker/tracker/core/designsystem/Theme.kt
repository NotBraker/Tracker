package com.notbraker.tracker.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp
)

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }

private val DarkScheme = darkColorScheme(
    primary = HabitColors.PrimaryAccent,
    secondary = HabitColors.SecondaryAccent,
    tertiary = HabitColors.Success,
    background = HabitColors.Background,
    surface = HabitColors.Surface,
    surfaceContainer = HabitColors.SurfaceElevated,
    onPrimary = HabitColors.OnBackground,
    onSecondary = HabitColors.OnBackground,
    onTertiary = HabitColors.Background,
    onSurface = HabitColors.OnSurface,
    onBackground = HabitColors.OnBackground,
    error = HabitColors.Warning
)

@Composable
fun HabitTrackerTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppSpacing provides AppSpacing()) {
        MaterialTheme(
            colorScheme = DarkScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

object TrackerTheme {
    val spacing: AppSpacing
        @Composable get() = LocalAppSpacing.current
}
