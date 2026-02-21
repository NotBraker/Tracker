package com.notbraker.tracker.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

private data class OnboardingPage(
    val headline: String,
    val description: String
)

private val pages = listOf(
    OnboardingPage(
        headline = "Add habits in seconds",
        description = "Create habits and check them off each day. Build precision streaks with minimal friction."
    ),
    OnboardingPage(
        headline = "Routines",
        description = "Bundle habits into routines and run them in one go. Perfect for morning or evening blocks."
    ),
    OnboardingPage(
        headline = "Focus & block",
        description = "Use the focus timer and app blocking tools to stay in the zone when it matters."
    ),
    OnboardingPage(
        headline = "See your progress",
        description = "Consistency index, heatmaps, and weekly bars show how you're really doing."
    ),
    OnboardingPage(
        headline = "Start with templates",
        description = "Pick from curated templates or build from scratch. You're in control."
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val page = pages[currentPage]
    val spacing = TrackerTheme.spacing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HabitColors.BackgroundPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(32.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    HabitColors.SurfaceAccentGlow.copy(alpha = 0.5f),
                                    HabitColors.PrimaryElectricBlue.copy(alpha = 0.2f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${currentPage + 1}",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(spacing.xl))
                Text(
                    text = page.headline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(spacing.sm))
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = HabitColors.OnSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = spacing.md)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                for (index in pages.indices) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                color = if (index == currentPage) HabitColors.HighlightCyan else HabitColors.Surface,
                                shape = CircleShape
                            )
                    )
                }
            }
            Surface(
                onClick = {
                    if (currentPage < pages.size - 1) {
                        currentPage++
                    } else {
                        onFinish()
                    }
                },
                shape = MaterialTheme.shapes.medium,
                color = HabitColors.SurfaceAccentGlow
            ) {
                Text(
                    text = if (currentPage < pages.size - 1) "Next" else "Finish",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.md)
                )
            }
        }
    }
}
