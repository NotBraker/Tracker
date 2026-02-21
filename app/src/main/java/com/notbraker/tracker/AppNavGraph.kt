package com.notbraker.tracker

import android.app.Activity
import android.content.pm.ApplicationInfo
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.data.model.HabitFrequencyType
import com.notbraker.tracker.feature.habitdetail.HabitDetailRoute
import com.notbraker.tracker.feature.habitdetail.HabitDetailViewModel
import com.notbraker.tracker.feature.habitdetail.HabitDetailViewModelFactory
import com.notbraker.tracker.feature.insights.InsightsRoute
import com.notbraker.tracker.feature.insights.InsightsViewModel
import com.notbraker.tracker.feature.insights.InsightsViewModelFactory
import com.notbraker.tracker.feature.settings.SettingsPlaceholderScreen
import com.notbraker.tracker.feature.settings.SubscriptionScreen
import com.notbraker.tracker.feature.templates.TemplatesScreen
import com.notbraker.tracker.feature.today.TodayRoute
import com.notbraker.tracker.feature.today.TodayViewModel
import com.notbraker.tracker.feature.today.TodayViewModelFactory
import com.notbraker.tracker.feature.toolkit.ToolkitScreen
import com.notbraker.tracker.feature.toolkit.appblocker.AppBlockerScreen
import com.notbraker.tracker.feature.toolkit.focustimer.FocusTimerRoute
import com.notbraker.tracker.feature.toolkit.routines.RoutineDetailRoute
import com.notbraker.tracker.feature.toolkit.routines.RoutineDetailViewModel
import com.notbraker.tracker.feature.toolkit.routines.RoutineDetailViewModelFactory
import com.notbraker.tracker.feature.toolkit.routines.RoutineSessionRoute
import com.notbraker.tracker.feature.toolkit.routines.RoutineSessionViewModel
import com.notbraker.tracker.feature.toolkit.routines.RoutineSessionViewModelFactory
import com.notbraker.tracker.feature.toolkit.routines.RoutinesRoute
import com.notbraker.tracker.feature.toolkit.routines.RoutinesViewModel
import com.notbraker.tracker.feature.toolkit.routines.RoutinesViewModelFactory
import com.notbraker.tracker.onboarding.setOnboardingCompleted
import com.notbraker.tracker.widget.TodayWidgetUpdater
import java.time.DayOfWeek
import kotlinx.coroutines.launch

private sealed class AppRoute(
    val route: String,
    val label: String? = null
) {
    data object Today : AppRoute("today", "Today")
    data object Insights : AppRoute("insights", "Insights")
    data object Templates : AppRoute("templates", "Templates")
    data object Toolkit : AppRoute("toolkit", "Toolkit")
    data object Settings : AppRoute("settings")
    data object Subscription : AppRoute("subscription")
    data object Routines : AppRoute("toolkit/routines")
    data object FocusTimer : AppRoute("toolkit/focusTimer")
    data object AppBlocker : AppRoute("toolkit/appBlocker")
    data object RoutineDetail : AppRoute("toolkit/routineDetail/{routineId}") {
        fun create(routineId: Long): String = "toolkit/routineDetail/$routineId"
    }
    data object RoutineSession : AppRoute("toolkit/routineSession/{routineId}") {
        fun create(routineId: Long): String = "toolkit/routineSession/$routineId"
    }
    data object HabitDetail : AppRoute("habitDetail/{habitId}") {
        fun create(habitId: Long): String = "habitDetail/$habitId"
    }
}

