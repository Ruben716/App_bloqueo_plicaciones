package io.github.ruben716.lock_engine.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.WindowManager
import io.github.ruben716.lock_engine.EngineGraph
import io.github.ruben716.lock_engine.biometric.BiometricActivity
import io.github.ruben716.lock_engine.biometric.BiometricAvailability
import io.github.ruben716.lock_engine.model.CredentialType
import java.util.concurrent.Executors

/**
 * Muestra/oculta la pantalla de bloqueo como ventana superpuesta
 * (TYPE_APPLICATION_OVERLAY). Todas las operaciones de vista ocurren en el
 * hilo principal; la verificación del PIN (PBKDF2) en un hilo aparte.
 */
class LockOverlayController(context: Context, private val graph: EngineGraph) {

    private val appContext = context.applicationContext
    private val themedContext = ContextThemeWrapper(appContext, android.R.style.Theme_DeviceDefault_NoActionBar)
    private val main = Handler(Looper.getMainLooper())
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val authExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "lock-engine-auth").apply { isDaemon = true }
    }

    private var view: LockScreenView? = null
    private var shownFor: String? = null

    fun show(packageName: String) {
        main.post { showInternal(packageName) }
    }

    fun hide() {
        main.post { hideInternal() }
    }

    private fun showInternal(packageName: String) {
        if (shownFor == packageName && view != null) return
        val credentialType = graph.credentials.type()
        if (credentialType == null || !Settings.canDrawOverlays(appContext)) {
            graph.coordinator.onOverlayUnavailable()
            return
        }
        hideInternal()

        val settings = graph.config.current.settings
        val biometricAvailable = settings.biometricEnabled &&
            graph.biometrics.availability() == BiometricAvailability.AVAILABLE &&
            graph.biometrics.hasKey()
        val model = LockScreenModel(
            packageName = packageName,
            appLabel = graph.catalog.label(packageName),
            appIcon = runCatching { appContext.packageManager.getApplicationIcon(packageName) }.getOrNull(),
            credentialType = credentialType,
            pinLength = graph.credentials.pinLength(),
            biometricAvailable = biometricAvailable,
            fakeCrash = settings.fakeCrash,
            theme = LockTheme.byId(settings.themeId),
        )
        val lockView = LockScreenView(themedContext, model, listenerFor(packageName))
        try {
            windowManager.addView(lockView, layoutParams(settings.blockScreenshots))
        } catch (_: Exception) {
            graph.coordinator.onOverlayUnavailable()
            return
        }
        view = lockView
        shownFor = packageName

        val lockoutMs = graph.credentials.lockoutRemainingMs()
        if (lockoutMs > 0) lockView.startLockout(lockoutMs)
        if (!settings.fakeCrash) {
            if (credentialType == CredentialType.PASSWORD) lockView.requestPasswordFocus()
            if (biometricAvailable && lockoutMs == 0L) {
                main.postDelayed({ if (shownFor == packageName) launchBiometric(packageName) }, BIOMETRIC_DELAY_MS)
            }
        }
    }

    private fun hideInternal() {
        val current = view ?: return
        runCatching { windowManager.removeViewImmediate(current) }
        view = null
        shownFor = null
    }

    private fun listenerFor(packageName: String) = object : LockScreenView.Listener {
        override fun onSubmit(secret: String) {
            authExecutor.execute {
                val outcome = graph.credentials.verify(secret)
                main.post {
                    if (shownFor != packageName) return@post
                    if (outcome.success) {
                        graph.coordinator.onUnlockSucceeded(packageName, viaBiometric = false)
                    } else {
                        view?.showFailure(outcome.lockoutRemainingMs)
                        if (!outcome.throttled) graph.onFailedAttempt(packageName, outcome)
                    }
                }
            }
        }

        override fun onBiometricRequested() = launchBiometric(packageName)

        override fun onBackPressed() = graph.coordinator.goHome()
    }

    private fun launchBiometric(packageName: String) {
        runCatching { appContext.startActivity(BiometricActivity.intent(appContext, packageName)) }
    }

    private fun layoutParams(secure: Boolean): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        if (secure) flags = flags or WindowManager.LayoutParams.FLAG_SECURE
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.OPAQUE,
        ).apply {
            title = "LockEngineOverlay"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = 0
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                @Suppress("DEPRECATION")
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
        }
    }

    private companion object {
        const val BIOMETRIC_DELAY_MS = 250L
    }
}
