package io.github.ruben716.lock_engine.config

import io.github.ruben716.lock_engine.model.EngineConfig
import io.github.ruben716.lock_engine.model.GuestConfig
import io.github.ruben716.lock_engine.model.Profile
import io.github.ruben716.lock_engine.model.RelockMode
import io.github.ruben716.lock_engine.model.Settings
import io.github.ruben716.lock_engine.model.TimeWindow
import org.json.JSONArray
import org.json.JSONObject

/** Serialización de la configuración con org.json (incluido en Android). */
internal object ConfigJson {

    fun encode(config: EngineConfig): String = JSONObject().apply {
        put("version", 1)
        put("engineEnabled", config.engineEnabled)
        put("settings", encodeSettings(config.settings))
        put("profiles", JSONArray().apply { config.profiles.forEach { put(encodeProfile(it)) } })
        put("guest", JSONObject().apply {
            put("active", config.guest.active)
            put("allowed", JSONArray(config.guest.allowedPackages.toList()))
        })
    }.toString()

    fun decode(json: String?): EngineConfig {
        if (json.isNullOrBlank()) return EngineConfig()
        return runCatching {
            val root = JSONObject(json)
            val profiles = root.optJSONArray("profiles")?.let { array ->
                (0 until array.length()).map { decodeProfile(array.getJSONObject(it)) }
            }.orEmpty()
            val guest = root.optJSONObject("guest")
            EngineConfig(
                engineEnabled = root.optBoolean("engineEnabled", false),
                settings = decodeSettings(root.optJSONObject("settings")),
                profiles = ensureDefaultProfile(profiles),
                guest = GuestConfig(
                    active = guest?.optBoolean("active", false) ?: false,
                    allowedPackages = guest?.optJSONArray("allowed").toStringSet(),
                ),
            )
        }.getOrElse { EngineConfig() }
    }

    fun ensureDefaultProfile(profiles: List<Profile>): List<Profile> =
        if (profiles.any { it.isDefault }) profiles else listOf(Profile.default()) + profiles

    private fun encodeSettings(s: Settings) = JSONObject().apply {
        put("relockMode", s.relockMode.name)
        put("relockTimeoutMinutes", s.relockTimeoutMinutes)
        put("biometricEnabled", s.biometricEnabled)
        put("intruderEnabled", s.intruderEnabled)
        put("intruderThreshold", s.intruderThreshold)
        put("hideNotifications", s.hideNotifications)
        put("antiUninstall", s.antiUninstall)
        put("fakeCrash", s.fakeCrash)
        put("blockScreenshots", s.blockScreenshots)
        put("themeId", s.themeId)
    }

    private fun decodeSettings(o: JSONObject?): Settings {
        val defaults = Settings()
        if (o == null) return defaults
        return Settings(
            relockMode = RelockMode.entries.firstOrNull { it.name == o.optString("relockMode") }
                ?: defaults.relockMode,
            relockTimeoutMinutes = o.optInt("relockTimeoutMinutes", defaults.relockTimeoutMinutes),
            biometricEnabled = o.optBoolean("biometricEnabled", defaults.biometricEnabled),
            intruderEnabled = o.optBoolean("intruderEnabled", defaults.intruderEnabled),
            intruderThreshold = o.optInt("intruderThreshold", defaults.intruderThreshold),
            hideNotifications = o.optBoolean("hideNotifications", defaults.hideNotifications),
            antiUninstall = o.optBoolean("antiUninstall", defaults.antiUninstall),
            fakeCrash = o.optBoolean("fakeCrash", defaults.fakeCrash),
            blockScreenshots = o.optBoolean("blockScreenshots", defaults.blockScreenshots),
            themeId = o.optString("themeId", defaults.themeId),
        )
    }

    private fun encodeProfile(p: Profile) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("packages", JSONArray(p.packages.toList()))
        put("enabled", p.enabled)
        put("isDefault", p.isDefault)
        p.schedule?.let { w ->
            put("schedule", JSONObject().apply {
                put("days", JSONArray(w.days.sorted()))
                put("start", w.startMinute)
                put("end", w.endMinute)
            })
        }
    }

    private fun decodeProfile(o: JSONObject): Profile {
        val schedule = o.optJSONObject("schedule")?.let { w ->
            val days = w.optJSONArray("days")
            TimeWindow(
                days = if (days == null) emptySet() else (0 until days.length()).map { days.getInt(it) }.toSet(),
                startMinute = w.optInt("start", 0),
                endMinute = w.optInt("end", 0),
            )
        }
        return Profile(
            id = o.getString("id"),
            name = o.optString("name", "Perfil"),
            packages = o.optJSONArray("packages").toStringSet(),
            enabled = o.optBoolean("enabled", true),
            isDefault = o.optBoolean("isDefault", false),
            schedule = schedule,
        )
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return (0 until length()).map { getString(it) }.toSet()
    }
}
