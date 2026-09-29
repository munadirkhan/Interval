package com.munadir.interval.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.munadir.interval.data.CardStore
import com.munadir.interval.data.Settings

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        Settings.init(app)
        CardStore.init(app)

        if (!Settings.state.value.notificationsEnabled) return

        val due = CardStore.dueCards().size

        when (intent.action) {
            Scheduler.ACTION_DAILY -> {
                Notifier.showDaily(context, due)
                // Same time tomorrow.
                Scheduler.syncDailyReminder(context)
            }

            else -> {
                Notifier.showDue(context, due)
                Scheduler.scheduleNext(context)
            }
        }
    }
}
