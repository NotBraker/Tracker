package com.notbraker.tracker.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    val mdPlus: Dp = 20.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp
)

data class AppMotion(
    val short: Int = 220,
    val medium: Int = 420,
    val long: Int = 640
)

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
val LocalAppMotion = staticCompositionLocalOf { AppMotion() }

private val DarkScheme = darkColorScheme(
    primary = HabitColors.PrimaryElectricBlue,
    secondary = HabitColors.SecondaryViolet,
    tertiary = HabitColors.HighlightCyan,
    background = HabitColors.BackgroundPrimary,
    surface = HabitColors.Surface,
    surfaceContainer = HabitColors.SurfaceElevated,
    onPrimary = HabitColors.OnPrimary,
    onSecondary = HabitColors.OnPrimary,
    onTertiary = HabitColors.BackgroundPrimary,
    onSurface = HabitColors.OnSurface,
    onBackground = HabitColors.OnPrimary,
    error = HabitColors.WarningOrange
)

private val LightScheme = lightColorScheme(
    primary = HabitColors.PrimaryElectricBlue,
    secondary = HabitColors.SecondaryViolet,
    tertiary = HabitColors.HighlightCyan,
    background = HabitColors.OnPrimary,
    surface = HabitColors.OnPrimary,
    onPrimary = HabitColors.OnPrimary,
    onSecondary = HabitColors.OnPrimary,
    onBackground = HabitColors.BackgroundPrimary,
    onSurface = HabitColors.BackgroundPrimary,
    error = HabitColors.WarningOrange
)

@Composable
fun HabitTrackerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppSpacing provides AppSpacing(),
        LocalAppMotion provides AppMotion()
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

object TrackerTheme {
    val spacing: AppSpacing
        @Composable get() = LocalAppSpacing.current

    val motion: AppMotion
        @Composable get() = LocalAppMotion.current
}
