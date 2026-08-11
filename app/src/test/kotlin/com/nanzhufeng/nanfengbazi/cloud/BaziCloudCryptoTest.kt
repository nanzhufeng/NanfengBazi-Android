package com.nanzhufeng.nanfengbazi.cloud

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BaziCloudCryptoTest {
    @Test
    fun `recovery code unlocks vault key and payload round trips`() {
        val vaultKey = BaziCloudCrypto.generateVaultKey()
        val recovery = BaziCloudCrypto.generateRecoveryCode()
        try {
            val wrapped = BaziCloudCrypto.wrap(vaultKey, recovery)
            val envelope = BaziCloudKeyEnvelope(
                userId = "00000000-0000-0000-0000-000000000001",
                recoveryWrappedKey = wrapped.ciphertext,
                recoverySalt = wrapped.salt,
                recoveryNonce = wrapped.nonce,
            )
            val unlocked = BaziCloudCrypto.unwrap(envelope, recovery)
            try {
                assertArrayEquals(vaultKey, unlocked)
                val plaintext = "仅用于单元测试的结构化命例".encodeToByteArray()
                val encrypted = BaziCloudCrypto.encrypt(plaintext, unlocked)
                val document = BaziCloudDocument(
                    userId = envelope.userId,
                    appId = BAZI_CLOUD_APP_ID,
                    documentId = "structured-state",
                    revision = 1,
                    schemaVersion = 1,
                    ciphertext = encrypted.ciphertext,
                    nonce = encrypted.nonce,
                    ciphertextSha256 = encrypted.sha256,
                    byteSize = 42,
                )
                assertArrayEquals(plaintext, BaziCloudCrypto.decrypt(document, unlocked))
                plaintext.fill(0)
            } finally {
                unlocked.fill(0)
            }
        } finally {
            vaultKey.fill(0)
            recovery.fill('\u0000')
        }
    }

    @Test
    fun `ciphertext hash tampering is rejected before restore`() {
        val vaultKey = BaziCloudCrypto.generateVaultKey()
        try {
            val encrypted = BaziCloudCrypto.encrypt("snapshot".encodeToByteArray(), vaultKey)
            val document = BaziCloudDocument(
                userId = "00000000-0000-0000-0000-000000000001",
                appId = BAZI_CLOUD_APP_ID,
                documentId = "structured-state",
                revision = 1,
                schemaVersion = 1,
                ciphertext = encrypted.ciphertext,
                nonce = encrypted.nonce,
                ciphertextSha256 = "0".repeat(64),
                byteSize = 1,
            )
            assertThrows(IllegalArgumentException::class.java) {
                BaziCloudCrypto.decrypt(document, vaultKey)
            }
        } finally {
            vaultKey.fill(0)
        }
    }
}
