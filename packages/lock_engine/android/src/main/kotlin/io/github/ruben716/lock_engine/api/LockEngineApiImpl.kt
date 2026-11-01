package io.github.ruben716.lock_engine.api

import android.app.Activity
import io.github.ruben716.lock_engine.EngineGraph
import io.github.ruben716.lock_engine.api.Mappers.toApi
import io.github.ruben716.lock_engine.api.Mappers.toDomain
import io.github.ruben716.lock_engine.biometric.BiometricAvailability
import io.github.ruben716.lock_engine.biometric.BiometricOutcome
import io.github.ruben716.lock_engine.model.LogKind
import io.github.ruben716.lock_engine.monitor.AppMonitorService

/**
 * Implementación de la API Pigeon. Solo traduce y delega: la lógica vive en
 * los módulos del motor (security, rules, config, monitor...).
 */
internal class LockEngineApiImpl(
    private val graph: EngineGraph,
    private val activityProvider: () -> Activity?,
    private val routeProvider: () -> String?,
    private val onSettingsChanged: () -> Unit,
) : LockEngineHostApi {

    // ------------------------------------------------------------- Estado

    override fun getStatus(): EngineStatus {
        graph.syncServiceState()
        val config = graph.config.current
        val credentials = graph.credentials
        val activeProfileIds = graph.coordinator.activeProfileIds()
        val lockedApps = config.profiles
            .filter { it.id in activeProfileIds }
            .flatMap { it.packages }
            .toSet()
        return EngineStatus(
            enabled = config.engineEnabled,
            running = AppMonitorService.isRunning,
            credentialSet = credentials.hasCredential(),
            credentialKind = credentials.type()?.toApi(),
            pinLength = credentials.pinLength()?.toLong(),
            guestActive = config.guest.active,
            activeProfileIds = activeProfileIds,
            lockedAppCount = lockedApps.size.toLong(),
            intruderCount = graph.intruders.count().toLong(),
            missingRequiredPermissions = graph.permissions.all(config.settings).any { it.required && !it.granted },
        )
    }

    override fun setEngineEnabled(enabled: Boolean) {
        if (enabled && !graph.credentials.hasCredential()) {
            throw LockEngineError("no_credential", "Configura primero tu PIN, contraseña o patrón.")
        }
        graph.config.update { it.copy(engineEnabled = enabled) }
        graph.syncServiceState()
    }

    // ---------------------------------------------------------- Credencial

    override fun validateCredential(kind: CredentialKind, secret: String): String? =
        graph.credentials.validate(kind.toDomain(), secret)

    override fun setCredential(kind: CredentialKind, secret: String): String? = try {
        graph.credentials.set(kind.toDomain(), secret)
    } catch (e: IllegalArgumentException) {
        throw LockEngineError("weak_credential", e.message)
    }

    override fun verifyCredential(secret: String): AuthResult {
        val outcome = graph.credentials.verify(secret)
        if (!outcome.success && !outcome.throttled) {
            graph.onFailedAttempt(graph.appContext.packageName, outcome)
        }
        return AuthResult(
            success = outcome.success,
            failedAttempts = outcome.failedAttempts.toLong(),
            lockoutRemainingMs = outcome.lockoutRemainingMs,
        )
    }

    override fun getLockoutRemainingMs(): Long = graph.credentials.lockoutRemainingMs()

    override fun resetWithRecoveryCode(code: String, kind: CredentialKind, newSecret: String): RecoveryResult {
        val outcome = graph.credentials.resetWithRecoveryCode(code, kind.toDomain(), newSecret)
        if (outcome.success) graph.log.add(LogKind.RECOVERY_USED)
        return RecoveryResult(
            success = outcome.success,
            lockoutRemainingMs = outcome.lockoutRemainingMs,
            newRecoveryCode = outcome.newRecoveryCode,
            errorMessage = outcome.errorMessage,
        )
    }

    override fun regenerateRecoveryCode(): String = graph.credentials.regenerateRecoveryCode()

    // ----------------------------------------------------------- Biometría

    override fun getBiometricStatus(): BiometricStatus {
        val availability = graph.biometrics.availability()
        if (availability != BiometricAvailability.AVAILABLE) return availability.toApi()
        if (graph.config.current.settings.biometricEnabled && graph.biometrics.prepareCipher() == null) {
            graph.onBiometricInvalidated()
            return BiometricStatus.INVALIDATED
        }
        return BiometricStatus.AVAILABLE
    }

    override fun setBiometricEnabled(enabled: Boolean): Boolean {
        val active = enabled &&
            graph.biometrics.availability() == BiometricAvailability.AVAILABLE &&
            graph.biometrics.createKey()
        if (!active) graph.biometrics.deleteKey()
        graph.config.update { it.copy(settings = it.settings.copy(biometricEnabled = active)) }
        return active
    }

    override fun authenticateBiometric(title: String, subtitle: String, callback: (Result<Boolean>) -> Unit) {
        val activity = activityProvider()
        if (activity == null || !graph.config.current.settings.biometricEnabled) {
            callback(Result.success(false))
            return
        }
        graph.biometrics.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            negativeText = graph.credentialFallbackLabel(),
        ) { outcome ->
            when (outcome) {
                BiometricOutcome.SUCCESS -> {
                    graph.credentials.recordExternalSuccess()
                    callback(Result.success(true))
                }
                BiometricOutcome.INVALIDATED -> {
                    graph.onBiometricInvalidated()
                    callback(Result.success(false))
                }
                BiometricOutcome.CANCELLED, BiometricOutcome.ERROR -> callback(Result.success(false))
            }
        }
    }

    // ---------------------------------------------------------------- Apps

    override fun getInstalledApps(): List<InstalledApp> = graph.catalog.installedApps().map { it.toApi() }

    override fun getAppIcon(packageName: String, sizePx: Long): ByteArray? =
        graph.catalog.iconPng(packageName, sizePx.toInt())

    // ------------------------------------------------------- Configuración

    override fun getSettings(): EngineSettings = graph.config.current.settings.toApi()

    override fun updateSettings(settings: EngineSettings) {
        graph.config.update { it.copy(settings = settings.toDomain(it.settings)) }
        onSettingsChanged()
    }

    override fun getProfiles(): List<LockProfile> = graph.config.current.profiles.map { it.toApi() }

    override fun saveProfile(profile: LockProfile) {
        graph.config.saveProfile(profile.toDomain())
    }

    override fun deleteProfile(id: String) {
        graph.config.deleteProfile(id)
    }

    override fun setAppLocked(packageName: String, locked: Boolean) {
        graph.config.setAppLocked(packageName, locked)
    }

    override fun getGuestState(): GuestState {
        val guest = graph.config.current.guest
        return GuestState(active = guest.active, allowedPackages = guest.allowedPackages.sorted())
    }

    override fun setGuestMode(active: Boolean) = graph.setGuestMode(active)

    override fun setGuestAllowedPackages(packages: List<String>) {
        graph.config.update { it.copy(guest = it.guest.copy(allowedPackages = packages.toSet())) }
    }

    // ------------------------------------------------------------ Permisos

    override fun getPermissions(): List<PermissionState> =
        graph.permissions.all(graph.config.current.settings).map { it.toApi() }

    override fun requestPermission(kind: PermissionKind) {
        graph.permissions.request(kind.toDomain(), activityProvider())
    }

    // ----------------------------------------------- Intrusos / actividad

    override fun getIntruderPhotos(): List<IntruderPhoto> =
        graph.intruders.list().map { it.toApi(graph.catalog.label(it.packageName)) }

    override fun deleteIntruderPhoto(path: String) {
        graph.intruders.delete(path)
    }

    override fun clearIntruderPhotos() = graph.intruders.clear()

    override fun getActivityLog(): List<ActivityEntry> =
        graph.log.entries().map { entry -> entry.toApi(entry.packageName?.let(graph.catalog::label)) }

    override fun clearActivityLog() = graph.log.clear()

    // ---------------------------------------------------------- Navegación

    override fun consumeLaunchRoute(): String? = routeProvider()
}
