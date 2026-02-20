package com.notbraker.tracker

import android.content.ComponentName
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notbraker.tracker.reminder.BootReceiver
import com.notbraker.tracker.reminder.ReminderReceiver
import com.notbraker.tracker.widget.TodayWidgetReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.notbraker.tracker", appContext.packageName)
    }

    @Test
    fun receiversAreRegisteredInManifest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager

        val reminder = packageManager.getReceiverInfo(ComponentName(context, ReminderReceiver::class.java), 0)
        val boot = packageManager.getReceiverInfo(ComponentName(context, BootReceiver::class.java), 0)
        val widget = packageManager.getReceiverInfo(ComponentName(context, TodayWidgetReceiver::class.java), 0)

        assertNotNull(reminder)
        assertNotNull(boot)
        assertNotNull(widget)
    }
}