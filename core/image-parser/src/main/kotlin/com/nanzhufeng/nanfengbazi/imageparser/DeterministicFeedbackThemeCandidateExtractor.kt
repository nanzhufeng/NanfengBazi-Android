package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.FEEDBACK_THEME_CANDIDATE_RULE_VERSION
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidate
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateErrorCode
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateExtractionInput
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateExtractionResult
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateExtractor
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateFailure
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateSet
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeSourceEvidence
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeTextRange
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import java.security.MessageDigest

class DeterministicFeedbackThemeCandidateExtractor : FeedbackThemeCandidateExtractor {
    override fun extract(
        input: FeedbackThemeCandidateExtractionInput,
    ): FeedbackThemeCandidateExtractionResult {
        if (input.sourceRevision < 0) {
            return failure(
                FeedbackThemeCandidateErrorCode.INVALID_SOURCE_REVISION,
                "命主反馈版本无效，请返回详情重新打开。",
            )
        }
        if (input.sourceRecordType != CaseTextRecordType.OWNER_FEEDBACK) {
            return failure(
                FeedbackThemeCandidateErrorCode.NOT_OWNER_FEEDBACK,
                "只有命主反馈记录可以提取主题标签候选。",
            )
        }
        if (input.sourceContent.isBlank()) {
            return failure(
                FeedbackThemeCandidateErrorCode.EMPTY_SOURCE,
                "命主反馈原文为空，无法提取主题标签候选。",
            )
        }
        if (input.ruleVersion != FEEDBACK_THEME_CANDIDATE_RULE_VERSION) {
            return failure(
                FeedbackThemeCandidateErrorCode.UNSUPPORTED_RULE_VERSION,
                "当前版本不支持该主题提取规则，请重新提取。",
            )
        }

        val sentences = sentenceEvidence(input.sourceContent)
        val candidates = THEME_RULES.mapNotNull { rule ->
            val evidence = sentences.mapNotNull { sentence ->
                val matched = rule.terms.filter(sentence.excerpt::contains)
                matched.takeIf(List<String>::isNotEmpty)?.let {
                    FeedbackThemeSourceEvidence(
                        range = sentence.range,
                        excerpt = sentence.excerpt,
                        matchedTerms = matched,
                    )
                }
            }
            if (evidence.isEmpty()) return@mapNotNull null
            FeedbackThemeCandidate(
                id = stableId(
                    sourceRecordId = input.sourceRecordId,
                    sourceRevision = input.sourceRevision,
                    canonicalTagName = rule.tagName,
                    evidence = evidence,
                ),
                sourceRecordId = input.sourceRecordId,
                sourceRevision = input.sourceRevision,
                canonicalTagName = rule.tagName,
                proposedTagName = rule.tagName,
                suggestedEventCategory = rule.category,
                sourceEvidence = evidence,
                ruleId = rule.ruleId,
                ruleExplanation = "命中本地反馈主题词，仅生成可编辑标签建议，不代表用户确认。",
            )
        }
        if (candidates.isEmpty()) {
            return failure(
                FeedbackThemeCandidateErrorCode.NO_CANDIDATES,
                "未找到可解释的主题标签；完整命主反馈保持不变。",
            )
        }
        return FeedbackThemeCandidateExtractionResult.Success(
            FeedbackThemeCandidateSet(
                ruleVersion = input.ruleVersion,
                sourceRecordId = input.sourceRecordId,
                sourceRevision = input.sourceRevision,
                candidates = candidates,
            ),
        )
    }

    private fun sentenceEvidence(source: String): List<SentenceEvidence> {
        val ranges = mutableListOf<FeedbackThemeTextRange>()
        var segmentStart = 0
        source.forEachIndexed { index, character ->
            if (character in SENTENCE_DELIMITERS) {
                addTrimmedRange(source, segmentStart, index, ranges)
                segmentStart = index + 1
            }
        }
        addTrimmedRange(source, segmentStart, source.length, ranges)
        return ranges.map { range ->
            SentenceEvidence(
                range = range,
                excerpt = requireNotNull(range.readFrom(source)),
            )
        }
    }

    private fun addTrimmedRange(
        source: String,
        rawStart: Int,
        rawEnd: Int,
        destination: MutableList<FeedbackThemeTextRange>,
    ) {
        var start = rawStart
        var end = rawEnd
        while (start < end && source[start].isWhitespace()) start += 1
        while (end > start && source[end - 1].isWhitespace()) end -= 1
        if (start < end && source.substring(start, end).any(Char::isLetterOrDigit)) {
            destination += FeedbackThemeTextRange(start, end)
        }
    }

    private fun stableId(
        sourceRecordId: String,
        sourceRevision: Int,
        canonicalTagName: String,
        evidence: List<FeedbackThemeSourceEvidence>,
    ): String {
        val ranges = evidence.joinToString(",") {
            "${it.range.startInclusive}-${it.range.endExclusive}"
        }
        val payload = "$sourceRecordId|$sourceRevision|$canonicalTagName|$ranges"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
        return "feedback-theme-" + digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun failure(
        code: FeedbackThemeCandidateErrorCode,
        message: String,
    ) = FeedbackThemeCandidateExtractionResult.Failure(
        FeedbackThemeCandidateFailure(code, message),
    )

    private data class SentenceEvidence(
        val range: FeedbackThemeTextRange,
        val excerpt: String,
    )

    private data class ThemeRule(
        val tagName: String,
        val category: CaseEventCategory,
        val ruleId: String,
        val terms: List<String>,
    )

    private companion object {
        val SENTENCE_DELIMITERS = setOf('。', '！', '？', '!', '?', '；', ';', '\n', '\r')
        val THEME_RULES = listOf(
            ThemeRule(
                "学业",
                CaseEventCategory.EDUCATION,
                "feedback-theme-education-v1",
                listOf("学业", "学习", "学校", "考试", "毕业", "学历"),
            ),
            ThemeRule(
                "事业",
                CaseEventCategory.CAREER,
                "feedback-theme-career-v1",
                listOf("事业", "工作", "职业", "公司", "入职", "离职", "升职", "创业"),
            ),
            ThemeRule(
                "财运",
                CaseEventCategory.WEALTH,
                "feedback-theme-wealth-v1",
                listOf("财运", "收入", "钱财", "财务", "破财", "投资", "亏损"),
            ),
            ThemeRule(
                "感情",
                CaseEventCategory.RELATIONSHIP,
                "feedback-theme-relationship-v1",
                listOf("感情", "恋爱", "婚姻", "结婚", "分手", "离婚", "伴侣", "配偶"),
            ),
            ThemeRule(
                "家庭",
                CaseEventCategory.FAMILY,
                "feedback-theme-family-v1",
                listOf("家庭", "父母", "子女", "生育", "兄弟", "姐妹"),
            ),
            ThemeRule(
                "健康",
                CaseEventCategory.HEALTH,
                "feedback-theme-health-v1",
                listOf("健康", "身体", "生病", "疾病", "手术", "住院", "医院"),
            ),
            ThemeRule(
                "迁移",
                CaseEventCategory.OTHER,
                "feedback-theme-migration-v1",
                listOf("搬家", "迁居", "移居", "出国", "回国", "迁移"),
            ),
        )
    }
}
