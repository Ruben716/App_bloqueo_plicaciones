package io.github.ruben716.lock_engine.rules

import io.github.ruben716.lock_engine.model.EngineConfig
import io.github.ruben716.lock_engine.model.GuestConfig
import io.github.ruben716.lock_engine.model.Profile
import io.github.ruben716.lock_engine.model.Settings
import io.github.ruben716.lock_engine.model.SystemPackages
import io.github.ruben716.lock_engine.model.TimeWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LockRulesTest {

    private val system = SystemPackages(
        launcherApps = setOf("com.whatsapp", "com.instagram.android", "com.android.chrome", "com.launcher", "com.dialer"),
        antiUninstallTargets = setOf("com.android.settings", "com.android.vending"),
        neverLock = setOf("com.launcher", "com.dialer", "io.sigilo"),
        transient = setOf("io.sigilo"),
    )

    private val base = EngineConfig(
        engineEnabled = true,
        profiles = listOf(Profile.default().copy(packages = setOf("com.whatsapp"))),
    )

    @Test
    fun `motor desactivado no bloquea nada`() {
        val locked = LockRules.effectiveLocked(base.copy(engineEnabled = false), system, 1, 600)
        assertTrue(locked.isEmpty())
    }

    @Test
    fun `perfil por defecto siempre activo`() {
        assertEquals(setOf("com.whatsapp"), LockRules.effectiveLocked(base, system, 3, 0))
    }

    @Test
    fun `perfil con horario solo suma dentro de la ventana`() {
        val work = Profile("work", "Trabajo", setOf("com.instagram.android"), true, false, TimeWindow(setOf(1), 540, 1080))
        val config = base.copy(profiles = base.profiles + work)
        assertEquals(setOf("com.whatsapp", "com.instagram.android"), LockRules.effectiveLocked(config, system, 1, 600))
        assertEquals(setOf("com.whatsapp"), LockRules.effectiveLocked(config, system, 1, 1200))
        assertEquals(setOf("com.whatsapp"), LockRules.effectiveLocked(config, system, 2, 600))
    }

    @Test
    fun `perfil desactivado se ignora`() {
        val off = Profile("x", "X", setOf("com.android.chrome"), false, false, null)
        assertEquals(setOf("com.whatsapp"), LockRules.effectiveLocked(base.copy(profiles = base.profiles + off), system, 1, 0))
    }

    @Test
    fun `anti desinstalacion protege ajustes y tienda`() {
        val config = base.copy(settings = Settings(antiUninstall = true))
        assertEquals(
            setOf("com.whatsapp", "com.android.settings", "com.android.vending"),
            LockRules.effectiveLocked(config, system, 1, 0),
        )
    }

    @Test
    fun `modo invitado bloquea todo salvo lo permitido y lo esencial`() {
        val config = base.copy(guest = GuestConfig(active = true, allowedPackages = setOf("com.android.chrome")))
        assertEquals(
            setOf("com.whatsapp", "com.instagram.android", "com.android.settings", "com.android.vending"),
            LockRules.effectiveLocked(config, system, 1, 0),
        )
    }

    @Test
    fun `nunca se bloquean launcher ni telefono aunque se elijan`() {
        val config = base.copy(profiles = listOf(Profile.default().copy(packages = setOf("com.launcher", "com.dialer"))))
        assertTrue(LockRules.effectiveLocked(config, system, 1, 0).isEmpty())
    }
}
