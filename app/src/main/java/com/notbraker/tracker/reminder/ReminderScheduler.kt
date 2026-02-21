package com.notbraker.tracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
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
        val pendingIntent = createReminderPendingIntent(
            habitId = habitId,
            habitName = habitName,
            hour = hour,
            minute = minute,
            isSnooze = false,
            triggerAtMillis = null
        )
        alarmManager.cancel(pendingIntent)
        val triggerAtMillis = nextTriggerMillis(hour, minute)
        scheduleAt(triggerAtMillis = triggerAtMillis, pendingIntent = pendingIntent)
    }

    fun scheduleSnoozeReminder(
        habitId: Long,
        habitName: String,
        hour: Int,
        minute: Int,
        snoozeMinutes: Int = 10
    ) {
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes.coerceIn(1, 120) * 60_000L)
        val pendingIntent = createReminderPendingIntent(
            habitId = habitId,
            habitName = habitName,
            hour = hour,
            minute = minute,
            isSnooze = true,
            triggerAtMillis = triggerAtMillis
        )
        alarmManager.cancel(pendingIntent)
        scheduleAt(triggerAtMillis = triggerAtMillis, pendingIntent = pendingIntent)
    }

    fun cancelReminder(habitId: Long) {
        val dailyPendingIntent = createReminderPendingIntent(
            habitId = habitId,
            habitName = "",
            hour = 0,
            minute = 0,
            isSnooze = false,
            triggerAtMillis = null
        )
        alarmManager.cancel(dailyPendingIntent)
        dailyPendingIntent.cancel()
        val snoozePendingIntent = createReminderPendingIntent(
            habitId = habitId,
            habitName = "",
            hour = 0,
            minute = 0,
            isSnooze = true,
            triggerAtMillis = null
        )
        alarmManager.cancel(snoozePendingIntent)
        snoozePendingIntent.cancel()
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
        minute: Int,
        isSnooze: Boolean,
        triggerAtMillis: Long?
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            data = if (isSnooze) {
                Uri.parse("tracker://reminder/snooze/$habitId")
            } else {
                Uri.parse("tracker://reminder/daily/$habitId")
            }
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderReceiver.EXTRA_HABIT_NAME, habitName)
            putExtra(ReminderReceiver.EXTRA_HOUR, hour)
            putExtra(ReminderReceiver.EXTRA_MINUTE, minute)
            putExtra(ReminderReceiver.EXTRA_IS_SNOOZE, isSnooze)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeForHabit(habitId, if (isSnooze) 2 else 1),
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

    private fun requestCodeForHabit(habitId: Long, salt: Int): Int =
        (((habitId xor (habitId ushr 32)).toInt()) * 31 + salt)

    private fun scheduleAt(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return
            } catch (_: SecurityException) {
                // Continue with inexact fallback when exact alarms are not permitted.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
