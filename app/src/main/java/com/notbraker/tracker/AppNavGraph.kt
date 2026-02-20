package com.notbraker.tracker

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notbraker.tracker.feature.habitdetail.HabitDetailRoute
import com.notbraker.tracker.feature.habitdetail.HabitDetailViewModelFactory
import com.notbraker.tracker.feature.insights.InsightsRoute
import com.notbraker.tracker.feature.insights.InsightsViewModelFactory
import com.notbraker.tracker.feature.settings.PaywallScreen
import com.notbraker.tracker.feature.settings.SettingsScreen
import com.notbraker.tracker.feature.templates.TemplatesScreen
import com.notbraker.tracker.feature.today.TodayRoute
import com.notbraker.tracker.feature.today.TodayViewModelFactory
import com.notbraker.tracker.widget.TodayWidgetUpdater
import kotlinx.coroutines.launch

private enum class HomeTab(val label: String, val icon: String) {
    TODAY("Today", "●"),
    INSIGHTS("Insights", "▣"),
    TEMPLATES("Templates", "✦"),
    SETTINGS("Settings", "⚙")
}

@Composable
fun AppNavGraph(appContainer: AppContainer) {
    val isPremium by appContainer.billingManager.isPremium.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(HomeTab.TODAY) }
    var detailHabitId by remember { mutableLongStateOf(-1L) }
    var showPaywall by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val refreshWidget: () -> Unit = {
        scope.launch { TodayWidgetUpdater.update(appContainer.appContext) }
    }
    val onHabitDeleted: (Long) -> Unit = { habitId ->
        appContainer.reminderScheduler.cancelReminder(habitId)
        refreshWidget()
    }
    val onHabitArchived: (Long) -> Unit = { habitId ->
        appContainer.reminderScheduler.cancelReminder(habitId)
        refreshWidget()
    }
    val onReminderUpdated: (Long, String, Boolean, Int?, Int?) -> Unit = { habitId, habitName, enabled, hour, minute ->
        if (enabled && hour != null && minute != null) {
            appContainer.reminderScheduler.scheduleDailyReminder(habitId, habitName, hour, minute)
        } else {
            appContainer.reminderScheduler.cancelReminder(habitId)
        }
        refreshWidget()
    }

    if (showPaywall) {
        PaywallScreen(
            isPremium = isPremium,
            onRestore = {
                scope.launch { appContainer.billingManager.restorePurchases() }
            },
            onContinue = {
                scope.launch { appContainer.billingManager.startPurchaseFlow() }
            },
            onBack = { showPaywall = false }
        )
        return
    }

    if (detailHabitId > 0L) {
        val detailVm: com.notbraker.tracker.feature.habitdetail.HabitDetailViewModel = viewModel(
            key = "habit_detail_$detailHabitId",
            factory = HabitDetailViewModelFactory(
                habitId = detailHabitId,
                repository = appContainer.repository,
                isPremiumProvider = { isPremium },
                onHabitDeleted = onHabitDeleted,
                onHabitArchived = onHabitArchived,
                onReminderUpdated = onReminderUpdated,
                onDataChanged = refreshWidget
            )
        )
        HabitDetailRoute(
            viewModel = detailVm,
            onClose = { detailHabitId = -1L }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Crossfade(
            targetState = activeTab,
            modifier = Modifier.padding(paddingValues),
            label = "mainTabCrossfade"
        ) { tab ->
            when (tab) {
                HomeTab.TODAY -> {
                    val todayVm: com.notbraker.tracker.feature.today.TodayViewModel = viewModel(
                        key = "today_vm",
                        factory = TodayViewModelFactory(
                            repository = appContainer.repository,
                            isPremiumProvider = { isPremium },
                            onHabitDeleted = onHabitDeleted,
                            onHabitArchived = onHabitArchived,
                            onDataChanged = refreshWidget
                        )
                    )
                    TodayRoute(
                        viewModel = todayVm,
                        onHabitClick = { habitId -> detailHabitId = habitId }
                    )
                }

                HomeTab.INSIGHTS -> {
                    val insightsVm: com.notbraker.tracker.feature.insights.InsightsViewModel = viewModel(
                        key = "insights_vm",
                        factory = InsightsViewModelFactory(
                            repository = appContainer.repository,
                            isPremiumProvider = { isPremium }
                        )
                    )
                    InsightsRoute(
                        viewModel = insightsVm,
                        onOpenPaywall = { showPaywall = true }
                    )
                }

                HomeTab.TEMPLATES -> {
                    TemplatesScreen(
                        onTemplateCreate = { template ->
                            scope.launch {
                                val result = appContainer.repository.createHabit(
                                    name = template.title,
                                    description = template.description,
                                    icon = template.icon,
                                    frequencyLabel = "Daily",
                                    reminderHour = template.defaultReminderHour,
                                    reminderMinute = template.defaultReminderMinute,
                                    templateId = template.id,
                                    isPremium = isPremium
                                )
                                if (result.success && result.habitId != null) {
                                    appContainer.reminderScheduler.scheduleDailyReminder(
                                        habitId = result.habitId,
                                        habitName = template.title,
                                        hour = template.defaultReminderHour,
                                        minute = template.defaultReminderMinute
                                    )
                                    refreshWidget()
                                }
                            }
                        }
                    )
                }

                HomeTab.SETTINGS -> {
                    SettingsScreen(
                        isPremium = isPremium,
                        onPremiumToggle = { enabled ->
                            scope.launch {
                                appContainer.billingManager.setPremiumOverride(enabled)
                                refreshWidget()
                            }
                        },
                        onOpenPaywall = { showPaywall = true }
                    )
                }
            }
        }
    }
}