@Composable
fun AppNavGraph(
    appContainer: AppContainer,
    startAtSubscription: Boolean = false,
    onStartDestinationConsumed: () -> Unit = {},
    themeMode: String = "system",
    onThemeModeChanged: (String) -> Unit = {}
) {
    LaunchedEffect(Unit) {
        appContainer.billingManager.refreshEntitlements()
    }
    LaunchedEffect(startAtSubscription) {
        if (startAtSubscription) onStartDestinationConsumed()
    }
    val billingState by appContainer.billingManager.billingState.collectAsStateWithLifecycle()
    val isPremium = billingState.isPremium
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current
    val activity = context as? Activity
    val isDebuggable = (appContainer.appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val scope = rememberCoroutineScope()
    val refreshWidget: () -> Unit = {
        scope.launch { TodayWidgetUpdater.update(appContainer.appContext) }
    }
    val cancelReminderAndRefresh: (Long) -> Unit = { habitId ->
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

    val topLevelRoutes = listOf(
        AppRoute.Today,
        AppRoute.Insights,
        AppRoute.Toolkit
    )
    val showBottomBar = currentRoute in topLevelRoutes.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    topLevelRoutes.forEach { destination ->
                        val selected = currentRoute == destination.route
                        val icon = when (destination) {
                            AppRoute.Today -> Icons.Rounded.Today
                            AppRoute.Insights -> Icons.Rounded.AutoGraph
                            AppRoute.Templates -> Icons.Rounded.GridView
                            AppRoute.Toolkit -> Icons.Rounded.Build
                            else -> Icons.Rounded.Today
                        }
                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.08f else 1f,
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            label = "tabScale"
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(AppRoute.Today.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Column {
                                    Icon(imageVector = icon, contentDescription = destination.label, modifier = Modifier.scale(scale))
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(width = 18.dp, height = 2.dp)
                                            .background(
                                                if (selected) HabitColors.HighlightCyan else Color.Transparent,
                                                shape = MaterialTheme.shapes.small
                                            )
                                    )
                                }
                            },
                            label = { androidx.compose.material3.Text(destination.label.orEmpty()) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (startAtSubscription) AppRoute.Subscription.route else AppRoute.Today.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                    animationSpec = tween(260),
                    initialOffsetX = { it / 8 }
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                    animationSpec = tween(220),
                    targetOffsetX = { -it / 8 }
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                    animationSpec = tween(260),
                    initialOffsetX = { -it / 8 }
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                    animationSpec = tween(220),
                    targetOffsetX = { it / 8 }
                )
            }
        ) {
            composable(AppRoute.Today.route) {
                val todayVm: TodayViewModel = viewModel(
                    key = "today_vm",
                    factory = TodayViewModelFactory(
                        repository = appContainer.repository,
                        isPremiumProvider = { isPremium },
                        onHabitDeleted = cancelReminderAndRefresh,
                        onHabitArchived = cancelReminderAndRefresh,
                        onDataChanged = refreshWidget
                    )
                )
                TodayRoute(
                    viewModel = todayVm,
                    onHabitClick = { habitId ->
                        navController.navigate(AppRoute.HabitDetail.create(habitId))
                    },
                    onOpenTemplates = { navController.navigate(AppRoute.Templates.route) },
                    onRoutineClick = { routineId ->
                        navController.navigate(AppRoute.RoutineDetail.create(routineId))
                    }
                )
            }
            composable(AppRoute.Insights.route) {
                val insightsVm: InsightsViewModel = viewModel(
                    key = "insights_vm",
                    factory = InsightsViewModelFactory(
                        repository = appContainer.repository,
                        isPremiumProvider = { isPremium }
                    )
                )
                InsightsRoute(
                    viewModel = insightsVm,
                    onOpenPaywall = { navController.navigate(AppRoute.Subscription.route) }
                )
            }
            composable(AppRoute.Templates.route) {
                val usedTemplateIds by appContainer.repository.observeUsedTemplateIds().collectAsStateWithLifecycle(initialValue = emptySet())
                TemplatesScreen(
                    usedTemplateIds = usedTemplateIds,
                    onBack = { navController.popBackStack() },
                    onTemplateCreate = { template ->
                        scope.launch {
                            val isWeekly = template.tags.contains("WEEKLY")
                            val isTarget = template.tags.contains("TARGET")
                            val result = appContainer.repository.createHabit(
                                name = template.title,
                                description = template.description,
                                icon = template.icon,
                                frequencyLabel = if (isWeekly) "Weekly" else "Daily",
                                reminderHour = template.defaultReminderHour,
                                reminderMinute = template.defaultReminderMinute,
                                templateId = template.id,
                                isPremium = isPremium,
                                frequencyType = if (isWeekly) HabitFrequencyType.WEEKLY else HabitFrequencyType.DAILY,
                                weeklyTarget = if (isWeekly) 4 else 7,
                                dailyTarget = if (isTarget) 2 else 1,
                                scheduledDays = if (isWeekly) {
                                    setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                                } else {
                                    emptySet()
                                }
                            )
                            if (result.success && result.habitId != null) {
                                appContainer.reminderScheduler.scheduleDailyReminder(
                                    habitId = result.habitId,
                                    habitName = template.title,
                                    hour = template.defaultReminderHour,
                                    minute = template.defaultReminderMinute
                                )
                                refreshWidget()
                                navController.navigate(AppRoute.Today.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                )
            }
            composable(AppRoute.Toolkit.route) {
                ToolkitScreen(
                    onOpenToday = { navController.navigate(AppRoute.Today.route) },
                    onOpenRoutines = { navController.navigate(AppRoute.Routines.route) },
                    onOpenFocusTimer = { navController.navigate(AppRoute.FocusTimer.route) },
                    onOpenAppBlocker = { navController.navigate(AppRoute.AppBlocker.route) },
                    onOpenSettings = { navController.navigate(AppRoute.Settings.route) },
                    onOpenSubscription = { navController.navigate(AppRoute.Subscription.route) },
                    isPremium = isPremium
                )
            }
            composable(AppRoute.Settings.route) {
                SettingsPlaceholderScreen(
                    onClose = { navController.popBackStack() },
                    onResetApp = {
                        scope.launch {
                            appContainer.repository.resetAllData()
                            appContainer.billingManager.clearPremiumForReset()
                            context.applicationContext.setOnboardingCompleted(false)
                            refreshWidget()
                        }
                    }
                )
            }
            composable(AppRoute.Routines.route) {
                val routinesVm: RoutinesViewModel = viewModel(
                    key = "routines_vm",
                    factory = RoutinesViewModelFactory(appContainer.repository)
                )
                RoutinesRoute(
                    viewModel = routinesVm,
                    onOpenRoutine = { routineId ->
                        navController.navigate(AppRoute.RoutineDetail.create(routineId))
                    }
                )
            }
            composable(
                route = AppRoute.RoutineDetail.route,
                arguments = listOf(navArgument("routineId") { type = NavType.LongType })
            ) { entry ->
                val routineId = entry.arguments?.getLong("routineId") ?: return@composable
                val vm: RoutineDetailViewModel = viewModel(
                    key = "routine_detail_$routineId",
                    factory = RoutineDetailViewModelFactory(routineId, appContainer.repository)
                )
                RoutineDetailRoute(
                    viewModel = vm,
                    onStartSession = { id -> navController.navigate(AppRoute.RoutineSession.create(id)) }
                )
            }
            composable(
                route = AppRoute.RoutineSession.route,
                arguments = listOf(navArgument("routineId") { type = NavType.LongType })
            ) { entry ->
                val routineId = entry.arguments?.getLong("routineId") ?: return@composable
                val vm: RoutineSessionViewModel = viewModel(
                    key = "routine_session_$routineId",
                    factory = RoutineSessionViewModelFactory(routineId, appContainer.repository)
                )
                RoutineSessionRoute(viewModel = vm)
            }
            composable(AppRoute.FocusTimer.route) {
                FocusTimerRoute()
            }
            composable(AppRoute.AppBlocker.route) {
                AppBlockerScreen()
            }
            composable(
                route = AppRoute.HabitDetail.route,
                arguments = listOf(navArgument("habitId") { type = NavType.LongType })
            ) { entry ->
                val habitId = entry.arguments?.getLong("habitId") ?: return@composable
                val detailVm: HabitDetailViewModel = viewModel(
                    key = "habit_detail_$habitId",
                    factory = HabitDetailViewModelFactory(
                        habitId = habitId,
                        repository = appContainer.repository,
                        isPremiumProvider = { isPremium },
                        onHabitDeleted = cancelReminderAndRefresh,
                        onHabitArchived = cancelReminderAndRefresh,
                        onReminderUpdated = onReminderUpdated,
                        onDataChanged = refreshWidget
                    )
                )
                HabitDetailRoute(
                    viewModel = detailVm,
                    onClose = { navController.popBackStack() }
                )
            }
            composable(AppRoute.Subscription.route) {
                SubscriptionScreen(
                    billingStateFlow = appContainer.billingManager.billingState,
                    showDebugTools = isDebuggable,
                    onBack = { navController.popBackStack() },
                    onRestore = {
                        scope.launch { appContainer.billingManager.restorePurchases() }
                    },
                    onPurchase = { plan ->
                        if (activity != null) {
                            appContainer.billingManager.startPurchaseFlow(activity, plan)
                        }
                    },
                    onToggleDebugPremium = {
                        scope.launch {
                            appContainer.billingManager.setDebugPremiumOverride(!billingState.isPremium)
                        }
                    }
                )
            }
        }
    }
}
