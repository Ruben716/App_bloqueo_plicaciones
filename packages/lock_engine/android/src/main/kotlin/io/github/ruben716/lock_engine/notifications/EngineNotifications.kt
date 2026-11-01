package io.github.ruben716.lock_engine.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import io.github.ruben716.lock_engine.R

/** Canales y notificaciones propias del motor. */
object EngineNotifications {

    const val MONITOR_ID = 7101
    const val HIDDEN_SUMMARY_ID = 7102

    private const val CHANNEL_MONITOR = "lock_engine_monitor"
    private const val CHANNEL_HIDDEN = "lock_engine_hidden"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITOR,
                context.getString(R.string.lock_engine_channel_monitor),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_HIDDEN,
                context.getString(R.string.lock_engine_channel_hidden),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
    }

    fun monitor(context: Context, guestActive: Boolean): Notification {
        ensureChannels(context)
        val text = if (guestActive) {
            R.string.lock_engine_monitor_text_guest
        } else {
            R.string.lock_engine_monitor_text
        }
        return Notification.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_lock_engine_shield)
            .setContentTitle(context.getString(R.string.lock_engine_monitor_title))
            .setContentText(context.getString(text))
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setContentIntent(openAppIntent(context))
            .build()
    }

    fun hiddenSummary(context: Context, count: Int): Notification {
        ensureChannels(context)
        return Notification.Builder(context, CHANNEL_HIDDEN)
            .setSmallIcon(R.drawable.ic_lock_engine_shield)
            .setContentTitle(context.getString(R.string.lock_engine_hidden_title))
            .setContentText(
                context.resources.getQuantityString(R.plurals.lock_engine_hidden_text, count, count),
            )
            .setNumber(count)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
    }

    private fun openAppIntent(context: Context): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
