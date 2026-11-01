package io.github.ruben716.lock_engine.security

import io.github.ruben716.lock_engine.model.CredentialType

/** Reglas mínimas de calidad de la credencial. Devuelve mensajes en español. */
object CredentialPolicy {

    const val PIN_MIN = 4
    const val PIN_MAX = 12
    const val PASSWORD_MIN = 6
    const val PASSWORD_MAX = 64
    const val PATTERN_MIN_NODES = 4

    /** @return mensaje de error o `null` si la credencial es válida. */
    fun validate(type: CredentialType, secret: String): String? = when (type) {
        CredentialType.PIN -> validatePin(secret)
        CredentialType.PASSWORD -> validatePassword(secret)
        CredentialType.PATTERN -> validatePattern(secret)
    }

    private fun validatePin(pin: String): String? {
        if (!pin.all { it in '0'..'9' }) return "El PIN solo puede contener números"
        if (pin.length !in PIN_MIN..PIN_MAX) return "El PIN debe tener entre $PIN_MIN y $PIN_MAX dígitos"
        if (pin.toSet().size == 1) return "Evita repetir el mismo dígito"
        if (isSequential(pin)) return "Evita secuencias como 1234"
        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.length < PASSWORD_MIN) return "La contraseña debe tener al menos $PASSWORD_MIN caracteres"
        if (password.length > PASSWORD_MAX) return "La contraseña es demasiado larga"
        if (password.isBlank()) return "La contraseña no puede estar vacía"
        return null
    }

    private fun validatePattern(pattern: String): String? {
        if (pattern.isEmpty() || !pattern.all { it in '0'..'8' }) return "Patrón inválido"
        if (pattern.toSet().size != pattern.length) return "Patrón inválido"
        if (pattern.length < PATTERN_MIN_NODES) return "Une al menos $PATTERN_MIN_NODES puntos"
        return null
    }

    private fun isSequential(pin: String): Boolean {
        if (pin.length < 3) return false
        val diffs = pin.zipWithNext { a, b -> b - a }.toSet()
        return diffs == setOf(1) || diffs == setOf(-1)
    }
}
