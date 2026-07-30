package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WenzhenPageClassifierTest {
    private val classifier = AnchorBasedWenzhenPageClassifier()

    @Test
    fun `四类 P0 页面都要求标题和至少一个辅助锚点`() {
        val cases = mapOf(
            WenzhenPageType.USER_LIST to "问真八字 用户列表 名人库 筛选 阳历1992年8月24日",
            WenzhenPageType.BASIC_INFO to "问真八字 基本信息 出生地区 真太阳时 五行能量",
            WenzhenPageType.FEEDBACK to "命主反馈 职业 学历 关键事件反馈记录",
            WenzhenPageType.COMMENTARY to "师傅点评 旺衰 格局 总结",
        )

        cases.forEach { (expected, text) ->
            val result = classifier.classify(document(text))
            assertEquals(expected, result.pageType)
            assertTrue(result.matchedAnchors.size >= 2)
            assertTrue(result.confidence >= 0.7f)
        }
    }

    @Test
    fun `只有单一关键词时保持未知而不猜测页面`() {
        val result = classifier.classify(document("这张合成图片只出现师傅点评"))
        assertEquals(WenzhenPageType.UNKNOWN, result.pageType)
        assertEquals(0f, result.confidence)
    }

    @Test
    fun `OCR 输出常见繁体锚点时归一化但保留双锚点门禁`() {
        val result = classifier.classify(
            document("問真八字 用戶列表 名人库 篩選 陽歷1992年8月24日"),
        )
        assertEquals(WenzhenPageType.USER_LIST, result.pageType)
        assertTrue(result.matchedAnchors.contains("用户列表"))
    }

    private fun document(text: String): OcrDocument = OcrDocument(
        imageId = "image-1",
        rawText = text,
        blocks = emptyList(),
        engineId = "fixture-offline",
        engineVersion = "1",
        recognizedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
