package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import java.time.Instant

enum class CaseSortOrder {
    LAST_VIEWED_DESC,
    UPDATED_DESC,
    CREATED_DESC,
    BIRTH_ASC,
}

enum class CaseVisibility {
    ACTIVE,
    TRASHED,
    ALL,
}

data class CaseSearchRequest(
    val query: String = "",
    val groupId: String? = null,
    val tagId: String? = null,
    val sortOrder: CaseSortOrder = CaseSortOrder.UPDATED_DESC,
    val visibility: CaseVisibility = CaseVisibility.ACTIVE,
)

enum class DuplicateReason {
    SAME_BIRTH_INPUT,
    SAME_FOUR_PILLARS,
}

data class DuplicateCaseCandidate(
    val summary: CaseSummary,
    val reasons: Set<DuplicateReason>,
)

interface CaseRepository {
    suspend fun save(
        case: BaziCase,
        expectedRevision: Long?,
    ): CaseWriteResult

    suspend fun findById(id: String): BaziCase?

    suspend fun search(request: CaseSearchRequest = CaseSearchRequest()): List<CaseSummary>

    suspend fun findDuplicateCandidates(
        birthInput: BirthInput,
        fourPillars: FourPillars?,
        canonicalSolarDateTime: CivilDateTime? = null,
        excludeCaseId: String? = null,
    ): List<DuplicateCaseCandidate>

    suspend fun markViewed(
        caseId: String,
        viewedAt: Instant,
    ): Boolean
}

sealed interface CaseWriteResult {
    data class Created(
        val caseId: String,
        val revision: Long,
    ) : CaseWriteResult

    data class Updated(
        val caseId: String,
        val revision: Long,
    ) : CaseWriteResult

    data class AlreadyExists(
        val caseId: String,
        val revision: Long,
    ) : CaseWriteResult

    data class RevisionConflict(
        val caseId: String,
        val expectedRevision: Long,
        val actualRevision: Long,
    ) : CaseWriteResult
}
