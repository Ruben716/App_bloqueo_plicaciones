package io.github.ruben716.lock_engine.biometric

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.view.WindowManager
import io.github.ruben716.lock_engine.EngineGraph
import io.github.ruben716.lock_engine.R
import io.github.ruben716.lock_engine.model.LogKind

/**
 * El diálogo biométrico del sistema necesita una Activity en primer plano.
 * Esta actividad opaca (mismo fondo que la pantalla de bloqueo) se abre sobre
 * la app protegida, muestra el diálogo y se cierra en cuanto hay resultado.
 */
class BiometricActivity : Activity() {

    private var cancellation: CancellationSignal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        if (packageName == null) {
            finishSilently()
            return
        }

        val graph = EngineGraph.get(this)
        cancellation = graph.biometrics.authenticate(
            activity = this,
            title = getString(R.string.lock_engine_biometric_title, graph.catalog.label(packageName)),
            subtitle = getString(R.string.lock_engine_biometric_subtitle),
            negativeText = graph.credentialFallbackLabel(),
        ) { outcome ->
            when (outcome) {
                BiometricOutcome.SUCCESS -> graph.coordinator.onUnlockSucceeded(packageName, viaBiometric = true)
                BiometricOutcome.INVALIDATED -> graph.onBiometricInvalidated()
                BiometricOutcome.CANCELLED, BiometricOutcome.ERROR -> Unit
            }
            finishSilently()
        }
    }

    override fun onDestroy() {
        cancellation?.cancel()
        cancellation = null
        super.onDestroy()
    }

    private fun finishSilently() {
        if (isFinishing) return
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val EXTRA_PACKAGE = "lock_engine.extra.PACKAGE"

        fun intent(context: Context, packageName: String): Intent =
            Intent(context, BiometricActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                )
    }
}
