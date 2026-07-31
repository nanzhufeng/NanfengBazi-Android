package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateErrorCode
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateExtractionInput
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateExtractionResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicFeedbackThemeCandidateExtractorTest {
    private val extractor = DeterministicFeedbackThemeCandidateExtractor()

    @Test
    fun `rejects wrong source blank source and unsupported version separately`() {
        assertFailure(
            input(type = CaseTextRecordType.MASTER_COMMENTARY),
            FeedbackThemeCandidateErrorCode.NOT_OWNER_FEEDBACK,
        )
        assertFailure(
            input(content = " \n "),
            FeedbackThemeCandidateErrorCode.EMPTY_SOURCE,
        )
        assertFailure(
            input(ruleVersion = 99),
            FeedbackThemeCandidateErrorCode.UNSUPPORTED_RULE_VERSION,
        )
        assertFailure(
            input(sourceRevision = -1),
            FeedbackThemeCandidateErrorCode.INVALID_SOURCE_REVISION,
        )
    }

    @Test
    fun `returns no candidates when feedback has no supported theme evidence`() {
        assertFailure(
            input(content = "今天记录了一段无法归类的普通叙述。"),
            FeedbackThemeCandidateErrorCode.NO_CANDIDATES,
        )
    }

    @Test
    fun `aggregates repeated theme into one candidate with exact ordered evidence`() {
        val source = "  2018年参加考试并毕业。\n2020年换工作，收入提高；后来工作稳定。"
        val result = assertSuccess(extractor.extract(input(content = source)))
        assertEquals(listOf("学业", "事业", "财运"), result.candidateSet.candidates.map {
            it.canonicalTagName
        })
        val career = result.candidateSet.candidates[1]
        assertEquals(CaseEventCategory.CAREER, career.suggestedEventCategory)
        assertEquals(2, career.sourceEvidence.size)
        assertEquals(
            listOf("2020年换工作，收入提高", "后来工作稳定"),
            career.sourceEvidence.map { it.excerpt },
        )
        assertTrue(career.isExactEvidenceOf(source))
        result.candidateSet.candidates.forEach { candidate ->
            assertTrue(candidate.sourceEvidence.all { it.isExactExcerptOf(source) })
        }
    }

    @Test
    fun `same source yields stable deduplicated candidates and ids`() {
        val source = "工作有变化。工作有变化。结婚后家庭稳定。"
        val first = assertSuccess(extractor.extract(input(content = source))).candidateSet
        val second = assertSuccess(extractor.extract(input(content = source))).candidateSet
        assertEquals(first, second)
        assertEquals(first.candidates.size, first.candidates.map { it.canonicalTagName }.toSet().size)
        assertEquals(2, first.candidates.first { it.canonicalTagName == "事业" }.sourceEvidence.size)
    }

    @Test
    fun `preserves utf16 half open ranges across punctuation and blank lines`() {
        val source = "🙂工作调整。\r\n  健康需要复查！"
        val result = assertSuccess(extractor.extract(input(content = source))).candidateSet
        val career = result.candidates.first { it.canonicalTagName == "事业" }
        val health = result.candidates.first { it.canonicalTagName == "健康" }
        assertEquals("🙂工作调整", career.sourceEvidence.single().excerpt)
        assertEquals(0, career.sourceEvidence.single().range.startInclusive)
        assertEquals("健康需要复查", health.sourceEvidence.single().excerpt)
        assertEquals(
            source.indexOf("健康"),
            health.sourceEvidence.single().range.startInclusive,
        )
        assertTrue(career.isExactEvidenceOf(source))
        assertTrue(health.isExactEvidenceOf(source))
    }

    private fun input(
        type: CaseTextRecordType = CaseTextRecordType.OWNER_FEEDBACK,
        content: String = "工作有变化。",
        sourceRevision: Int = 2,
        ruleVersion: Int = 1,
    ) = FeedbackThemeCandidateExtractionInput(
        sourceRecordId = "feedback-1",
        sourceRecordType = type,
        sourceContent = content,
        sourceRevision = sourceRevision,
        ruleVersion = ruleVersion,
    )

    private fun assertFailure(
        input: FeedbackThemeCandidateExtractionInput,
        code: FeedbackThemeCandidateErrorCode,
    ) {
        val result = extractor.extract(input)
        assertTrue(result is FeedbackThemeCandidateExtractionResult.Failure)
        val failure = result as FeedbackThemeCandidateExtractionResult.Failure
        assertEquals(code, failure.failure.code)
    }

    private fun assertSuccess(
        result: FeedbackThemeCandidateExtractionResult,
    ): FeedbackThemeCandidateExtractionResult.Success {
        assertTrue(result is FeedbackThemeCandidateExtractionResult.Success)
        return result as FeedbackThemeCandidateExtractionResult.Success
    }
}
