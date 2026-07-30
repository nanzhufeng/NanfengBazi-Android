package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary

interface CaseRepository {
    suspend fun save(
        case: BaziCase,
        expectedRevision: Long?,
    ): CaseWriteResult

    suspend fun findById(id: String): BaziCase?

    suspend fun search(query: String): List<CaseSummary>
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
