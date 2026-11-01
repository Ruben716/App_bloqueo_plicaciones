package io.github.ruben716.lock_engine.intruder

import android.content.Context
import java.io.File

data class IntruderRecord(val file: File, val timestampMs: Long, val packageName: String)

/**
 * Fotos de intrusos guardadas en el almacenamiento PRIVADO de la app (nunca en
 * la galería). Nombre de archivo: `<timestamp>__<paquete>.jpg`.
 */
class IntruderStore(context: Context) {

    private val directory = File(context.applicationContext.filesDir, DIRECTORY)

    fun newFile(packageName: String, nowMs: Long = System.currentTimeMillis()): File {
        directory.mkdirs()
        val safePackage = packageName.replace(Regex("[^A-Za-z0-9._]"), "_")
        return File(directory, "$nowMs$SEPARATOR$safePackage.jpg")
    }

    fun list(): List<IntruderRecord> =
        directory.listFiles { file -> file.isFile && file.name.endsWith(".jpg") && file.length() > 0 }
            .orEmpty()
            .mapNotNull(::parse)
            .sortedByDescending { it.timestampMs }

    fun count(): Int = list().size

    fun delete(path: String): Boolean {
        val file = File(path)
        // Solo se permite borrar archivos dentro del directorio propio.
        if (file.parentFile?.canonicalPath != directory.canonicalPath) return false
        return file.delete()
    }

    fun clear() {
        directory.listFiles().orEmpty().forEach { it.delete() }
    }

    /** Conserva como máximo [MAX_PHOTOS] fotos (borra las más antiguas). */
    fun trim() {
        list().drop(MAX_PHOTOS).forEach { it.file.delete() }
    }

    private fun parse(file: File): IntruderRecord? {
        val parts = file.nameWithoutExtension.split(SEPARATOR, limit = 2)
        val timestamp = parts.getOrNull(0)?.toLongOrNull() ?: return null
        return IntruderRecord(file, timestamp, parts.getOrNull(1).orEmpty())
    }

    private companion object {
        const val DIRECTORY = "intruders"
        const val SEPARATOR = "__"
        const val MAX_PHOTOS = 50
    }
}
