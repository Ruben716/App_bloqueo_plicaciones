package io.github.ruben716.lock_engine.system

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import io.github.ruben716.lock_engine.model.Settings as EngineSettings
import io.github.ruben716.lock_engine.notifications.PrivacyNotificationListener

enum class EnginePermission {
    USAGE_ACCESS,
    OVERLAY,
    NOTIFICATIONS,
    NOTIFICATION_LISTENER,
    CAMERA,
    BATTERY_OPTIMIZATION,
}

data class PermissionInfo(val permission: EnginePermission, val granted: Boolean, val required: Boolean)

/** Comprueba y solicita los permisos especiales que necesita el motor. */
class PermissionsManager(context: Context) {

    private val appContext = context.applicationContext
    private val packageName = appContext.packageName

    fun isGranted(permission: EnginePermission): Boolean = when (permission) {
        EnginePermission.USAGE_ACCESS -> hasUsageAccess()
        EnginePermission.OVERLAY -> Settings.canDrawOverlays(appContext)
        EnginePermission.NOTIFICATIONS -> hasNotificationPermission()
        EnginePermission.NOTIFICATION_LISTENER -> hasNotificationListenerAccess()
        EnginePermission.CAMERA -> appContext.checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        EnginePermission.BATTERY_OPTIMIZATION ->
            appContext.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == true
    }

    /** Requeridos = imprescindibles para las funciones activadas. */
    fun all(settings: EngineSettings): List<PermissionInfo> = EnginePermission.entries
        .filter { it != EnginePermission.NOTIFICATION_LISTENER || isNotificationListenerDeclared() }
        .map { permission ->
            val required = when (permission) {
                EnginePermission.USAGE_ACCESS, EnginePermission.OVERLAY -> true
                EnginePermission.CAMERA -> settings.intruderEnabled
                EnginePermission.NOTIFICATION_LISTENER -> settings.hideNotifications
                EnginePermission.NOTIFICATIONS, EnginePermission.BATTERY_OPTIMIZATION -> false
            }
            PermissionInfo(permission, isGranted(permission), required)
        }

    /**
     * La app puede quitar el servicio del manifiesto (ver
     * app/android/app/src/release/AndroidManifest.xml): Play Protect bloquea la
     * instalación fuera de Play Store de apps que declaran un NotificationListener.
     */
    fun isNotificationListenerDeclared(): Boolean = try {
        @Suppress("DEPRECATION")
        appContext.packageManager.getServiceInfo(
            ComponentName(appContext, PrivacyNotificationListener::class.java),
            PackageManager.MATCH_DISABLED_COMPONENTS,
        )
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun coreGranted(): Boolean = hasUsageAccess() && Settings.canDrawOverlays(appContext)

    fun request(permission: EnginePermission, activity: Activity?) {
        when (permission) {
            EnginePermission.USAGE_ACCESS -> openSettings(
                activity,
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).setData(packageUri()),
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            )
            EnginePermission.OVERLAY -> openSettings(
                activity,
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri()),
            )
            EnginePermission.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null) {
                    activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
                } else {
                    openSettings(
                        activity,
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
                    )
                }
            EnginePermission.NOTIFICATION_LISTENER -> openSettings(
                activity,
                notificationListenerIntent(),
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            )
            EnginePermission.CAMERA ->
                if (activity != null && activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA).not() &&
                    wasCameraAsked()
                ) {
                    // Denegado permanentemente: solo queda abrir los ajustes de la app.
                    openSettings(activity, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
                } else {
                    markCameraAsked()
                    activity?.requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE)
                }
            EnginePermission.BATTERY_OPTIMIZATION -> openSettings(
                activity,
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            )
        }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = appContext.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Deprecado en API 36 pero sigue siendo la forma soportada desde API 29.
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            appContext.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    private fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            appContext.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true
        }

    private fun hasNotificationListenerAccess(): Boolean {
        val component = ComponentName(appContext, PrivacyNotificationListener::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            appContext.getSystemService(NotificationManager::class.java)
                ?.isNotificationListenerAccessGranted(component) == true
        } else {
            Settings.Secure.getString(appContext.contentResolver, "enabled_notification_listeners")
                ?.contains(component.flattenToString()) == true
        }
    }

    private fun notificationListenerIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(appContext, PrivacyNotificationListener::class.java).flattenToString(),
            )
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }

    private fun packageUri(): Uri = Uri.parse("package:$packageName")

    /** Intenta cada intent en orden (algunos fabricantes no soportan todos). */
    private fun openSettings(activity: Activity?, vararg intents: Intent) {
        val context: Context = activity ?: appContext
        for (intent in intents) {
            if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                continue
            } catch (_: SecurityException) {
                continue
            }
        }
    }

    private fun wasCameraAsked(): Boolean =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CAMERA_ASKED, false)

    private fun markCameraAsked() {
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_CAMERA_ASKED, true).apply()
    }

    private companion object {
        const val REQUEST_CODE = 7160
        const val PREFS = "lock_engine_permissions"
        const val KEY_CAMERA_ASKED = "camera_asked"
    }
}
