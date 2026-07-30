package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.model.EvidenceBoundingBox
import com.nanzhufeng.nanfengbazi.domain.model.ImportCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.domain.model.ImportedLongTextType
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.OcrTextBlock
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WenzhenP0ParserTest {
    private val parser = WenzhenP0Parser()

    @Test
    fun `用户列表一张图拆出多条命例且字段等待人工采用`() {
        val image = image("user-list", WenzhenPageType.USER_LIST)
        val document = document(
            imageId = image.id,
            blocks = listOf(
                block("name-1", "案例甲 男", 30, 100, 210, 140),
                block("stems-1", "壬戊壬丙", 560, 105, 760, 125),
                block("date-1", "阳历1992年8月24日 申申申午", 30, 160, 760, 195),
                block("name-2", "案例乙 女", 30, 300, 210, 340),
                block("stems-2", "庚 癸 乙 甲", 560, 305, 760, 325),
                block("branches-2", "辰 未 未 申", 560, 330, 760, 350),
                block("date-2", "阳历2000年8月5日", 30, 360, 300, 395),
            ),
        )

        val result = parser.parse(
            images = listOf(image),
            documents = listOf(document),
            groupedCandidates = listOf(candidate(image.id)),
        )

        assertEquals(2, result.candidates.size)
        assertEquals(listOf("案例甲", "案例乙"), result.candidates.map { it.suggestedAlias })
        assertEquals(8, result.fields.size)
        assertTrue(result.fields.all { it.adoptedValue == null && !it.userEdited })
        assertTrue(result.candidates.all { it.requiresReview && it.fieldEvidenceIds.size == 4 })

        val firstFields = result.fields.filter { it.id in result.candidates.first().fieldEvidenceIds }
        assertEquals(
            "1992-08-24",
            (firstFields.single { it.fieldKey == "birth.solar_date" }.normalizedValue as
                TypedFieldValue.Text).value,
        )
        assertEquals(
            listOf("壬申", "戊申", "壬申", "丙午"),
            (firstFields.single { it.fieldKey == "chart.four_pillars" }.normalizedValue as
                TypedFieldValue.FourPillarsValue).value.let {
                listOf(it.year, it.month, it.day, it.hour)
            },
        )
    }

    @Test
    fun `命主反馈和师傅点评完整保留OCR原文并关联同一候选`() {
        val feedback = image("feedback", WenzhenPageType.FEEDBACK)
        val commentary = image("commentary", WenzhenPageType.COMMENTARY)
        val feedbackText = "姓名：案例甲\n阳历1992年8月24日\n命主反馈\n第一段。\n第二段保留。"
        val commentaryText = "姓名：案例甲\n公历1992-08-24\n师傅点评\n原文不可截断。"

        val result = parser.parse(
            images = listOf(feedback, commentary),
            documents = listOf(
                document(feedback.id, feedbackText),
                document(commentary.id, commentaryText),
            ),
            groupedCandidates = listOf(
                ImportCaseCandidate(
                    id = "grouped",
                    imageIds = listOf(feedback.id, commentary.id),
                    suggestedAlias = "案例甲",
                    groupingConfidence = 0.84f,
                    requiresReview = true,
                ),
            ),
        )

        assertEquals(2, result.longTexts.size)
        assertEquals(
            setOf(ImportedLongTextType.OWNER_FEEDBACK, ImportedLongTextType.MASTER_COMMENTARY),
            result.longTexts.mapTo(mutableSetOf()) { it.type },
        )
        assertEquals(feedbackText, result.longTexts.single { it.imageId == feedback.id }.rawText)
        assertEquals(
            commentaryText,
            result.longTexts.single { it.imageId == commentary.id }.rawText,
        )
        assertEquals(
            result.longTexts.map { it.id }.toSet(),
            result.candidates.single().longTextEvidenceIds.toSet(),
        )
        assertTrue(result.fields.all { it.adoptedValue == null })
    }

    @Test
    fun `无法识别用户列表行时保留原待核对候选而不制造空命例`() {
        val image = image("unreadable-list", WenzhenPageType.USER_LIST)
        val original = candidate(image.id)

        val result = parser.parse(
            images = listOf(image),
            documents = listOf(document(image.id, "用户列表\n局部文字")),
            groupedCandidates = listOf(original),
        )

        assertEquals(listOf(original), result.candidates)
        assertTrue(result.fields.isEmpty())
        assertTrue(result.longTexts.isEmpty())
        assertNull(result.candidates.single().suggestedAlias)
    }

    private fun image(id: String, pageType: WenzhenPageType) = ImportImageRef(
        id = id,
        originalFileName = "$id.png",
        mimeType = "image/png",
        relativePath = "session/$id.png",
        sha256 = id.first().code.toString(16).padStart(64, '0').takeLast(64),
        byteSize = 1,
        pageType = pageType,
        pageConfidence = 0.95f,
        createdAt = NOW,
    )

    private fun candidate(imageId: String) = ImportCaseCandidate(
        id = "candidate-$imageId",
        imageIds = listOf(imageId),
        groupingConfidence = null,
        requiresReview = true,
    )

    private fun document(
        imageId: String,
        text: String,
    ) = OcrDocument(
        imageId = imageId,
        rawText = text,
        blocks = emptyList(),
        engineId = "fixture",
        engineVersion = "1",
        recognizedAt = NOW,
    )

    private fun document(
        imageId: String,
        blocks: List<OcrTextBlock>,
    ) = OcrDocument(
        imageId = imageId,
        rawText = blocks.joinToString("\n") { it.text },
        blocks = blocks,
        engineId = "fixture",
        engineVersion = "1",
        recognizedAt = NOW,
    )

    private fun block(
        id: String,
        text: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) = OcrTextBlock(
        id = id,
        text = text,
        confidence = 0.98f,
        boundingBox = EvidenceBoundingBox(left, top, right, bottom),
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
