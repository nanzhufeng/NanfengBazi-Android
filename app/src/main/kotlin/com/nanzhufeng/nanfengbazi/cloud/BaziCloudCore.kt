package com.nanzhufeng.nanfengbazi.cloud

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nanzhufeng.nanfengbazi.readUtf8Bounded
import com.nanzhufeng.nanfengbazi.data.backup.BackupExportResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupPreviewResult
import com.nanzhufeng.nanfengbazi.data.backup.CaseBackupOperations
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val BAZI_CLOUD_APP_ID = "nanfengbazi.android"
private const val BAZI_CLOUD_DOCUMENT_ID = "structured-state"
private const val BAZI_CLOUD_SCHEMA_VERSION = 1

data class BaziCloudSession(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val displayName: String? = null,
    val avatarUrl: String? = null,
)

data class BaziGoogleIdentity(
    val displayName: String?,
    val avatarUrl: String?,
)

data class BaziCloudMetadata(
    val remoteRevision: Long = 0,
    val localFingerprint: String = "",
    val lastSyncedAt: Long? = null,
)

data class BaziCloudCredentials(
    val session: BaziCloudSession,
    val vaultKey: ByteArray,
    val deviceId: String,
    val metadata: BaziCloudMetadata,
)

@Serializable
data class BaziCloudKeyEnvelope(
    @SerialName("user_id") val userId: String,
    @SerialName("recovery_wrapped_key") val recoveryWrappedKey: String,
    @SerialName("recovery_salt") val recoverySalt: String,
    @SerialName("recovery_nonce") val recoveryNonce: String,
)

@Serializable
data class BaziCloudDocument(
    @SerialName("user_id") val userId: String,
    @SerialName("app_id") val appId: String,
    @SerialName("document_id") val documentId: String,
    val revision: Long,
    @SerialName("schema_version") val schemaVersion: Int,
    val ciphertext: String,
    val nonce: String,
    @SerialName("ciphertext_sha256") val ciphertextSha256: String,
    @SerialName("byte_size") val byteSize: Int,
)

@Serializable
data class BaziCloudDocumentMetadata(
    val revision: Long,
    @SerialName("ciphertext_sha256") val ciphertextSha256: String,
)

data class BaziCloudSnapshot(
    val payload: ByteArray,
    val fingerprint: String,
    val meaningfulCaseCount: Int,
)

internal enum class BaziCloudSyncAction {
    ALREADY_CURRENT,
    UPLOAD_LOCAL,
    RESTORE_REMOTE,
}

enum class BaziCloudSyncRunResult {
    SUCCESS,
    SKIPPED,
    RETRY,
    FAILURE,
}

private enum class BaziCloudSyncOrigin {
    MANUAL,
    BACKGROUND,
}

/**
 * 南枫八字的云端是“本机加密备份”，不是双向合并器。
 *
 * 只有空白本机可以恢复云端；本机已有命例时，任何不一致都以本机生成新备份。
 */
internal fun decideBaziCloudSyncAction(
    localHasData: Boolean,
    remoteRevision: Long?,
    knownRemoteRevision: Long,
    localFingerprintMatches: Boolean,
): BaziCloudSyncAction = when {
    remoteRevision == null -> BaziCloudSyncAction.UPLOAD_LOCAL
    remoteRevision == knownRemoteRevision && localFingerprintMatches -> BaziCloudSyncAction.ALREADY_CURRENT
    !localHasData -> BaziCloudSyncAction.RESTORE_REMOTE
    else -> BaziCloudSyncAction.UPLOAD_LOCAL
}

sealed interface BaziCloudSyncState {
    data object Unconfigured : BaziCloudSyncState
    data object SignedOut : BaziCloudSyncState
    data class Working(val message: String) : BaziCloudSyncState
    data class RecoveryCodeReady(val code: String) : BaziCloudSyncState
    data object RecoveryCodeRequired : BaziCloudSyncState
    data class AccountEntryChoice(
        val email: String,
        val displayName: String?,
        val avatarUrl: String?,
        val hasRemoteData: Boolean,
        val localHasData: Boolean,
    ) : BaziCloudSyncState
    data class Ready(
        val email: String,
        val displayName: String?,
        val avatarUrl: String?,
        val revision: Long,
        val lastSyncedAt: Long?,
        val message: String,
    ) : BaziCloudSyncState
    data class Failure(val message: String, val detail: String) : BaziCloudSyncState
}

sealed interface BaziGoogleSignInResult {
    data class Success(
        val idToken: String,
        val nonce: String,
        val displayName: String?,
        val avatarUrl: String?,
    ) : BaziGoogleSignInResult
    data object Cancelled : BaziGoogleSignInResult
    data class Failure(val message: String, val detail: String) : BaziGoogleSignInResult
}

class BaziGoogleSignInClient(
    context: Context,
    private val serverClientId: String,
) {
    private val credentialManager = CredentialManager.create(context.applicationContext)

    val configured: Boolean get() = serverClientId.trim().endsWith(".apps.googleusercontent.com")

    suspend fun signIn(activityContext: Context): BaziGoogleSignInResult {
        if (!configured) return BaziGoogleSignInResult.Failure("Google 登录尚未配置", "missing Google web client ID")
        val nonce = randomNonce()
        return try {
            val response = credentialManager.getCredential(
                context = activityContext,
                request = GetCredentialRequest.Builder().addCredentialOption(
                    GetSignInWithGoogleOption.Builder(serverClientId.trim())
                        .setNonce(BaziCloudCrypto.sha256(nonce.encodeToByteArray()))
                        .build(),
                ).build(),
            )
            val credential = response.credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                BaziGoogleSignInResult.Failure("Google 没有返回可验证的账号凭据", "unexpected credential type")
            } else {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                BaziGoogleSignInResult.Success(
                    idToken = google.idToken,
                    nonce = nonce,
                    displayName = google.displayName?.trim()?.takeIf(String::isNotBlank),
                    avatarUrl = google.profilePictureUri
                        ?.takeIf { it.scheme == "https" && !it.host.isNullOrBlank() }
                        ?.toString(),
                )
            }
        } catch (_: GetCredentialCancellationException) {
            BaziGoogleSignInResult.Cancelled
        } catch (_: NoCredentialException) {
            BaziGoogleSignInResult.Failure("设备上没有可用的 Google 账号", "no Google credential")
        } catch (error: GetCredentialException) {
            BaziGoogleSignInResult.Failure("无法打开 Google 登录，请稍后重试", error.type)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            BaziGoogleSignInResult.Failure("Google 登录凭据无法识别", error.javaClass.simpleName)
        }
    }

    suspend fun clearCredentialState() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // 清理系统账号候选记忆失败不影响本机数据；下次仍可重新打开选择器。
        }
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        return try { Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING) } finally { bytes.fill(0) }
    }
}

class BaziCloudCredentialStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("nanfeng_bazi_cloud", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun read(): BaziCloudCredentials? {
        val value = preferences.getString("state_v1", null) ?: return null
        return json.decodeFromString(Persisted.serializer(), decrypt(value)).toRuntime()
    }

    fun write(value: BaziCloudCredentials) {
        require(value.vaultKey.size == 32) { "云端保险库密钥长度无效" }
        require(runCatching { UUID.fromString(value.deviceId) }.isSuccess) { "设备同步 ID 无效" }
        val encoded = json.encodeToString(Persisted.serializer(), Persisted.from(value))
        check(preferences.edit().putString("state_v1", encrypt(encoded)).commit()) { "本机云端凭据保存失败" }
    }

    fun clear() {
        check(preferences.edit().remove("state_v1").commit()) { "本机云端凭据清除失败" }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val plain = value.encodeToByteArray()
        return try {
            val encrypted = cipher.doFinal(plain)
            try { Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP) } finally { encrypted.fill(0) }
        } finally { plain.fill(0) }
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > 12) { "无效的本机云端凭据" }
        val iv = payload.copyOfRange(0, 12)
        val encrypted = payload.copyOfRange(12, payload.size)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(encrypted)
            try { plain.toString(StandardCharsets.UTF_8) } finally { plain.fill(0) }
        } finally { payload.fill(0); iv.fill(0); encrypted.fill(0) }
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("nanfengbazi.cloud.credentials.v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(
                "nanfengbazi.cloud.credentials.v1",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    @Serializable
    private data class Persisted(
        val userId: String, val email: String, val accessToken: String, val refreshToken: String,
        val expiresAtEpochSeconds: Long, val displayName: String? = null, val avatarUrl: String? = null, val vaultKey: String,
        val deviceId: String, val remoteRevision: Long, val localFingerprint: String, val lastSyncedAt: Long? = null,
    ) {
        fun toRuntime() = BaziCloudCredentials(
            BaziCloudSession(userId, email, accessToken, refreshToken, expiresAtEpochSeconds, displayName, avatarUrl),
            Base64.decode(vaultKey, Base64.NO_WRAP), deviceId,
            BaziCloudMetadata(remoteRevision, localFingerprint, lastSyncedAt),
        )
        companion object {
            fun from(value: BaziCloudCredentials) = Persisted(
                value.session.userId, value.session.email, value.session.accessToken, value.session.refreshToken,
                value.session.expiresAtEpochSeconds, value.session.displayName, value.session.avatarUrl,
                Base64.encodeToString(value.vaultKey, Base64.NO_WRAP), value.deviceId,
                value.metadata.remoteRevision, value.metadata.localFingerprint, value.metadata.lastSyncedAt,
            )
        }
    }
}

/**
 * Google 头像只属于已验证账号的展示资料，不进入命例快照，也不随备份上传。
 *
 * 缓存位于应用私有 cacheDir，键由账号与头像 URL 的摘要组成：更换账号或 Google
 * 更新头像 URL 时不会误显示另一账号的旧头像；网络暂时不可用时仍能立即显示已验证
 * 的头像。退出账号时会删除该账号的缓存。
 */
class BaziGoogleAvatarCache(context: Context) {
    private val directory = context.applicationContext.cacheDir.resolve("google-account-avatars")

    fun read(userId: String, avatarUrl: String): ByteArray? {
        val target = cacheFile(userId, avatarUrl)
        if (!target.isFile || target.length() !in 1..MAX_BYTES) return null
        return runCatching {
            target.inputStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > MAX_BYTES) return@use null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        }.getOrElse {
            target.delete()
            null
        }
    }

    fun write(userId: String, avatarUrl: String, bytes: ByteArray) {
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return
        val target = cacheFile(userId, avatarUrl)
        val parent = target.parentFile ?: return
        if (!parent.exists() && !parent.mkdirs()) return
        val temporary = File(parent, "${target.name}.tmp")
        runCatching {
            temporary.outputStream().use { it.write(bytes) }
            if (target.exists() && !target.delete()) return@runCatching
            if (!temporary.renameTo(target)) temporary.delete()
        }.onFailure { temporary.delete() }
    }

    fun clear(userId: String) {
        val prefix = "${digest(userId).take(20)}-"
        directory.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
    }

    private fun cacheFile(userId: String, avatarUrl: String): File =
        directory.resolve("${digest(userId).take(20)}-${digest(avatarUrl)}.image")

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.encodeToByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private companion object { const val MAX_BYTES = 2 * 1024 * 1024 }
}

object BaziCloudCrypto {
    private const val VAULT_KEY_BYTES = 32
    private const val KDF_ITERATIONS = 180_000
    private val encoder = java.util.Base64.getUrlEncoder().withoutPadding()
    private val decoder = java.util.Base64.getUrlDecoder()

    fun generateVaultKey(): ByteArray = ByteArray(VAULT_KEY_BYTES).also(SecureRandom()::nextBytes)
    fun generateRecoveryCode(): CharArray = encoder.encodeToString(ByteArray(VAULT_KEY_BYTES).also(SecureRandom()::nextBytes)).toCharArray()

