package com.nanzhufeng.nanfengbazi.data.backup

import com.nanzhufeng.nanfengbazi.data.exchange.PasswordCrypto
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable

@Serializable
internal data class BackupEncryptionHeader(
    val containerType: String,
    val protectionVersion: Int,
    val kdfAlgorithm: String,
    val kdfIterations: Int,
    val saltBase64: String,
    val cipherAlgorithm: String,
    val nonceBase64: String,
)

internal class BackupEncryption(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val exportIterations: Int = PasswordCrypto.DEFAULT_KDF_ITERATIONS,
) {
    fun encryptingStream(
        output: OutputStream,
        password: CharArray,
    ): OutputStream {
        require(password.isNotEmpty()) { "Password must not be empty" }
        require(exportIterations in PasswordCrypto.MIN_KDF_ITERATIONS..PasswordCrypto.MAX_KDF_ITERATIONS) {
            "KDF iterations out of range"
        }
        val salt = randomBytes(PasswordCrypto.SALT_BYTES)
        val nonce = randomBytes(PasswordCrypto.NONCE_BYTES)
        val header = BackupEncryptionHeader(
            containerType = CONTAINER_TYPE,
            protectionVersion = PROTECTION_VERSION,
            kdfAlgorithm = PasswordCrypto.KDF_ALGORITHM,
            kdfIterations = exportIterations,
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            cipherAlgorithm = PasswordCrypto.CIPHER_ALGORITHM,
            nonceBase64 = Base64.getEncoder().encodeToString(nonce),
        )
        val headerBytes = DomainJson.encodeToString(
            BackupEncryptionHeader.serializer(),
            header,
        ).encodeToByteArray()
        check(headerBytes.size <= MAX_HEADER_BYTES)
        DataOutputStream(output).apply {
            write(MAGIC_BYTES)
            writeInt(headerBytes.size)
            write(headerBytes)
            flush()
        }
        val cipher = Cipher.getInstance(PasswordCrypto.CIPHER_ALGORITHM)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            PasswordCrypto.deriveKey(password, salt, exportIterations),
            GCMParameterSpec(PasswordCrypto.GCM_TAG_BITS, nonce),
        )
        cipher.updateAAD(aad(exportIterations))
        return CipherOutputStream(output, cipher)
    }

    fun decryptAfterMagic(
        input: InputStream,
        password: CharArray,
        targetZip: Path,
    ) {
        require(password.isNotEmpty()) { "Password must not be empty" }
        val data = DataInputStream(input)
        val headerSize = data.readInt()
        require(headerSize in 1..MAX_HEADER_BYTES) { "Invalid header size" }
        val headerBytes = ByteArray(headerSize)
        data.readFully(headerBytes)
        val header = DomainJson.decodeFromString<BackupEncryptionHeader>(
            headerBytes.decodeToString(),
        )
        validateHeader(header)
        val decoder = Base64.getDecoder()
        val salt = decoder.decode(header.saltBase64)
        val nonce = decoder.decode(header.nonceBase64)
        require(salt.size == PasswordCrypto.SALT_BYTES) { "Invalid salt size" }
        require(nonce.size == PasswordCrypto.NONCE_BYTES) { "Invalid nonce size" }
        val cipher = Cipher.getInstance(PasswordCrypto.CIPHER_ALGORITHM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            PasswordCrypto.deriveKey(password, salt, header.kdfIterations),
            GCMParameterSpec(PasswordCrypto.GCM_TAG_BITS, nonce),
        )
        cipher.updateAAD(aad(header.kdfIterations))
        var total = 0L
        CipherInputStream(data, cipher).use { decrypted ->
            Files.newOutputStream(targetZip).buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = decrypted.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_DECRYPTED_BACKUP_BYTES) {
                        "Decrypted backup is too large"
                    }
                    output.write(buffer, 0, count)
                }
            }
        }
    }

    private fun validateHeader(header: BackupEncryptionHeader) {
        require(header.containerType == CONTAINER_TYPE) { "Unsupported container type" }
        require(header.protectionVersion == PROTECTION_VERSION) {
            "Unsupported protection version"
        }
        require(header.kdfAlgorithm == PasswordCrypto.KDF_ALGORITHM) { "Unsupported KDF" }
        require(
            header.kdfIterations in
                PasswordCrypto.MIN_KDF_ITERATIONS..PasswordCrypto.MAX_KDF_ITERATIONS,
        ) { "KDF iterations out of range" }
        require(header.cipherAlgorithm == PasswordCrypto.CIPHER_ALGORITHM) {
            "Unsupported cipher"
        }
    }

    private fun aad(iterations: Int): ByteArray = PasswordCrypto.aad(
        CONTAINER_TYPE,
        PROTECTION_VERSION.toString(),
        PasswordCrypto.KDF_ALGORITHM,
        iterations.toString(),
        PasswordCrypto.CIPHER_ALGORITHM,
    )

    private fun randomBytes(size: Int) = ByteArray(size).also(secureRandom::nextBytes)

    companion object {
        const val CONTAINER_TYPE = "nanfeng-bazi-full-backup-encrypted"
        const val PROTECTION_VERSION = 1
        const val MAX_DECRYPTED_BACKUP_BYTES = 2L * 1024 * 1024 * 1024
        const val MAX_HEADER_BYTES = 16 * 1024
        val MAGIC_BYTES = "NANFENG_BAZI_BACKUP_ENCRYPTED\n".encodeToByteArray()
    }
}
