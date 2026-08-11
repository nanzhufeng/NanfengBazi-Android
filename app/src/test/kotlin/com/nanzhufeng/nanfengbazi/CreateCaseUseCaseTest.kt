package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.TimeZoneChoiceRequiredException
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateCaseUseCaseTest {
    @Test
    fun `即时排盘允许空别名且不检查重复不写仓储`() = runTest {
        val engine = RecordingEngine()
        val repository = FakeCaseRepository().apply {
            stored["existing"] = sampleStoredCase("existing")
        }
        val useCase = CreateCaseUseCase(engine, repository)

        val result = useCase.preview(validForm().copy(alias = ""))

        assertTrue(result is PreviewCaseResult.Calculated)
        assertEquals(1, engine.calls)
        assertEquals(setOf("existing"), repository.stored.keys)
    }

    @Test
    fun `有效表单通过唯一引擎计算并由仓储保存`() = runTest {
        val engine = RecordingEngine()
        val repository = FakeCaseRepository()
        val ids = ArrayDeque(listOf("case-created", "snapshot-created", "candidate-created"))
        val useCase = CreateCaseUseCase(
            baziEngine = engine,
            caseRepository = repository,
            clock = Clock.fixed(FixedInstant, ZoneOffset.UTC),
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        val result = useCase(validForm())

        assertEquals("case-created", (result as CreateCaseResult.Created).caseId)
        assertEquals(1, engine.calls)
        assertEquals(null, repository.lastExpectedRevision)
        val saved = repository.stored.getValue("case-created")
        assertEquals("合成命例甲", saved.alias)
        assertEquals("测试甲", saved.name.value)
        assertEquals(
            calculationResult(engine.lastInput!!).fourPillars,
            saved.calculationSnapshots.single().result.fourPillars,
        )
        assertTrue(saved.calculationSnapshots.single().adopted)
        assertEquals("candidate-created", saved.birthTimeCandidates.single().id)
        assertTrue(saved.birthTimeCandidates.single().adopted)
        assertEquals(
            saved.calculationSnapshots.single().id,
            saved.birthTimeCandidates.single().calculationSnapshotId,
        )
    }

    @Test
    fun `保存已排出的预览不会再次调用排盘引擎`() = runTest {
        val engine = RecordingEngine()
        val repository = FakeCaseRepository()
        val ids = ArrayDeque(listOf("prepared-case", "prepared-snapshot", "prepared-candidate"))
        val useCase = CreateCaseUseCase(
            baziEngine = engine,
            caseRepository = repository,
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        val preview = useCase.preview(validForm()) as PreviewCaseResult.Calculated
        val result = useCase.savePrepared(validForm(), preview)

        assertEquals("prepared-case", (result as CreateCaseResult.Created).caseId)
        assertEquals(1, engine.calls)
        assertEquals("prepared-case", repository.stored.keys.single())
    }

    @Test
    fun `不能保存已被后续输入覆盖的预览`() = runTest {
        val useCase = CreateCaseUseCase(RecordingEngine(), FakeCaseRepository())
        val preview = useCase.preview(validForm()) as PreviewCaseResult.Calculated

        val result = useCase.savePrepared(validForm().copy(minute = "38"), preview)

        assertEquals(
            CreateCaseResult.ValidationFailed("排盘输入已更新，请重新开始排盘后再保存。"),
            result,
        )
    }

    @Test
    fun `名人录入入口将命例保存到名人案例库`() = runTest {
        val repository = FakeCaseRepository()
        val ids = ArrayDeque(listOf("celebrity-case", "celebrity-snapshot", "celebrity-candidate"))

        val result = CreateCaseUseCase(
            baziEngine = RecordingEngine(),
            caseRepository = repository,
            idGenerator = IdGenerator { ids.removeFirst() },
        )(validForm().copy(libraryType = CaseLibraryType.CELEBRITY))

        assertEquals("celebrity-case", (result as CreateCaseResult.Created).caseId)
        assertEquals(
            CaseLibraryType.CELEBRITY,
            repository.stored.getValue("celebrity-case").libraryType,
        )
    }

    @Test
    fun `首页所选分组由仓储目录解析并随命例一次保存`() = runTest {
        val repository = FakeCaseRepository().apply {
            groupCatalog += CaseGroup("group-family", "家人")
        }
        val ids = ArrayDeque(listOf("grouped-case", "grouped-snapshot", "grouped-candidate"))
        val result = CreateCaseUseCase(
            baziEngine = RecordingEngine(),
            caseRepository = repository,
            idGenerator = IdGenerator { ids.removeFirst() },
        )(validForm().copy(groupId = "group-family"))

        assertEquals("grouped-case", (result as CreateCaseResult.Created).caseId)
        assertEquals(listOf(CaseGroup("group-family", "家人")), repository.stored
            .getValue("grouped-case").groups)
    }

    @Test
    fun `首页所选分组已删除时拒绝保存且不生成孤立关联`() = runTest {
        val repository = FakeCaseRepository()

        val result = CreateCaseUseCase(RecordingEngine(), repository)(
            validForm().copy(groupId = "missing-group"),
        )

        assertEquals(
            CreateCaseResult.ValidationFailed("所选分组已不存在，请重新选择。"),
            result,
        )
        assertTrue(repository.stored.isEmpty())
    }

    @Test
    fun `保存使用引擎规范化后的时区证据而非未解析输入`() = runTest {
        val repository = FakeCaseRepository()
        val engine = BaziEngine { input, _ ->
            calculationResult(
                input.copy(
                    resolvedUtcOffsetSeconds = 28_800,
                    timeZoneDataVersion = "tzdb:test",
                ),
            )
        }
        val ids = ArrayDeque(
            listOf("normalized-case", "normalized-snapshot", "normalized-candidate"),
        )
        val result = CreateCaseUseCase(
            baziEngine = engine,
            caseRepository = repository,
            idGenerator = IdGenerator { ids.removeFirst() },
        )(validForm())

        assertEquals("normalized-case", (result as CreateCaseResult.Created).caseId)
        val saved = repository.stored.getValue("normalized-case")
        assertEquals(28_800, saved.birthInput.resolvedUtcOffsetSeconds)
        assertEquals("tzdb:test", saved.birthInput.timeZoneDataVersion)
        assertEquals(saved.birthInput, saved.calculationSnapshots.single().result.normalizedInput)
    }

    @Test
    fun `真太阳时表单选择对应版本化计算配置`() = runTest {
        val engine = RecordingEngine()
        val repository = FakeCaseRepository()

        val result = CreateCaseUseCase(engine, repository)(
            validForm().copy(
                longitude = "118.68",
                latitude = "33.73",
                useTrueSolarTime = true,
            ),
        )

        assertTrue(result is CreateCaseResult.Created)
        assertTrue(engine.lastInput?.useTrueSolarTime == true)
        assertEquals(SolarTimeMode.TRUE_SOLAR_TIME, engine.lastProfile?.solarTimeMode)
        assertEquals("tyme-true-solar-provisional-v1", engine.lastProfile?.id)
    }

    @Test
    fun `晚子时表单选择进入版本化计算快照`() = runTest {
        val engine = RecordingEngine()
        val repository = FakeCaseRepository()

        val result = CreateCaseUseCase(engine, repository)(
            validForm().copy(
                hour = "23",
                ratHourRule = RatHourRule.LATE_RAT_SAME_DAY,
            ),
        )

        assertTrue(result is CreateCaseResult.Created)
        assertEquals(RatHourRule.LATE_RAT_SAME_DAY, engine.lastProfile?.ratHourRule)
        assertEquals("tyme-late-rat-same-day-v1", engine.lastProfile?.id)
        assertEquals(
            RatHourRule.LATE_RAT_SAME_DAY,
            repository.stored.values.single()
                .calculationSnapshots.single()
                .result.profile.ratHourRule,
        )
    }

    @Test
    fun `夏令时重叠以结构化候选返回且不写库`() = runTest {
        val repository = FakeCaseRepository()
        val engine = BaziEngine { _, _ ->
            throw TimeZoneChoiceRequiredException(
                timeZoneId = "America/New_York",
                validUtcOffsetSeconds = listOf(-14_400, -18_000),
                timeZoneDataVersion = "tzdb:test",
            )
        }

        val result = CreateCaseUseCase(engine, repository)(
            validForm().copy(timeZoneId = "America/New_York"),
        )

        assertEquals(
            CreateCaseResult.TimeZoneChoiceRequired(
                timeZoneId = "America/New_York",
                validUtcOffsetSeconds = listOf(-14_400, -18_000),
                timeZoneDataVersion = "tzdb:test",
            ),
            result,
        )
        assertTrue(repository.stored.isEmpty())
    }

    @Test
    fun `计算失败不会调用仓储且保留可读原因`() = runTest {
        val engine = BaziEngine { _, _: CalculationProfile ->
            throw IllegalArgumentException("当前口径不受支持")
        }
        val repository = FakeCaseRepository()
        val useCase = CreateCaseUseCase(engine, repository)

        val result = useCase(validForm())

        assertEquals(
            CreateCaseResult.CalculationFailed("当前口径不受支持"),
            result,
        )
        assertTrue(repository.stored.isEmpty())
    }

    @Test
    fun `保存冲突与数据库异常映射为结构化结果`() = runTest {
        val engine = RecordingEngine()
        val repository = FakeCaseRepository()
        repository.writeOverride = CaseWriteResult.AlreadyExists("duplicate", 2)
        val useCase = CreateCaseUseCase(engine, repository)

        assertEquals(
            CreateCaseResult.AlreadyExists("duplicate"),
            useCase(validForm()),
        )

        repository.writeOverride = null
        repository.saveFailure = IllegalStateException("database closed")
        assertTrue(useCase(validForm()) is CreateCaseResult.StorageFailed)
    }

    @Test
    fun `发现重复候选时不会写入且明确确认后可保存`() = runTest {
        val engine = RecordingEngine()
        val repository = FakeCaseRepository().apply {
            stored["existing"] = sampleStoredCase("existing")
        }
        val ids = ArrayDeque(listOf("copy", "snapshot-copy", "candidate-copy"))
        val useCase = CreateCaseUseCase(
            baziEngine = engine,
            caseRepository = repository,
            clock = Clock.fixed(FixedInstant, ZoneOffset.UTC),
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        val warning = useCase(validForm())

        assertTrue(warning is CreateCaseResult.DuplicateCandidates)
        assertEquals(setOf("existing"), repository.stored.keys)

        val saved = useCase(validForm(), allowDuplicate = true)

        assertEquals("copy", (saved as CreateCaseResult.Created).caseId)
        assertEquals(setOf("existing", "copy"), repository.stored.keys)
    }
}
