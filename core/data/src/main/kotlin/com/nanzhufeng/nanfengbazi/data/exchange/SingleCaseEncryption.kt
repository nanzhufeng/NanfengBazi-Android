package com.nanzhufeng.nanfengbazi.data.exchange

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

internal class SingleCaseEncryption(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val exportIterations: Int = DEFAULT_KDF_ITERATIONS,
) {
    fun encrypt(
        plaintext: ByteArray,
        password: CharArray,
    ): SingleCaseEncryptedDocument {
        require(password.isNotEmpty()) { "Password must not be empty" }
        require(exportIterations in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS) {
            "KDF iterations out of range"
        }
        val salt = randomBytes(SALT_BYTES)
        val nonce = randomBytes(NONCE_BYTES)
        val key = PasswordCrypto.deriveKey(password, salt, exportIterations)
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key,
            GCMParameterSpec(GCM_TAG_BITS, nonce),
        )
        cipher.updateAAD(aad(exportIterations))
        val ciphertext = cipher.doFinal(plaintext)
        return SingleCaseEncryptedDocument(
            containerType = CONTAINER_TYPE,
            protectionVersion = PROTECTION_VERSION,
            kdfAlgorithm = KDF_ALGORITHM,
            kdfIterations = exportIterations,
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            cipherAlgorithm = CIPHER_ALGORITHM,
            nonceBase64 = Base64.getEncoder().encodeToString(nonce),
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext),
        )
    }

    fun decrypt(
        document: SingleCaseEncryptedDocument,
        password: CharArray,
    ): ByteArray {
        require(password.isNotEmpty()) { "Password must not be empty" }
        validateMetadata(document)
        val decoder = Base64.getDecoder()
        val salt = decoder.decode(document.saltBase64)
        val nonce = decoder.decode(document.nonceBase64)
        val ciphertext = decoder.decode(document.ciphertextBase64)
        require(salt.size == SALT_BYTES) { "Invalid salt size" }
        require(nonce.size == NONCE_BYTES) { "Invalid nonce size" }
        require(ciphertext.size >= GCM_TAG_BITS / Byte.SIZE_BITS) {
            "Ciphertext is shorter than the authentication tag"
        }
        val key = PasswordCrypto.deriveKey(password, salt, document.kdfIterations)
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(GCM_TAG_BITS, nonce),
        )
        cipher.updateAAD(aad(document.kdfIterations))
        return cipher.doFinal(ciphertext)
    }

    private fun validateMetadata(document: SingleCaseEncryptedDocument) {
        require(document.containerType == CONTAINER_TYPE) { "Unsupported container type" }
        require(document.protectionVersion == PROTECTION_VERSION) {
            "Unsupported protection version"
        }
        require(document.kdfAlgorithm == KDF_ALGORITHM) { "Unsupported KDF" }
        require(document.kdfIterations in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS) {
            "KDF iterations out of range"
        }
        require(document.cipherAlgorithm == CIPHER_ALGORITHM) { "Unsupported cipher" }
    }

    private fun randomBytes(size: Int) = ByteArray(size).also(secureRandom::nextBytes)

    private fun aad(iterations: Int): ByteArray = PasswordCrypto.aad(
        CONTAINER_TYPE,
        PROTECTION_VERSION.toString(),
        KDF_ALGORITHM,
        iterations.toString(),
        CIPHER_ALGORITHM,
    )

    companion object {
        const val CONTAINER_TYPE = "nanfeng-bazi-single-case-encrypted"
        const val PROTECTION_VERSION = 1
        const val KDF_ALGORITHM = PasswordCrypto.KDF_ALGORITHM
        const val CIPHER_ALGORITHM = PasswordCrypto.CIPHER_ALGORITHM
        const val DEFAULT_KDF_ITERATIONS = PasswordCrypto.DEFAULT_KDF_ITERATIONS
        const val MIN_KDF_ITERATIONS = PasswordCrypto.MIN_KDF_ITERATIONS
        const val MAX_KDF_ITERATIONS = PasswordCrypto.MAX_KDF_ITERATIONS
        private const val SALT_BYTES = PasswordCrypto.SALT_BYTES
        private const val NONCE_BYTES = PasswordCrypto.NONCE_BYTES
        private const val GCM_TAG_BITS = PasswordCrypto.GCM_TAG_BITS
    }
}
