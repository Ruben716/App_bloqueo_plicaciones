package io.github.ruben716.lock_engine

import android.content.Intent
import android.view.WindowManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry
import io.github.ruben716.lock_engine.api.LockEngineApiImpl
import io.github.ruben716.lock_engine.api.LockEngineHostApi

/** Punto de entrada del plugin: conecta la API Pigeon con el motor nativo. */
class LockEnginePlugin : FlutterPlugin, ActivityAware {

    private var graph: EngineGraph? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var pendingRoute: String? = null

    private val newIntentListener = PluginRegistry.NewIntentListener { intent ->
        captureRoute(intent)
        false
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val engineGraph = EngineGraph.get(binding.applicationContext)
        graph = engineGraph
        LockEngineHostApi.setUp(
            binding.binaryMessenger,
            LockEngineApiImpl(
                graph = engineGraph,
                activityProvider = { activityBinding?.activity },
                routeProvider = ::consumeRoute,
                onSettingsChanged = ::applySecureFlag,
            ),
        )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        LockEngineHostApi.setUp(binding.binaryMessenger, null)
        graph = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        binding.addOnNewIntentListener(newIntentListener)
        captureRoute(binding.activity.intent)
        applySecureFlag()
    }

    override fun onDetachedFromActivityForConfigChanges() = onDetachedFromActivity()

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) = onAttachedToActivity(binding)

    override fun onDetachedFromActivity() {
        activityBinding?.removeOnNewIntentListener(newIntentListener)
        activityBinding = null
    }

    private fun captureRoute(intent: Intent?) {
        val route = intent?.getStringExtra(LaunchRoutes.EXTRA_ROUTE) ?: return
        pendingRoute = route
        intent.removeExtra(LaunchRoutes.EXTRA_ROUTE)
    }

    private fun consumeRoute(): String? = pendingRoute.also { pendingRoute = null }

    /** Impide capturas de pantalla y oculta la miniatura en "Recientes". */
    private fun applySecureFlag() {
        val activity = activityBinding?.activity ?: return
        val secure = graph?.config?.current?.settings?.blockScreenshots ?: true
        if (secure) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
