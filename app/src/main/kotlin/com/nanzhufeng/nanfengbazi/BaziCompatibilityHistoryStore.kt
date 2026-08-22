package com.nanzhufeng.nanfengbazi

import android.content.Context
import android.util.AtomicFile
import com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityRecord
import com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityReport
import java.io.File
import java.util.UUID
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 合盘记录是本机私有的报告快照，不重新读取或改写双方命例。
 * 同一对已采用快照重复合盘时复用同一条记录，避免产生无意义的重复历史。
 */
interface BaziCompatibilityHistoryStore {
    fun list(): List<BaziCompatibilityRecord>
    fun save(report: BaziCompatibilityReport, createdAtEpochMillis: Long): BaziCompatibilityRecord
    fun replace(record: BaziCompatibilityRecord): BaziCompatibilityRecord
    fun delete(ids: Set<String>): Int
}

class CompatibilityHistoryUnreadableException(
    cause: Throwable,
) : IllegalStateException("合盘记录文件无法读取，原文件已保留，不能当作空记录覆盖。", cause)

class CompatibilityHistoryCapacityException : IllegalStateException(
    "合盘记录超过 4 MiB 安全上限；请先导出或删除不需要的记录后再保存。",
)

class InMemoryBaziCompatibilityHistoryStore : BaziCompatibilityHistoryStore {
    private val records = mutableListOf<BaziCompatibilityRecord>()

    override fun list(): List<BaziCompatibilityRecord> = synchronized(records) { records.toList() }

    override fun save(
        report: BaziCompatibilityReport,
        createdAtEpochMillis: Long,
    ): BaziCompatibilityRecord = synchronized(records) {
        val existingIndex = records.indexOfFirst { it.report.sameAdoptedSnapshotsAs(report) }
        if (existingIndex >= 0) {
            return@synchronized records.removeAt(existingIndex).copy(
                createdAtEpochMillis = createdAtEpochMillis,
                report = report,
            ).also { records.add(0, it) }
        }
        BaziCompatibilityRecord(
            id = UUID.randomUUID().toString(),
            createdAtEpochMillis = createdAtEpochMillis,
            report = report,
        ).also { records.add(0, it) }
    }

    override fun replace(record: BaziCompatibilityRecord): BaziCompatibilityRecord = synchronized(records) {
        val index = records.indexOfFirst { it.id == record.id }
        if (index >= 0) records[index] = record
        record
    }

    override fun delete(ids: Set<String>): Int = synchronized(records) {
        val before = records.size
        records.removeAll { it.id in ids }
        before - records.size
    }
}

class LocalBaziCompatibilityHistoryStore(
    context: Context,
) : BaziCompatibilityHistoryStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))
    private val lock = Any()

    override fun list(): List<BaziCompatibilityRecord> = synchronized(lock) {
        decodeRecords()
    }

    override fun save(
        report: BaziCompatibilityReport,
        createdAtEpochMillis: Long,
    ): BaziCompatibilityRecord = synchronized(lock) {
        val records = decodeRecords().toMutableList()
        val existingIndex = records.indexOfFirst { it.report.sameAdoptedSnapshotsAs(report) }
        val record = if (existingIndex >= 0) {
            records.removeAt(existingIndex).copy(
                createdAtEpochMillis = createdAtEpochMillis,
                report = report,
            )
        } else {
            BaziCompatibilityRecord(
                id = UUID.randomUUID().toString(),
                createdAtEpochMillis = createdAtEpochMillis,
                report = report,
            )
        }
        records.add(0, record)
        writeRecords(records)
        record
    }

    override fun replace(record: BaziCompatibilityRecord): BaziCompatibilityRecord = synchronized(lock) {
        val records = decodeRecords().toMutableList()
        val index = records.indexOfFirst { it.id == record.id }
        if (index >= 0) {
            records[index] = record
            writeRecords(records)
        }
        record
    }

    override fun delete(ids: Set<String>): Int = synchronized(lock) {
        if (ids.isEmpty()) return 0
        val records = decodeRecords().toMutableList()
        val before = records.size
        records.removeAll { it.id in ids }
        val deleted = before - records.size
        if (deleted > 0) writeRecords(records)
        deleted
    }

    private fun decodeRecords(): List<BaziCompatibilityRecord> {
        if (!file.baseFile.isFile) return emptyList()
        if (file.baseFile.length() > MAX_HISTORY_BYTES) throw CompatibilityHistoryCapacityException()
        return try {
            file.openRead().bufferedReader().use { reader ->
                json.decodeFromString(
                    ListSerializer(BaziCompatibilityRecord.serializer()),
                    reader.readText(),
                )
            }
        } catch (failure: CompatibilityHistoryCapacityException) {
            throw failure
        } catch (failure: Throwable) {
            throw CompatibilityHistoryUnreadableException(failure)
        }
    }

    private fun writeRecords(records: List<BaziCompatibilityRecord>) {
        val payload = json.encodeToString(ListSerializer(BaziCompatibilityRecord.serializer()), records)
            .toByteArray(Charsets.UTF_8)
        if (payload.size > MAX_HISTORY_BYTES) throw CompatibilityHistoryCapacityException()
        val output = file.startWrite()
        try {
            output.write(payload)
            file.finishWrite(output)
        } catch (failure: Throwable) {
            file.failWrite(output)
            throw failure
        }
    }

    private companion object {
        const val FILE_NAME = "bazi-compatibility-history-v1.json"
        const val MAX_HISTORY_BYTES = 4 * 1024 * 1024
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

private fun BaziCompatibilityReport.sameAdoptedSnapshotsAs(other: BaziCompatibilityReport): Boolean =
    left.caseId == other.left.caseId &&
        left.adoptedSnapshotId == other.left.adoptedSnapshotId &&
        right.caseId == other.right.caseId &&
        right.adoptedSnapshotId == other.right.adoptedSnapshotId
