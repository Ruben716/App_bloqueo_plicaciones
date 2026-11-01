package io.github.ruben716.lock_engine.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager
import io.github.ruben716.lock_engine.model.SystemPackages
import java.io.ByteArrayOutputStream

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val canLock: Boolean,
    val isRecommended: Boolean,
)

/**
 * Catálogo de apps instaladas. Usa `<queries>` con la categoría LAUNCHER en
 * lugar de QUERY_ALL_PACKAGES (permiso muy restringido en Google Play).
 */
class AppCatalog(context: Context) {

    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val ownPackage = appContext.packageName

    @Volatile
    private var cachedSystem: SystemPackages? = null

    @Volatile
    private var cachedAtMs = 0L

    private val labelCache = HashMap<String, String>()

    fun installedApps(): List<AppInfo> {
        val system = systemPackages(forceRefresh = true)
        return launcherActivities()
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != ownPackage }
            .map { info ->
                val isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                    info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
                AppInfo(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    isSystem = isSystem,
                    canLock = info.packageName !in system.neverLock,
                    isRecommended = isRecommended(info, system),
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }

    /** Paquetes del sistema, cacheados 60 s (los usa el bucle del monitor). */
    fun systemPackages(forceRefresh: Boolean = false): SystemPackages {
        val now = System.currentTimeMillis()
        val cached = cachedSystem
        if (!forceRefresh && cached != null && now - cachedAtMs < CACHE_TTL_MS) return cached

        val launcher = launcherActivities().map { it.activityInfo.packageName }.toSet()
        val home = queryPackages(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        val settings = queryPackages(Intent(Settings.ACTION_SETTINGS)).ifEmpty { setOf(SETTINGS) }
        val keyboards = inputMethodPackages()
        val dialer = defaultDialer()

        val neverLock = buildSet {
            add(ownPackage)
            add(SYSTEM_UI)
            addAll(home)
            addAll(keyboards)
            dialer?.let(::add)
        }
        val transient = buildSet {
            add(ownPackage)
            add(SYSTEM_UI)
            add(ANDROID)
            addAll(INTENT_RESOLVERS)
            addAll(PERMISSION_CONTROLLERS)
            addAll(keyboards)
        }
        val antiUninstall = buildSet {
            addAll(settings)
            add(PLAY_STORE)
            addAll(PACKAGE_INSTALLERS)
        }

        return SystemPackages(
            launcherApps = launcher,
            antiUninstallTargets = antiUninstall,
            neverLock = neverLock,
            transient = transient,
        ).also {
            cachedSystem = it
            cachedAtMs = now
        }
    }

    fun label(packageName: String): String = synchronized(labelCache) {
        labelCache.getOrPut(packageName) {
            runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault(packageName)
        }
    }

    fun iconPng(packageName: String, sizePx: Int): ByteArray? = runCatching {
        val drawable = pm.getApplicationIcon(packageName)
        val size = sizePx.coerceIn(24, 512)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }.getOrNull()

    fun launchIntent(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)

    @Suppress("DEPRECATION")
    private fun launcherActivities() =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)

    @Suppress("DEPRECATION")
    private fun queryPackages(intent: Intent): Set<String> =
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .toSet()

    private fun inputMethodPackages(): Set<String> = runCatching {
        appContext.getSystemService(InputMethodManager::class.java)
            ?.inputMethodList
            ?.map { it.packageName }
            ?.toSet()
    }.getOrNull().orEmpty()

    private fun defaultDialer(): String? = runCatching {
        appContext.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
    }.getOrNull()

    private fun isRecommended(info: ApplicationInfo, system: SystemPackages): Boolean {
        if (info.packageName in RECOMMENDED || info.packageName in system.antiUninstallTargets) return true
        return info.category == ApplicationInfo.CATEGORY_SOCIAL ||
            info.category == ApplicationInfo.CATEGORY_IMAGE
    }

    private companion object {
        const val CACHE_TTL_MS = 60_000L
        const val ANDROID = "android"
        const val SYSTEM_UI = "com.android.systemui"
        const val SETTINGS = "com.android.settings"
        const val PLAY_STORE = "com.android.vending"
        val PACKAGE_INSTALLERS = setOf("com.google.android.packageinstaller", "com.android.packageinstaller")
        val INTENT_RESOLVERS = setOf("com.android.intentresolver")
        val PERMISSION_CONTROLLERS = setOf(
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
        )
        val RECOMMENDED = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.thoughtcrime.securesms",
            "com.instagram.android",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.zhiliaoapp.musically",
            "com.snapchat.android",
            "com.twitter.android",
            "com.google.android.gm",
            "com.google.android.apps.photos",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.sec.android.gallery3d",
            "com.miui.gallery",
            "com.google.android.apps.docs",
            "com.discord",
            "com.tinder",
        )
    }
}
