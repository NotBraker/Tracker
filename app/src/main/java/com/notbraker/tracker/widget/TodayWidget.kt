package com.notbraker.tracker.widget

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.notbraker.tracker.MainActivity
import com.notbraker.tracker.data.database.HabitDatabase
import com.notbraker.tracker.data.model.Habit
import com.notbraker.tracker.data.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import androidx.glance.text.Text

private val Context.billingDataStore by preferencesDataStore(name = "billing_state")
private val Context.debugBillingDataStore by preferencesDataStore(name = "billing_prefs")

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = HabitDatabase.getInstance(context).habitDao()
        val todayDate = LocalDate.now()
        val today = todayDate.toEpochDay()
        val habits = dao.getActiveHabits()
        val completedHabitIds = dao.getCompletedHabitIdsForDay(today).toSet()
        val total = habits.sumOf { habit -> habit.targetForDate(todayDate) }
        val completed = habits.count { habit -> habit.targetForDate(todayDate) > 0 && completedHabitIds.contains(habit.id) }
        val progress = if (total == 0) 0f else (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        val premiumFromState = context.billingDataStore.data.first()[booleanPreferencesKey("premium_enabled")] ?: false
        val premiumFromDebug = context.debugBillingDataStore.data.first()[booleanPreferencesKey("premium_enabled")] ?: false
        val isPremium = premiumFromState || premiumFromDebug
        val launchAppAction = actionStartActivity(Intent(context, MainActivity::class.java))

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(launchAppAction)
                    .padding(16.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(text = "Today")
                Spacer(GlanceModifier.height(8.dp))
                Text(text = "◉ ${(progress * 100).toInt()}%")
                Text(text = "$completed/$total completed")
                Spacer(GlanceModifier.height(8.dp))
                Text(text = "Tap to open app")
                if (isPremium) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(text = "Expanded premium mode")
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

class ProQuickDoneWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = HabitDatabase.getInstance(context).habitDao()
        val today = LocalDate.now().toEpochDay()
        val premiumFromState = context.billingDataStore.data.first()[booleanPreferencesKey("premium_enabled")] ?: false
        val premiumFromDebug = context.debugBillingDataStore.data.first()[booleanPreferencesKey("premium_enabled")] ?: false
        val isPremium = premiumFromState || premiumFromDebug
        val activeHabits = dao.getActiveHabits()
        val completedHabitIds = dao.getCompletedHabitIdsForDay(today).toSet()
        val pendingHabits = activeHabits
            .filter { it.targetForDate(LocalDate.now()) > 0 }
            .filterNot { completedHabitIds.contains(it.id) }
            .take(3)

        provideContent {
            if (!isPremium) {
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text("Pro Quick Mark")
                    Spacer(GlanceModifier.height(6.dp))
                    Text("Available in Tracker Pro")
                }
            } else {
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text("Quick Mark")
                    Spacer(GlanceModifier.height(8.dp))
                    pendingHabits.forEach { habit ->
                        Row(
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(habit.name)
                            Spacer(GlanceModifier.width(8.dp))
                            Text(
                                text = "Done",
                                modifier = GlanceModifier.clickable(
                                    actionRunCallback<MarkDoneActionCallback>(
                                        actionParametersOf(HABIT_ID_KEY to habit.id.toString())
                                    )
                                )
                            )
                        }
                        Spacer(GlanceModifier.height(6.dp))
                    }
                    if (pendingHabits.isEmpty()) {
                        Text("All habits completed")
                    }
                }
            }
        }
    }
}

class ProQuickDoneWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProQuickDoneWidget()
}

class MarkDoneActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[HABIT_ID_KEY]?.toLongOrNull() ?: return
        val repository = HabitRepository(HabitDatabase.getInstance(context).habitDao())
        repository.setHabitCompletion(habitId, LocalDate.now().toEpochDay(), true)
        TodayWidgetUpdater.update(context)
    }
}

private val HABIT_ID_KEY = ActionParameters.Key<String>("habit_id")

object TodayWidgetUpdater {
    suspend fun update(context: Context) {
        TodayWidget().updateAll(context)
        ProQuickDoneWidget().updateAll(context)
    }
}

private fun Habit.targetForDate(date: LocalDate): Int {
    val scheduledDays = if (scheduledDays.isBlank()) {
        emptySet()
    } else {
        scheduledDays.split(",")
            .mapNotNull { raw -> runCatching { DayOfWeek.valueOf(raw.trim().uppercase()) }.getOrNull() }
            .toSet()
    }
    val isScheduled = when (frequencyType.uppercase()) {
        "DAILY" -> scheduledDays.isEmpty() || scheduledDays.contains(date.dayOfWeek)
        "WEEKLY" -> scheduledDays.isEmpty() || scheduledDays.contains(date.dayOfWeek)
        else -> true
    }
    if (!isScheduled) return 0
    return (dailyTarget.coerceAtLeast(1) * timesPerDayTarget.coerceAtLeast(1)).coerceAtLeast(1)
}
