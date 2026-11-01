package io.github.ruben716.lock_engine.intruder

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Toma una foto silenciosa con la cámara frontal usando Camera2 sin vista
 * previa visible. Una secuencia corta de fotogramas YUV permite que la
 * exposición automática se estabilice antes de la captura JPEG.
 */
class IntruderCamera(context: Context) {

    private val manager = context.getSystemService(CameraManager::class.java)
    private val thread = HandlerThread("lock-engine-intruder").apply { start() }
    private val handler = Handler(thread.looper)
    private val finished = AtomicBoolean(false)

    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var previewReader: ImageReader? = null
    private var jpegReader: ImageReader? = null
    private var onDone: ((Boolean) -> Unit)? = null

    /** Requiere el permiso CAMERA y que la app esté en primer plano. */
    @SuppressLint("MissingPermission")
    fun capture(output: File, onDone: (Boolean) -> Unit) {
        this.onDone = onDone
        handler.postDelayed({ finish(false) }, TIMEOUT_MS)
        try {
            val cameraId = frontCameraId() ?: return finish(false)
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return finish(false)
            val jpegSize = pickSize(map.getOutputSizes(ImageFormat.JPEG), MAX_JPEG_PIXELS) ?: return finish(false)
            val previewSize = pickSize(map.getOutputSizes(ImageFormat.YUV_420_888), MAX_PREVIEW_PIXELS)
                ?: return finish(false)
            val orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 270

            val preview = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 2)
            preview.setOnImageAvailableListener({ reader -> reader.acquireLatestImage()?.close() }, handler)
            previewReader = preview

            val jpeg = ImageReader.newInstance(jpegSize.width, jpegSize.height, ImageFormat.JPEG, 1)
            jpeg.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val saved = runCatching {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining()).also(buffer::get)
                    output.writeBytes(bytes)
                }.isSuccess
                image.close()
                finish(saved)
            }, handler)
            jpegReader = jpeg

            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    device = camera
                    startSession(camera, preview, jpeg, orientation)
                }

                override fun onDisconnected(camera: CameraDevice) = finish(false)

                override fun onError(camera: CameraDevice, error: Int) = finish(false)
            }, handler)
        } catch (_: Exception) {
            finish(false)
        }
    }

    @Suppress("DEPRECATION")
    private fun startSession(camera: CameraDevice, preview: ImageReader, jpeg: ImageReader, orientation: Int) {
        try {
            camera.createCaptureSession(
                listOf(preview.surface, jpeg.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(captureSession: CameraCaptureSession) {
                        session = captureSession
                        try {
                            val warmUp = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                addTarget(preview.surface)
                                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                            }
                            captureSession.setRepeatingRequest(warmUp.build(), null, handler)
                            handler.postDelayed({ takeStill(camera, captureSession, jpeg, orientation) }, WARM_UP_MS)
                        } catch (_: Exception) {
                            finish(false)
                        }
                    }

                    override fun onConfigureFailed(captureSession: CameraCaptureSession) = finish(false)
                },
                handler,
            )
        } catch (_: Exception) {
            finish(false)
        }
    }

    private fun takeStill(camera: CameraDevice, captureSession: CameraCaptureSession, jpeg: ImageReader, orientation: Int) {
        if (finished.get()) return
        try {
            val still = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(jpeg.surface)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.JPEG_ORIENTATION, orientation)
                set(CaptureRequest.JPEG_QUALITY, 85.toByte())
            }
            captureSession.stopRepeating()
            captureSession.capture(still.build(), null, handler)
        } catch (_: Exception) {
            finish(false)
        }
    }

    private fun frontCameraId(): String? = manager.cameraIdList.firstOrNull { id ->
        manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
            CameraCharacteristics.LENS_FACING_FRONT
    }

    /** El tamaño más grande que no supere [maxPixels] (o el más pequeño disponible). */
    private fun pickSize(sizes: Array<Size>?, maxPixels: Int): Size? {
        if (sizes.isNullOrEmpty()) return null
        return sizes.filter { it.width * it.height <= maxPixels }.maxByOrNull { it.width * it.height }
            ?: sizes.minByOrNull { it.width * it.height }
    }

    private fun finish(success: Boolean) {
        if (!finished.compareAndSet(false, true)) return
        runCatching { session?.close() }
        runCatching { device?.close() }
        runCatching { previewReader?.close() }
        runCatching { jpegReader?.close() }
        session = null
        device = null
        val callback = onDone
        onDone = null
        callback?.invoke(success)
        thread.quitSafely()
    }

    fun release() = handler.post { finish(false) }

    private companion object {
        const val TIMEOUT_MS = 6_000L
        const val WARM_UP_MS = 700L
        const val MAX_JPEG_PIXELS = 1920 * 1080
        const val MAX_PREVIEW_PIXELS = 640 * 480
    }
}
