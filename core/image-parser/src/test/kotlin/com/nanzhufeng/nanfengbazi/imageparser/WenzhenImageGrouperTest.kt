package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WenzhenImageGrouperTest {
    private val grouper = WenzhenImageGrouper()

    @Test
    fun `横排和上下两行四柱配合出生日期可把多页归为同一候选`() {
        val first = image("basic", WenzhenPageType.BASIC_INFO)
        val second = image("feedback", WenzhenPageType.FEEDBACK)
        val candidates = grouper.group(
            images = listOf(first, second),
            documents = listOf(
                document(
                    first.id,
                    "姓名：合成甲\n阳历：1992年8月24日\n壬申 戊申 壬申 丙午",
                ),
                document(
                    second.id,
                    "姓名：合成甲\n公历1992-08-24\n壬 戊 壬 丙\n申 申 申 午",
                ),
            ),
        )

        assertEquals(1, candidates.size)
        assertEquals(listOf("basic", "feedback"), candidates.single().imageIds)
        assertEquals("合成甲", candidates.single().suggestedAlias)
        assertEquals(0.98f, candidates.single().groupingConfidence)
    }

    @Test
    fun `只有相同姓名时保持两个待核对候选`() {
        val first = image("first", WenzhenPageType.BASIC_INFO)
        val second = image("second", WenzhenPageType.COMMENTARY)

        val candidates = grouper.group(
            images = listOf(first, second),
            documents = listOf(
                document(first.id, "姓名：合成同名\n基本信息"),
                document(second.id, "姓名：合成同名\n师傅点评"),
            ),
        )

        assertEquals(2, candidates.size)
        assertTrue(candidates.all { it.imageIds.size == 1 && it.requiresReview })
    }

    @Test
    fun `相同姓名加出生日期可以归组但仍需人工核对`() {
        val first = image("first", WenzhenPageType.BASIC_INFO)
        val second = image("second", WenzhenPageType.COMMENTARY)

        val candidate = grouper.group(
            images = listOf(first, second),
            documents = listOf(
                document(first.id, "姓名：合成乙\n阳历1995年3月18日\n基本信息"),
                document(second.id, "姓名：合成乙\n公历：1995/3/18\n师傅点评"),
            ),
        ).single()

        assertEquals(0.84f, candidate.groupingConfidence)
        assertTrue(candidate.requiresReview)
    }

    @Test
    fun `任一身份字段冲突时即使四柱相同也不得自动归组`() {
        val first = image("first", WenzhenPageType.BASIC_INFO)
        val second = image("second", WenzhenPageType.FEEDBACK)

        val candidates = grouper.group(
            images = listOf(first, second),
            documents = listOf(
                document(first.id, "姓名：合成丙\n阳历2000年8月5日\n庚辰 癸未 乙未 甲申"),
                document(second.id, "姓名：合成丙\n阳历2000年8月6日\n庚辰 癸未 乙未 甲申"),
            ),
        )

        assertEquals(2, candidates.size)
    }

    @Test
    fun `用户列表页不与详情页直接归组`() {
        val list = image("list", WenzhenPageType.USER_LIST)
        val detail = image("detail", WenzhenPageType.BASIC_INFO)
        val identity = "姓名：合成丁\n阳历2012年12月12日\n壬辰 壬子 丁未 戊申"

        val candidates = grouper.group(
            images = listOf(list, detail),
            documents = listOf(document(list.id, identity), document(detail.id, identity)),
        )

        assertEquals(2, candidates.size)
    }

    private fun image(id: String, pageType: WenzhenPageType) = ImportImageRef(
        id = id,
        originalFileName = "$id.png",
        mimeType = "image/png",
        relativePath = "session/$id.png",
        sha256 = id.first().code.toString(16).padStart(64, '0').takeLast(64),
        byteSize = 1,
        pageType = pageType,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun document(imageId: String, text: String) = OcrDocument(
        imageId = imageId,
        rawText = text,
        blocks = emptyList(),
        engineId = "fixture",
        engineVersion = "1",
        recognizedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
