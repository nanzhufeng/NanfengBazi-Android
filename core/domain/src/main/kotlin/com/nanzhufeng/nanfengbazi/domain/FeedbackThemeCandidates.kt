package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType

const val FEEDBACK_THEME_CANDIDATE_RULE_VERSION = 1

data class FeedbackThemeCandidateExtractionInput(
    val sourceRecordId: String,
    val sourceRecordType: CaseTextRecordType,
    val sourceContent: String,
    val sourceRevision: Int,
    val ruleVersion: Int = FEEDBACK_THEME_CANDIDATE_RULE_VERSION,
)

data class FeedbackThemeTextRange(
    val startInclusive: Int,
    val endExclusive: Int,
) {
    init {
        require(startInclusive >= 0) { "主题证据起点不能小于零" }
        require(endExclusive > startInclusive) { "主题证据终点必须晚于起点" }
    }

    fun readFrom(source: String): String? {
        if (endExclusive > source.length) return null
        return source.substring(startInclusive, endExclusive)
    }
}

data class FeedbackThemeSourceEvidence(
    val range: FeedbackThemeTextRange,
    val excerpt: String,
    val matchedTerms: List<String>,
) {
    init {
        require(excerpt.isNotBlank()) { "主题来源片段不能为空" }
        require(matchedTerms.isNotEmpty()) { "主题来源必须包含命中词" }
        require(matchedTerms.all(String::isNotBlank)) { "主题命中词不能为空" }
    }

    fun isExactExcerptOf(source: String): Boolean = range.readFrom(source) == excerpt
}

enum class FeedbackThemeCandidateStatus {
    PENDING,
    ADOPTED,
    REJECTED,
}

data class FeedbackThemeCandidate(
    val id: String,
    val sourceRecordId: String,
    val sourceRevision: Int,
    val canonicalTagName: String,
    val proposedTagName: String,
    val suggestedEventCategory: CaseEventCategory,
    val sourceEvidence: List<FeedbackThemeSourceEvidence>,
    val ruleId: String,
    val ruleExplanation: String,
    val status: FeedbackThemeCandidateStatus = FeedbackThemeCandidateStatus.PENDING,
) {
    init {
        require(id.isNotBlank()) { "主题候选 id 不能为空" }
        require(sourceRecordId.isNotBlank()) { "主题候选必须关联来源记录" }
        require(sourceRevision >= 0) { "主题来源版本不能小于零" }
        require(canonicalTagName.isNotBlank()) { "规范主题标签不能为空" }
        require(ruleId.isNotBlank()) { "主题规则 id 不能为空" }
        require(ruleExplanation.isNotBlank()) { "主题规则说明不能为空" }
        require(sourceEvidence.isNotEmpty()) { "主题候选必须包含来源证据" }
    }

    fun isExactEvidenceOf(source: String): Boolean =
        sourceEvidence.all { evidence ->
            evidence.isExactExcerptOf(source) &&
                evidence.matchedTerms.all(evidence.excerpt::contains)
        }
}

data class FeedbackThemeCandidateSet(
    val ruleVersion: Int,
    val sourceRecordId: String,
    val sourceRevision: Int,
    val candidates: List<FeedbackThemeCandidate>,
) {
    init {
        require(ruleVersion > 0) { "主题规则版本必须大于零" }
        require(sourceRecordId.isNotBlank()) { "主题候选集必须关联来源记录" }
        require(sourceRevision >= 0) { "主题来源版本不能小于零" }
        require(candidates.isNotEmpty()) { "主题候选集不能为空" }
        require(candidates.all { it.sourceRecordId == sourceRecordId }) {
            "主题候选集来源记录不一致"
        }
        require(candidates.all { it.sourceRevision == sourceRevision }) {
            "主题候选集来源版本不一致"
        }
        require(candidates.map { it.id }.distinct().size == candidates.size) {
            "主题候选 id 不能重复"
        }
        require(candidates.map { it.canonicalTagName }.distinct().size == candidates.size) {
            "规范主题标签不能重复"
        }
    }
}

enum class FeedbackThemeCandidateErrorCode {
    INVALID_SOURCE_REVISION,
    NOT_OWNER_FEEDBACK,
    EMPTY_SOURCE,
    UNSUPPORTED_RULE_VERSION,
    NO_CANDIDATES,
}

data class FeedbackThemeCandidateFailure(
    val code: FeedbackThemeCandidateErrorCode,
    val message: String,
)

sealed interface FeedbackThemeCandidateExtractionResult {
    data class Success(
        val candidateSet: FeedbackThemeCandidateSet,
    ) : FeedbackThemeCandidateExtractionResult

    data class Failure(
        val failure: FeedbackThemeCandidateFailure,
    ) : FeedbackThemeCandidateExtractionResult
}

fun interface FeedbackThemeCandidateExtractor {
    fun extract(
        input: FeedbackThemeCandidateExtractionInput,
    ): FeedbackThemeCandidateExtractionResult
}

enum class FeedbackThemeAdoptionErrorCode {
    CONTEXT_NOT_READY,
    CANDIDATE_NOT_PENDING,
    EMPTY_TAG_NAME,
    TAG_NAME_TOO_LONG,
    CASE_NOT_FOUND,
    SOURCE_NOT_FOUND,
    SOURCE_TYPE_CHANGED,
    SOURCE_REVISION_STALE,
    SOURCE_EVIDENCE_STALE,
    CASE_REVISION_CONFLICT,
    TAG_ALREADY_PRESENT,
    TOO_MANY_TAGS,
    STORAGE_FAILED,
}

data class FeedbackThemeAdoptionFailure(
    val code: FeedbackThemeAdoptionErrorCode,
    val message: String,
)

sealed interface FeedbackThemeAdoptionResult {
    data class Saved(
        val caseId: String,
        val revision: Long,
        val tagId: String,
        val tagName: String,
    ) : FeedbackThemeAdoptionResult

    data class Failure(
        val failure: FeedbackThemeAdoptionFailure,
    ) : FeedbackThemeAdoptionResult
}
