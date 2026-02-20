package com.notbraker.tracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.notbraker.tracker.data.model.Habit
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDailyReminder(
        habitId: Long,
        habitName: String,
        hour: Int,
        minute: Int
    ) {
        val pendingIntent = createReminderPendingIntent(habitId, habitName, hour, minute)
        alarmManager.cancel(pendingIntent)
        val triggerAtMillis = nextTriggerMillis(hour, minute)
        if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminder(habitId: Long) {
        val pendingIntent = createReminderPendingIntent(
            habitId = habitId,
            habitName = "",
            hour = 0,
            minute = 0
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun rescheduleAll(habits: List<Habit>) {
        habits.forEach { habit ->
            val hour = habit.reminderHour
            val minute = habit.reminderMinute
            if (habit.reminderEnabled && hour != null && minute != null) {
                scheduleDailyReminder(habit.id, habit.name, hour, minute)
            }
        }
    }

    private fun createReminderPendingIntent(
        habitId: Long,
        habitName: String,
        hour: Int,
        minute: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderReceiver.EXTRA_HABIT_NAME, habitName)
            putExtra(ReminderReceiver.EXTRA_HOUR, hour)
            putExtra(ReminderReceiver.EXTRA_MINUTE, minute)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeForHabit(habitId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val zoneId = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zoneId)
        var trigger = now
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!trigger.isAfter(now)) {
            trigger = trigger.plusDays(1)
        }
        return trigger.toInstant().toEpochMilli()
    }

    private fun requestCodeForHabit(habitId: Long): Int {
        return (habitId % Int.MAX_VALUE).toInt()
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
