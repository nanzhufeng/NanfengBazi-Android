package com.nanzhufeng.nanfengbazi

import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCommentaryFeatureTest {
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
