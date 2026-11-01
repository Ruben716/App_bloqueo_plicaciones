package io.github.ruben716.lock_engine.notifications

import android.app.Notification
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.ruben716.lock_engine.EngineGraph

/**
 * Oculta las notificaciones de las apps bloqueadas para que su contenido no
 * se vea en la barra de notificaciones. En su lugar muestra un aviso genérico
 * ("N notificaciones ocultas") sin remitente ni texto.
 *
 * No se guarda el contenido de las notificaciones en ningún sitio.
 */
class PrivacyNotificationListener : NotificationListenerService() {

    private var hiddenCount = 0

    override fun onListenerConnected() {
        super.onListenerConnected()
        runCatching { activeNotifications }.getOrNull()?.forEach(::maybeHide)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        maybeHide(sbn)
    }

    private fun maybeHide(sbn: StatusBarNotification) {
        val graph = EngineGraph.get(this)
        if (!graph.config.current.settings.hideNotifications) return
        if (sbn.packageName == packageName) return
        if (sbn.isOngoing || sbn.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return
        if (sbn.packageName !in graph.coordinator.currentlyLocked()) return

        runCatching { cancelNotification(sbn.key) }
        // Las agrupaciones (resúmenes) no cuentan como notificación nueva.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        if (!isSummaryVisible()) hiddenCount = 0
        hiddenCount++
        getSystemService(NotificationManager::class.java)?.notify(
            EngineNotifications.HIDDEN_SUMMARY_ID,
            EngineNotifications.hiddenSummary(this, hiddenCount),
        )
    }

    /** Si el usuario descartó o abrió el aviso, el contador vuelve a empezar. */
    private fun isSummaryVisible(): Boolean = runCatching {
        getSystemService(NotificationManager::class.java)
            ?.activeNotifications
            ?.any { it.id == EngineNotifications.HIDDEN_SUMMARY_ID }
    }.getOrNull() == true
}
