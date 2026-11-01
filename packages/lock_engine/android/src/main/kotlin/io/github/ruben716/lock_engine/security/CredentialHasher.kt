package io.github.ruben716.lock_engine.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Secreto adicional aplicado tras PBKDF2. En el dispositivo es un HMAC con una
 * clave no exportable del Android Keystore ([KeystorePepper]): aunque alguien
 * copie el archivo de preferencias no puede probar PINs fuera del teléfono.
 */
interface Pepper {
    /** @param forWrite `true` al guardar una credencial nueva (puede crear la clave). */
    fun apply(input: ByteArray, forWrite: Boolean): ByteArray
}

object NoPepper : Pepper {
    override fun apply(input: ByteArray, forWrite: Boolean): ByteArray = input
}

/** Hash almacenado: `v1$iteraciones$salt$hash` (Base64). */
class StoredHash(val iterations: Int, val salt: ByteArray, val hash: ByteArray) {

    fun encode(): String = listOf(
        VERSION,
        iterations.toString(),
        Base64.getEncoder().encodeToString(salt),
        Base64.getEncoder().encodeToString(hash),
    ).joinToString(SEPARATOR)

    companion object {
        private const val VERSION = "v1"
        private const val SEPARATOR = "$"

        fun decode(encoded: String?): StoredHash? {
            val parts = encoded?.split(SEPARATOR) ?: return null
            if (parts.size != 4 || parts[0] != VERSION) return null
            return runCatching {
                StoredHash(
                    iterations = parts[1].toInt(),
                    salt = Base64.getDecoder().decode(parts[2]),
                    hash = Base64.getDecoder().decode(parts[3]),
                )
            }.getOrNull()
        }
    }
}

/**
 * Deriva credenciales con PBKDF2-HMAC-SHA256 + sal aleatoria. Nunca se guarda
 * el PIN/contraseña/patrón en claro. La comparación es en tiempo constante.
 */
class CredentialHasher(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {

    fun hash(secret: String, pepper: Pepper): StoredHash {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val derived = derive(secret, salt, iterations)
        return StoredHash(iterations, salt, pepper.apply(derived, forWrite = true))
    }

    fun verify(secret: String, stored: StoredHash, pepper: Pepper): Boolean {
        val derived = derive(secret, stored.salt, stored.iterations)
        val candidate = pepper.apply(derived, forWrite = false)
        return MessageDigest.isEqual(candidate, stored.hash)
    }

    private fun derive(secret: String, salt: ByteArray, rounds: Int): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, rounds, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        const val DEFAULT_ITERATIONS = 60_000
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
    }
}
