package io.github.ruben716.lock_engine.biometric

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

enum class BiometricAvailability { AVAILABLE, NOT_ENROLLED, UNSUPPORTED, INVALIDATED }

enum class BiometricOutcome { SUCCESS, CANCELLED, ERROR, INVALIDATED }

/**
 * Biometría como atajo de la credencial de Sigilo, nunca como sustituto del
 * bloqueo del sistema:
 *
 * - Solo biometría fuerte (clase 3) y SIN alternativa de PIN del dispositivo:
 *   el botón secundario vuelve al PIN/patrón propio de Sigilo.
 * - Se liga a una clave del Keystore invalidada al registrar una huella nueva.
 *   Si alguien que conoce el PIN del teléfono añade su huella, la biometría de
 *   Sigilo deja de funcionar hasta volver a introducir la credencial propia.
 */
class BiometricGate(context: Context) {

    private val appContext = context.applicationContext

    fun availability(): BiometricAvailability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return BiometricAvailability.UNSUPPORTED
        val manager = appContext.getSystemService(BiometricManager::class.java)
            ?: return BiometricAvailability.UNSUPPORTED
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            manager.canAuthenticate()
        }
        return when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            else -> BiometricAvailability.UNSUPPORTED
        }
    }

    fun hasKey(): Boolean = runCatching { keyStore().containsAlias(KEY_ALIAS) }.getOrDefault(false)

    /** Crea la clave ligada a biometría. Falla si no hay huellas registradas. */
    fun createKey(): Boolean = runCatching {
        deleteKey()
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(builder.build())
            generateKey()
        }
        true
    }.getOrDefault(false)

    fun deleteKey() {
        runCatching { keyStore().deleteEntry(KEY_ALIAS) }
    }

    /** `null` si la clave no existe o fue invalidada por una huella nueva. */
    fun prepareCipher(): Cipher? {
        val key = runCatching { keyStore().getKey(KEY_ALIAS, null) as? SecretKey }.getOrNull() ?: return null
        return try {
            Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
        } catch (_: KeyPermanentlyInvalidatedException) {
            deleteKey()
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Muestra el diálogo biométrico del sistema sobre [activity].
     * [onResult] se invoca exactamente una vez en el hilo principal.
     */
    fun authenticate(
        activity: Activity,
        title: String,
        subtitle: String,
        negativeText: String,
        onResult: (BiometricOutcome) -> Unit,
    ): CancellationSignal? {
        if (availability() != BiometricAvailability.AVAILABLE) {
            onResult(BiometricOutcome.ERROR)
            return null
        }
        val cipher = prepareCipher()
        if (cipher == null) {
            onResult(BiometricOutcome.INVALIDATED)
            return null
        }

        var delivered = false
        fun deliver(outcome: BiometricOutcome) {
            if (!delivered) {
                delivered = true
                onResult(outcome)
            }
        }

        val executor = activity.mainExecutor
        val builder = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(negativeText, executor) { _, _ -> deliver(BiometricOutcome.CANCELLED) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) builder.setConfirmationRequired(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        }

        val cancellation = CancellationSignal()
        builder.build().authenticate(
            BiometricPrompt.CryptoObject(cipher),
            cancellation,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // La operación criptográfica solo funciona si el Keystore
                    // confirmó la autenticación biométrica fuerte.
                    val verified = runCatching {
                        result.cryptoObject?.cipher?.doFinal(PROOF) != null
                    }.getOrDefault(false)
                    deliver(if (verified) BiometricOutcome.SUCCESS else BiometricOutcome.ERROR)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    val cancelled = errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                        errorCode == BiometricPrompt.BIOMETRIC_ERROR_NEGATIVE_BUTTON
                    deliver(if (cancelled) BiometricOutcome.CANCELLED else BiometricOutcome.ERROR)
                }
            },
        )
        return cancellation
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "lock_engine_biometric"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        val PROOF = byteArrayOf(0x53, 0x49, 0x47)
    }
}
