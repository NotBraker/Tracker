package com.notbraker.tracker.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.notbraker.tracker.billing.BillingUiState
import kotlinx.coroutines.flow.StateFlow
import com.notbraker.tracker.billing.SubscriptionPlan
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

@Composable
fun SubscriptionScreen(
    billingStateFlow: StateFlow<BillingUiState>,
    showDebugTools: Boolean,
    onBack: () -> Unit,
    onRestore: () -> Unit,
    onPurchase: (SubscriptionPlan) -> Unit,
    onToggleDebugPremium: () -> Unit
) {
    val billingState by billingStateFlow.collectAsStateWithLifecycle()
    val spacing = TrackerTheme.spacing
    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.YEARLY) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.BackgroundPrimary,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HabitColors.BackgroundSecondary)
                    .navigationBarsPadding()
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    onClick = onBack,
                    shape = MaterialTheme.shapes.medium,
                    color = HabitColors.Surface
                ) {
                    Text(
                        "Continue free with ads",
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.sm),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    onClick = { onPurchase(selectedPlan) },
                    shape = MaterialTheme.shapes.medium,
                    color = HabitColors.SurfaceAccentGlow
                ) {
                    Text(
                        if (billingState.isPremium) "Premium Active" else "Start free trial",
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.sm),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    HabitColors.PrimaryElectricBlue.copy(alpha = 0.55f),
                                    HabitColors.SecondaryViolet.copy(alpha = 0.45f),
                                    HabitColors.BackgroundPrimary
                                )
                            )
                        )
                        .padding(horizontal = spacing.lg, vertical = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    Text("Upgrade to Tracker Pro", style = MaterialTheme.typography.displayLarge)
                    Text(
                        "7-day free trial, then choose monthly or yearly.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = HabitColors.OnSurface
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            Text("Feature matrix", style = MaterialTheme.typography.titleLarge)
                            FeatureRow("Unlimited habits", pro = true)
                            FeatureRow("Unlimited reminders", pro = true)
                            FeatureRow("90-day insights", pro = true)
                            FeatureRow("Pro quick widget", pro = true)
                            FeatureRow("Ad-free experience", pro = true)
                        }
                    }
                    SubscriptionPlanCard(
                        title = "Monthly",
                        subtitle = "7 days free, then \$3.99/mo",
                        selected = selectedPlan == SubscriptionPlan.MONTHLY,
                        onClick = { selectedPlan = SubscriptionPlan.MONTHLY }
                    )
                    SubscriptionPlanCard(
                        title = "Yearly",
                        subtitle = "\$24.99/year · Best value",
                        selected = selectedPlan == SubscriptionPlan.YEARLY,
                        onClick = { selectedPlan = SubscriptionPlan.YEARLY }
                    )
                    Surface(
                        onClick = onRestore,
                        shape = MaterialTheme.shapes.medium,
                        color = HabitColors.Surface
                    ) {
                        Text(
                            "Restore purchases",
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
                        )
                    }
                }
            }

            if (showDebugTools) {
                item {
                    TextButton(onClick = onToggleDebugPremium) {
                        Text(
                            text = "Developer Toggle Premium",
                            color = HabitColors.HighlightCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(label: String, pro: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(if (pro) "Pro" else "Free", style = MaterialTheme.typography.labelMedium, color = HabitColors.OnSurfaceMuted)
    }
}

@Composable
private fun SubscriptionPlanCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) HabitColors.SurfaceAccentGlow else HabitColors.Surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) TrackerTheme.spacing.xxs else 1.dp,
            color = if (selected) HabitColors.HighlightCyan else HabitColors.OutlineSubtle
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TrackerTheme.spacing.md,
                    vertical = TrackerTheme.spacing.sm
                )
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.labelMedium)
        }
    }
}
