package com.notbraker.tracker.widget

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import com.notbraker.tracker.MainActivity
import com.notbraker.tracker.data.database.HabitDatabase
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import androidx.glance.text.Text

private val Context.billingDataStore by preferencesDataStore(name = "billing_prefs")

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = HabitDatabase.getInstance(context).habitDao()
        val today = LocalDate.now().toEpochDay()
        val total = dao.countActiveHabits()
        val completed = dao.getCompletedCountForDay(today)
        val progress = if (total == 0) 0f else (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        val isPremium = context.billingDataStore.data.first()[booleanPreferencesKey("premium_enabled")] ?: false
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
                Text(text = "◍ ${(progress * 100).toInt()}%")
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

object TodayWidgetUpdater {
    suspend fun update(context: Context) {
        TodayWidget().updateAll(context)
    }
}
