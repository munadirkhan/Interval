package com.munadir.interval.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.munadir.interval.MainActivity
import com.munadir.interval.R

object Notifier {

    const val CHANNEL_ID = "reviews"
    const val NOTIFICATION_ID = 1001
    const val EXTRA_OPEN_REVIEW = "open_review"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Review reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Nudges you when cards are ready to review." }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    /** Fired when cards cross their due time. */
    fun showDue(context: Context, count: Int) {
        if (count <= 0) return
        post(
            context,
            title = "Time to review",
            text = if (count == 1) "1 card is ready" else "$count cards are ready"
        )
    }

    /** Fired at the user's chosen study time, whether or not anything is strictly due. */
    fun showDaily(context: Context, dueCount: Int) {
        post(
            context,
            title = if (dueCount > 0) "Your daily review" else "Keep the streak",
            text = when {
                dueCount == 1 -> "1 card is waiting"
                dueCount > 1 -> "$dueCount cards are waiting"
                else -> "Nothing due, but a quick review keeps the habit"
            }
        )
    }

    private fun post(context: Context, title: String, text: String) {
        if (!canPost(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_REVIEW, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }
}
