package io.github.ruben716.lock_engine.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.ruben716.lock_engine.EngineGraph

/** Reinicia el motor tras reiniciar el teléfono o actualizar la app. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                EngineGraph.get(context).syncServiceState()
        }
    }
}
