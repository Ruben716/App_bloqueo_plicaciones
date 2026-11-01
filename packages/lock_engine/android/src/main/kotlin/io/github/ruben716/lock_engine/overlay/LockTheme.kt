package io.github.ruben716.lock_engine.overlay

import android.graphics.Color

/**
 * Temas de la pantalla de bloqueo. Deben coincidir con los de
 * `packages/design_system/lib/src/tokens/lock_themes.dart`.
 */
data class LockTheme(
    val id: String,
    val backgroundTop: Int,
    val backgroundBottom: Int,
    val accent: Int,
) {
    companion object {
        val ALL = listOf(
            LockTheme("aurora", Color.parseColor("#1E1B4B"), Color.parseColor("#4C1D95"), Color.parseColor("#A78BFA")),
            LockTheme("ocean", Color.parseColor("#082F49"), Color.parseColor("#134E4A"), Color.parseColor("#38BDF8")),
            LockTheme("forest", Color.parseColor("#052E16"), Color.parseColor("#14532D"), Color.parseColor("#4ADE80")),
            LockTheme("sunset", Color.parseColor("#431407"), Color.parseColor("#831843"), Color.parseColor("#FB923C")),
            LockTheme("midnight", Color.parseColor("#020617"), Color.parseColor("#1E293B"), Color.parseColor("#94A3B8")),
        )

        fun byId(id: String): LockTheme = ALL.firstOrNull { it.id == id } ?: ALL.first()
    }
}
