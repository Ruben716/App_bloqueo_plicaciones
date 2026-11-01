package io.github.ruben716.lock_engine.rules

import io.github.ruben716.lock_engine.model.RelockMode
import kotlin.test.Test
import kotlin.test.assertEquals

class LockDeciderTest {

    private val locked = setOf("chat")
    private val transient = setOf("own", "systemui")
    private val minute = 60_000L

    private fun LockDecider.tick(fg: String?, now: Long, mode: RelockMode = RelockMode.IMMEDIATELY, timeout: Long = 5 * minute) =
        onTick(fg, transient, locked, mode, timeout, now)

    @Test
    fun `abrir app bloqueada muestra overlay una sola vez`() {
        val decider = LockDecider()
        assertEquals(OverlayCommand.Show("chat"), decider.tick("chat", 0))
        assertEquals(OverlayCommand.None, decider.tick("chat", 250))
    }

    @Test
    fun `app no bloqueada no hace nada`() {
        assertEquals(OverlayCommand.None, LockDecider().tick("browser", 0))
    }

    @Test
    fun `salir al launcher oculta el overlay`() {
        val decider = LockDecider()
        decider.tick("chat", 0)
        assertEquals(OverlayCommand.Hide, decider.tick("launcher", 100))
    }

    @Test
    fun `paquetes transitorios no cuentan como salir`() {
        val decider = LockDecider()
        decider.tick("chat", 0)
        decider.onUnlocked("chat")
        assertEquals(OverlayCommand.None, decider.tick("own", 100))
        assertEquals(OverlayCommand.None, decider.tick("chat", 200))
    }

    @Test
    fun `IMMEDIATELY vuelve a bloquear al salir y entrar`() {
        val decider = LockDecider()
        decider.tick("chat", 0)
        decider.onUnlocked("chat")
        assertEquals(OverlayCommand.None, decider.tick("chat", 100))
        decider.tick("launcher", 200)
        assertEquals(OverlayCommand.Show("chat"), decider.tick("chat", 300))
    }

    @Test
    fun `AFTER_TIMEOUT respeta el tiempo de gracia`() {
        val mode = RelockMode.AFTER_TIMEOUT
        val decider = LockDecider()
        decider.tick("chat", 0, mode)
        decider.onUnlocked("chat")
        decider.tick("chat", 10, mode)
        decider.tick("launcher", minute, mode)
        assertEquals(OverlayCommand.None, decider.tick("chat", 3 * minute, mode))
        decider.tick("launcher", 4 * minute, mode)
        assertEquals(OverlayCommand.Show("chat"), decider.tick("chat", 10 * minute, mode))
    }

    @Test
    fun `ON_SCREEN_OFF mantiene la sesion hasta apagar la pantalla`() {
        val mode = RelockMode.ON_SCREEN_OFF
        val decider = LockDecider()
        decider.tick("chat", 0, mode)
        decider.onUnlocked("chat")
        decider.tick("launcher", 100, mode)
        assertEquals(OverlayCommand.None, decider.tick("chat", 60 * minute, mode))
        decider.onScreenOff()
        assertEquals(OverlayCommand.Show("chat"), decider.tick("chat", 61 * minute, mode))
    }

    @Test
    fun `si la app deja de estar bloqueada se oculta el overlay`() {
        val decider = LockDecider()
        decider.tick("chat", 0)
        assertEquals(OverlayCommand.Hide, decider.onTick("chat", transient, emptySet(), RelockMode.IMMEDIATELY, 0, 100))
    }
}
