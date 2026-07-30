package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.ImportSession
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus

interface ImportSessionRepository {
    suspend fun save(
        session: ImportSession,
        expectedRevision: Long?,
    ): ImportSessionWriteResult

    suspend fun findById(id: String): ImportSession?

    suspend fun list(statuses: Set<ImportStatus> = emptySet()): List<ImportSession>

    suspend fun delete(
        id: String,
        expectedRevision: Long,
    ): ImportSessionDeleteResult
}

sealed interface ImportSessionWriteResult {
    data class Created(
        val sessionId: String,
        val revision: Long,
    ) : ImportSessionWriteResult

    data class Updated(
        val sessionId: String,
        val revision: Long,
    ) : ImportSessionWriteResult

    data class AlreadyExists(
        val sessionId: String,
        val revision: Long,
    ) : ImportSessionWriteResult

    data class RevisionConflict(
        val sessionId: String,
        val expectedRevision: Long,
        val actualRevision: Long,
    ) : ImportSessionWriteResult

    data class InvalidTransition(
        val sessionId: String,
        val from: ImportStatus,
        val to: ImportStatus,
    ) : ImportSessionWriteResult
}

sealed interface ImportSessionDeleteResult {
    data object Deleted : ImportSessionDeleteResult
    data object NotFound : ImportSessionDeleteResult

    data class RevisionConflict(
        val expectedRevision: Long,
        val actualRevision: Long,
    ) : ImportSessionDeleteResult
}
