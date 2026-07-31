package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.MASTER_COMMENTARY_CANDIDATE_RULE_VERSION
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateErrorCode
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateExtractionInput
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateExtractionResult
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicMasterCommentaryCandidateExtractorTest {
    private val extractor = DeterministicMasterCommentaryCandidateExtractor()

    @Test
    fun `非点评记录与空原文返回不同结构化失败`() {
        assertFailure(
            input(type = CaseTextRecordType.NOTE, content = "事业有变化"),
            MasterCommentaryCandidateErrorCode.NOT_MASTER_COMMENTARY,
        )
        assertFailure(
            input(content = " \n "),
            MasterCommentaryCandidateErrorCode.EMPTY_SOURCE,
        )
    }

    @Test
    fun `无可解释字符返回无候选而不伪造句段`() {
        assertFailure(
            input(content = "……；！？\n"),
            MasterCommentaryCandidateErrorCode.NO_CANDIDATES,
        )
    }

    @Test
    fun `多句候选保留精确字符区间并提供保守分类证据`() {
        val source = "  性格较内向。事业需要自己核对；普通描述  "
        val result = extractor.extract(input(content = source)).success()

        assertEquals(3, result.candidates.size)
        result.candidates.forEach { candidate ->
            assertEquals(
                candidate.sourceExcerpt,
                source.substring(
                    candidate.sourceRange.startInclusive,
                    candidate.sourceRange.endExclusive,
                ),
            )
            assertTrue(candidate.ruleEvidence.isNotEmpty())
        }
        assertEquals(AnalysisCategory.PERSONALITY, result.candidates[0].proposedCategory)
        assertEquals(AnalysisCategory.CAREER, result.candidates[1].proposedCategory)
        assertEquals(AnalysisCategory.GENERAL, result.candidates[2].proposedCategory)
    }

    @Test
    fun `重复句只保留首次出现且相同输入结果完全稳定`() {
        val source = "事业有变化。事业有变化！财运需核对"
        val first = extractor.extract(input(content = source))
        val second = extractor.extract(input(content = source))

        assertEquals(first, second)
        assertEquals(2, first.success().candidates.size)
        assertEquals(0, first.success().candidates.first().sourceRange.startInclusive)
    }

    @Test
    fun `换行和首尾边界不吞掉来源字符`() {
        val source = "\n第一段\r\n 第二段\n第三段"
        val candidates = extractor.extract(input(content = source)).success().candidates

        assertEquals(listOf("第一段", "第二段", "第三段"), candidates.map { it.sourceExcerpt })
        assertEquals(listOf(1, 7, 11), candidates.map { it.sourceRange.startInclusive })
    }

    @Test
    fun `不支持规则版本和非法来源版本明确拒绝`() {
        assertFailure(
            input(ruleVersion = MASTER_COMMENTARY_CANDIDATE_RULE_VERSION + 1),
            MasterCommentaryCandidateErrorCode.UNSUPPORTED_RULE_VERSION,
        )
        assertFailure(
            input(sourceRevision = -1),
            MasterCommentaryCandidateErrorCode.INVALID_SOURCE_REVISION,
        )
    }

    private fun input(
        type: CaseTextRecordType = CaseTextRecordType.MASTER_COMMENTARY,
        content: String = "事业需要核对",
        sourceRevision: Int = 3,
        ruleVersion: Int = MASTER_COMMENTARY_CANDIDATE_RULE_VERSION,
    ) = MasterCommentaryCandidateExtractionInput(
        sourceRecordId = "commentary-1",
        sourceRecordType = type,
        sourceContent = content,
        sourceRevision = sourceRevision,
        ruleVersion = ruleVersion,
    )

    private fun assertFailure(
        input: MasterCommentaryCandidateExtractionInput,
        code: MasterCommentaryCandidateErrorCode,
    ) {
        val result = extractor.extract(input)
        assertTrue(result is MasterCommentaryCandidateExtractionResult.Failure)
        assertEquals(
            code,
            (result as MasterCommentaryCandidateExtractionResult.Failure).failure.code,
        )
    }

    private fun MasterCommentaryCandidateExtractionResult.success() =
        (this as MasterCommentaryCandidateExtractionResult.Success).candidateSet
}
