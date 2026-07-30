package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import com.nanzhufeng.nanfengbazi.domain.model.EventDatePrecision
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.RecordChangeType
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseManagementTest {
    private val fixedClock = Clock.fixed(FixedInstant.plusSeconds(3600), ZoneOffset.UTC)

    @Test
    fun `编辑出生资料会重算并保留旧快照`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-edit"] = sampleStoredCase("case-edit")
        }
        val engine = RecordingEngine()
        val useCase = EditCaseUseCase(
            baziEngine = engine,
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { "snapshot-new" },
        )

        val result = useCase(
            caseId = "case-edit",
            expectedRevision = 1,
            form = validForm().copy(
                alias = "合成命例乙",
                sex = SexForFortuneDirection.WOMAN,
                hour = "11",
            ),
        )

        assertEquals(CaseMutationResult.Saved("case-edit", 2), result)
        val saved = repository.stored.getValue("case-edit")
        assertEquals("合成命例乙", saved.alias)
        assertEquals(2, saved.calculationSnapshots.size)
        assertFalse(saved.calculationSnapshots.first().adopted)
        assertTrue(saved.calculationSnapshots.last().adopted)
        assertEquals(1, engine.calls)
    }

    @Test
    fun `编辑时清空已有姓名保存为CLEARED而非ABSENT`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-name"] = sampleStoredCase("case-name")
        }
        val useCase = EditCaseUseCase(
            baziEngine = RecordingEngine(),
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { "snapshot-name" },
        )

        useCase("case-name", 1, validForm().copy(name = ""))

        assertEquals(
            ExplicitText.cleared(),
            repository.stored.getValue("case-name").name,
        )
    }

    @Test
    fun `旧修订在重算前被拒绝`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-edit"] = sampleStoredCase("case-edit").copy(revision = 3)
        }
        val engine = RecordingEngine()
        val useCase = EditCaseUseCase(engine, repository)

        val result = useCase("case-edit", 1, validForm())

        assertEquals(CaseMutationResult.RevisionConflict(3), result)
        assertEquals(0, engine.calls)
    }

    @Test
    fun `文本记录新增修改删除保留完整版本并统一分析分类`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-record"] = sampleStoredCase("case-record")
        }
        val useCase = TextRecordUseCase(
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { "record-1" },
        )

        assertEquals(
            CaseMutationResult.Saved("case-record", 2),
            useCase.save(
                "case-record",
                1,
                null,
                TextRecordDraft(CaseTextRecordType.OWNER_FEEDBACK, "合成反馈"),
            ),
        )
        val created = repository.stored.getValue("case-record").textRecords.single()
        assertEquals("record-1", created.id)
        assertEquals("合成反馈", created.content)

        useCase.save(
            "case-record",
            2,
            "record-1",
            TextRecordDraft(
                type = CaseTextRecordType.ANALYSIS,
                content = "修改后的合成分析",
                analysisCategory = AnalysisCategory.CAREER,
            ),
        )
        val edited = repository.stored.getValue("case-record").textRecords.single()
        assertEquals(created.createdAt, edited.createdAt)
        assertEquals(CaseTextRecordType.ANALYSIS, edited.type)
        assertEquals(AnalysisCategory.CAREER, edited.analysisCategory)

        assertEquals(
            CaseMutationResult.Saved("case-record", 4),
            useCase.delete("case-record", 3, "record-1"),
        )
        val deleted = repository.stored.getValue("case-record")
        assertTrue(deleted.textRecords.isEmpty())
        assertEquals(
            listOf(
                RecordChangeType.CREATED,
                RecordChangeType.UPDATED,
                RecordChangeType.DELETED,
            ),
            deleted.textRecordRevisions.map { it.changeType },
        )
        assertEquals(listOf(1, 2, 3), deleted.textRecordRevisions.map { it.version })
        assertEquals("合成反馈", deleted.textRecordRevisions.first().snapshot.content)
        assertEquals(
            "修改后的合成分析",
            deleted.textRecordRevisions.last().snapshot.content,
        )
    }

    @Test
    fun `空记录与无效事件日期不会写入`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-invalid"] = sampleStoredCase("case-invalid")
        }
        val records = TextRecordUseCase(repository)
        val events = CaseEventUseCase(repository)

        assertEquals(
            CaseMutationResult.ValidationFailed("记录内容不能为空。"),
            records.save("case-invalid", 1, null, TextRecordDraft()),
        )
        assertEquals(
            CaseMutationResult.ValidationFailed("事件日期无效，请检查年月日。"),
            events.save(
                "case-invalid",
                1,
                null,
                EventDraft("2025", "2", "29", rawText = "合成事件"),
            ),
        )
        assertEquals(
            CaseMutationResult.ValidationFailed("事件标题不能超过 80 个字符。"),
            events.save(
                "case-invalid",
                1,
                null,
                EventDraft(
                    rawText = "合成事件",
                    title = "题".repeat(81),
                ),
            ),
        )
        assertTrue(repository.stored.getValue("case-invalid").events.isEmpty())
    }

    @Test
    fun `旧记录首次修改先补建基线版本`() = runTest {
        val legacy = CaseTextRecord(
            id = "legacy-record",
            type = CaseTextRecordType.NOTE,
            content = "旧版原文",
            createdAt = FixedInstant,
            updatedAt = FixedInstant,
        )
        val repository = FakeCaseRepository().apply {
            stored["case-legacy-record"] = sampleStoredCase("case-legacy-record").copy(
                textRecords = listOf(legacy),
            )
        }

        TextRecordUseCase(repository, fixedClock).save(
            "case-legacy-record",
            1,
            legacy.id,
            TextRecordDraft(content = "新版原文"),
        )

        val revisions = repository.stored
            .getValue("case-legacy-record")
            .textRecordRevisions
        assertEquals(listOf("旧版原文", "新版原文"), revisions.map { it.snapshot.content })
        assertEquals(
            listOf(RecordChangeType.CREATED, RecordChangeType.UPDATED),
            revisions.map { it.changeType },
        )
    }

    @Test
    fun `关键事件支持未知日期与年月日精度`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-event"] = sampleStoredCase("case-event")
        }
        val ids = ArrayDeque(listOf("event-unknown", "event-day"))
        val useCase = CaseEventUseCase(
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        useCase.save(
            "case-event",
            1,
            null,
            EventDraft(rawText = "时间待核对的合成事件"),
        )
        useCase.save(
            "case-event",
            2,
            null,
            EventDraft(
                year = "2024",
                month = "6",
                day = "1",
                status = "已确认",
                rawText = "有日期的合成事件",
                title = "事业阶段变化",
                category = CaseEventCategory.CAREER,
            ),
        )

        val events = repository.stored.getValue("case-event").events
        assertEquals(EventDatePrecision.UNKNOWN, events[0].datePrecision)
        assertEquals(EventDatePrecision.DAY, events[1].datePrecision)
        assertEquals("已确认", events[1].status)
        assertEquals("事业阶段变化", events[1].title)
        assertEquals(CaseEventCategory.CAREER, events[1].category)
        assertEquals(
            listOf(1, 1),
            repository.stored.getValue("case-event").eventRevisions.map { it.version },
        )
        assertTrue(
            repository.stored.getValue("case-event").eventRevisions.all {
                it.changeType == RecordChangeType.CREATED
            },
        )
    }

    @Test
    fun `旧事件修改删除保留基线和删除快照`() = runTest {
        val legacy = CaseEvent(
            id = "legacy-event",
            year = 2020,
            datePrecision = EventDatePrecision.YEAR,
            rawText = "旧版事件原文",
            createdAt = FixedInstant,
        )
        val repository = FakeCaseRepository().apply {
            stored["case-legacy-event"] = sampleStoredCase("case-legacy-event").copy(
                events = listOf(legacy),
            )
        }
        val useCase = CaseEventUseCase(repository, fixedClock)

        useCase.save(
            "case-legacy-event",
            1,
            legacy.id,
            EventDraft("2021", rawText = "新版事件原文"),
        )
        useCase.delete("case-legacy-event", 2, legacy.id)

        val stored = repository.stored.getValue("case-legacy-event")
        assertTrue(stored.events.isEmpty())
        assertEquals(
            listOf(
                RecordChangeType.CREATED,
                RecordChangeType.UPDATED,
                RecordChangeType.DELETED,
            ),
            stored.eventRevisions.map { it.changeType },
        )
        assertEquals(
            listOf("旧版事件原文", "新版事件原文", "新版事件原文"),
            stored.eventRevisions.map { it.snapshot.rawText },
        )
    }

    @Test
    fun `读取数据库异常不会被误报为记录不存在`() = runTest {
        val repository = FakeCaseRepository().apply {
            readFailure = IllegalStateException("database closed")
        }
        val useCase = TextRecordUseCase(repository)

        val result = useCase.save(
            "case-1",
            1,
            null,
            TextRecordDraft(content = "合成记录"),
        )

        assertTrue(result is CaseMutationResult.StorageFailed)
    }

    @Test
    fun `分类编辑复用同名目录并保存收藏置顶`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-meta"] = sampleStoredCase("case-meta")
            stored["case-catalog"] = sampleStoredCase("case-catalog").copy(
                groups = listOf(CaseGroup("group-existing", "家人")),
                tags = listOf(CaseTag("tag-existing", "已核对")),
            )
        }
        val ids = ArrayDeque(listOf("group-new", "tag-new"))
        val useCase = CaseMetadataUseCase(
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        val result = useCase.save(
            caseId = "case-meta",
            expectedRevision = 1,
            draft = CaseMetadataDraft(
                groupNames = "家人，研究",
                tagNames = "已核对, 待复盘",
                isFavorite = true,
                isPinned = true,
            ),
        )

        assertEquals(CaseMutationResult.Saved("case-meta", 2), result)
        val saved = repository.stored.getValue("case-meta")
        assertEquals(listOf("group-existing", "group-new"), saved.groups.map { it.id })
        assertEquals(listOf("tag-existing", "tag-new"), saved.tags.map { it.id })
        assertTrue(saved.isFavorite)
        assertTrue(saved.isPinned)
    }

    @Test
    fun `分类数量和名称长度在写入前校验`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-meta"] = sampleStoredCase("case-meta")
        }
        val useCase = CaseMetadataUseCase(repository)

        val tooMany = (1..11).joinToString("，") { "分组$it" }
        assertEquals(
            CaseMutationResult.ValidationFailed("每个命例最多设置 10 个分组。"),
            useCase.save("case-meta", 1, CaseMetadataDraft(groupNames = tooMany)),
        )
        assertEquals(1L, repository.stored.getValue("case-meta").revision)
    }

    @Test
    fun `软删除完整保留聚合并可恢复`() = runTest {
        val record = CaseTextRecord(
            id = "record-preserved",
            type = CaseTextRecordType.NOTE,
            content = "必须保留的合成记录",
            createdAt = FixedInstant,
            updatedAt = FixedInstant,
        )
        val repository = FakeCaseRepository().apply {
            stored["case-trash"] = sampleStoredCase("case-trash").copy(
                textRecords = listOf(record),
            )
        }
        val useCase = CaseLifecycleUseCase(repository, fixedClock)

        assertEquals(
            CaseMutationResult.Saved("case-trash", 2),
            useCase.moveToTrash("case-trash", 1),
        )
        val trashed = repository.stored.getValue("case-trash")
        assertTrue(trashed.deletedAt != null)
        assertEquals(listOf(record), trashed.textRecords)

        assertEquals(
            CaseMutationResult.Saved("case-trash", 3),
            useCase.restore("case-trash", 2),
        )
        assertEquals(null, repository.stored.getValue("case-trash").deletedAt)
        assertEquals(listOf(record), repository.stored.getValue("case-trash").textRecords)
    }

    @Test
    fun `复制生成新身份并只复制出生资料与计算快照`() = runTest {
        val sourceRecord = CaseTextRecord(
            id = "source-record",
            type = CaseTextRecordType.NOTE,
            content = "不应进入副本的记录",
            createdAt = FixedInstant,
            updatedAt = FixedInstant,
        )
        val repository = FakeCaseRepository().apply {
            stored["case-source"] = sampleStoredCase("case-source").copy(
                textRecords = listOf(sourceRecord),
                textRecordRevisions = listOf(
                    CaseTextRecordRevision(
                        id = "source-record-revision",
                        recordId = sourceRecord.id,
                        version = 1,
                        changeType = RecordChangeType.CREATED,
                        snapshot = sourceRecord,
                        changedAt = FixedInstant,
                    ),
                ),
            )
        }
        val ids = ArrayDeque(listOf("case-copy", "snapshot-copy"))
        val useCase = CaseLifecycleUseCase(
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        val result = useCase.duplicate("case-source", 1)

        assertEquals(CaseMutationResult.Saved("case-copy", 1), result)
        val copied = repository.stored.getValue("case-copy")
        assertEquals("case-source", copied.copiedFromCaseId)
        assertEquals(CaseSourceType.CASE_COPY, copied.sourceType)
        assertEquals("snapshot-copy", copied.calculationSnapshots.single().id)
        assertTrue(copied.textRecords.isEmpty())
        assertTrue(copied.textRecordRevisions.isEmpty())
        assertTrue(copied.events.isEmpty())
        assertTrue(copied.eventRevisions.isEmpty())
        assertTrue(copied.attachments.isEmpty())
        assertTrue(copied.fieldEvidence.isEmpty())
    }
}
