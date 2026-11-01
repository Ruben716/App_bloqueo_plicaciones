package io.github.ruben716.lock_engine.config

import android.content.Context
import io.github.ruben716.lock_engine.model.EngineConfig
import io.github.ruben716.lock_engine.model.Profile
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Fuente única de verdad de la configuración. La escribe Flutter (vía Pigeon)
 * y la lee el servicio nativo, que funciona aunque Flutter no esté cargado.
 */
class ConfigStore(context: Context) {

    fun interface Listener {
        fun onConfigChanged(config: EngineConfig)
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val listeners = CopyOnWriteArrayList<Listener>()

    @Volatile
    var current: EngineConfig = ConfigJson.decode(prefs.getString(KEY_CONFIG, null))
        private set

    fun update(transform: (EngineConfig) -> EngineConfig): EngineConfig {
        val updated: EngineConfig
        synchronized(this) {
            val next = transform(current)
            updated = next.copy(profiles = ConfigJson.ensureDefaultProfile(next.profiles))
            current = updated
            prefs.edit().putString(KEY_CONFIG, ConfigJson.encode(updated)).apply()
        }
        listeners.forEach { it.onConfigChanged(updated) }
        return updated
    }

    fun setAppLocked(packageName: String, locked: Boolean) = update { config ->
        config.copy(
            profiles = config.profiles.map { profile ->
                if (!profile.isDefault) {
                    profile
                } else {
                    profile.copy(
                        packages = if (locked) profile.packages + packageName else profile.packages - packageName,
                    )
                }
            },
        )
    }

    fun saveProfile(profile: Profile) = update { config ->
        val existing = config.profiles.indexOfFirst { it.id == profile.id }
        val sanitized = if (profile.id == Profile.DEFAULT_ID) {
            profile.copy(isDefault = true, enabled = true, schedule = null)
        } else {
            profile.copy(isDefault = false)
        }
        val profiles = config.profiles.toMutableList()
        if (existing >= 0) profiles[existing] = sanitized else profiles += sanitized
        config.copy(profiles = profiles)
    }

    fun deleteProfile(id: String) = update { config ->
        config.copy(profiles = config.profiles.filterNot { it.id == id && !it.isDefault })
    }

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    private companion object {
        const val PREFS_NAME = "lock_engine_config"
        const val KEY_CONFIG = "config_json"
    }
}
