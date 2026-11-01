package io.github.ruben716.lock_engine.security

import io.github.ruben716.lock_engine.model.CredentialType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PoliciesTest {

    @Test
    fun `lockout crece exponencialmente con tope`() {
        assertEquals(0L, LockoutPolicy.lockoutMsFor(4))
        assertEquals(30_000L, LockoutPolicy.lockoutMsFor(5))
        assertEquals(60_000L, LockoutPolicy.lockoutMsFor(6))
        assertEquals(120_000L, LockoutPolicy.lockoutMsFor(7))
        assertEquals(LockoutPolicy.MAX_LOCKOUT_MS, LockoutPolicy.lockoutMsFor(40))
    }

    @Test
    fun `lockout restante nunca supera el maximo aunque cambie el reloj`() {
        assertEquals(LockoutPolicy.MAX_LOCKOUT_MS, LockoutPolicy.remainingMs(lockoutUntilMs = Long.MAX_VALUE / 2, nowMs = 0))
        assertEquals(0L, LockoutPolicy.remainingMs(lockoutUntilMs = 100, nowMs = 200))
    }

    @Test
    fun `politica de PIN`() {
        assertNull(CredentialPolicy.validate(CredentialType.PIN, "2580"))
        assertNotNull(CredentialPolicy.validate(CredentialType.PIN, "123"))
        assertNotNull(CredentialPolicy.validate(CredentialType.PIN, "4321"))
        assertNotNull(CredentialPolicy.validate(CredentialType.PIN, "12a4"))
        assertNotNull(CredentialPolicy.validate(CredentialType.PIN, "7777"))
    }

    @Test
    fun `politica de contrasena y patron`() {
        assertNull(CredentialPolicy.validate(CredentialType.PASSWORD, "mi clave"))
        assertNotNull(CredentialPolicy.validate(CredentialType.PASSWORD, "corta"))
        assertNull(CredentialPolicy.validate(CredentialType.PATTERN, "0124"))
        assertNotNull(CredentialPolicy.validate(CredentialType.PATTERN, "012"))
        assertNotNull(CredentialPolicy.validate(CredentialType.PATTERN, "0102"))
        assertNotNull(CredentialPolicy.validate(CredentialType.PATTERN, "0129"))
    }

    @Test
    fun `codigos de recuperacion`() {
        val code = RecoveryCodes.generate()
        assertTrue(Regex("^[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$").matches(code))
        assertTrue(RecoveryCodes.isWellFormed(code.lowercase()))
        assertEquals(code.replace("-", ""), RecoveryCodes.normalize(" ${code.lowercase()} "))
        assertFalse(RecoveryCodes.isWellFormed("0000-1111-OOOO"))
    }

    @Test
    fun `hash codifica y decodifica`() {
        val hasher = CredentialHasher(iterations = 1_000)
        val stored = hasher.hash("clave", NoPepper)
        val decoded = StoredHash.decode(stored.encode())
        assertNotNull(decoded)
        assertTrue(hasher.verify("clave", decoded, NoPepper))
        assertFalse(hasher.verify("otra", decoded, NoPepper))
        assertNull(StoredHash.decode("basura"))
    }
}