    data class Encrypted(val ciphertext: String, val nonce: String, val sha256: String)
    data class Wrapped(val ciphertext: String, val salt: String, val nonce: String)

    fun wrap(key: ByteArray, secret: CharArray): Wrapped = cryptKey(Cipher.ENCRYPT_MODE, key, secret)
    fun unwrap(wrapped: BaziCloudKeyEnvelope, secret: CharArray): ByteArray {
        val salt = decoder.decode(wrapped.recoverySalt)
        val nonce = decoder.decode(wrapped.recoveryNonce)
        val derived = derive(secret, salt)
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(derived, "AES"), GCMParameterSpec(128, nonce))
                updateAAD("nanfeng-cloud-vault-key-v1".encodeToByteArray())
                doFinal(decoder.decode(wrapped.recoveryWrappedKey)).also { require(it.size == VAULT_KEY_BYTES) }
            }
        } finally { salt.fill(0); nonce.fill(0); derived.fill(0) }
    }

    fun encrypt(payload: ByteArray, key: ByteArray): Encrypted {
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        return try {
            val ciphertext = Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                updateAAD(aad())
                doFinal(payload)
            }
            try { Encrypted(encoder.encodeToString(ciphertext), encoder.encodeToString(nonce), sha256(ciphertext)) }
            finally { ciphertext.fill(0) }
        } finally { nonce.fill(0) }
    }

    fun decrypt(document: BaziCloudDocument, key: ByteArray): ByteArray {
        val ciphertext = decoder.decode(document.ciphertext)
        require(MessageDigest.isEqual(sha256(ciphertext).encodeToByteArray(), document.ciphertextSha256.encodeToByteArray())) {
            "云端密文完整性校验失败"
        }
        val nonce = decoder.decode(document.nonce)
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                updateAAD(aad())
                doFinal(ciphertext)
            }
        } finally { ciphertext.fill(0); nonce.fill(0) }
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun cryptKey(mode: Int, vaultKey: ByteArray, secret: CharArray): Wrapped {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        val derived = derive(secret, salt)
        return try {
            val encrypted = Cipher.getInstance("AES/GCM/NoPadding").run {
                init(mode, SecretKeySpec(derived, "AES"), GCMParameterSpec(128, nonce))
                updateAAD("nanfeng-cloud-vault-key-v1".encodeToByteArray())
                doFinal(vaultKey)
            }
            try { Wrapped(encoder.encodeToString(encrypted), encoder.encodeToString(salt), encoder.encodeToString(nonce)) }
            finally { encrypted.fill(0) }
        } finally { salt.fill(0); nonce.fill(0); derived.fill(0) }
    }

    private fun derive(secret: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret, salt, KDF_ITERATIONS, VAULT_KEY_BYTES * 8)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }

    private fun aad() = "nanfeng-cloud-payload-v1|$BAZI_CLOUD_APP_ID|$BAZI_CLOUD_SCHEMA_VERSION".encodeToByteArray()
}

data class BaziSupabaseConfig(val url: String, val publishableKey: String) {
    val configured: Boolean get() = url.startsWith("https://") && publishableKey.isNotBlank()
}

internal class BaziCloudHttpException(
    val statusCode: Int,
    val serviceCode: String?,
) : IOException("南枫云请求失败（HTTP $statusCode${serviceCode?.let { ": $it" }.orEmpty()}）")

internal fun isBaziCloudRetryable(error: Exception): Boolean = when (error) {
    is BaziCloudHttpException -> error.statusCode == 408 || error.statusCode == 429 || error.statusCode in 500..599
    is IOException -> true
    else -> false
}

internal fun shouldRetryBaziCloudRequest(method: String, completedAttempt: Int, error: Exception): Boolean =
    method == "GET" && completedAttempt < 3 && isBaziCloudRetryable(error)

