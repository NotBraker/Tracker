package com.notbraker.tracker

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.notbraker.tracker.billing.BillingManager
import com.notbraker.tracker.billing.FakeBillingManager
import com.notbraker.tracker.core.designsystem.HabitTrackerTheme
import com.notbraker.tracker.data.database.HabitDatabase
import com.notbraker.tracker.data.repository.HabitRepository
import com.notbraker.tracker.reminder.ReminderScheduler

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
    HabitTrackerTheme {
        CompositionLocalProvider(LocalAppContainer provides appContainer) {
            AppNavGraph(appContainer = appContainer)
        }
    }
}
