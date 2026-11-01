package io.github.ruben716.lock_engine.rules

import io.github.ruben716.lock_engine.model.RelockMode

/** Orden para la capa de UI (overlay) resultante de cada evaluación. */
sealed interface OverlayCommand {
    data class Show(val packageName: String) : OverlayCommand
    data object Hide : OverlayCommand
    data object None : OverlayCommand
}

/**
 * Máquina de estados que, a partir de la app en primer plano, decide si hay
 * que mostrar u ocultar la pantalla de bloqueo. No depende de Android.
 */
class LockDecider(private val sessions: SessionManager = SessionManager()) {

    /** Última app "real" en primer plano (se ignoran los paquetes transitorios). */
    var foreground: String? = null
        private set

    /** App para la que se está mostrando la pantalla de bloqueo, si hay alguna. */
    var overlayFor: String? = null
        private set

    fun onTick(
        foregroundPackage: String?,
        transient: Set<String>,
        locked: Set<String>,
        mode: RelockMode,
        timeoutMs: Long,
        nowMs: Long,
    ): OverlayCommand {
        if (foregroundPackage == null || foregroundPackage in transient) {
            // Si la app protegida dejó de estar bloqueada (p. ej. se quitó del
            // perfil) mientras un diálogo transitorio estaba encima, ocultamos.
            val current = overlayFor
            if (current != null && current !in locked) {
                overlayFor = null
                return OverlayCommand.Hide
            }
            return OverlayCommand.None
        }

        if (foregroundPackage != foreground) {
            foreground?.let { sessions.onLeft(it, nowMs) }
            foreground = foregroundPackage
            sessions.onEntered(foregroundPackage, nowMs, mode, timeoutMs)
        }

        val needsLock = foregroundPackage in locked &&
            !sessions.isUnlocked(foregroundPackage, nowMs, mode, timeoutMs)

        return when {
            needsLock && overlayFor != foregroundPackage -> {
                overlayFor = foregroundPackage
                OverlayCommand.Show(foregroundPackage)
            }
            !needsLock && overlayFor != null -> {
                overlayFor = null
                OverlayCommand.Hide
            }
            else -> OverlayCommand.None
        }
    }

    fun onUnlocked(packageName: String) {
        sessions.grant(packageName)
        if (overlayFor == packageName) overlayFor = null
    }

    fun onScreenOff() {
        sessions.revokeAll()
    }

    /** Se quitó el overlay por otra vía (p. ej. botón atrás -> inicio). */
    fun onOverlayDismissed() {
        overlayFor = null
    }

    fun revokeAll() {
        sessions.revokeAll()
    }
}
