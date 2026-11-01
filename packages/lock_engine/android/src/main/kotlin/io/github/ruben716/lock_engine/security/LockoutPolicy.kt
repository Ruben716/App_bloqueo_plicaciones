package io.github.ruben716.lock_engine.security

/**
 * Bloqueo progresivo contra fuerza bruta.
 * Los primeros [FREE_ATTEMPTS] - 1 fallos no tienen espera; a partir del
 * fallo número [FREE_ATTEMPTS] la espera empieza en 30 s y se duplica en cada
 * fallo adicional, hasta un máximo de 15 minutos.
 */
object LockoutPolicy {
    const val FREE_ATTEMPTS = 5
    const val BASE_LOCKOUT_MS = 30_000L
    const val MAX_LOCKOUT_MS = 15 * 60_000L

    fun lockoutMsFor(failedAttempts: Int): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0L
        val exponent = (failedAttempts - FREE_ATTEMPTS).coerceAtMost(16)
        return (BASE_LOCKOUT_MS shl exponent).coerceAtMost(MAX_LOCKOUT_MS)
    }

    /** Protege frente a cambios del reloj: nunca se espera más del máximo. */
    fun remainingMs(lockoutUntilMs: Long, nowMs: Long): Long =
        (lockoutUntilMs - nowMs).coerceIn(0L, MAX_LOCKOUT_MS)
}
