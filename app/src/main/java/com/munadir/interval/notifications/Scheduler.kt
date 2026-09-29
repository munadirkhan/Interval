package com.munadir.interval.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.munadir.interval.data.CardStore
import com.munadir.interval.data.Settings
import java.util.Calendar

/**
 * Two alarms: one that fires when the next card comes due, and one at the user's chosen study
 * time each day.
 *
 * Both are deliberately *inexact*. Exact alarms need a special permission on Android 12+ and
 * exist for calendar events and timers. A study reminder landing a few minutes late is fine,
 * and letting the OS batch it spares the battery.
 */
object Scheduler {

    const val ACTION_DUE = "com.munadir.interval.DUE"
    const val ACTION_DAILY = "com.munadir.interval.DAILY"

    private const val REQ_DUE = 2001
    private const val REQ_DAILY = 2002

    private fun intentFor(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    // ------------------------------------------------------------------ due alarm

    fun scheduleNext(context: Context) {
        if (!Settings.state.value.notificationsEnabled) {
            cancel(context, ACTION_DUE, REQ_DUE)
            return
        }
        val due = CardStore.nextDueAt() ?: return
        // Cards already due must not fire instantly, or the receiver re-arms in a tight loop.
        val at = maxOf(due, System.currentTimeMillis() + 60_000)
        alarm(context, at, ACTION_DUE, REQ_DUE)
    }

    /** Backs the "send a test reminder" button, so a demo does not require waiting a day. */
    fun scheduleIn(context: Context, millisFromNow: Long) {
        alarm(context, System.currentTimeMillis() + millisFromNow, ACTION_DUE, REQ_DUE)
    }

    // ------------------------------------------------------------------ daily reminder

    fun syncDailyReminder(context: Context) {
        val prefs = Settings.state.value
        if (!prefs.notificationsEnabled || !prefs.dailyReminderEnabled) {
            cancel(context, ACTION_DAILY, REQ_DAILY)
            return
        }
        alarm(
            context,
            nextOccurrenceOf(prefs.dailyReminderHour, prefs.dailyReminderMinute),
            ACTION_DAILY,
            REQ_DAILY
        )
    }

    private fun nextOccurrenceOf(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    // ------------------------------------------------------------------ plumbing

    private fun alarm(context: Context, atMillis: Long, action: String, requestCode: Int) {
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            atMillis,
            intentFor(context, action, requestCode)
        )
    }

    private fun cancel(context: Context, action: String, requestCode: Int) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(intentFor(context, action, requestCode))
    }
}
