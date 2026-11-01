package io.github.ruben716.lock_engine

import android.content.Context
import io.github.ruben716.lock_engine.apps.AppCatalog
import io.github.ruben716.lock_engine.biometric.BiometricGate
import io.github.ruben716.lock_engine.config.ConfigStore
import io.github.ruben716.lock_engine.intruder.IntruderCaptureActivity
import io.github.ruben716.lock_engine.intruder.IntruderStore
import io.github.ruben716.lock_engine.model.CredentialType
import io.github.ruben716.lock_engine.model.LogKind
import io.github.ruben716.lock_engine.monitor.AppMonitorService
import io.github.ruben716.lock_engine.monitor.LockCoordinator
import io.github.ruben716.lock_engine.overlay.LockOverlayController
import io.github.ruben716.lock_engine.security.AuthOutcome
import io.github.ruben716.lock_engine.security.CredentialManager
import io.github.ruben716.lock_engine.security.KeystorePepper
import io.github.ruben716.lock_engine.security.SharedPreferencesStore
import io.github.ruben716.lock_engine.system.ActivityLog
import io.github.ruben716.lock_engine.system.PermissionsManager

/** Rutas que la app Flutter puede recibir al abrirse desde el sistema. */
object LaunchRoutes {
    const val EXTRA_ROUTE = "io.github.ruben716.lock_engine.extra.ROUTE"
    const val GUEST_MODE = "/guest"
}

/**
 * Contenedor de dependencias del motor (service locator). Una única instancia
 * por proceso compartida por el plugin de Flutter, el servicio, el overlay,
 * el mosaico de Ajustes rápidos y los receptores.
 */
class EngineGraph private constructor(context: Context) {

    val appContext: Context = context.applicationContext

    val config = ConfigStore(appContext)
    val credentials = CredentialManager(
        store = SharedPreferencesStore(appContext.getSharedPreferences(SECURE_PREFS, Context.MODE_PRIVATE)),
        pepper = KeystorePepper(),
    )
    val catalog = AppCatalog(appContext)
    val log = ActivityLog(appContext)
    val intruders = IntruderStore(appContext)
    val biometrics = BiometricGate(appContext)
    val permissions = PermissionsManager(appContext)
    val overlay by lazy { LockOverlayController(appContext, this) }
    val coordinator by lazy { LockCoordinator(appContext, this) }

    /** Arranca o detiene el servicio según la configuración y los permisos. */
    fun syncServiceState() {
        val shouldRun = config.current.engineEnabled &&
            credentials.hasCredential() &&
            permissions.coreGranted()
        if (shouldRun && !AppMonitorService.isRunning) {
            AppMonitorService.start(appContext)
        } else if (!shouldRun && AppMonitorService.isRunning) {
            AppMonitorService.stop(appContext)
        }
    }

    fun setGuestMode(active: Boolean) {
        if (config.current.guest.active == active) return
        config.update { it.copy(guest = it.guest.copy(active = active)) }
        log.add(if (active) LogKind.GUEST_MODE_ON else LogKind.GUEST_MODE_OFF)
    }

    /** Registro y foto del intruso tras un intento fallido (overlay o app). */
    fun onFailedAttempt(packageName: String, outcome: AuthOutcome) {
        log.add(LogKind.FAILED_ATTEMPT, packageName)
        if (outcome.lockoutRemainingMs > 0) log.add(LogKind.LOCKOUT, packageName)

        val settings = config.current.settings
        val threshold = settings.intruderThreshold.coerceAtLeast(1)
        if (settings.intruderEnabled && outcome.failedAttempts > 0 && outcome.failedAttempts % threshold == 0) {
            runCatching { appContext.startActivity(IntruderCaptureActivity.intent(appContext, packageName)) }
        }
    }

    /** Se registró una huella nueva en el sistema: la biometría queda anulada. */
    fun onBiometricInvalidated() {
        biometrics.deleteKey()
        if (config.current.settings.biometricEnabled) {
            config.update { it.copy(settings = it.settings.copy(biometricEnabled = false)) }
            log.add(LogKind.BIOMETRIC_INVALIDATED)
        }
    }

    fun credentialFallbackLabel(): String = appContext.getString(
        when (credentials.type()) {
            CredentialType.PASSWORD -> R.string.lock_engine_use_password
            CredentialType.PATTERN -> R.string.lock_engine_use_pattern
            else -> R.string.lock_engine_use_pin
        },
    )

    companion object {
        private const val SECURE_PREFS = "lock_engine_secure"

        @Volatile
        private var instance: EngineGraph? = null

        fun get(context: Context): EngineGraph =
            instance ?: synchronized(this) {
                instance ?: EngineGraph(context).also { instance = it }
            }
    }
}
