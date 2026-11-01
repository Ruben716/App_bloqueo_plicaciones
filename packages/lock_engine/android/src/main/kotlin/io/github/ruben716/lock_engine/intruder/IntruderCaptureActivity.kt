package io.github.ruben716.lock_engine.intruder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import io.github.ruben716.lock_engine.EngineGraph
import io.github.ruben716.lock_engine.model.LogKind

/**
 * Actividad invisible que toma la foto del intruso.
 *
 * Android solo permite usar la cámara a apps en primer plano, así que se lanza
 * una actividad transparente por debajo del overlay de bloqueo (el usuario no
 * ve nada) y se cierra sola al terminar.
 */
class IntruderCaptureActivity : Activity() {

    private var camera: IntruderCamera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            finishSilently()
            return
        }

        val graph = EngineGraph.get(this)
        val output = graph.intruders.newFile(packageName)
        camera = IntruderCamera(this).also { cam ->
            cam.capture(output) { success ->
                if (success) {
                    graph.intruders.trim()
                    graph.log.add(LogKind.INTRUDER_CAPTURED, packageName)
                } else {
                    output.delete()
                }
                runOnUiThread { finishSilently() }
            }
        }
    }

    override fun onDestroy() {
        camera?.release()
        camera = null
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
            Intent(context, IntruderCaptureActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                )
    }
}
