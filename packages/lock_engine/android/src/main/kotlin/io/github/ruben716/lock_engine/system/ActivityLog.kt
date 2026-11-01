package io.github.ruben716.lock_engine.system

import android.content.Context
import io.github.ruben716.lock_engine.model.LogEntry
import io.github.ruben716.lock_engine.model.LogKind
import org.json.JSONArray
import org.json.JSONObject

/** Historial local de eventos (sin contenido sensible), limitado a [MAX_ENTRIES]. */
class ActivityLog(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun add(kind: LogKind, packageName: String? = null, nowMs: Long = System.currentTimeMillis()) {
        val entries = (listOf(LogEntry(nowMs, kind, packageName)) + read()).take(MAX_ENTRIES)
        prefs.edit().putString(KEY, encode(entries)).apply()
    }

    @Synchronized
    fun entries(): List<LogEntry> = read()

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private fun read(): List<LogEntry> = runCatching {
        val array = JSONArray(prefs.getString(KEY, "[]"))
        (0 until array.length()).mapNotNull { index ->
            val o = array.getJSONObject(index)
            val kind = LogKind.entries.firstOrNull { it.name == o.optString("k") } ?: return@mapNotNull null
            LogEntry(
                timestampMs = o.optLong("t"),
                kind = kind,
                packageName = if (o.has("p")) o.getString("p") else null,
            )
        }
    }.getOrDefault(emptyList())

    private fun encode(entries: List<LogEntry>): String = JSONArray().apply {
        entries.forEach { entry ->
            put(JSONObject().apply {
                put("t", entry.timestampMs)
                put("k", entry.kind.name)
                entry.packageName?.let { put("p", it) }
            })
        }
    }.toString()

    private companion object {
        const val PREFS_NAME = "lock_engine_activity"
        const val KEY = "entries"
        const val MAX_ENTRIES = 200
    }
}
