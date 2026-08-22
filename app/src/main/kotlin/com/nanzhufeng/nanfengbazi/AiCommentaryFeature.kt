package com.nanzhufeng.nanfengbazi

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal const val AI_COMMENTARY_PROMPT_VERSION = 1
internal const val AI_COMMENTARY_MARKER = "【AI 点评】"
private const val DEFAULT_AI_READ_TIMEOUT_MILLIS = 120_000
private const val QWEN_AI_READ_TIMEOUT_MILLIS = 180_000
private const val OPEN_ROUTER_MAX_AUTOMATIC_RETRY_DELAY_MILLIS = 5_000L
private const val OPEN_ROUTER_FALLBACK_RETRY_DELAY_MILLIS = 750L
private const val MAX_AUTOMATIC_RESPONSE_RETRIES = 1
private const val MAX_AI_RESPONSE_BYTES = 4 * 1024 * 1024

enum class AiCommentaryProviderId(val displayName: String) {
    OPEN_ROUTER("OpenRouter"),
    DEEPSEEK("DeepSeek"),
    QWEN("千问"),
}

internal fun aiCommentaryReadTimeoutMillis(providerId: AiCommentaryProviderId): Int = when (providerId) {
    AiCommentaryProviderId.QWEN -> QWEN_AI_READ_TIMEOUT_MILLIS
    AiCommentaryProviderId.OPEN_ROUTER,
    AiCommentaryProviderId.DEEPSEEK,
    -> DEFAULT_AI_READ_TIMEOUT_MILLIS
}

internal fun aiCommentaryTransportFailureMessage(
    providerId: AiCommentaryProviderId,
    cause: Throwable,
): String = when (cause) {
    is SocketTimeoutException ->
        "${providerId.displayName}响应超时，请稍后重试或选择更快模型。"
    is UnknownHostException ->
        "无法连接${providerId.displayName}服务域名，请检查当前网络或代理规则。"
    is IOException ->
        "${providerId.displayName}网络连接中断，请检查网络后重试。"
    else ->
        "${providerId.displayName}请求未能发出，请返回后重试。"
}

/**
 * Only retry responses that OpenRouter explicitly documents as transient before a model result
 * exists. Transport timeouts are intentionally excluded: a POST may already have been accepted
 * upstream and retrying it silently can duplicate cost.
 */
internal fun aiCommentaryAutomaticRetryDelayMillis(
    providerId: AiCommentaryProviderId,
    statusCode: Int,
    retryAfterHeader: String?,
): Long? {
    if (providerId != AiCommentaryProviderId.OPEN_ROUTER || statusCode !in setOf(429, 503)) {
        return null
    }
    val retryAfterMillis = retryAfterHeader?.trim()?.toLongOrNull()
        ?.times(1_000L)
        ?.takeIf { it in 1L..OPEN_ROUTER_MAX_AUTOMATIC_RETRY_DELAY_MILLIS }
    return retryAfterMillis ?: OPEN_ROUTER_FALLBACK_RETRY_DELAY_MILLIS.takeIf { statusCode == 503 }
}

/** Keeps provider diagnostics actionable without retaining a response body or user material. */
internal fun aiCommentaryHttpFailureMessage(
    providerId: AiCommentaryProviderId,
    statusCode: Int,
    responseBody: String,
): String {
    if (providerId == AiCommentaryProviderId.OPEN_ROUTER && statusCode == 403) {
        val errorMessage = runCatching {
            Json.parseToJsonElement(responseBody).jsonObject["error"]?.jsonObject
                ?.get("message")?.jsonPrimitive?.contentOrNull.orEmpty()
        }.getOrDefault("").lowercase()
        return when {
            errorMessage.contains("guardrail") ||
                errorMessage.contains("prompt injection") ||
                errorMessage.contains("moderation") ||
                errorMessage.contains("content policy") ->
                "OpenRouter 安全策略拦截了本次材料（HTTP 403），请检查该 Key 的 Guardrail 或内容策略。"
            else ->
                "OpenRouter 拒绝本次请求（HTTP 403），请检查该 Key 的模型白名单、用量上限或组织权限。"
        }
    }
    val hint = when (statusCode) {
        401, 403 -> "请检查 API Key 与权限"
        402 -> "账户余额不足"
        408 -> "请求超时"
        429 -> "请求过于频繁，请稍后重试"
        else -> "请检查模型名称、接口地址和服务状态"
    }
    return "请求失败（HTTP $statusCode）：$hint。"
}

