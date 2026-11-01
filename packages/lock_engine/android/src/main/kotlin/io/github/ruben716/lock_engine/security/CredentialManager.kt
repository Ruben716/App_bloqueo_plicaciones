package io.github.ruben716.lock_engine.security

import io.github.ruben716.lock_engine.model.CredentialType
import java.security.SecureRandom

data class AuthOutcome(
    val success: Boolean,
    val failedAttempts: Int,
    val lockoutRemainingMs: Long,
    /** `true` si ni se comprobó la credencial porque hay un bloqueo temporal activo. */
    val throttled: Boolean = false,
)

data class RecoveryOutcome(
    val success: Boolean,
    val newRecoveryCode: String?,
    val lockoutRemainingMs: Long,
    val errorMessage: String?,
)

/**
 * Credencial propia de Sigilo, totalmente independiente del PIN/patrón del
 * sistema. Guarda solo hashes (PBKDF2 + pepper del Keystore), aplica bloqueo
 * progresivo ante fallos y gestiona el código de recuperación.
 *
 * Es thread-safe: la usan a la vez el overlay, la app Flutter y la recuperación.
 */
class CredentialManager(
    private val store: KeyValueStore,
    private val pepper: Pepper,
    private val hasher: CredentialHasher = CredentialHasher(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: SecureRandom = SecureRandom(),
) {

    @Synchronized
    fun hasCredential(): Boolean = store.getString(K_HASH) != null

    @Synchronized
    fun type(): CredentialType? =
        store.getString(K_TYPE)?.let { name -> CredentialType.entries.firstOrNull { it.name == name } }

    @Synchronized
    fun pinLength(): Int? =
        if (type() == CredentialType.PIN) store.getLong(K_PIN_LENGTH, 0).toInt().takeIf { it > 0 } else null

    fun validate(type: CredentialType, secret: String): String? = CredentialPolicy.validate(type, secret)

    /**
     * Guarda una nueva credencial.
     * @return el código de recuperación si se acaba de generar (primera vez),
     * o `null` si se conserva el existente.
     * @throws IllegalArgumentException si la credencial no cumple la política.
     */
    @Synchronized
    fun set(type: CredentialType, secret: String): String? {
        CredentialPolicy.validate(type, secret)?.let { throw IllegalArgumentException(it) }

        val credentialHash = hasher.hash(secret, pepper)
        val newRecoveryCode = if (store.getString(K_RECOVERY) == null) RecoveryCodes.generate(random) else null
        val recoveryHash = newRecoveryCode?.let { hasher.hash(RecoveryCodes.normalize(it), NoPepper) }

        store.edit {
            putString(K_TYPE, type.name)
            putString(K_HASH, credentialHash.encode())
            putLong(K_PIN_LENGTH, if (type == CredentialType.PIN) secret.length.toLong() else 0L)
            if (recoveryHash != null) putString(K_RECOVERY, recoveryHash.encode())
            putLong(K_FAILURES, 0)
            putLong(K_LOCKOUT_UNTIL, 0)
        }
        return newRecoveryCode
    }

    @Synchronized
    fun verify(secret: String): AuthOutcome {
        val now = clock()
        val remaining = lockoutRemainingMs(now)
        if (remaining > 0) return AuthOutcome(false, failures(), remaining, throttled = true)

        val stored = StoredHash.decode(store.getString(K_HASH))
            ?: return AuthOutcome(false, failures(), 0)
        val matches = runCatching { hasher.verify(secret, stored, pepper) }.getOrDefault(false)
        return if (matches) {
            resetFailures()
            AuthOutcome(true, 0, 0)
        } else {
            registerFailure(now)
        }
    }

    /** Un desbloqueo correcto por otra vía (biometría) reinicia los fallos. */
    @Synchronized
    fun recordExternalSuccess() = resetFailures()

    @Synchronized
    fun lockoutRemainingMs(now: Long = clock()): Long =
        LockoutPolicy.remainingMs(store.getLong(K_LOCKOUT_UNTIL, 0), now)

    @Synchronized
    fun resetWithRecoveryCode(code: String, type: CredentialType, newSecret: String): RecoveryOutcome {
        val now = clock()
        val remaining = lockoutRemainingMs(now)
        if (remaining > 0) {
            return RecoveryOutcome(false, null, remaining, "Demasiados intentos. Espera un momento.")
        }
        CredentialPolicy.validate(type, newSecret)?.let { return RecoveryOutcome(false, null, 0, it) }

        val stored = StoredHash.decode(store.getString(K_RECOVERY))
            ?: return RecoveryOutcome(false, null, 0, "No hay un código de recuperación configurado.")
        val matches = RecoveryCodes.isWellFormed(code) &&
            hasher.verify(RecoveryCodes.normalize(code), stored, NoPepper)
        if (!matches) {
            val outcome = registerFailure(now)
            return RecoveryOutcome(false, null, outcome.lockoutRemainingMs, "Código de recuperación incorrecto.")
        }

        // El código usado se invalida y se emite uno nuevo.
        store.edit { putString(K_RECOVERY, null) }
        val newCode = set(type, newSecret)
        return RecoveryOutcome(true, newCode, 0, null)
    }

    @Synchronized
    fun regenerateRecoveryCode(): String {
        val code = RecoveryCodes.generate(random)
        val hash = hasher.hash(RecoveryCodes.normalize(code), NoPepper)
        store.edit { putString(K_RECOVERY, hash.encode()) }
        return code
    }

    private fun failures(): Int = store.getLong(K_FAILURES, 0).toInt()

    private fun resetFailures() {
        store.edit {
            putLong(K_FAILURES, 0)
            putLong(K_LOCKOUT_UNTIL, 0)
        }
    }

    private fun registerFailure(now: Long): AuthOutcome {
        val failures = failures() + 1
        val lockoutMs = LockoutPolicy.lockoutMsFor(failures)
        store.edit {
            putLong(K_FAILURES, failures.toLong())
            putLong(K_LOCKOUT_UNTIL, if (lockoutMs > 0) now + lockoutMs else 0)
        }
        return AuthOutcome(false, failures, lockoutMs)
    }

    private companion object {
        const val K_TYPE = "credential_type"
        const val K_HASH = "credential_hash"
        const val K_PIN_LENGTH = "credential_pin_length"
        const val K_RECOVERY = "recovery_hash"
        const val K_FAILURES = "failed_attempts"
        const val K_LOCKOUT_UNTIL = "lockout_until"
    }
}
