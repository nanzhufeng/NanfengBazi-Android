package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.CommentaryCandidateRuleEvidence
import com.nanzhufeng.nanfengbazi.domain.CommentaryTextRange
import com.nanzhufeng.nanfengbazi.domain.MASTER_COMMENTARY_CANDIDATE_RULE_VERSION
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidate
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateErrorCode
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateExtractionInput
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateExtractionResult
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateExtractor
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateFailure
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateSet
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import java.security.MessageDigest

class DeterministicMasterCommentaryCandidateExtractor :
    MasterCommentaryCandidateExtractor {
    override fun extract(
        input: MasterCommentaryCandidateExtractionInput,
    ): MasterCommentaryCandidateExtractionResult {
        if (input.sourceRevision < 0) {
            return failure(
                MasterCommentaryCandidateErrorCode.INVALID_SOURCE_REVISION,
                "师傅点评版本无效，请返回详情重新打开。",
            )
        }
        if (input.sourceRecordType != CaseTextRecordType.MASTER_COMMENTARY) {
            return failure(
                MasterCommentaryCandidateErrorCode.NOT_MASTER_COMMENTARY,
                "只有师傅点评记录可以提取观点候选。",
            )
        }
        if (input.sourceContent.isBlank()) {
            return failure(
                MasterCommentaryCandidateErrorCode.EMPTY_SOURCE,
                "师傅点评原文为空，无法提取观点候选。",
            )
        }
        if (input.ruleVersion != MASTER_COMMENTARY_CANDIDATE_RULE_VERSION) {
            return failure(
                MasterCommentaryCandidateErrorCode.UNSUPPORTED_RULE_VERSION,
                "当前版本不支持该观点提取规则，请重新提取。",
            )
        }

        val seen = linkedSetOf<String>()
        val candidates = sentenceRanges(input.sourceContent).mapNotNull { range ->
            val excerpt = range.readFrom(input.sourceContent) ?: return@mapNotNull null
            if (excerpt.none(Char::isLetterOrDigit)) return@mapNotNull null
            val deduplicationKey = excerpt.replace(WHITESPACE, " ").trim()
            if (!seen.add(deduplicationKey)) return@mapNotNull null
            val categoryMatch = classify(excerpt)
            MasterCommentaryCandidate(
                id = stableId(
                    input.sourceRecordId,
                    input.sourceRevision,
                    range,
                    excerpt,
                ),
                sourceRecordId = input.sourceRecordId,
                sourceRevision = input.sourceRevision,
                sourceRange = range,
                sourceExcerpt = excerpt,
                proposedContent = excerpt,
                proposedCategory = categoryMatch.category,
                ruleEvidence = listOf(
                    CommentaryCandidateRuleEvidence(
                        ruleId = categoryMatch.ruleId,
                        explanation = categoryMatch.explanation,
                        matchedTerms = categoryMatch.matchedTerms,
                    ),
                    CommentaryCandidateRuleEvidence(
                        ruleId = "sentence-boundary-v1",
                        explanation = "按原文标点或换行保留可精确定位的完整句段。",
                    ),
                ),
            )
        }
        if (candidates.isEmpty()) {
            return failure(
                MasterCommentaryCandidateErrorCode.NO_CANDIDATES,
                "未找到可解释的完整句段；原点评保持不变，可继续人工整理。",
            )
        }
        return MasterCommentaryCandidateExtractionResult.Success(
            MasterCommentaryCandidateSet(
                ruleVersion = input.ruleVersion,
                sourceRecordId = input.sourceRecordId,
                sourceRevision = input.sourceRevision,
                candidates = candidates,
            ),
        )
    }

    private fun sentenceRanges(source: String): List<CommentaryTextRange> {
        val ranges = mutableListOf<CommentaryTextRange>()
        var segmentStart = 0
        source.forEachIndexed { index, character ->
            if (character in SENTENCE_DELIMITERS) {
                addTrimmedRange(source, segmentStart, index, ranges)
                segmentStart = index + 1
            }
        }
        addTrimmedRange(source, segmentStart, source.length, ranges)
        return ranges
    }

    private fun addTrimmedRange(
        source: String,
        rawStart: Int,
        rawEnd: Int,
        destination: MutableList<CommentaryTextRange>,
    ) {
        var start = rawStart
        var end = rawEnd
        while (start < end && source[start].isWhitespace()) start += 1
        while (end > start && source[end - 1].isWhitespace()) end -= 1
        if (start < end) destination += CommentaryTextRange(start, end)
    }

    private fun classify(excerpt: String): CategoryMatch {
        val matches = CATEGORY_RULES.map { rule ->
            rule to rule.terms.filter(excerpt::contains)
        }.filter { (_, terms) -> terms.isNotEmpty() }
        val selected = matches.maxWithOrNull(
            compareBy<Pair<CategoryRule, List<String>>> { it.second.size }
                .thenBy { -CATEGORY_RULES.indexOf(it.first) },
        )
        if (selected == null) {
            return CategoryMatch(
                category = AnalysisCategory.GENERAL,
                ruleId = "category-general-v1",
                explanation = "未命中特定类别词，保守建议为综合；需由用户确认。",
                matchedTerms = emptyList(),
            )
        }
        val (rule, terms) = selected
        return CategoryMatch(
            category = rule.category,
            ruleId = rule.ruleId,
            explanation = "命中本地类别词，仅作为可编辑建议，不判断观点正确性。",
            matchedTerms = terms,
        )
    }

    private fun stableId(
        sourceRecordId: String,
        sourceRevision: Int,
        range: CommentaryTextRange,
        excerpt: String,
    ): String {
        val payload = "$sourceRecordId|$sourceRevision|${range.startInclusive}|" +
            "${range.endExclusive}|$excerpt"
        val bytes = MessageDigest.getInstance("SHA-256").digest(
            payload.toByteArray(Charsets.UTF_8),
        )
        return "commentary-" + bytes.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun failure(
        code: MasterCommentaryCandidateErrorCode,
        message: String,
    ) = MasterCommentaryCandidateExtractionResult.Failure(
        MasterCommentaryCandidateFailure(code, message),
    )

    private data class CategoryRule(
        val category: AnalysisCategory,
        val ruleId: String,
        val terms: List<String>,
    )

    private data class CategoryMatch(
        val category: AnalysisCategory,
        val ruleId: String,
        val explanation: String,
        val matchedTerms: List<String>,
    )

    private companion object {
        val SENTENCE_DELIMITERS = setOf('。', '！', '？', '!', '?', '；', ';', '\n', '\r')
        val WHITESPACE = Regex("\\s+")
        val CATEGORY_RULES = listOf(
            CategoryRule(
                AnalysisCategory.PERSONALITY,
                "category-personality-v1",
                listOf("性格", "个性", "脾气", "心性", "内向", "外向"),
            ),
            CategoryRule(
                AnalysisCategory.CAREER,
                "category-career-v1",
                listOf("事业", "工作", "职业", "职场", "升职", "创业"),
            ),
            CategoryRule(
                AnalysisCategory.WEALTH,
                "category-wealth-v1",
                listOf("财运", "钱财", "收入", "破财", "财富"),
            ),
            CategoryRule(
                AnalysisCategory.RELATIONSHIP,
                "category-relationship-v1",
                listOf("感情", "婚姻", "恋爱", "伴侣", "配偶"),
            ),
            CategoryRule(
                AnalysisCategory.HEALTH,
                "category-health-v1",
                listOf("健康", "身体", "疾病", "病痛"),
            ),
            CategoryRule(
                AnalysisCategory.EDUCATION,
                "category-education-v1",
                listOf("学业", "学习", "考试", "学历"),
            ),
            CategoryRule(
                AnalysisCategory.FAMILY,
                "category-family-v1",
                listOf("家庭", "父母", "子女", "兄弟", "姐妹"),
            ),
        )
    }
}