/**
 * Screenshot intake sends an image, unlike commentary. Do not spend time or quota sending that
 * payload to a model which is publicly documented as text-only.
 */
internal fun aiVisionModelSupportMessage(config: AiCommentaryProviderConfig): String? = when {
    config.providerId == AiCommentaryProviderId.QWEN &&
        config.model.startsWith("qwen3.7-max") ->
        "当前千问模型 ${config.model} 仅支持文本输入，不能用于截图识别；请改用支持图片输入的视觉模型后再识别。"
    else -> null
}

internal data class AiCommentaryResponsePayload(
    val content: String,
    val inputTokens: Long?,
    val outputTokens: Long?,
)

/**
 * The providers use the OpenAI SSE shape for streaming responses. We only retain the completed
 * content and token counts in memory; neither the wire payload nor reasoning fragments are logged.
 */
internal fun decodeAiCommentaryResponsePayload(responseBody: String): AiCommentaryResponsePayload? {
    val directRoot = runCatching { Json.parseToJsonElement(responseBody).jsonObject }.getOrNull()
    directRoot?.let { root ->
        val content = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
        if (content != null) {
            val usage = root["usage"] as? JsonObject
            return AiCommentaryResponsePayload(
                content = content,
                inputTokens = usage?.longValue("prompt_tokens"),
                outputTokens = usage?.longValue("completion_tokens"),
            )
        }
    }

    val content = StringBuilder()
    var inputTokens: Long? = null
    var outputTokens: Long? = null
    var receivedEvent = false
    responseBody.lineSequence().forEach { line ->
        val data = line.trim().removePrefix("data:").trim()
        if (!line.trim().startsWith("data:") || data == "[DONE]") return@forEach
        val event = runCatching { Json.parseToJsonElement(data).jsonObject }.getOrNull()
            ?: return@forEach
        receivedEvent = true
        event["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
            ?.let(content::append)
        val usage = event["usage"] as? JsonObject
        inputTokens = usage?.longValue("prompt_tokens") ?: inputTokens
        outputTokens = usage?.longValue("completion_tokens") ?: outputTokens
    }
    return if (receivedEvent && content.isNotEmpty()) {
        AiCommentaryResponsePayload(content.toString(), inputTokens, outputTokens)
    } else {
        null
    }
}

data class AiCommentaryProviderConfig(
    val providerId: AiCommentaryProviderId,
    val enabled: Boolean,
    val baseUrl: String,
    val model: String,
    val hasApiKey: Boolean = false,
)

internal data class AiCommentaryModelPreset(
    val label: String,
    val summary: String,
    val model: String,
)

internal object AiCommentaryProviderPresets {
    val providerIds = listOf(
        AiCommentaryProviderId.OPEN_ROUTER,
        AiCommentaryProviderId.DEEPSEEK,
        AiCommentaryProviderId.QWEN,
    )

    fun defaults(providerId: AiCommentaryProviderId): AiCommentaryProviderConfig = when (providerId) {
        AiCommentaryProviderId.OPEN_ROUTER -> AiCommentaryProviderConfig(
            providerId,
            enabled = false,
            baseUrl = "https://openrouter.ai/api/v1",
            model = models(providerId).first().model,
        )
        AiCommentaryProviderId.DEEPSEEK -> AiCommentaryProviderConfig(
            providerId,
            enabled = false,
            baseUrl = "https://api.deepseek.com",
            model = models(providerId).first().model,
        )
        AiCommentaryProviderId.QWEN -> AiCommentaryProviderConfig(
            providerId,
            enabled = false,
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            model = models(providerId).first().model,
        )
    }

    fun models(providerId: AiCommentaryProviderId): List<AiCommentaryModelPreset> = when (providerId) {
        AiCommentaryProviderId.OPEN_ROUTER -> listOf(
            AiCommentaryModelPreset("自动选择", "OpenRouter 按可用性与任务自动路由", "openrouter/auto"),
            AiCommentaryModelPreset("OpenAI · GPT-5.6 Sol", "旗舰档 · 复杂专业分析", "openai/gpt-5.6-sol"),
            AiCommentaryModelPreset("OpenAI · GPT-5.6 Terra", "均衡档 · 能力与成本平衡", "openai/gpt-5.6-terra"),
            AiCommentaryModelPreset("OpenAI · GPT-5.6 Luna", "快速档 · 低成本高吞吐", "openai/gpt-5.6-luna"),
            AiCommentaryModelPreset("Anthropic · Claude Fable 5", "最高能力档 · 长时间复杂任务", "anthropic/claude-fable-5"),
            AiCommentaryModelPreset("Anthropic · Claude Opus 5", "旗舰档 · 复杂推理与专业工作", "anthropic/claude-opus-5"),
            AiCommentaryModelPreset("Anthropic · Claude Sonnet 5", "均衡档 · 速度与智能平衡", "anthropic/claude-sonnet-5"),
            AiCommentaryModelPreset("Anthropic · Claude Haiku 4.5", "快速档 · 低延迟", "anthropic/claude-haiku-4.5"),
            AiCommentaryModelPreset("DeepSeek · V4 Pro", "高能力档 · 复杂分析", "deepseek/deepseek-v4-pro"),
            AiCommentaryModelPreset("DeepSeek · V4 Flash", "快速档 · 成本与速度优先", "deepseek/deepseek-v4-flash"),
            AiCommentaryModelPreset("Qwen · 3.8 Max", "最新最高能力档", "qwen/qwen3.8-max"),
            AiCommentaryModelPreset("Qwen · 3.7 Max", "旗舰档 · 复杂推理", "qwen/qwen3.7-max"),
            AiCommentaryModelPreset("Qwen · 3.7 Plus", "均衡档 · 综合能力与成本平衡", "qwen/qwen3.7-plus"),
            AiCommentaryModelPreset("Qwen · 3.7 Flash", "快速档 · 低成本", "qwen/qwen3.7-flash"),
        )
        AiCommentaryProviderId.DEEPSEEK -> listOf(
            AiCommentaryModelPreset("DeepSeek V4 Pro", "高能力档 · 复杂分析优先", "deepseek-v4-pro"),
            AiCommentaryModelPreset("DeepSeek V4 Flash", "快速档 · 速度与成本优先", "deepseek-v4-flash"),
        )
        AiCommentaryProviderId.QWEN -> listOf(
            AiCommentaryModelPreset("Qwen 3.7 Max", "旗舰档 · 复杂推理优先", "qwen3.7-max"),
            AiCommentaryModelPreset("Qwen 3.7 Plus", "均衡档 · 综合能力与成本平衡", "qwen3.7-plus"),
            AiCommentaryModelPreset("Qwen 3.7 Flash", "快速档 · 低成本", "qwen3.7-flash"),
        )
    }

    fun selectedModel(providerId: AiCommentaryProviderId, storedModel: String): String =
        models(providerId).firstOrNull { it.model == storedModel }?.model
            ?: models(providerId).first().model

    fun displayModel(providerId: AiCommentaryProviderId, model: String): String =
        models(providerId).firstOrNull { it.model == model }?.label
            ?: models(providerId).first().label
}

interface AiCommentarySettingsStore {
    fun configs(): Map<AiCommentaryProviderId, AiCommentaryProviderConfig>
    fun selectedProvider(): AiCommentaryProviderId
    fun saveConfig(config: AiCommentaryProviderConfig, apiKey: String?)
    fun selectProvider(providerId: AiCommentaryProviderId)
    fun apiKey(providerId: AiCommentaryProviderId): String?
}

@Serializable
data class AiCommentaryCallRecord(
    val id: String,
    val requestedAtEpochMillis: Long,
    val durationMillis: Long,
    val providerName: String,
    val model: String,
    val succeeded: Boolean,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val errorSummary: String? = null,
)

interface AiCommentaryCallLogStore {
    fun records(): List<AiCommentaryCallRecord>
    fun append(record: AiCommentaryCallRecord)
}

internal class LocalAiCommentaryCallLogStore(context: Context) : AiCommentaryCallLogStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES,
        Context.MODE_PRIVATE,
    )
    private val serializer = ListSerializer(AiCommentaryCallRecord.serializer())
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Synchronized
    override fun records(): List<AiCommentaryCallRecord> = preferences
        .getString(RECORDS, null)
        ?.let { payload -> runCatching { json.decodeFromString(serializer, payload) }.getOrNull() }
        .orEmpty()
        .sortedByDescending(AiCommentaryCallRecord::requestedAtEpochMillis)

    @Synchronized
    override fun append(record: AiCommentaryCallRecord) {
        val updated = (listOf(record) + records().filterNot { it.id == record.id }).take(MAX_RECORDS)
        preferences.edit().putString(RECORDS, json.encodeToString(serializer, updated)).apply()
    }

    private companion object {
        const val PREFERENCES = "ai_commentary_call_log"
        const val RECORDS = "records_v1"
        const val MAX_RECORDS = 200
    }
}

