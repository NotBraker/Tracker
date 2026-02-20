package com.notbraker.tracker.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.notbraker.tracker.MainActivity
import com.notbraker.tracker.R
import com.notbraker.tracker.data.database.HabitDatabase
import com.notbraker.tracker.data.repository.HabitRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMIND -> handleReminder(context, intent)
            ACTION_MARK_DONE -> handleMarkDone(context, intent)
        }
    }

    private fun handleReminder(context: Context, intent: Intent) {
        createChannel(context)
        if (!canNotify(context)) return

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId <= 0L) return
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME).orEmpty().ifBlank { "Your habit" }
        val hour = intent.getIntExtra(EXTRA_HOUR, 20)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)

        val appIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            habitId.toInt(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_HABIT_ID, habitId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            markDoneRequestCode(habitId),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(habitName)
            .setContentText("Time to keep your streak alive.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Mark Done", donePendingIntent)
            .build()

        tryNotify(context, habitId, notification)

        ReminderScheduler(context).scheduleDailyReminder(
            habitId = habitId,
            habitName = habitName,
            hour = hour,
            minute = minute
        )
    }

    private fun handleMarkDone(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId <= 0L) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = HabitRepository(HabitDatabase.getInstance(context).habitDao())
                repository.setHabitCompletion(habitId, LocalDate.now().toEpochDay(), true)
                NotificationManagerCompat.from(context).cancel(notificationIdForHabit(habitId))
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Habit Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily reminders for habits"
        }
        manager.createNotificationChannel(channel)
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun tryNotify(context: Context, habitId: Long, notification: android.app.Notification) {
        if (!canNotify(context)) return
        try {
            NotificationManagerCompat.from(context).notify(notificationIdForHabit(habitId), notification)
        } catch (_: SecurityException) {
            // If permission is revoked between check and notify, ignore safely.
        }
    }

    private fun notificationIdForHabit(habitId: Long): Int = (habitId % Int.MAX_VALUE).toInt()

    private fun markDoneRequestCode(habitId: Long): Int = ((habitId + 10_000) % Int.MAX_VALUE).toInt()

    companion object {
        const val CHANNEL_ID = "habit_reminders_channel"
        const val ACTION_REMIND = "com.notbraker.tracker.action.REMIND"
        const val ACTION_MARK_DONE = "com.notbraker.tracker.action.MARK_DONE"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
    }
}
