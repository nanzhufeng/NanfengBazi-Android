package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import java.time.Instant

enum class CaseSortOrder {
    LAST_VIEWED_DESC,
    UPDATED_DESC,
    CREATED_DESC,
    BIRTH_ASC,
}

data class CaseSearchRequest(
    val query: String = "",
    val groupId: String? = null,
    val tagId: String? = null,
    val sortOrder: CaseSortOrder = CaseSortOrder.UPDATED_DESC,
)

interface CaseRepository {
    suspend fun save(
        case: BaziCase,
        expectedRevision: Long?,
    ): CaseWriteResult

    suspend fun findById(id: String): BaziCase?

    suspend fun search(request: CaseSearchRequest = CaseSearchRequest()): List<CaseSummary>

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