internal class SecureAiCommentarySettings(context: Context) : AiCommentarySettingsStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES,
        Context.MODE_PRIVATE,
    )

    override fun configs(): Map<AiCommentaryProviderId, AiCommentaryProviderConfig> =
        AiCommentaryProviderPresets.providerIds.associateWith { providerId ->
            val defaults = AiCommentaryProviderPresets.defaults(providerId)
            defaults.copy(
                enabled = preferences.getBoolean(key(providerId, "enabled"), defaults.enabled),
                baseUrl = defaults.baseUrl,
                model = AiCommentaryProviderPresets.selectedModel(
                    providerId,
                    preferences.getString(key(providerId, "model"), defaults.model)
                        ?.trim().orEmpty(),
                ),
                hasApiKey = !apiKey(providerId).isNullOrBlank(),
            )
        }

    override fun selectedProvider(): AiCommentaryProviderId = preferences
        .getString(SELECTED_PROVIDER, null)
        ?.let { saved -> AiCommentaryProviderId.entries.firstOrNull { it.name == saved } }
        ?: AiCommentaryProviderId.OPEN_ROUTER

    override fun saveConfig(config: AiCommentaryProviderConfig, apiKey: String?) {
        val editor = preferences.edit()
            .putBoolean(key(config.providerId, "enabled"), config.enabled)
            .putString(key(config.providerId, "base"), config.baseUrl.trim())
            .putString(key(config.providerId, "model"), config.model.trim())
        if (apiKey != null) {
            val normalized = apiKey.trim()
            if (normalized.isEmpty()) {
                editor.remove(key(config.providerId, "secret"))
            } else {
                editor.putString(key(config.providerId, "secret"), encrypt(normalized))
            }
        }
        editor.apply()
    }

    override fun selectProvider(providerId: AiCommentaryProviderId) {
        preferences.edit().putString(SELECTED_PROVIDER, providerId.name).apply()
    }

    override fun apiKey(providerId: AiCommentaryProviderId): String? = preferences
        .getString(key(providerId, "secret"), null)
        ?.let(::decrypt)

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(payload: String): String? = runCatching {
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        require(bytes.size > GCM_IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(GCM_TAG_BITS, bytes.copyOfRange(0, GCM_IV_BYTES)),
        )
        String(cipher.doFinal(bytes.copyOfRange(GCM_IV_BYTES, bytes.size)), StandardCharsets.UTF_8)
    }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    private fun key(providerId: AiCommentaryProviderId, suffix: String) =
        "${providerId.name.lowercase()}_$suffix"

    private companion object {
        const val PREFERENCES = "ai_commentary_settings"
        const val SELECTED_PROVIDER = "selected_provider"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "nanfeng.bazi.ai.commentary.credentials.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}

data class AiCommentaryGenerationRequest(
    val provider: AiCommentaryProviderConfig,
    val apiKey: String?,
    val promptId: String,
    val promptText: String,
)

data class AiCommentaryDraft(
    val providerId: AiCommentaryProviderId,
    val providerName: String,
    val model: String,
    val promptId: String,
    val promptVersion: Int,
    val generatedAt: Instant,
    val content: String,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
)

sealed interface AiCommentaryGenerationResult {
    data class Success(val draft: AiCommentaryDraft) : AiCommentaryGenerationResult
    data class Failure(val message: String, val retryable: Boolean) : AiCommentaryGenerationResult
}

fun interface AiCommentaryGenerator {
    suspend fun generate(request: AiCommentaryGenerationRequest): AiCommentaryGenerationResult
}

internal class OpenAiCompatibleAiCommentaryGenerator(
    private val now: () -> Instant = Instant::now,
) : AiCommentaryGenerator {
    override suspend fun generate(
        request: AiCommentaryGenerationRequest,
    ): AiCommentaryGenerationResult = withContext(Dispatchers.IO) {
        val key = request.apiKey?.trim().orEmpty()
        if (key.isEmpty()) {
            return@withContext AiCommentaryGenerationResult.Failure(
                "请先填写 ${request.provider.providerId.displayName} API Key。",
                retryable = false,
            )
        }
        if (!request.provider.enabled) {
            return@withContext AiCommentaryGenerationResult.Failure(
                "请先启用 ${request.provider.providerId.displayName}。",
                retryable = false,
            )
        }
        if (request.provider.baseUrl.isBlank() || request.provider.model.isBlank()) {
            return@withContext AiCommentaryGenerationResult.Failure(
                "模型名称和接口地址不能为空。",
                retryable = false,
            )
        }
        val body = buildJsonObject {
            put("model", request.provider.model)
            put("stream", true)
            put("stream_options", buildJsonObject { put("include_usage", true) })
            put("temperature", 0.2)
            if (request.provider.providerId == AiCommentaryProviderId.QWEN) {
                // This task only needs four concise evidence sections. Avoid an unbounded hidden
                // reasoning pass, which delays the first response enough to cause false timeouts.
                put("enable_thinking", false)
                put("max_tokens", 1_400)
            } else {
                put("response_format", buildJsonObject { put("type", "json_object") })
            }
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", request.promptText + OUTPUT_CONTRACT)
                })
            })
        }
        val endpoint = request.provider.baseUrl.trimEnd('/') + "/chat/completions"
        when (val response = post(endpoint, key, request.provider.providerId, body.toString())) {
            is HttpResult.Failure -> AiCommentaryGenerationResult.Failure(
                response.message,
                response.retryable,
            )
            is HttpResult.Success -> parseResponse(request, response.body)
        }
    }

    private fun parseResponse(
        request: AiCommentaryGenerationRequest,
        responseBody: String,
    ): AiCommentaryGenerationResult {
        val decoded = decodeAiCommentaryResponsePayload(responseBody)
            ?: return AiCommentaryGenerationResult.Failure("模型返回内容无法读取。", false)
        val content = decoded.content
            ?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim()
            .orEmpty()
        val result = runCatching { json.parseToJsonElement(content).jsonObject }.getOrNull()
            ?: return AiCommentaryGenerationResult.Failure(
                "模型没有按固定格式返回点评，请重试或更换模型。",
                false,
            )
        val rendered = renderStructuredCommentary(result)
        if (rendered == null) {
            return AiCommentaryGenerationResult.Failure("模型返回的点评内容为空。", false)
        }
        return AiCommentaryGenerationResult.Success(
            AiCommentaryDraft(
                providerId = request.provider.providerId,
                providerName = request.provider.providerId.displayName,
                model = request.provider.model,
                promptId = request.promptId,
                promptVersion = AI_COMMENTARY_PROMPT_VERSION,
                generatedAt = now(),
                content = rendered,
                inputTokens = decoded.inputTokens,
                outputTokens = decoded.outputTokens,
            ),
        )
    }

    private fun post(
        endpoint: String,
        apiKey: String,
        providerId: AiCommentaryProviderId,
        body: String,
    ): HttpResult {
        repeat(MAX_AUTOMATIC_RESPONSE_RETRIES + 1) { attempt ->
            val result = postOnce(endpoint, apiKey, providerId, body)
            val retryDelayMillis = (result as? HttpResult.Failure)?.automaticRetryDelayMillis
            if (retryDelayMillis == null || attempt == MAX_AUTOMATIC_RESPONSE_RETRIES) {
                return result
            }
            Thread.sleep(retryDelayMillis)
        }
        error("Automatic retry loop should always return a result")
    }

    private fun postOnce(
        endpoint: String,
        apiKey: String,
        providerId: AiCommentaryProviderId,
        body: String,
    ): HttpResult = runCatching {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = aiCommentaryReadTimeoutMillis(providerId)
            doInput = true
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (providerId == AiCommentaryProviderId.OPEN_ROUTER) {
                setRequestProperty("X-OpenRouter-Title", "南枫八字")
                setRequestProperty("X-OpenRouter-Metadata", "enabled")
            }
            outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
        }
        val code = connection.responseCode
        val retryAfterHeader = connection.getHeaderField("Retry-After")
        val payload = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.use { it.readUtf8Bounded(MAX_AI_RESPONSE_BYTES) }.orEmpty()
        connection.disconnect()
        if (code in 200..299) {
            HttpResult.Success(payload)
        } else {
            HttpResult.Failure(
                message = aiCommentaryHttpFailureMessage(providerId, code, payload),
                retryable = code == 408 || code == 429 || code >= 500,
                automaticRetryDelayMillis = aiCommentaryAutomaticRetryDelayMillis(
                    providerId,
                    code,
                    retryAfterHeader,
                ),
            )
        }
    }.getOrElse { cause ->
        HttpResult.Failure(
            message = aiCommentaryTransportFailureMessage(providerId, cause),
            retryable = cause is IOException,
        )
    }

    private sealed interface HttpResult {
        data class Success(val body: String) : HttpResult
        data class Failure(
            val message: String,
            val retryable: Boolean,
            val automaticRetryDelayMillis: Long? = null,
        ) : HttpResult
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        const val SYSTEM_PROMPT =
            "你是南枫八字的命理研究点评助手。只依据用户提供的固定命盘资料，" +
                "严格区分输入事实与传统命理推演，不编造经历，不作确定性断言，只返回 JSON。"
        const val OUTPUT_CONTRACT = """

# 内置 AI 点评输出合同
只返回一个 JSON 对象，不要使用 Markdown 代码围栏。所有字段都必须是简体中文字符串：
{"summary":"核心判断","blindSchool":"盲派断事观察","ziping":"子平交叉核对","careerWealth":"事业与财运","relationshipFamily":"婚恋与六亲","health":"健康提示","keyPeriods":"关键阶段","questions":"待核验问题","uncertainty":"依据与不确定性"}
要求：每段 2—6 条简洁结论；关键阶段必须写出资料中可支持的年份或大运范围及依据；健康仅作传统文化象意提示；不得输出医疗、法律、投资或婚姻决策。
"""
    }
}

