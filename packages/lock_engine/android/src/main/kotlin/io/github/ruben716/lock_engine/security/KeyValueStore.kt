package io.github.ruben716.lock_engine.security

import android.content.SharedPreferences

/** Almacenamiento clave-valor mínimo para poder probar la lógica en la JVM. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun getLong(key: String, default: Long): Long
    fun edit(block: Editor.() -> Unit)

    interface Editor {
        fun putString(key: String, value: String?)
        fun putLong(key: String, value: Long)
    }
}

class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        val editor = prefs.edit()
        object : KeyValueStore.Editor {
            override fun putString(key: String, value: String?) {
                if (value == null) editor.remove(key) else editor.putString(key, value)
            }

            override fun putLong(key: String, value: Long) {
                editor.putLong(key, value)
            }
        }.block()
        // commit() síncrono: el estado de seguridad no puede perderse si el
        // proceso muere justo después de un intento fallido.
        editor.commit()
    }
}
