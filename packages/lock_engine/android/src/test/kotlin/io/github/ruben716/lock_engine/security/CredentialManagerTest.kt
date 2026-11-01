package io.github.ruben716.lock_engine.security

import io.github.ruben716.lock_engine.model.CredentialType
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class MemoryStore : KeyValueStore {
    val values = HashMap<String, Any>()
    override fun getString(key: String): String? = values[key] as? String
    override fun getLong(key: String, default: Long): Long = values[key] as? Long ?: default
    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        object : KeyValueStore.Editor {
            override fun putString(key: String, value: String?) {
                if (value == null) values.remove(key) else values[key] = value
            }
            override fun putLong(key: String, value: Long) {
                values[key] = value
            }
        }.block()
    }
}

/** Pepper de prueba (HMAC con clave fija) en lugar del Android Keystore. */
private class FakePepper(private var available: Boolean = true) : Pepper {
    override fun apply(input: ByteArray, forWrite: Boolean): ByteArray {
        if (!available && !forWrite) throw PepperUnavailableException()
        available = true
        return Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec("test-key".toByteArray(), "HmacSHA256"))
            doFinal(input)
        }
    }
    fun lose() { available = false }
}

class CredentialManagerTest {

    private var now = 1_000_000L
    private val store = MemoryStore()
    private val pepper = FakePepper()
    private val manager = CredentialManager(store, pepper, CredentialHasher(iterations = 1_000), clock = { now })

    @Test
    fun `primera credencial devuelve codigo de recuperacion y no guarda el PIN en claro`() {
        val code = manager.set(CredentialType.PIN, "2580")
        assertNotNull(code)
        assertTrue(RecoveryCodes.isWellFormed(code))
        assertTrue(store.values.values.none { it == "2580" })
        assertEquals(CredentialType.PIN, manager.type())
        assertEquals(4, manager.pinLength())
    }

    @Test
    fun `cambiar la credencial conserva el codigo de recuperacion`() {
        manager.set(CredentialType.PIN, "2580")
        assertNull(manager.set(CredentialType.PASSWORD, "secreto-largo"))
        assertNull(manager.pinLength())
    }

    @Test
    fun `verifica correcto e incorrecto`() {
        manager.set(CredentialType.PIN, "2580")
        assertTrue(manager.verify("2580").success)
        val wrong = manager.verify("0852")
        assertFalse(wrong.success)
        assertEquals(1, wrong.failedAttempts)
    }

    @Test
    fun `bloqueo progresivo tras varios fallos y reinicio al acertar`() {
        manager.set(CredentialType.PIN, "2580")
        repeat(LockoutPolicy.FREE_ATTEMPTS - 1) { assertEquals(0L, manager.verify("1111").lockoutRemainingMs) }
        val locked = manager.verify("1111")
        assertEquals(LockoutPolicy.BASE_LOCKOUT_MS, locked.lockoutRemainingMs)

        // Durante el bloqueo ni siquiera el PIN correcto funciona.
        val throttled = manager.verify("2580")
        assertFalse(throttled.success)
        assertTrue(throttled.throttled)

        now += LockoutPolicy.BASE_LOCKOUT_MS
        assertTrue(manager.verify("2580").success)
        assertEquals(0L, manager.verify("9999").lockoutRemainingMs)
    }

    @Test
    fun `politica rechaza PIN debil`() {
        assertFailsWith<IllegalArgumentException> { manager.set(CredentialType.PIN, "1234") }
        assertFailsWith<IllegalArgumentException> { manager.set(CredentialType.PIN, "0000") }
        assertFalse(manager.hasCredential())
    }

    @Test
    fun `recuperacion con codigo valido cambia la credencial y rota el codigo`() {
        val code = manager.set(CredentialType.PIN, "2580")!!
        val outcome = manager.resetWithRecoveryCode(code.lowercase().replace("-", " "), CredentialType.PATTERN, "01258")
        assertTrue(outcome.success)
        assertNotNull(outcome.newRecoveryCode)
        assertNotEquals(code, outcome.newRecoveryCode)
        assertTrue(manager.verify("01258").success)
        // El código antiguo ya no sirve.
        assertFalse(manager.resetWithRecoveryCode(code, CredentialType.PIN, "2580").success)
    }

    @Test
    fun `recuperacion funciona aunque se pierda la clave del Keystore`() {
        val code = manager.set(CredentialType.PIN, "2580")!!
        pepper.lose()
        assertFalse(manager.verify("2580").success)
        assertTrue(manager.resetWithRecoveryCode(code, CredentialType.PIN, "3691").success)
        assertTrue(manager.verify("3691").success)
    }

    @Test
    fun `codigo de recuperacion incorrecto cuenta como fallo`() {
        manager.set(CredentialType.PIN, "2580")
        val outcome = manager.resetWithRecoveryCode("AAAA-BBBB-CCCC", CredentialType.PIN, "3691")
        assertFalse(outcome.success)
        assertTrue(manager.verify("2580").success)
    }

    @Test
    fun `biometria correcta reinicia los fallos`() {
        manager.set(CredentialType.PIN, "2580")
        repeat(3) { manager.verify("1111") }
        manager.recordExternalSuccess()
        assertEquals(1, manager.verify("1111").failedAttempts)
    }
}
