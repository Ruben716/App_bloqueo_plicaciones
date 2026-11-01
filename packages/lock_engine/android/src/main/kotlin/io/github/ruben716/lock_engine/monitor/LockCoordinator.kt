package io.github.ruben716.lock_engine.monitor

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import io.github.ruben716.lock_engine.EngineGraph
import io.github.ruben716.lock_engine.model.LogKind
import io.github.ruben716.lock_engine.rules.LockDecider
import io.github.ruben716.lock_engine.rules.LockRules
import io.github.ruben716.lock_engine.rules.OverlayCommand
import java.time.LocalDateTime

/**
 * Bucle del motor. Corre en su propio hilo: consulta la app en primer plano,
 * calcula las reglas y ordena al overlay mostrarse u ocultarse.
 */
class LockCoordinator(context: Context, private val graph: EngineGraph) {

    private val appContext = context.applicationContext
    private val thread = HandlerThread("lock-engine-monitor").apply { start() }
    private val handler = Handler(thread.looper)
    private val detector = ForegroundAppDetector(appContext)
    private val decider = LockDecider()

    @Volatile
    var isRunning = false
        private set

    private var screenOn = true

    /** Evita que el overlay reaparezca mientras el launcher toma el primer plano. */
    private var homeRequestedFor: String? = null
    private var homeRequestedAtMs = 0L

    private val tick = object : Runnable {
        override fun run() {
            if (!isRunning || !screenOn) return
            evaluate()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    fun start() = handler.post {
        if (isRunning) return@post
        isRunning = true
        screenOn = true
        handler.post(tick)
    }

    fun stop() = handler.post {
        isRunning = false
        handler.removeCallbacks(tick)
        decider.revokeAll()
        decider.onOverlayDismissed()
        detector.reset()
        graph.overlay.hide()
    }

    fun onScreenOff() = handler.post {
        screenOn = false
        handler.removeCallbacks(tick)
        decider.onScreenOff()
    }

    fun onScreenOn() = handler.post {
        if (screenOn) return@post
        screenOn = true
        if (isRunning) handler.post(tick)
    }

    /** Reevalúa inmediatamente (p. ej. tras un cambio de configuración). */
    fun refresh() = handler.post { if (isRunning && screenOn) evaluate() }

    fun onUnlockSucceeded(packageName: String, viaBiometric: Boolean) {
        if (viaBiometric) graph.credentials.recordExternalSuccess()
        graph.log.add(LogKind.UNLOCKED, packageName)
        graph.overlay.hide()
        handler.post { decider.onUnlocked(packageName) }
    }

    /** El overlay no pudo mostrarse (p. ej. permiso retirado): se reintentará. */
    fun onOverlayUnavailable() = handler.post { decider.onOverlayDismissed() }

    /** Botón atrás / "cerrar app": vuelve al inicio y quita el overlay. */
    fun goHome() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(home) }
        graph.overlay.hide()
        handler.post {
            homeRequestedFor = decider.overlayFor
            homeRequestedAtMs = System.currentTimeMillis()
            decider.onOverlayDismissed()
        }
    }

    /** Apps bloqueadas en este momento (lo usa también el filtro de notificaciones). */
    fun currentlyLocked(): Set<String> {
        val now = LocalDateTime.now()
        return LockRules.effectiveLocked(
            config = graph.config.current,
            system = graph.catalog.systemPackages(),
            day = now.dayOfWeek.value,
            minute = now.hour * 60 + now.minute,
        )
    }

    fun activeProfileIds(): List<String> {
        val now = LocalDateTime.now()
        return LockRules.activeProfiles(graph.config.current, now.dayOfWeek.value, now.hour * 60 + now.minute)
            .map { it.id }
    }

    private fun evaluate() {
        val config = graph.config.current
        val system = graph.catalog.systemPackages()
        val now = LocalDateTime.now()
        val locked = LockRules.effectiveLocked(config, system, now.dayOfWeek.value, now.hour * 60 + now.minute)
        val nowMs = System.currentTimeMillis()
        val foreground = detector.poll(nowMs)
        if (foreground != null && foreground == homeRequestedFor && nowMs - homeRequestedAtMs < HOME_GRACE_MS) return

        val command = decider.onTick(
            foregroundPackage = foreground,
            transient = system.transient,
            locked = locked,
            mode = config.settings.relockMode,
            timeoutMs = config.settings.relockTimeoutMinutes.coerceAtLeast(1) * 60_000L,
            nowMs = nowMs,
        )
        when (command) {
            is OverlayCommand.Show -> graph.overlay.show(command.packageName)
            OverlayCommand.Hide -> graph.overlay.hide()
            OverlayCommand.None -> Unit
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 250L
        const val HOME_GRACE_MS = 1_500L
    }
}
