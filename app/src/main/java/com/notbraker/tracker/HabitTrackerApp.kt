package com.notbraker.tracker

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notbraker.tracker.billing.BillingManager
import com.notbraker.tracker.billing.FakeBillingManager
import com.notbraker.tracker.core.designsystem.HabitTrackerTheme
import com.notbraker.tracker.data.database.HabitDatabase
import com.notbraker.tracker.data.repository.HabitRepository
import com.notbraker.tracker.feature.onboarding.OnboardingScreen
import com.notbraker.tracker.onboarding.getOnboardingCompletedFlow
import com.notbraker.tracker.onboarding.setOnboardingCompleted
import com.notbraker.tracker.onboarding.getThemeModeFlow
import com.notbraker.tracker.onboarding.setThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import com.notbraker.tracker.reminder.ReminderScheduler
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val repository: HabitRepository = HabitRepository(HabitDatabase.getInstance(appContext).habitDao())
    val billingManager: BillingManager = FakeBillingManager(appContext)
    val reminderScheduler: ReminderScheduler = ReminderScheduler(appContext)
}

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer was not provided")
}

@Composable
fun HabitTrackerApp() {
    val appContext = LocalContext.current.applicationContext
    val appContainer = remember(appContext) { AppContainer(appContext) }
    val onboardingCompleted by appContext.getOnboardingCompletedFlow().collectAsStateWithLifecycle(initialValue = false)
    val themeMode by appContext.getThemeModeFlow().collectAsStateWithLifecycle(initialValue = "system")
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
    var startAtSubscription by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    HabitTrackerTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalAppContainer provides appContainer) {
            if (!onboardingCompleted && !startAtSubscription) {
                OnboardingScreen(
                    onFinish = {
                        scope.launch {
                            appContext.setOnboardingCompleted(true)
                            startAtSubscription = true
                        }
                    }
                )
            } else {
                AppNavGraph(
                    appContainer = appContainer,
                    startAtSubscription = startAtSubscription,
                    onStartDestinationConsumed = { startAtSubscription = false },
                    themeMode = themeMode,
                    onThemeModeChanged = { mode ->
                        scope.launch { appContext.setThemeMode(mode) }
                    }
                )
            }
        }
    }
}
