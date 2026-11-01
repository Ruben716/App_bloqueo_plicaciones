package io.github.ruben716.lock_engine.security

import java.security.SecureRandom

/**
 * Código de recuperación de 12 caracteres (60 bits de entropía) con un
 * alfabeto sin caracteres ambiguos (sin 0/O ni 1/I). Formato: XXXX-XXXX-XXXX.
 * Permite restablecer la credencial sin depender del bloqueo del sistema.
 */
object RecoveryCodes {
    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val GROUPS = 3
    private const val GROUP_SIZE = 4

    fun generate(random: SecureRandom = SecureRandom()): String =
        (0 until GROUPS).joinToString("-") {
            buildString {
                repeat(GROUP_SIZE) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
            }
        }

    /** Quita guiones/espacios y pasa a mayúsculas para comparar. */
    fun normalize(input: String): String =
        input.uppercase().filter { it.isLetterOrDigit() }

    fun isWellFormed(input: String): Boolean {
        val normalized = normalize(input)
        return normalized.length == GROUPS * GROUP_SIZE && normalized.all { it in ALPHABET }
    }
}
