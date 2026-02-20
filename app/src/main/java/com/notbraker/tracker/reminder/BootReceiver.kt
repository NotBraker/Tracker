package com.notbraker.tracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notbraker.tracker.data.database.HabitDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> reschedule(context)
        }
    }

    private fun reschedule(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habits = HabitDatabase.getInstance(context).habitDao().getHabitsWithReminders()
                ReminderScheduler(context).rescheduleAll(habits)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
