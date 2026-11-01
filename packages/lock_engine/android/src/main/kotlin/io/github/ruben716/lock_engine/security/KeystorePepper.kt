package io.github.ruben716.lock_engine.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

class PepperUnavailableException : IllegalStateException("La clave del Keystore no está disponible")

/**
 * HMAC-SHA256 con una clave que vive dentro del Android Keystore (hardware
 * seguro cuando el dispositivo lo tiene) y nunca sale de él.
 */
class KeystorePepper(private val alias: String = DEFAULT_ALIAS) : Pepper {

    override fun apply(input: ByteArray, forWrite: Boolean): ByteArray {
        val key = loadKey() ?: if (forWrite) createKey() else throw PepperUnavailableException()
        val mac = Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256)
        mac.init(key)
        return mac.doFinal(input)
    }

    private fun loadKey(): SecretKey? = runCatching {
        keyStore().getKey(alias, null) as? SecretKey
    }.getOrNull()

    private fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE)
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN).build())
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEFAULT_ALIAS = "lock_engine_credential_pepper"
    }
}
