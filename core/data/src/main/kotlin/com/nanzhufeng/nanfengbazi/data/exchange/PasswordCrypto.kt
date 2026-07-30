package com.nanzhufeng.nanfengbazi.data.exchange

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object PasswordCrypto {
    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    const val DEFAULT_KDF_ITERATIONS = 600_000
    const val MIN_KDF_ITERATIONS = 100_000
    const val MAX_KDF_ITERATIONS = 2_000_000
    const val SALT_BYTES = 16
    const val NONCE_BYTES = 12
    const val KEY_BITS = 256
    const val GCM_TAG_BITS = 128

    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
    ): SecretKeySpec {
        val passwordCopy = password.copyOf()
        val spec = PBEKeySpec(passwordCopy, salt, iterations, KEY_BITS)
        passwordCopy.fill('\u0000')
        return try {
            val encoded = SecretKeyFactory.getInstance(KDF_ALGORITHM)
                .generateSecret(spec)
                .encoded
            try {
                SecretKeySpec(encoded, "AES")
            } finally {
                encoded.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    fun aad(vararg values: String): ByteArray =
        values.joinToString("\n").encodeToByteArray()
}
