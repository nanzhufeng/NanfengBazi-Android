package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType

const val MASTER_COMMENTARY_CANDIDATE_RULE_VERSION = 1

data class MasterCommentaryCandidateExtractionInput(
    val sourceRecordId: String,
    val sourceRecordType: CaseTextRecordType,
    val sourceContent: String,
    val sourceRevision: Int,
    val ruleVersion: Int = MASTER_COMMENTARY_CANDIDATE_RULE_VERSION,
)

data class CommentaryTextRange(
    val startInclusive: Int,
    val endExclusive: Int,
) {
    init {
        require(startInclusive >= 0) { "候选起点不能小于零" }
        require(endExclusive > startInclusive) { "候选终点必须晚于起点" }
    }

    fun readFrom(source: String): String? {
        if (endExclusive > source.length) return null
        return source.substring(startInclusive, endExclusive)
    }
}

data class CommentaryCandidateRuleEvidence(
    val ruleId: String,
    val explanation: String,
    val matchedTerms: List<String> = emptyList(),
) {
    init {
        require(ruleId.isNotBlank()) { "规则证据 id 不能为空" }
        require(explanation.isNotBlank()) { "规则证据说明不能为空" }
    }
}

enum class MasterCommentaryCandidateStatus {
    PENDING,
    ADOPTED,
    REJECTED,
}

data class MasterCommentaryCandidate(
    val id: String,
    val sourceRecordId: String,
    val sourceRevision: Int,
    val sourceRange: CommentaryTextRange,
    val sourceExcerpt: String,
    val proposedContent: String,
    val proposedCategory: AnalysisCategory,
    val ruleEvidence: List<CommentaryCandidateRuleEvidence>,
    val status: MasterCommentaryCandidateStatus = MasterCommentaryCandidateStatus.PENDING,
) {
    init {
        require(id.isNotBlank()) { "候选 id 不能为空" }
        require(sourceRecordId.isNotBlank()) { "候选必须关联来源记录" }
        require(sourceRevision >= 0) { "来源记录版本不能小于零" }
        require(sourceExcerpt.isNotBlank()) { "来源片段不能为空" }
        require(ruleEvidence.isNotEmpty()) { "候选必须包含规则证据" }
    }

    fun isExactExcerptOf(source: String): Boolean =
        sourceRange.readFrom(source) == sourceExcerpt
}

data class MasterCommentaryCandidateSet(
    val ruleVersion: Int,
    val sourceRecordId: String,
    val sourceRevision: Int,
    val candidates: List<MasterCommentaryCandidate>,
) {
    init {
        require(ruleVersion > 0) { "规则版本必须大于零" }
        require(sourceRecordId.isNotBlank()) { "候选集必须关联来源记录" }
        require(sourceRevision >= 0) { "来源记录版本不能小于零" }
        require(candidates.isNotEmpty()) { "候选集不能为空" }
        require(candidates.all { it.sourceRecordId == sourceRecordId }) {
            "候选集来源记录不一致"
        }
        require(candidates.all { it.sourceRevision == sourceRevision }) {
            "候选集来源版本不一致"
        }
        require(candidates.map { it.id }.distinct().size == candidates.size) {
            "候选 id 不能重复"
        }
    }
}

enum class MasterCommentaryCandidateErrorCode {
    INVALID_SOURCE_REVISION,
    NOT_MASTER_COMMENTARY,
    EMPTY_SOURCE,
    UNSUPPORTED_RULE_VERSION,
    NO_CANDIDATES,
}

data class MasterCommentaryCandidateFailure(
    val code: MasterCommentaryCandidateErrorCode,
    val message: String,
)

sealed interface MasterCommentaryCandidateExtractionResult {
    data class Success(
        val candidateSet: MasterCommentaryCandidateSet,
    ) : MasterCommentaryCandidateExtractionResult

    data class Failure(
        val failure: MasterCommentaryCandidateFailure,
    ) : MasterCommentaryCandidateExtractionResult
}

fun interface MasterCommentaryCandidateExtractor {
    fun extract(
        input: MasterCommentaryCandidateExtractionInput,
    ): MasterCommentaryCandidateExtractionResult
}

enum class MasterCommentaryCandidateAdoptionErrorCode {
    CONTEXT_NOT_READY,
    CANDIDATE_NOT_PENDING,
    EMPTY_ADOPTED_CONTENT,
    CASE_NOT_FOUND,
    SOURCE_NOT_FOUND,
    SOURCE_TYPE_CHANGED,
    SOURCE_REVISION_STALE,
    SOURCE_RANGE_STALE,
    CASE_REVISION_CONFLICT,
    STORAGE_FAILED,
}

data class MasterCommentaryCandidateAdoptionFailure(
    val code: MasterCommentaryCandidateAdoptionErrorCode,
    val message: String,
)

sealed interface MasterCommentaryCandidateAdoptionResult {
    data class Saved(
        val caseId: String,
        val revision: Long,
        val analysisRecordId: String,
    ) : MasterCommentaryCandidateAdoptionResult

    data class Failure(
        val failure: MasterCommentaryCandidateAdoptionFailure,
    ) : MasterCommentaryCandidateAdoptionResult
}
