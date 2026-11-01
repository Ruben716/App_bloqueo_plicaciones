package io.github.ruben716.lock_engine.rules

import io.github.ruben716.lock_engine.model.RelockMode

/**
 * Recuerda qué apps se desbloquearon y decide cuándo vuelven a bloquearse
 * según la política elegida. Lógica pura y no thread-safe: se usa solo desde
 * el hilo del motor.
 *
 * - IMMEDIATELY: la sesión termina al salir de la app.
 * - AFTER_TIMEOUT: la sesión sobrevive si se vuelve antes de X minutos.
 * - ON_SCREEN_OFF: la sesión dura hasta que se apaga la pantalla.
 *
 * En todos los modos apagar la pantalla revoca todas las sesiones.
 */
class SessionManager {

    private class Session(var leftAtMs: Long? = null)

    private val sessions = HashMap<String, Session>()

    fun grant(packageName: String) {
        sessions[packageName] = Session()
    }

    fun revoke(packageName: String) {
        sessions.remove(packageName)
    }

    fun revokeAll() {
        sessions.clear()
    }

    fun hasSession(packageName: String): Boolean = sessions.containsKey(packageName)

    /** La app [packageName] dejó de estar en primer plano. */
    fun onLeft(packageName: String, nowMs: Long) {
        val session = sessions[packageName] ?: return
        if (session.leftAtMs == null) session.leftAtMs = nowMs
    }

    /** La app [packageName] volvió a primer plano. */
    fun onEntered(packageName: String, nowMs: Long, mode: RelockMode, timeoutMs: Long) {
        if (isUnlocked(packageName, nowMs, mode, timeoutMs)) {
            sessions[packageName]?.leftAtMs = null
        }
    }

    fun isUnlocked(packageName: String, nowMs: Long, mode: RelockMode, timeoutMs: Long): Boolean {
        val session = sessions[packageName] ?: return false
        val leftAt = session.leftAtMs ?: return true
        val valid = when (mode) {
            RelockMode.IMMEDIATELY -> false
            RelockMode.AFTER_TIMEOUT -> nowMs - leftAt <= timeoutMs
            RelockMode.ON_SCREEN_OFF -> true
        }
        if (!valid) sessions.remove(packageName)
        return valid
    }
}
