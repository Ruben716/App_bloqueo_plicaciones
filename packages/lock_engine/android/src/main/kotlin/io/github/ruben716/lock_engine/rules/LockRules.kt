package io.github.ruben716.lock_engine.rules

import io.github.ruben716.lock_engine.model.EngineConfig
import io.github.ruben716.lock_engine.model.Profile
import io.github.ruben716.lock_engine.model.SystemPackages

/**
 * Calcula qué apps deben estar bloqueadas en un instante dado.
 *
 * Bloqueadas = unión de los perfiles activos
 *            + objetivos anti-desinstalación (si está activado)
 *            + todas las apps excepto las permitidas (si el modo invitado está activo)
 *            - paquetes que nunca se bloquean.
 */
object LockRules {

    fun activeProfiles(config: EngineConfig, day: Int, minute: Int): List<Profile> =
        config.profiles.filter { profile ->
            profile.enabled && ScheduleEvaluator.isActive(profile.schedule, day, minute)
        }

    fun effectiveLocked(
        config: EngineConfig,
        system: SystemPackages,
        day: Int,
        minute: Int,
    ): Set<String> {
        if (!config.engineEnabled) return emptySet()

        val locked = HashSet<String>()
        activeProfiles(config, day, minute).forEach { locked += it.packages }

        if (config.settings.antiUninstall) {
            locked += system.antiUninstallTargets
        }

        if (config.guest.active) {
            val allowed = config.guest.allowedPackages
            locked += system.launcherApps.filterNot { it in allowed }
            locked += system.antiUninstallTargets.filterNot { it in allowed }
        }

        locked.removeAll(system.neverLock)
        return locked
    }
}
