package io.github.ruben716.lock_engine.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Detecta la app en primer plano leyendo los eventos de UsageStatsManager
 * (permiso "Acceso a uso"). No requiere el servicio de Accesibilidad, que
 * Google Play restringe para usos que no son de accesibilidad.
 */
class ForegroundAppDetector(context: Context) {

    private val usageStats = context.getSystemService(UsageStatsManager::class.java)
    private var lastPackage: String? = null
    private var lastEventTs = 0L

    /** @return el paquete en primer plano, o `null` si aún no se conoce. */
    fun poll(nowMs: Long = System.currentTimeMillis()): String? {
        val manager = usageStats ?: return lastPackage
        // Si el reloj del sistema retrocedió, no ignoramos los eventos nuevos.
        if (lastEventTs > nowMs) lastEventTs = 0L
        val events = try {
            manager.queryEvents(nowMs - WINDOW_MS, nowMs)
        } catch (_: Exception) {
            return lastPackage
        } ?: return lastPackage

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // ACTIVITY_RESUMED (API 29) tiene el mismo valor que MOVE_TO_FOREGROUND.
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp >= lastEventTs) {
                lastEventTs = event.timeStamp
                lastPackage = event.packageName
            }
        }
        return lastPackage
    }

    fun reset() {
        lastPackage = null
        lastEventTs = 0L
    }

    private companion object {
        /** Ventana de consulta: tolera retrasos del bucle sin perder eventos recientes. */
        const val WINDOW_MS = 10_000L
    }
}
