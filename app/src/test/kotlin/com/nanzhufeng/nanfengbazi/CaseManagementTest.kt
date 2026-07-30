package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.EventDatePrecision
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
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
    fun `文本记录新增修改删除保持身份与顺序`() = runTest {
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
            TextRecordDraft(CaseTextRecordType.ANALYSIS, "修改后的合成分析"),
        )
        val edited = repository.stored.getValue("case-record").textRecords.single()
        assertEquals(created.createdAt, edited.createdAt)
        assertEquals(CaseTextRecordType.ANALYSIS, edited.type)

        assertEquals(
            CaseMutationResult.Saved("case-record", 4),
            useCase.delete("case-record", 3, "record-1"),
        )
        assertTrue(repository.stored.getValue("case-record").textRecords.isEmpty())
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
        assertTrue(repository.stored.getValue("case-invalid").events.isEmpty())
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
            EventDraft("2024", "6", "1", "已确认", "有日期的合成事件"),
        )

        val events = repository.stored.getValue("case-event").events
        assertEquals(EventDatePrecision.UNKNOWN, events[0].datePrecision)
        assertEquals(EventDatePrecision.DAY, events[1].datePrecision)
        assertEquals("已确认", events[1].status)
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
}
