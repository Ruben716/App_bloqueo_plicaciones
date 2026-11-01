package io.github.ruben716.lock_engine.monitor

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import io.github.ruben716.lock_engine.EngineGraph
import io.github.ruben716.lock_engine.config.ConfigStore
import io.github.ruben716.lock_engine.notifications.EngineNotifications

/**
 * Servicio en primer plano (tipo `specialUse`) que mantiene vivo el motor de
 * bloqueo aunque Flutter no esté en ejecución.
 */
class AppMonitorService : Service() {

    private lateinit var graph: EngineGraph
    private var lastGuestActive: Boolean? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> graph.coordinator.onScreenOff()
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> graph.coordinator.onScreenOn()
            }
        }
    }

    private val configListener = ConfigStore.Listener { config ->
        if (!config.engineEnabled) {
            stopSelf()
        } else {
            graph.coordinator.refresh()
            if (config.guest.active != lastGuestActive) updateNotification(config.guest.active)
        }
    }

    override fun onCreate() {
        super.onCreate()
        graph = EngineGraph.get(this)
        startInForeground(graph.config.current.guest.active)
        isRunning = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
        graph.config.addListener(configListener)
        graph.coordinator.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Cada startForegroundService() exige volver a llamar a startForeground().
        startInForeground(graph.config.current.guest.active)
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        graph.config.removeListener(configListener)
        runCatching { unregisterReceiver(screenReceiver) }
        graph.coordinator.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground(guestActive: Boolean) {
        lastGuestActive = guestActive
        val notification = EngineNotifications.monitor(this, guestActive)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                EngineNotifications.MONITOR_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(EngineNotifications.MONITOR_ID, notification)
        }
    }

    private fun updateNotification(guestActive: Boolean) {
        lastGuestActive = guestActive
        getSystemService(NotificationManager::class.java)
            ?.notify(EngineNotifications.MONITOR_ID, EngineNotifications.monitor(this, guestActive))
    }

    companion object {
        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            runCatching {
                context.startForegroundService(Intent(context, AppMonitorService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, AppMonitorService::class.java)) }
        }
    }
}