internal fun renderStructuredCommentary(root: JsonObject): String? {
    val sections = listOf(
        "核心判断" to root.text("summary"),
        "盲派断事观察" to root.text("blindSchool"),
        "子平交叉核对" to root.text("ziping"),
        "事业与财运" to root.text("careerWealth"),
        "婚恋与六亲" to root.text("relationshipFamily"),
        "健康提示" to root.text("health"),
        "关键阶段" to root.text("keyPeriods"),
        "待核验问题" to root.text("questions"),
        "依据与不确定性" to root.text("uncertainty"),
    ).filter { (_, value) -> value.isNotBlank() }
    if (sections.isEmpty()) return null
    return sections.joinToString("\n\n") { (title, value) -> "【$title】\n$value" }
}

internal fun AiCommentaryDraft.toRecordContent(editedContent: String = content): String = buildString {
    appendLine(AI_COMMENTARY_MARKER)
    appendLine("服务：$providerName")
    appendLine("模型：$model")
    appendLine("提示版本：$promptVersion")
    appendLine("资料编号：$promptId")
    appendLine("生成时间：$generatedAt")
    if (inputTokens != null || outputTokens != null) {
        appendLine("用量：输入 ${inputTokens ?: "未知"} / 输出 ${outputTokens ?: "未知"} tokens")
    }
    appendLine()
    append(editedContent.trim())
}

private fun JsonObject.text(key: String): String = this[key]?.jsonPrimitive
    ?.contentOrNull?.trim().orEmpty()

private fun JsonObject.longValue(key: String): Long? = this[key]?.jsonPrimitive
    ?.contentOrNull?.toLongOrNull()
