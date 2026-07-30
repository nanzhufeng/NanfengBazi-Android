package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateCaseUseCaseTest {
    @Test
    fun `有效表单通过唯一引擎计算并由仓储保存`() = runTest {
        val engine = RecordingEngine()
        val repository = FakeCaseRepository()
        val ids = ArrayDeque(listOf("case-created", "snapshot-created"))
        val useCase = CreateCaseUseCase(
            baziEngine = engine,
            caseRepository = repository,
            clock = Clock.fixed(FixedInstant, ZoneOffset.UTC),
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        val result = useCase(validForm())

        assertEquals(CreateCaseResult.Created("case-created"), result)
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
}
