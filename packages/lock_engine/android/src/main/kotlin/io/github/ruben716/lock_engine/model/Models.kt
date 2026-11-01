package io.github.ruben716.lock_engine.model

/**
 * Modelos de dominio del motor. Son independientes del código generado por
 * Pigeon para que la lógica (reglas, sesiones, seguridad) pueda probarse en
 * la JVM sin Flutter ni Android.
 */

enum class CredentialType { PIN, PASSWORD, PATTERN }

enum class RelockMode { IMMEDIATELY, AFTER_TIMEOUT, ON_SCREEN_OFF }

/**
 * Ventana horaria semanal.
 * @property days días ISO-8601 (1 = lunes ... 7 = domingo).
 * @property startMinute minuto del día en que empieza (0..1439).
 * @property endMinute minuto del día en que termina (0..1439). Si es menor que
 * [startMinute] la ventana cruza la medianoche. Si es igual, dura todo el día.
 */
data class TimeWindow(
    val days: Set<Int>,
    val startMinute: Int,
    val endMinute: Int,
)

data class Profile(
    val id: String,
    val name: String,
    val packages: Set<String>,
    val enabled: Boolean,
    val isDefault: Boolean,
    val schedule: TimeWindow?,
) {
    companion object {
        const val DEFAULT_ID = "default"

        fun default() = Profile(
            id = DEFAULT_ID,
            name = "Siempre",
            packages = emptySet(),
            enabled = true,
            isDefault = true,
            schedule = null,
        )
    }
}

data class Settings(
    val relockMode: RelockMode = RelockMode.IMMEDIATELY,
    val relockTimeoutMinutes: Int = 5,
    val biometricEnabled: Boolean = false,
    val intruderEnabled: Boolean = false,
    val intruderThreshold: Int = 3,
    val hideNotifications: Boolean = false,
    val antiUninstall: Boolean = false,
    val fakeCrash: Boolean = false,
    val blockScreenshots: Boolean = true,
    val themeId: String = "aurora",
)

data class GuestConfig(
    val active: Boolean = false,
    val allowedPackages: Set<String> = emptySet(),
)

data class EngineConfig(
    val engineEnabled: Boolean = false,
    val settings: Settings = Settings(),
    val profiles: List<Profile> = listOf(Profile.default()),
    val guest: GuestConfig = GuestConfig(),
) {
    val defaultProfile: Profile
        get() = profiles.firstOrNull { it.isDefault } ?: Profile.default()
}

/** Paquetes del sistema relevantes para calcular las reglas de bloqueo. */
data class SystemPackages(
    /** Todas las apps con icono en el launcher. */
    val launcherApps: Set<String>,
    /** Ajustes, tienda e instaladores: bloquearlos impide desinstalar Sigilo. */
    val antiUninstallTargets: Set<String>,
    /** Nunca se bloquean: launcher, teléfono, teclados, System UI y Sigilo. */
    val neverLock: Set<String>,
    /**
     * Si pasan a primer plano no cuentan como "salir" de la app actual
     * (diálogos del sistema, selector de compartir, teclado, Sigilo...).
     */
    val transient: Set<String>,
)

enum class LogKind {
    UNLOCKED,
    FAILED_ATTEMPT,
    LOCKOUT,
    INTRUDER_CAPTURED,
    GUEST_MODE_ON,
    GUEST_MODE_OFF,
    RECOVERY_USED,
    BIOMETRIC_INVALIDATED,
}

data class LogEntry(
    val timestampMs: Long,
    val kind: LogKind,
    val packageName: String?,
)
