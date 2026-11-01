package io.github.ruben716.lock_engine.api

import io.github.ruben716.lock_engine.apps.AppInfo
import io.github.ruben716.lock_engine.biometric.BiometricAvailability
import io.github.ruben716.lock_engine.intruder.IntruderRecord
import io.github.ruben716.lock_engine.model.CredentialType
import io.github.ruben716.lock_engine.model.LogEntry
import io.github.ruben716.lock_engine.model.LogKind
import io.github.ruben716.lock_engine.model.Profile
import io.github.ruben716.lock_engine.model.RelockMode
import io.github.ruben716.lock_engine.model.Settings
import io.github.ruben716.lock_engine.model.TimeWindow
import io.github.ruben716.lock_engine.system.EnginePermission
import io.github.ruben716.lock_engine.system.PermissionInfo

/** Conversión entre el modelo de dominio y los tipos generados por Pigeon. */
internal object Mappers {

    fun CredentialKind.toDomain(): CredentialType = when (this) {
        CredentialKind.PIN -> CredentialType.PIN
        CredentialKind.PASSWORD -> CredentialType.PASSWORD
        CredentialKind.PATTERN -> CredentialType.PATTERN
    }

    fun CredentialType.toApi(): CredentialKind = when (this) {
        CredentialType.PIN -> CredentialKind.PIN
        CredentialType.PASSWORD -> CredentialKind.PASSWORD
        CredentialType.PATTERN -> CredentialKind.PATTERN
    }

    fun Settings.toApi() = EngineSettings(
        relockPolicy = when (relockMode) {
            RelockMode.IMMEDIATELY -> RelockPolicy.IMMEDIATELY
            RelockMode.AFTER_TIMEOUT -> RelockPolicy.AFTER_TIMEOUT
            RelockMode.ON_SCREEN_OFF -> RelockPolicy.ON_SCREEN_OFF
        },
        relockTimeoutMinutes = relockTimeoutMinutes.toLong(),
        biometricEnabled = biometricEnabled,
        intruderEnabled = intruderEnabled,
        intruderThreshold = intruderThreshold.toLong(),
        hideNotifications = hideNotifications,
        antiUninstall = antiUninstall,
        fakeCrash = fakeCrash,
        blockScreenshots = blockScreenshots,
        themeId = themeId,
    )

    /** La biometría no se cambia desde aquí: tiene su propio flujo con el Keystore. */
    fun EngineSettings.toDomain(current: Settings) = current.copy(
        relockMode = when (relockPolicy) {
            RelockPolicy.IMMEDIATELY -> RelockMode.IMMEDIATELY
            RelockPolicy.AFTER_TIMEOUT -> RelockMode.AFTER_TIMEOUT
            RelockPolicy.ON_SCREEN_OFF -> RelockMode.ON_SCREEN_OFF
        },
        relockTimeoutMinutes = relockTimeoutMinutes.toInt().coerceIn(1, 24 * 60),
        intruderEnabled = intruderEnabled,
        intruderThreshold = intruderThreshold.toInt().coerceIn(1, 10),
        hideNotifications = hideNotifications,
        antiUninstall = antiUninstall,
        fakeCrash = fakeCrash,
        blockScreenshots = blockScreenshots,
        themeId = themeId,
    )

    fun Profile.toApi() = LockProfile(
        id = id,
        name = name,
        packages = packages.sorted(),
        enabled = enabled,
        isDefault = isDefault,
        schedule = schedule?.let {
            Schedule(days = it.days.sorted().map(Int::toLong), startMinute = it.startMinute.toLong(), endMinute = it.endMinute.toLong())
        },
    )

    fun LockProfile.toDomain() = Profile(
        id = id,
        name = name.trim().ifEmpty { "Perfil" },
        packages = packages.toSet(),
        enabled = enabled,
        isDefault = isDefault,
        schedule = schedule?.let { s ->
            TimeWindow(
                days = s.days.map(Long::toInt).filter { it in 1..7 }.toSet(),
                startMinute = s.startMinute.toInt().coerceIn(0, 1439),
                endMinute = s.endMinute.toInt().coerceIn(0, 1439),
            )
        },
    )

    fun AppInfo.toApi() = InstalledApp(
        packageName = packageName,
        label = label,
        isSystem = isSystem,
        canLock = canLock,
        isRecommended = isRecommended,
    )

    fun BiometricAvailability.toApi(): BiometricStatus = when (this) {
        BiometricAvailability.AVAILABLE -> BiometricStatus.AVAILABLE
        BiometricAvailability.NOT_ENROLLED -> BiometricStatus.NOT_ENROLLED
        BiometricAvailability.UNSUPPORTED -> BiometricStatus.UNSUPPORTED
        BiometricAvailability.INVALIDATED -> BiometricStatus.INVALIDATED
    }

    fun PermissionKind.toDomain(): EnginePermission = when (this) {
        PermissionKind.USAGE_ACCESS -> EnginePermission.USAGE_ACCESS
        PermissionKind.OVERLAY -> EnginePermission.OVERLAY
        PermissionKind.NOTIFICATIONS -> EnginePermission.NOTIFICATIONS
        PermissionKind.NOTIFICATION_LISTENER -> EnginePermission.NOTIFICATION_LISTENER
        PermissionKind.CAMERA -> EnginePermission.CAMERA
        PermissionKind.BATTERY_OPTIMIZATION -> EnginePermission.BATTERY_OPTIMIZATION
    }

    fun PermissionInfo.toApi() = PermissionState(
        kind = when (permission) {
            EnginePermission.USAGE_ACCESS -> PermissionKind.USAGE_ACCESS
            EnginePermission.OVERLAY -> PermissionKind.OVERLAY
            EnginePermission.NOTIFICATIONS -> PermissionKind.NOTIFICATIONS
            EnginePermission.NOTIFICATION_LISTENER -> PermissionKind.NOTIFICATION_LISTENER
            EnginePermission.CAMERA -> PermissionKind.CAMERA
            EnginePermission.BATTERY_OPTIMIZATION -> PermissionKind.BATTERY_OPTIMIZATION
        },
        granted = granted,
        required = required,
    )

    fun IntruderRecord.toApi(label: String) = IntruderPhoto(
        path = file.absolutePath,
        timestampMs = timestampMs,
        packageName = packageName,
        appLabel = label,
    )

    fun LogEntry.toApi(label: String?) = ActivityEntry(
        timestampMs = timestampMs,
        kind = when (kind) {
            LogKind.UNLOCKED -> ActivityKind.UNLOCKED
            LogKind.FAILED_ATTEMPT -> ActivityKind.FAILED_ATTEMPT
            LogKind.LOCKOUT -> ActivityKind.LOCKOUT
            LogKind.INTRUDER_CAPTURED -> ActivityKind.INTRUDER_CAPTURED
            LogKind.GUEST_MODE_ON -> ActivityKind.GUEST_MODE_ON
            LogKind.GUEST_MODE_OFF -> ActivityKind.GUEST_MODE_OFF
            LogKind.RECOVERY_USED -> ActivityKind.RECOVERY_USED
            LogKind.BIOMETRIC_INVALIDATED -> ActivityKind.BIOMETRIC_INVALIDATED
        },
        packageName = packageName,
        appLabel = label,
    )
}
