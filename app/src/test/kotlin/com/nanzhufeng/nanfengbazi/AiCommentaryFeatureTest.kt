package com.nanzhufeng.nanfengbazi

import java.time.Instant
import java.net.SocketTimeoutException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCommentaryFeatureTest {
    @Test
    fun `千问为长响应保留更长超时并给出可行动提示`() {
        assertEquals(180_000, aiCommentaryReadTimeoutMillis(AiCommentaryProviderId.QWEN))
        assertEquals(120_000, aiCommentaryReadTimeoutMillis(AiCommentaryProviderId.DEEPSEEK))
        assertEquals(
            "千问响应超时，请稍后重试或选择更快模型。",
            aiCommentaryTransportFailureMessage(
                AiCommentaryProviderId.QWEN,
                SocketTimeoutException(),
            ),
        )
    }

    @Test
    fun `OpenRouter 仅为明确的短暂服务故障短退避一次`() {
        assertEquals(
            2_000L,
            aiCommentaryAutomaticRetryDelayMillis(
                AiCommentaryProviderId.OPEN_ROUTER,
                429,
                "2",
            ),
        )
        assertEquals(
            750L,
            aiCommentaryAutomaticRetryDelayMillis(
                AiCommentaryProviderId.OPEN_ROUTER,
                503,
                null,
            ),
        )
        assertEquals(
            null,
            aiCommentaryAutomaticRetryDelayMillis(
                AiCommentaryProviderId.OPEN_ROUTER,
                408,
                "1",
            ),
        )
        assertEquals(
            null,
            aiCommentaryAutomaticRetryDelayMillis(
                AiCommentaryProviderId.DEEPSEEK,
                503,
                "1",
            ),
        )
    }

    @Test
    fun `OpenRouter 403 安全策略与权限被分开呈现且不保留原始错误体`() {
        assertEquals(
            "OpenRouter 安全策略拦截了本次材料（HTTP 403），请检查该 Key 的 Guardrail 或内容策略。",
            aiCommentaryHttpFailureMessage(
                AiCommentaryProviderId.OPEN_ROUTER,
                403,
                """{"error":{"message":"Request blocked: prompt injection patterns detected"}}""",
            ),
        )
        assertEquals(
            "OpenRouter 拒绝本次请求（HTTP 403），请检查该 Key 的模型白名单、用量上限或组织权限。",
            aiCommentaryHttpFailureMessage(
                AiCommentaryProviderId.OPEN_ROUTER,
                403,
                """{"error":{"message":"permission denied"}}""",
            ),
        )
    }

    @Test
    fun `流式响应仅聚合最终正文与用量`() {
        val payload = decodeAiCommentaryResponsePayload(
            """
            data: {"choices":[{"delta":{"reasoning_content":"先分析"}}]}
            data: {"choices":[{"delta":{"content":"{\\\"summary\\\":\\\"结论"}}]}
            data: {"choices":[{"delta":{"content":"\\\"}"}}],"usage":{"prompt_tokens":123,"completion_tokens":45}}
            data: [DONE]
            """.trimIndent(),
        )

        requireNotNull(payload)
        assertEquals("{\\\"summary\\\":\\\"结论\\\"}", payload.content)
        assertEquals(123L, payload.inputTokens)
        assertEquals(45L, payload.outputTokens)
    }

    @Test
    fun `截图录入拒绝已知的千问纯文本模型但允许视觉模型`() {
        assertEquals(
            "当前千问模型 qwen3.7-max 仅支持文本输入，不能用于截图识别；请改用支持图片输入的视觉模型后再识别。",
            aiVisionModelSupportMessage(
                AiCommentaryProviderConfig(
                    providerId = AiCommentaryProviderId.QWEN,
                    enabled = true,
                    baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
                    model = "qwen3.7-max",
                ),
            ),
        )
        assertEquals(
            null,
            aiVisionModelSupportMessage(
                AiCommentaryProviderConfig(
                    providerId = AiCommentaryProviderId.OPEN_ROUTER,
                    enabled = true,
                    baseUrl = "https://openrouter.ai/api/v1",
                    model = "openai/gpt-5.6-terra",
                ),
            ),
        )
    }

    @Test
    fun `三家默认服务使用固定地址和当前主流分档模型`() {
        val openRouter = AiCommentaryProviderPresets.defaults(AiCommentaryProviderId.OPEN_ROUTER)
        val deepSeek = AiCommentaryProviderPresets.defaults(AiCommentaryProviderId.DEEPSEEK)
        val qwen = AiCommentaryProviderPresets.defaults(AiCommentaryProviderId.QWEN)

        assertEquals("https://openrouter.ai/api/v1", openRouter.baseUrl)
        assertEquals("openrouter/auto", openRouter.model)
        assertEquals("https://api.deepseek.com", deepSeek.baseUrl)
        assertEquals("deepseek-v4-pro", deepSeek.model)
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", qwen.baseUrl)
        assertEquals("qwen3.7-max", qwen.model)
        assertFalse(openRouter.enabled)
        assertFalse(deepSeek.enabled)
        assertFalse(qwen.enabled)
        assertEquals(
            listOf(
                "openrouter/auto",
                "openai/gpt-5.6-sol", "openai/gpt-5.6-terra", "openai/gpt-5.6-luna",
                "anthropic/claude-fable-5", "anthropic/claude-opus-5",
                "anthropic/claude-sonnet-5", "anthropic/claude-haiku-4.5",
                "deepseek/deepseek-v4-pro", "deepseek/deepseek-v4-flash",
                "qwen/qwen3.8-max", "qwen/qwen3.7-max",
                "qwen/qwen3.7-plus", "qwen/qwen3.7-flash",
            ),
            AiCommentaryProviderPresets.models(AiCommentaryProviderId.OPEN_ROUTER).map { it.model },
        )
        assertEquals(
            listOf("deepseek-v4-pro", "deepseek-v4-flash"),
            AiCommentaryProviderPresets.models(AiCommentaryProviderId.DEEPSEEK).map { it.model },
        )
        assertEquals(
            listOf("qwen3.7-max", "qwen3.7-plus", "qwen3.7-flash"),
            AiCommentaryProviderPresets.models(AiCommentaryProviderId.QWEN).map { it.model },
        )
    }

    @Test
    fun `模型 JSON 被固定渲染成可保存的点评章节`() {
        val root = Json.parseToJsonElement(
            """{"summary":"核心结论","blindSchool":"盲派观察","ziping":"子平核对","uncertainty":"仍需现实核验"}""",
        ).jsonObject

        assertEquals(
            "【核心判断】\n核心结论\n\n" +
                "【盲派断事观察】\n盲派观察\n\n" +
                "【子平交叉核对】\n子平核对\n\n" +
                "【依据与不确定性】\n仍需现实核验",
            renderStructuredCommentary(root),
        )
    }

    @Test
    fun `保存正文保留服务模型提示版本和资料编号`() {
        val content = AiCommentaryDraft(
            providerId = AiCommentaryProviderId.DEEPSEEK,
            providerName = "DeepSeek",
            model = "deepseek-v4-flash",
            promptId = "ai-prompt-1234567890abcdef",
            promptVersion = 1,
            generatedAt = Instant.parse("2026-08-10T08:00:00Z"),
            content = "【核心判断】\n测试",
            inputTokens = 120,
            outputTokens = 80,
        ).toRecordContent()

        assertTrue(content.startsWith(AI_COMMENTARY_MARKER))
        assertTrue(content.contains("服务：DeepSeek"))
        assertTrue(content.contains("模型：deepseek-v4-flash"))
        assertTrue(content.contains("提示版本：1"))
        assertTrue(content.contains("资料编号：ai-prompt-1234567890abcdef"))
        assertTrue(content.contains("用量：输入 120 / 输出 80 tokens"))
        assertTrue(content.endsWith("【核心判断】\n测试"))
    }

    @Test
    fun `调用记录只持久模型用量和状态摘要`() {
        val record = AiCommentaryCallRecord(
            id = "call-1",
            requestedAtEpochMillis = 1_786_348_800_000L,
            durationMillis = 1_280L,
            providerName = "DeepSeek",
            model = "deepseek-v4-pro",
            succeeded = false,
            inputTokens = 120L,
            outputTokens = 0L,
            errorSummary = "请求超时",
        )

        val payload = Json.encodeToString(AiCommentaryCallRecord.serializer(), record)
        val restored = Json.decodeFromString(AiCommentaryCallRecord.serializer(), payload)

        assertEquals(record, restored)
        assertFalse(payload.contains("apiKey", ignoreCase = true))
        assertFalse(payload.contains("promptText", ignoreCase = true))
        assertFalse(payload.contains("命盘正文"))
    }
}