class BaziSupabaseGateway(private val config: BaziSupabaseConfig) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    val configured: Boolean get() = config.configured

    suspend fun signInWithGoogle(idToken: String, nonce: String): BaziCloudSession {
        val response = request("POST", "/auth/v1/token?grant_type=id_token", null, json.encodeToString(AuthRequest("google", idToken, nonce)))
        return json.decodeFromString(AuthResponse.serializer(), response).toSession()
    }

    suspend fun refresh(session: BaziCloudSession): BaziCloudSession {
        if (session.expiresAtEpochSeconds > System.currentTimeMillis() / 1000L + 60) return session
        val response = request("POST", "/auth/v1/token?grant_type=refresh_token", null, json.encodeToString(RefreshRequest(session.refreshToken)))
        val refreshed = json.decodeFromString(AuthResponse.serializer(), response).toSession()
        return refreshed.copy(
            displayName = refreshed.displayName ?: session.displayName,
            avatarUrl = refreshed.avatarUrl ?: session.avatarUrl,
        )
    }

    suspend fun signOut(session: BaziCloudSession) { request("POST", "/auth/v1/logout", session, "{}") }

    suspend fun readGoogleIdentity(session: BaziCloudSession): BaziGoogleIdentity {
        val user = json.decodeFromString(User.serializer(), request("GET", "/auth/v1/user", session, null))
        return BaziGoogleIdentity(
            displayName = user.metadata?.fullName?.trim()?.takeIf(String::isNotBlank),
            avatarUrl = (user.metadata?.avatarUrl ?: user.metadata?.picture)
                ?.trim()
                ?.takeIf { candidate -> runCatching { URL(candidate).isTrustedGoogleAvatar() }.getOrDefault(false) },
        )
    }

    suspend fun fetchGoogleAvatar(session: BaziCloudSession, avatarUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        val source = runCatching { URL(avatarUrl) }.getOrNull() ?: return@withContext null
        if (!source.isTrustedGoogleAvatar()) return@withContext null
        val connection = (URL(config.url.trimEnd('/') + "/functions/v1/google-avatar").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 8_000
            doOutput = true
            setRequestProperty("apikey", config.publishableKey)
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "image/*")
        }
        try {
            connection.outputStream.use { it.write(json.encodeToString(AvatarRequest(avatarUrl)).encodeToByteArray()) }
            if (connection.responseCode !in 200..299 || connection.contentLengthLong > 2L * 1024L * 1024L) return@withContext null
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 2 * 1024 * 1024) return@withContext null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } catch (_: IOException) {
            null
        } finally { connection.disconnect() }
    }

    /**
     * 与南枫记一致的头像读取兜底：云函数不可用时直接向已校验的 Google CDN 请求。
     * URL 仅允许 googleusercontent 的 HTTPS 子域、禁止重定向，且结果不会离开应用私有缓存。
     */
    suspend fun fetchGoogleAvatarDirect(avatarUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        val source = runCatching { URL(avatarUrl) }.getOrNull() ?: return@withContext null
        if (!source.isTrustedGoogleAvatar()) return@withContext null
        val connection = runCatching { source.openConnection() as HttpURLConnection }.getOrNull()
            ?: return@withContext null
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "image/*")
            connection.setRequestProperty("User-Agent", "NanfengBazi-Android/1.0")
            if (connection.responseCode !in 200..299 ||
                connection.contentLengthLong > 2L * 1024L * 1024L ||
                connection.contentType?.substringBefore(';')?.startsWith("image/") != true
            ) return@withContext null
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 2 * 1024 * 1024) return@withContext null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } catch (_: IOException) {
            null
        } finally { connection.disconnect() }
    }

    suspend fun readKey(session: BaziCloudSession): BaziCloudKeyEnvelope? =
        json.decodeFromString<List<BaziCloudKeyEnvelope>>(BaziCloudKeyEnvelopeList, request(
            "GET", "/rest/v1/nanfeng_account_keys?select=user_id,recovery_wrapped_key,recovery_salt,recovery_nonce&user_id=eq.${q(session.userId)}", session, null,
        )).singleOrNull()

    suspend fun createKey(session: BaziCloudSession, envelope: BaziCloudKeyEnvelope) {
        request("POST", "/rest/v1/nanfeng_account_keys", session, json.encodeToString(BaziCloudKeyEnvelope.serializer(), envelope))
    }

    suspend fun readDocument(session: BaziCloudSession): BaziCloudDocument? =
        json.decodeFromString<List<BaziCloudDocument>>(BaziCloudDocumentList, request(
            "GET", "/rest/v1/nanfeng_sync_documents?select=user_id,app_id,document_id,revision,schema_version,ciphertext,nonce,ciphertext_sha256,byte_size&app_id=eq.$BAZI_CLOUD_APP_ID&document_id=eq.$BAZI_CLOUD_DOCUMENT_ID", session, null,
        )).singleOrNull()

    suspend fun readDocumentMetadata(session: BaziCloudSession): BaziCloudDocumentMetadata? =
        json.decodeFromString<List<BaziCloudDocumentMetadata>>(BaziCloudDocumentMetadataList, request(
            "GET", "/rest/v1/nanfeng_sync_documents?select=revision,ciphertext_sha256&app_id=eq.$BAZI_CLOUD_APP_ID&document_id=eq.$BAZI_CLOUD_DOCUMENT_ID", session, null,
        )).singleOrNull()

    suspend fun commit(session: BaziCloudSession, expectedRevision: Long, encrypted: BaziCloudCrypto.Encrypted, byteSize: Int, deviceId: String): Long? {
        val response = request("POST", "/rest/v1/rpc/nanfeng_commit_sync_document", session, json.encodeToString(CommitRequest(
            BAZI_CLOUD_APP_ID, BAZI_CLOUD_DOCUMENT_ID, expectedRevision, BAZI_CLOUD_SCHEMA_VERSION,
            encrypted.ciphertext, encrypted.nonce, encrypted.sha256, byteSize, deviceId,
        )))
        val result = json.decodeFromString<List<CommitResponse>>(CommitResponseList, response).singleOrNull()
            ?: error("云端提交没有返回版本")
        return if (result.outcome == "committed") result.committedRevision else null
    }

    private suspend fun request(method: String, path: String, session: BaziCloudSession?, body: String?): String = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (completedAttempt in 1..3) {
            try {
                return@withContext executeRequest(method, path, session, body)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (!shouldRetryBaziCloudRequest(method, completedAttempt, error)) throw error
                lastError = error
                delay(if (completedAttempt == 1) 250L else 750L)
            }
        }
        throw checkNotNull(lastError)
    }

    private fun executeRequest(method: String, path: String, session: BaziCloudSession?, body: String?): String {
        if (!configured) error("南枫云尚未配置")
        val bodyBytes = body?.encodeToByteArray()
        val connection = (URL(config.url.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 45_000
            useCaches = false
            setRequestProperty("apikey", config.publishableKey)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Connection", "close")
            session?.let { setRequestProperty("Authorization", "Bearer ${it.accessToken}") }
            if (bodyBytes != null) {
                doOutput = true
                setFixedLengthStreamingMode(bodyBytes.size)
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (bodyBytes != null) connection.outputStream.use { it.write(bodyBytes) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { it.readUtf8Bounded(MAX_CLOUD_RESPONSE_BYTES) }.orEmpty()
            if (code !in 200..299) throw BaziCloudHttpException(code, response.safeServiceCode())
            response
        } finally {
            bodyBytes?.fill(0)
            connection.disconnect()
        }
    }

    private fun String.safeServiceCode(): String? =
        Regex("\\\"(?:code|error_code)\\\"\\s*:\\s*\\\"([A-Za-z0-9_.-]{1,80})\\\"")
            .find(this)?.groupValues?.getOrNull(1)

    private fun URL.isTrustedGoogleAvatar(): Boolean {
        val host = host.lowercase()
        return protocol == "https" && (host == "googleusercontent.com" || host.endsWith(".googleusercontent.com"))
    }

    @Serializable private data class AuthRequest(val provider: String, @SerialName("id_token") val idToken: String, val nonce: String)
    @Serializable private data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)
    @Serializable private data class AvatarRequest(val url: String)
    @Serializable private data class AuthResponse(@SerialName("access_token") val accessToken: String, @SerialName("refresh_token") val refreshToken: String, @SerialName("expires_at") val expiresAt: Long, val user: User) {
        fun toSession() = BaziCloudSession(
            user.id, user.email.orEmpty(), accessToken, refreshToken, expiresAt,
            user.metadata?.fullName,
            user.metadata?.avatarUrl ?: user.metadata?.picture,
        )
    }
    @Serializable private data class User(val id: String, val email: String? = null, @SerialName("user_metadata") val metadata: Metadata? = null)
    @Serializable private data class Metadata(
        @SerialName("full_name") val fullName: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null,
        val picture: String? = null,
    )
    @Serializable private data class CommitRequest(
        @SerialName("p_app_id") val appId: String, @SerialName("p_document_id") val documentId: String,
        @SerialName("p_expected_revision") val expectedRevision: Long, @SerialName("p_schema_version") val schemaVersion: Int,
        @SerialName("p_ciphertext") val ciphertext: String, @SerialName("p_nonce") val nonce: String,
        @SerialName("p_ciphertext_sha256") val sha256: String, @SerialName("p_byte_size") val byteSize: Int,
        @SerialName("p_device_id") val deviceId: String,
    )
    @Serializable private data class CommitResponse(val outcome: String, @SerialName("committed_revision") val committedRevision: Long? = null)
    private companion object {
        const val MAX_CLOUD_RESPONSE_BYTES = 64 * 1024 * 1024
        val BaziCloudKeyEnvelopeList = kotlinx.serialization.builtins.ListSerializer(BaziCloudKeyEnvelope.serializer())
        val BaziCloudDocumentList = kotlinx.serialization.builtins.ListSerializer(BaziCloudDocument.serializer())
        val BaziCloudDocumentMetadataList = kotlinx.serialization.builtins.ListSerializer(BaziCloudDocumentMetadata.serializer())
        val CommitResponseList = kotlinx.serialization.builtins.ListSerializer(CommitResponse.serializer())
        fun q(value: String) = URLEncoder.encode(value, "UTF-8")
    }
}

class BaziCloudSnapshotBridge(
    private val backups: CaseBackupOperations,
    private val attachmentRoot: java.nio.file.Path,
    private val workRoot: java.nio.file.Path,
    private val appVersion: String,
) {
    suspend fun capture(): BaziCloudSnapshot = withContext(Dispatchers.IO) {
        val output = ByteArrayOutputStream()
        val result = backups.exportCloudSnapshot(output, appVersion)
        val bytes = output.toByteArray()
        return@withContext when (result) {
            is BackupExportResult.Success -> BaziCloudSnapshot(
                payload = bytes,
                fingerprint = BaziCloudCrypto.sha256(bytes),
                meaningfulCaseCount = result.counts.cases,
            )
            is BackupExportResult.Rejected -> {
                bytes.fill(0)
                error(result.message)
            }
        }
    }

    suspend fun restoreIntoEmpty(payload: ByteArray): Int {
        return when (val result = backups.restoreCloudSnapshotIntoEmptyStore(
            input = ByteArrayInputStream(payload), workRoot = workRoot, attachmentRoot = attachmentRoot,
        )) {
            is com.nanzhufeng.nanfengbazi.data.backup.BackupRestoreResult.Success -> result.preview.manifest.counts.cases
            is com.nanzhufeng.nanfengbazi.data.backup.BackupRestoreResult.Rejected -> error(result.message)
        }
    }

    suspend fun validateRemote(payload: ByteArray): Int = when (val result = backups.preview(
        input = ByteArrayInputStream(payload), workRoot = workRoot,
    )) {
        is BackupPreviewResult.Success -> result.preview.manifest.counts.cases
        is BackupPreviewResult.Rejected -> error(result.message)
    }
}

class BaziCloudSyncCoordinator(
    private val gateway: BaziSupabaseGateway,
    private val credentialStore: BaziCloudCredentialStore,
    private val avatarCache: BaziGoogleAvatarCache,
    private val snapshotBridge: BaziCloudSnapshotBridge,
    private val onSignedIn: () -> Unit = {},
    private val onSignedOut: () -> Unit = {},
    private val onRetryRequested: () -> Unit = {},
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(if (gateway.configured) BaziCloudSyncState.SignedOut else BaziCloudSyncState.Unconfigured)
    val state: StateFlow<BaziCloudSyncState> = mutableState.asStateFlow()
    private var pendingSession: BaziCloudSession? = null
    private var pendingEnvelope: BaziCloudKeyEnvelope? = null
    private var pendingDocument: BaziCloudDocument? = null
    private var pendingLocalHasData = false

    fun restorePersistedState() {
        if (!gateway.configured) { mutableState.value = BaziCloudSyncState.Unconfigured; return }
        val stored = runCatching { credentialStore.read() }.getOrNull()
        mutableState.value = stored?.let {
            BaziCloudSyncState.Ready(
                email = it.session.email,
                displayName = it.session.displayName,
                avatarUrl = it.session.avatarUrl,
                revision = it.metadata.remoteRevision,
                lastSyncedAt = it.metadata.lastSyncedAt,
                message = "已登录，等待同步",
            )
        }
            ?: BaziCloudSyncState.SignedOut
        stored?.vaultKey?.fill(0)
    }

    suspend fun signInWithGoogle(idToken: String, nonce: String, displayName: String?, avatarUrl: String?) {
        if (!gateway.configured) { mutableState.value = BaziCloudSyncState.Unconfigured; return }
        mutableState.value = BaziCloudSyncState.Working("正在验证 Google 账号…")
        try {
            val session = gateway.signInWithGoogle(idToken, nonce).copy(
                displayName = displayName,
                avatarUrl = avatarUrl,
            )
            val stored = credentialStore.read()
            if (stored?.session?.userId == session.userId) {
                credentialStore.write(stored.copy(session = session))
                stored.vaultKey.fill(0)
                // Selecting the already connected account only refreshes identity.
                // It must not start a write/restore decision while the account page is
                // being opened, otherwise a harmless re-login can repeatedly surface a
                // two-sided conflict.
                restorePersistedState()
                return
            }
            stored?.vaultKey?.fill(0)
            pendingSession = session
            pendingEnvelope = gateway.readKey(session)
            pendingDocument = gateway.readDocument(session)
            val local = snapshotBridge.capture()
            pendingLocalHasData = local.meaningfulCaseCount > 0
            local.payload.fill(0)
            mutableState.value = BaziCloudSyncState.AccountEntryChoice(
                email = session.email,
                displayName = session.displayName,
                avatarUrl = session.avatarUrl,
                hasRemoteData = pendingDocument != null,
                localHasData = pendingLocalHasData,
            )
        } catch (cancelled: CancellationException) {
            clearPending()
            restorePersistedState()
            throw cancelled
        } catch (error: Exception) {
            fail("Google 登录未完成，请稍后重试", error)
        }
    }

    suspend fun continueAccountEntry() {
        val session = pendingSession ?: run { mutableState.value = BaziCloudSyncState.SignedOut; return }
        val envelope = pendingEnvelope
        if (envelope == null) {
            bootstrapNewAccount(session)
        } else {
            mutableState.value = BaziCloudSyncState.RecoveryCodeRequired
        }
    }

    fun cancelPendingSignIn() {
        clearPending()
        restorePersistedState()
    }

    suspend fun loadCachedGoogleAvatar(avatarUrl: String): ByteArray? {
        val stored = credentialStore.read() ?: return null
        return try { avatarCache.read(stored.session.userId, avatarUrl) }
        finally { stored.vaultKey.fill(0) }
    }

    suspend fun refreshGoogleAvatar(avatarUrl: String): ByteArray? {
        val stored = credentialStore.read() ?: return null
        return try {
            (gateway.fetchGoogleAvatar(stored.session, avatarUrl)
                ?: gateway.fetchGoogleAvatarDirect(avatarUrl))?.also {
                avatarCache.write(stored.session.userId, avatarUrl, it)
            }
        } finally { stored.vaultKey.fill(0) }
    }

    /** 保留给尚未接入两阶段渲染的入口：优先缓存，缓存缺失才请求。 */
    suspend fun loadGoogleAvatar(avatarUrl: String): ByteArray? =
        loadCachedGoogleAvatar(avatarUrl) ?: refreshGoogleAvatar(avatarUrl)

    /** 兼容旧会话：补齐 Supabase 已验证身份资料，不读取或上传任何命例。 */
    suspend fun refreshGoogleIdentity() {
        val stored = credentialStore.read() ?: return
        try {
            val identity = gateway.readGoogleIdentity(stored.session)
            val refreshed = stored.session.copy(
                displayName = identity.displayName ?: stored.session.displayName,
                avatarUrl = identity.avatarUrl ?: stored.session.avatarUrl,
            )
            if (refreshed == stored.session) return
            val updated = stored.copy(session = refreshed)
            credentialStore.write(updated)
            val current = mutableState.value
            if (current is BaziCloudSyncState.Ready) {
                ready(updated, current.message)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // 身份资料刷新失败不影响已建立的本机命例或云端同步会话。
        } finally {
            stored.vaultKey.fill(0)
        }
    }

    suspend fun recoverWithCode(code: CharArray) {
        val session = pendingSession ?: run { mutableState.value = BaziCloudSyncState.SignedOut; return }
        val envelope = pendingEnvelope ?: run { mutableState.value = BaziCloudSyncState.SignedOut; return }
        mutableState.value = BaziCloudSyncState.Working("正在验证恢复码…")
        try {
            val key = BaziCloudCrypto.unwrap(envelope, code)
            credentialStore.write(BaziCloudCredentials(session, key, UUID.randomUUID().toString(), BaziCloudMetadata()))
            key.fill(0)
            onSignedIn()
            val remote = pendingDocument
            val localHasData = pendingLocalHasData
            clearPending()
            if (remote != null && !localHasData) {
                restoreRemote(remote)
            } else if (remote != null && localHasData) {
                overwriteRemoteWithLocal()
            } else {
                syncNow()
            }
        } catch (cancelled: CancellationException) {
            restorePersistedState()
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = BaziCloudSyncState.RecoveryCodeRequired
        } finally { code.fill('\u0000') }
    }

    suspend fun acknowledgeRecoveryCode() {
        val stored = credentialStore.read() ?: return
        stored.vaultKey.fill(0)
        clearPending()
        onSignedIn()
        // 恢复为已登录状态后再同步，即使页面离开导致任务取消，
        // 一次性恢复码也不会重新暴露或留在“正在处理”状态。
        restorePersistedState()
        syncNow()
    }

    /**
     * Used only by the delayed worker that Room schedules after a local table
     * invalidation.  A case can be invalidated by device-local view history, so
     * avoid turning an unchanged cloud snapshot into a visible sync attempt.
     * Manual and periodic syncs still read the remote document normally.
     */
    suspend fun syncAfterLocalChange(): BaziCloudSyncRunResult {
        val shouldSync = mutex.withLock {
            val stored = credentialStore.read() ?: return@withLock false
            try {
                val local = snapshotBridge.capture()
                try {
                    stored.metadata.localFingerprint != local.fingerprint
                } finally {
                    local.payload.fill(0)
                }
            } finally {
                stored.vaultKey.fill(0)
            }
        }
        return if (shouldSync) syncNow(BaziCloudSyncOrigin.BACKGROUND) else BaziCloudSyncRunResult.SKIPPED
    }

    suspend fun syncNow(): BaziCloudSyncRunResult = syncNow(BaziCloudSyncOrigin.MANUAL)

    suspend fun syncInBackground(): BaziCloudSyncRunResult = syncNow(BaziCloudSyncOrigin.BACKGROUND)

    private suspend fun syncNow(origin: BaziCloudSyncOrigin): BaziCloudSyncRunResult = mutex.withLock {
        val stored = credentialStore.read() ?: run {
            mutableState.value = if (gateway.configured) BaziCloudSyncState.SignedOut else BaziCloudSyncState.Unconfigured
            return@withLock BaziCloudSyncRunResult.SKIPPED
        }
        var activeCredentials = stored
        progress(origin, "正在生成本机结构化快照…")
        try {
            val refreshed = gateway.refresh(stored.session)
            val credentials = stored.copy(session = refreshed)
            activeCredentials = credentials
            credentialStore.write(credentials)
            val local = snapshotBridge.capture()
            progress(origin, "正在检查南枫云版本…")
            val remoteMetadata = gateway.readDocumentMetadata(credentials.session)
            try {
                when (
                    decideBaziCloudSyncAction(
                        localHasData = local.meaningfulCaseCount > 0,
                        remoteRevision = remoteMetadata?.revision,
                        knownRemoteRevision = credentials.metadata.remoteRevision,
                        localFingerprintMatches = credentials.metadata.localFingerprint == local.fingerprint,
                    )
                ) {
                    BaziCloudSyncAction.ALREADY_CURRENT -> {
                        ready(credentials, "本机与云端数据一致")
                        BaziCloudSyncRunResult.SUCCESS
                    }
                    BaziCloudSyncAction.UPLOAD_LOCAL -> {
                        upload(credentials, local, remoteMetadata?.revision ?: 0, origin)
                        BaziCloudSyncRunResult.SUCCESS
                    }
                    BaziCloudSyncAction.RESTORE_REMOTE -> restoreRemote(
                        checkNotNull(gateway.readDocument(credentials.session)),
                        origin,
                    )
                }
            } finally { local.payload.fill(0) }
        } catch (cancelled: CancellationException) {
            // 页面离开、WorkManager 停止或应用更新都属于正常取消，
            // 不是网络失败，也不应把 JobCancellationException 暴露给用户。
            restorePersistedState()
            throw cancelled
        } catch (error: Exception) {
            handleSyncFailure(activeCredentials, "云端同步未完成，请检查网络后重试", error, origin)
        } finally { stored.vaultKey.fill(0) }
    }

    suspend fun overwriteRemoteWithLocal(): BaziCloudSyncRunResult = mutex.withLock {
        val stored = credentialStore.read() ?: return@withLock BaziCloudSyncRunResult.SKIPPED
        var activeCredentials = stored
        mutableState.value = BaziCloudSyncState.Working("正在以本机数据更新云端…")
        try {
            val credentials = stored.copy(session = gateway.refresh(stored.session))
            activeCredentials = credentials
            credentialStore.write(credentials)
            val local = snapshotBridge.capture()
            try {
                upload(
                    credentials,
                    local,
                    gateway.readDocumentMetadata(credentials.session)?.revision ?: 0,
                    BaziCloudSyncOrigin.MANUAL,
                )
                BaziCloudSyncRunResult.SUCCESS
            }
            finally { local.payload.fill(0) }
        } catch (cancelled: CancellationException) {
            restorePersistedState()
            throw cancelled
        } catch (error: Exception) {
            handleSyncFailure(activeCredentials, "云端数据未更新", error, BaziCloudSyncOrigin.MANUAL)
        } finally { stored.vaultKey.fill(0) }
    }

    suspend fun signOut() {
        val stored = runCatching { credentialStore.read() }.getOrNull()
        try { stored?.let { gateway.signOut(it.session) } } finally {
            stored?.let { avatarCache.clear(it.session.userId) }
            credentialStore.clear(); stored?.vaultKey?.fill(0); clearPending(); onSignedOut(); mutableState.value = BaziCloudSyncState.SignedOut
        }
    }

    private suspend fun bootstrapNewAccount(session: BaziCloudSession) {
        mutableState.value = BaziCloudSyncState.Working("正在创建端到端加密保险库…")
        val key = BaziCloudCrypto.generateVaultKey()
        val recovery = BaziCloudCrypto.generateRecoveryCode()
        try {
            val wrapped = BaziCloudCrypto.wrap(key, recovery)
            gateway.createKey(session, BaziCloudKeyEnvelope(session.userId, wrapped.ciphertext, wrapped.salt, wrapped.nonce))
            credentialStore.write(BaziCloudCredentials(session, key, UUID.randomUUID().toString(), BaziCloudMetadata()))
            mutableState.value = BaziCloudSyncState.RecoveryCodeReady(recovery.concatToString())
        } catch (cancelled: CancellationException) {
            clearPending()
            restorePersistedState()
            throw cancelled
        } catch (error: Exception) {
            fail("无法创建云端加密保险库", error)
        } finally { key.fill(0); recovery.fill('\u0000') }
    }

    private suspend fun restoreRemote(
        remote: BaziCloudDocument,
        origin: BaziCloudSyncOrigin = BaziCloudSyncOrigin.MANUAL,
    ): BaziCloudSyncRunResult {
        val stored = credentialStore.read() ?: return BaziCloudSyncRunResult.SKIPPED
        progress(origin, "正在验证并恢复云端命例…")
        try {
            val payload = BaziCloudCrypto.decrypt(remote, stored.vaultKey)
            try {
                snapshotBridge.validateRemote(payload)
                snapshotBridge.restoreIntoEmpty(payload)
            } finally { payload.fill(0) }
            val local = snapshotBridge.capture()
            val metadata = BaziCloudMetadata(remote.revision, local.fingerprint, System.currentTimeMillis())
            local.payload.fill(0)
            val updated = stored.copy(metadata = metadata)
            credentialStore.write(updated)
            ready(updated, "云端结构化命例已恢复；来源图片仍只保留在原设备")
            return BaziCloudSyncRunResult.SUCCESS
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return handleSyncFailure(stored, "云端数据没有写入本机", error, origin)
        } finally { stored.vaultKey.fill(0) }
    }

    private suspend fun upload(
        credentials: BaziCloudCredentials,
        local: BaziCloudSnapshot,
        expectedRevision: Long,
        origin: BaziCloudSyncOrigin,
    ) {
        progress(origin, "正在加密并上传结构化快照…")
        val encrypted = BaziCloudCrypto.encrypt(local.payload, credentials.vaultKey)
        val size = java.util.Base64.getUrlDecoder().decode(encrypted.ciphertext).size
        require(size in 1..16_777_216) { "云端结构化快照超过 16 MiB 上限" }
        // A second device may finish a backup in the small interval between the
        // read above and this atomic write.  This product's confirmed policy is
        // local-device backup, so retry once against that latest revision instead
        // of surfacing a manual conflict workflow.
        val revision = commitWithReconciliation(credentials, expectedRevision, encrypted, size)
        progress(origin, "正在回读校验云端快照…")
        val readBack = gateway.readDocument(credentials.session)
        val readBackCiphertext = readBack?.ciphertext?.let(java.util.Base64.getUrlDecoder()::decode)
        try {
            check(
                readBack?.revision == revision &&
                    readBack.ciphertextSha256 == encrypted.sha256 &&
                    readBackCiphertext != null &&
                    BaziCloudCrypto.sha256(readBackCiphertext) == encrypted.sha256
            ) { "云端回读校验失败" }
        } finally {
            readBackCiphertext?.fill(0)
        }
        val updated = credentials.copy(metadata = BaziCloudMetadata(revision, local.fingerprint, System.currentTimeMillis()))
        credentialStore.write(updated)
        ready(updated, "端到端加密数据已上传并完成回读校验")
    }

    private suspend fun commitWithReconciliation(
        credentials: BaziCloudCredentials,
        expectedRevision: Long,
        encrypted: BaziCloudCrypto.Encrypted,
        size: Int,
    ): Long {
        val firstRevision = try {
            gateway.commit(credentials.session, expectedRevision, encrypted, size, credentials.deviceId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (!isBaziCloudRetryable(error)) throw error
            null
        }
        if (firstRevision != null) return firstRevision

        // 提交响应可能在代理切换时丢失。先回读密文哈希；若云端已经收到，直接确认成功，
        // 否则才基于最新 revision 再提交一次，避免盲目重复写入。
        val latest = gateway.readDocumentMetadata(credentials.session)
        if (latest?.ciphertextSha256 == encrypted.sha256) return latest.revision
        val retryRevision = gateway.commit(
            credentials.session,
            latest?.revision ?: expectedRevision,
            encrypted,
            size,
            credentials.deviceId,
        )
        if (retryRevision != null) return retryRevision
        return gateway.readDocumentMetadata(credentials.session)
            ?.takeIf { it.ciphertextSha256 == encrypted.sha256 }
            ?.revision
            ?: error("云端版本在上传时连续更新，请稍后重试")
    }

    private fun ready(credentials: BaziCloudCredentials, message: String) {
        mutableState.value = BaziCloudSyncState.Ready(
            email = credentials.session.email,
            displayName = credentials.session.displayName,
            avatarUrl = credentials.session.avatarUrl,
            revision = credentials.metadata.remoteRevision,
            lastSyncedAt = credentials.metadata.lastSyncedAt,
            message = message,
        )
    }

    private fun progress(origin: BaziCloudSyncOrigin, message: String) {
        if (origin == BaziCloudSyncOrigin.MANUAL) {
            mutableState.value = BaziCloudSyncState.Working(message)
        }
    }

    private fun handleSyncFailure(
        credentials: BaziCloudCredentials,
        fallbackMessage: String,
        error: Exception,
        origin: BaziCloudSyncOrigin,
    ): BaziCloudSyncRunResult {
        val failure = classifyFailure(fallbackMessage, error)
        Log.w("BaziCloudSync", "sync issue code=${failure.diagnosticCode} retryable=${failure.retryable}")
        return if (failure.retryable) {
            ready(credentials, "连接暂时不稳定，网络恢复后会自动同步")
            if (origin == BaziCloudSyncOrigin.MANUAL) onRetryRequested()
            BaziCloudSyncRunResult.RETRY
        } else {
            mutableState.value = BaziCloudSyncState.Failure(failure.userMessage, failure.diagnosticCode)
            BaziCloudSyncRunResult.FAILURE
        }
    }

    private fun fail(message: String, error: Exception) {
        val failure = classifyFailure(message, error)
        mutableState.value = BaziCloudSyncState.Failure(failure.userMessage, failure.diagnosticCode)
    }

    private fun classifyFailure(message: String, error: Exception): ClassifiedFailure {
        val (userMessage, diagnosticCode) = when (error) {
            is BaziCloudHttpException -> when (error.statusCode) {
                401, 403 -> "Google 账号会话已失效，请重新登录" to "CLOUD_SESSION_EXPIRED"
                408, 429 -> "南枫云暂时繁忙，请稍后重试" to "CLOUD_TEMPORARILY_BUSY"
                in 500..599 -> "南枫云服务暂时不可用，请稍后重试" to "CLOUD_SERVER_ERROR"
                else -> message to "CLOUD_HTTP_${error.statusCode}"
            }
            is IOException -> "暂时无法连接南枫云，请稍后重试" to "CLOUD_NETWORK_IO"
            else -> message to "CLOUD_OPERATION_FAILED"
        }
        return ClassifiedFailure(userMessage, diagnosticCode, isBaziCloudRetryable(error))
    }

    private data class ClassifiedFailure(
        val userMessage: String,
        val diagnosticCode: String,
        val retryable: Boolean,
    )

    private fun clearPending() {
        pendingSession = null; pendingEnvelope = null; pendingDocument = null; pendingLocalHasData = false
    }
}
