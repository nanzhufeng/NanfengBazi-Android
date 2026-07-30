package com.nanzhufeng.nanfengbazi

import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StageTwoViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `列表加载与姓名别名搜索均走仓储`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-1"] = sampleStoredCase("case-1")
        }
        val viewModel = createViewModel(repository)

        assertEquals(listOf("case-1"), viewModel.state.value.cases.map { it.id })

        viewModel.updateQuery("测试甲")

        assertEquals("测试甲", repository.searchQueries.last())
        assertEquals(listOf("case-1"), viewModel.state.value.cases.map { it.id })
        assertFalse(viewModel.state.value.listLoading)
    }

    @Test
    fun `表单错误保留在新建页且成功后返回刷新列表`() = runTest {
        val repository = FakeCaseRepository()
        val viewModel = createViewModel(repository)
        viewModel.openCreate()

        viewModel.submitCase()

        assertEquals(AppDestination.CreateCase, viewModel.state.value.destination)
        assertEquals("请填写命例别名。", viewModel.state.value.formError)

        viewModel.updateForm { validForm() }
        viewModel.submitCase()

        assertEquals(AppDestination.CaseList, viewModel.state.value.destination)
        assertEquals(1, viewModel.state.value.cases.size)
        assertEquals("命例已完成排盘并保存。", viewModel.state.value.message)
    }

    @Test
    fun `点击列表项读取同一仓储详情`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-detail"] = sampleStoredCase()
        }
        val viewModel = createViewModel(repository)

        viewModel.openDetail("case-detail")

        assertEquals(
            AppDestination.CaseDetail("case-detail"),
            viewModel.state.value.destination,
        )
        assertNotNull(viewModel.state.value.detail)
        assertEquals("合成命例甲", viewModel.state.value.detail?.alias)
    }

    private fun createViewModel(repository: FakeCaseRepository): StageTwoViewModel {
        val engine = RecordingEngine()
        val ids = generateSequence(1) { it + 1 }
            .map { "generated-$it" }
            .iterator()
        return StageTwoViewModel(
            caseRepository = repository,
            createCase = CreateCaseUseCase(
                baziEngine = engine,
                caseRepository = repository,
                clock = Clock.fixed(FixedInstant, ZoneOffset.UTC),
                idGenerator = IdGenerator { ids.next() },
            ),
        )
    }
}
