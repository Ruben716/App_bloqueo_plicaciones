package io.github.ruben716.lock_engine.guest

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.ruben716.lock_engine.EngineGraph
import io.github.ruben716.lock_engine.LaunchRoutes

/**
 * Mosaico de Ajustes rápidos para el modo invitado.
 * - Activarlo es inmediato (un toque antes de prestar el móvil).
 * - Desactivarlo abre la app, que exige la credencial propia.
 */
class GuestModeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val graph = EngineGraph.get(this)
        val config = graph.config.current
        if (!graph.credentials.hasCredential() || !config.engineEnabled) {
            openApp(null)
            return
        }
        if (config.guest.active) {
            openApp(LaunchRoutes.GUEST_MODE)
        } else {
            graph.setGuestMode(true)
            updateTile()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val active = EngineGraph.get(this).config.current.guest.active
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp(route: String?) {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (route != null) launch.putExtra(LaunchRoutes.EXTRA_ROUTE, route)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                route.hashCode(),
                launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launch)
        }
    }
}
