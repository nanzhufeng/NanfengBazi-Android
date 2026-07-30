package com.nanzhufeng.nanfengbazi.data.repository

import androidx.room.withTransaction
import com.nanzhufeng.nanfengbazi.data.db.ImportSessionEntity
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.domain.ImportSessionDeleteResult
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.ImportSession
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
import com.nanzhufeng.nanfengbazi.domain.model.canTransitionTo
import java.time.Instant

class RoomImportSessionRepository(
    private val database: NanfengBaziDatabase,
) : ImportSessionRepository {
    private val dao = database.importSessionDao()

    override suspend fun save(
        session: ImportSession,
        expectedRevision: Long?,
    ): ImportSessionWriteResult = database.withTransaction {
        val current = dao.findById(session.id)
        if (current == null) {
            if (expectedRevision != null && expectedRevision != 0L) {
                return@withTransaction ImportSessionWriteResult.RevisionConflict(
                    sessionId = session.id,
                    expectedRevision = expectedRevision,
                    actualRevision = 0,
                )
            }
            val persisted = session.copy(revision = 1)
            dao.insert(persisted.toEntity())
            ImportSessionWriteResult.Created(session.id, 1)
        } else {
            if (expectedRevision == null) {
                return@withTransaction ImportSessionWriteResult.AlreadyExists(
                    sessionId = session.id,
                    revision = current.revision,
                )
            }
            if (expectedRevision != current.revision) {
                return@withTransaction ImportSessionWriteResult.RevisionConflict(
                    sessionId = session.id,
                    expectedRevision = expectedRevision,
                    actualRevision = current.revision,
                )
            }
            val previous = current.toDomain()
            if (!previous.status.canTransitionTo(session.status)) {
                return@withTransaction ImportSessionWriteResult.InvalidTransition(
                    sessionId = session.id,
                    from = previous.status,
                    to = session.status,
                )
            }
            val persisted = session.copy(
                createdAt = Instant.ofEpochMilli(current.createdAtEpochMillis),
                revision = current.revision + 1,
            )
            dao.update(persisted.toEntity())
            ImportSessionWriteResult.Updated(session.id, persisted.revision)
        }
    }

    override suspend fun findById(id: String): ImportSession? =
        database.withTransaction { dao.findById(id)?.toDomain() }

    override suspend fun list(statuses: Set<ImportStatus>): List<ImportSession> =
        database.withTransaction {
            dao.all()
                .asSequence()
                .map(ImportSessionEntity::toDomain)
                .filter { statuses.isEmpty() || it.status in statuses }
                .toList()
        }

    override suspend fun delete(
        id: String,
        expectedRevision: Long,
    ): ImportSessionDeleteResult = database.withTransaction {
        val current = dao.findById(id) ?: return@withTransaction ImportSessionDeleteResult.NotFound
        if (current.revision != expectedRevision) {
            return@withTransaction ImportSessionDeleteResult.RevisionConflict(
                expectedRevision = expectedRevision,
                actualRevision = current.revision,
            )
        }
        dao.delete(id)
        ImportSessionDeleteResult.Deleted
    }
}

private fun ImportSession.toEntity(): ImportSessionEntity = ImportSessionEntity(
    id = id,
    status = status.name,
    sessionJson = DomainJson.encodeToString(ImportSession.serializer(), this),
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    revision = revision,
)

private fun ImportSessionEntity.toDomain(): ImportSession =
    DomainJson.decodeFromString(ImportSession.serializer(), sessionJson)
