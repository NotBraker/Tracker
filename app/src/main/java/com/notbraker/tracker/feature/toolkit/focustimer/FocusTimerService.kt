package com.notbraker.tracker.feature.toolkit.focustimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.notbraker.tracker.R
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusTimerService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 25).coerceIn(1, 180)
                startTimer(minutes)
            }

            ACTION_STOP -> {
                stopTimer()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        timerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startTimer(minutes: Int) {
        createChannel()
        timerJob?.cancel()
        var secondsLeft = minutes * 60
        startForeground(NOTIFICATION_ID, buildNotification(secondsLeft))
        timerJob = scope.launch {
            while (isActive && secondsLeft > 0) {
                delay(1_000L)
                secondsLeft -= 1
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, buildNotification(secondsLeft))
            }
            if (secondsLeft <= 0) {
                addFocusStats(minutes)
                stopSelf()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus Timer",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(secondsLeft: Int): Notification {
        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Focus timer running")
            .setContentText(String.format("%02d:%02d remaining", minutes, seconds))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun addFocusStats(minutes: Int) {
        val prefs = getSharedPreferences(STATS_PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toEpochDay()
        val storedDay = prefs.getLong(KEY_DAY, today)
        val sessions = if (storedDay == today) prefs.getInt(KEY_SESSIONS, 0) else 0
        val totalMinutes = if (storedDay == today) prefs.getInt(KEY_MINUTES, 0) else 0
        prefs.edit()
            .putLong(KEY_DAY, today)
            .putInt(KEY_SESSIONS, sessions + 1)
            .putInt(KEY_MINUTES, totalMinutes + minutes)
            .apply()
    }

    companion object {
        private const val CHANNEL_ID = "focus_timer_channel"
        private const val NOTIFICATION_ID = 3371
        private const val EXTRA_MINUTES = "extra_minutes"
        private const val ACTION_START = "com.notbraker.tracker.action.FOCUS_START"
        private const val ACTION_STOP = "com.notbraker.tracker.action.FOCUS_STOP"
        const val STATS_PREFS = "focus_timer_stats"
        const val KEY_DAY = "day"
        const val KEY_SESSIONS = "sessions"
        const val KEY_MINUTES = "minutes"

        fun start(context: Context, minutes: Int) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MINUTES, minutes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, FocusTimerService::class.java).apply { action = ACTION_STOP }
            )
        }
    }
}
