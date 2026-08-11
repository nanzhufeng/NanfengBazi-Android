package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.data.backup.BackupCounts
import com.nanzhufeng.nanfengbazi.data.backup.BackupExportResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupManifest
import com.nanzhufeng.nanfengbazi.data.backup.BackupPreviewResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupProtection
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseRestoreDecision
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseRestoreAction
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseMergePreparationResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseMergePreparation
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseRestorePreview
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseConflictCandidate
import com.nanzhufeng.nanfengbazi.data.backup.BackupCaseConflictReason
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestorePlan
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestoreExecutionResult
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestoreExecutionSummary
import com.nanzhufeng.nanfengbazi.data.backup.BackupRestorePlanResult
import com.nanzhufeng.nanfengbazi.data.backup.CaseBackupOperations
import com.nanzhufeng.nanfengbazi.data.backup.RestorePreview
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseExchangeService
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseAttachmentMode
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseBundleOperations
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseDocument
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseDocumentProtection
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseExportResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseFieldKey
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseImportDecision
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseImportResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergeModule
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseMergePlan
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseValueChoice
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCasePreview
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCasePreviewResult
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseProtection
import com.nanzhufeng.nanfengbazi.data.exchange.CaseMergeAnalysis
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseCounts
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseFieldDifference
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.AlmanacMonthQuery
import com.nanzhufeng.nanfengbazi.domain.AlmanacDoubleHours
import com.nanzhufeng.nanfengbazi.domain.AlmanacReader
import com.nanzhufeng.nanfengbazi.domain.AlmanacResult
import com.nanzhufeng.nanfengbazi.domain.TimeZoneChoiceRequiredException
import com.nanzhufeng.nanfengbazi.domain.CaseSortOrder
import com.nanzhufeng.nanfengbazi.domain.CaseAdvancedFilter
import com.nanzhufeng.nanfengbazi.domain.FourPillarsSearchFilter
import com.nanzhufeng.nanfengbazi.domain.PillarCharacterFilter
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseImageDeliveryMode
import com.nanzhufeng.nanfengbazi.domain.FortunePosition
import com.nanzhufeng.nanfengbazi.domain.FortunePositionResolver
import com.nanzhufeng.nanfengbazi.domain.FortunePositionStatus
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookup
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupCandidate
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupEvidence
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupResult
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeAdoptionErrorCode
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidateStatus
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateAdoptionErrorCode
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateStatus
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.AnnualFortune
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import com.nanzhufeng.nanfengbazi.domain.model.RecordChangeType
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CaseProfile
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.LunarDateTime
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SourceAttachment
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeAlmanacReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `列表首次加载后姓名别名搜索复用缓存`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-1"] = sampleStoredCase("case-1")
        }
        val viewModel = createViewModel(repository)
        val catalogReadCount = repository.searchRequests.size

        assertEquals(listOf("case-1"), viewModel.state.value.cases.map { it.id })

        viewModel.updateQuery("测试甲")

        assertEquals(catalogReadCount, repository.searchRequests.size)
        assertEquals("测试甲", viewModel.state.value.query)
        assertEquals(listOf("case-1"), viewModel.state.value.cases.map { it.id })
        assertFalse(viewModel.state.value.listLoading)
    }

    @Test
    fun `主导航切换不触发无关命例目录重投影`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-nav"] = sampleStoredCase("case-nav")
        }
        val viewModel = createViewModel(repository)
        val catalogReadCount = repository.searchRequests.size

        viewModel.openSettings()
        viewModel.openCreate()
        viewModel.openRecordHub()

        assertEquals(catalogReadCount, repository.searchRequests.size)
        assertFalse(viewModel.state.value.listLoading)
    }

    @Test
    fun `应用级命例目录在重建页面时先提供已有快照再静默校准`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-shared"] = sampleStoredCase("case-shared")
        }
        val catalogStore = RepositoryCaseCatalogStore(repository, dispatcher)
        catalogStore.load(forceRefresh = false)
        val readsBeforeRecreation = repository.searchRequests.size
        catalogStore.load(forceRefresh = false)
        assertEquals(readsBeforeRecreation, repository.searchRequests.size)

        val viewModel = createViewModel(
            repository = repository,
            caseCatalogStore = catalogStore,
        )

        assertEquals(listOf("case-shared"), viewModel.state.value.cases.map { it.id })
        assertFalse(viewModel.state.value.listLoading)
        assertTrue(repository.searchRequests.size > readsBeforeRecreation)
    }

    @Test
    fun `进程重建优先使用持久目录快照并在后台校准`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-persisted"] = sampleStoredCase("case-persisted")
        }
        val persistedSnapshot = CaseCatalogSnapshot(
            cases = repository.search(CaseSearchRequest(visibility = CaseVisibility.ALL)),
            groupsByLibrary = CaseLibraryType.entries.associateWith { emptyList<CaseGroup>() },
        )
        repository.searchRequests.clear()
        val persistentCache = object : CaseCatalogSnapshotCache {
            override suspend fun read(): CaseCatalogSnapshot = persistedSnapshot

            override suspend fun write(snapshot: CaseCatalogSnapshot) = Unit
        }
        val catalogStore = RepositoryCaseCatalogStore(
            repository = repository,
            ioDispatcher = dispatcher,
            persistentCache = persistentCache,
        )

        val bootstrap = catalogStore.bootstrap()

        assertTrue(bootstrap.fromPersistentCache)
        assertEquals(listOf("case-persisted"), bootstrap.snapshot.cases.map { it.id })
        assertEquals(0, repository.searchRequests.size)

        catalogStore.load(forceRefresh = true)

        assertEquals(1, repository.searchRequests.size)
    }

    @Test
    fun `子时口径从设置存储读取并同步到新排盘与四柱反查`() = runTest {
        val repository = FakeCaseRepository()
        val preferenceStore = InMemoryCalculationPreferenceStore(
            RatHourRule.LATE_RAT_SAME_DAY,
        )
        val viewModel = createViewModel(
            repository = repository,
            calculationPreferenceStore = preferenceStore,
        )

        assertEquals(RatHourRule.LATE_RAT_SAME_DAY, viewModel.state.value.defaultRatHourRule)
        assertEquals(RatHourRule.LATE_RAT_SAME_DAY, viewModel.state.value.form.ratHourRule)

        viewModel.updateDefaultRatHourRule(RatHourRule.TYME_DEFAULT)
        viewModel.openFourPillarsLookup(listOf("甲子", "乙丑", "丙寅", "丁卯"))

        assertEquals(RatHourRule.TYME_DEFAULT, preferenceStore.readRatHourRule())
        assertEquals(RatHourRule.TYME_DEFAULT, viewModel.state.value.form.ratHourRule)
        assertEquals(
            RatHourRule.TYME_DEFAULT,
            viewModel.state.value.fourPillarsLookupForm.ratHourRule,
        )
        assertEquals("甲子", viewModel.state.value.fourPillarsLookupForm.yearPillar)
    }

    @Test
    fun `万年历所选日期直接打开未保存的即时专业细盘`() = runTest {
        val repository = FakeCaseRepository()
        val viewModel = createViewModel(
            repository = repository,
            almanacReader = TymeAlmanacReader(),
        )
        viewModel.updateForm { validForm() }

        viewModel.openAlmanac()

        assertEquals(AppDestination.Almanac, viewModel.state.value.destination)
        assertEquals(2026, viewModel.state.value.almanacYear)
        assertEquals(7, viewModel.state.value.almanacMonth)
        assertEquals(30, viewModel.state.value.almanacSelectedDay)
        assertNotNull(viewModel.state.value.almanacView)

        viewModel.moveAlmanacMonth(1)
        viewModel.selectAlmanacDate(
            com.nanzhufeng.nanfengbazi.domain.AlmanacDate(2026, 8, 2),
        )
        viewModel.selectAlmanacDoubleHour(6)
        viewModel.useAlmanacDateForChart()

        assertEquals(
            AppDestination.CaseDetail("instant-almanac-preview"),
            viewModel.state.value.destination,
        )
        assertEquals(CaseDetailSection.FORTUNE, viewModel.state.value.detailSection)
        assertEquals("即时排盘案例", viewModel.state.value.detail?.alias)
        assertEquals(true, viewModel.state.value.detailIsTransient)
        assertEquals(emptySet<String>(), repository.stored.keys)
        assertEquals(CalendarSystem.SOLAR, viewModel.state.value.form.calendarSystem)
        assertEquals("2026", viewModel.state.value.form.year)
        assertEquals("8", viewModel.state.value.form.month)
        assertEquals("2", viewModel.state.value.form.day)
        assertEquals("11", viewModel.state.value.form.hour)

        viewModel.closeTransientDetail()
        assertEquals(AppDestination.Almanac, viewModel.state.value.destination)
        assertNull(viewModel.state.value.detail)
    }

    @Test
    fun `万年历快捷跳转一次性更新日期与对应时辰`() = runTest {
        val viewModel = createViewModel(
            repository = FakeCaseRepository(),
            almanacReader = TymeAlmanacReader(),
        )

        viewModel.openAlmanac()
        viewModel.selectAlmanacDateTime(
            com.nanzhufeng.nanfengbazi.domain.AlmanacDate(2026, 8, 7),
            civilHour = 15,
        )

        assertEquals(2026, viewModel.state.value.almanacYear)
        assertEquals(8, viewModel.state.value.almanacMonth)
        assertEquals(7, viewModel.state.value.almanacSelectedDay)
        assertEquals(8, viewModel.state.value.almanacSelectedDoubleHourIndex)
        assertEquals("申", viewModel.state.value.almanacView?.selected?.selectedDoubleHour?.branch)
    }

    @Test
    fun `万年历今天刷新期间保持完整旧画面并在完成后原子替换`() = runTest {
        val reader = GateableAlmanacReader()
        val viewModel = createViewModel(
            repository = FakeCaseRepository(),
            almanacReader = reader,
        )

        viewModel.openAlmanac()
        reader.gate = CompletableDeferred()
        viewModel.moveAlmanacMonth(1)
        viewModel.moveAlmanacMonth(1)
        assertEquals(7, viewModel.state.value.almanacMonth)
        reader.gate?.complete(Unit)
        assertEquals(9, viewModel.state.value.almanacMonth)
        viewModel.moveAlmanacMonth(-1)
        assertEquals(8, viewModel.state.value.almanacMonth)
        val visibleBeforeToday = requireNotNull(viewModel.state.value.almanacView)

        reader.gate = CompletableDeferred()
        viewModel.showTodayInAlmanac()

        assertEquals(8, viewModel.state.value.almanacMonth)
        assertEquals(visibleBeforeToday, viewModel.state.value.almanacView)
        assertFalse(viewModel.state.value.almanacLoading)
        assertTrue(viewModel.state.value.almanacRefreshingSelection)

        reader.gate?.complete(Unit)

        assertEquals(2026, viewModel.state.value.almanacYear)
        assertEquals(7, viewModel.state.value.almanacMonth)
        assertEquals(30, viewModel.state.value.almanacSelectedDay)
        assertEquals(
            AlmanacDoubleHours.indexForCivilHour(8),
            viewModel.state.value.almanacSelectedDoubleHourIndex,
        )
        assertEquals(7, viewModel.state.value.almanacView?.query?.month)
        assertEquals(30, viewModel.state.value.almanacView?.selected?.date?.day)
        assertEquals(
            "辰",
            viewModel.state.value.almanacView?.selected?.selectedDoubleHour?.branch,
        )
        assertFalse(viewModel.state.value.almanacRefreshingSelection)

        reader.gate = CompletableDeferred()
        viewModel.showTodayInAlmanac()
        assertFalse(viewModel.state.value.almanacRefreshingSelection)
    }

    @Test
    fun `出生时间今天同时准备公历农历与当前四柱`() = runTest {
        val viewModel = createViewModel(
            repository = FakeCaseRepository(),
            almanacReader = TymeAlmanacReader(),
        )

        viewModel.prepareBirthPickerToday()

        val today = requireNotNull(viewModel.state.value.birthPickerTodaySnapshot)
        assertEquals(2026, today.solarYear)
        assertEquals(7, today.solarMonth)
        assertEquals(30, today.solarDay)
        assertEquals(8, today.hour)
        assertEquals(0, today.minute)
        assertTrue(today.lunarYear in 2025..2026)
        assertTrue(today.lunarMonth in 1..12)
        assertTrue(today.lunarDay in 1..30)
        assertEquals(4, today.pillars.size)
        assertTrue(today.pillars.all { it.length == 2 })
    }

    @Test
    fun `命例对比读取全部活动命例并生成客观字段报告`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["left"] = sampleStoredCase("left").copy(alias = "甲盘")
            stored["right"] = sampleStoredCase("right").copy(
                alias = "乙盘",
                calculationSnapshots = sampleStoredCase("right").calculationSnapshots.map {
                    it.copy(id = "snapshot-right")
                },
            )
        }
        val viewModel = createViewModel(repository)

        viewModel.openCaseComparison()

        val state = viewModel.state.value
        assertEquals(AppDestination.CaseComparison, state.destination)
        assertEquals(setOf("left", "right"), state.comparisonCandidates.map { it.id }.toSet())
        assertNotNull(state.comparisonReport)
        assertEquals(
            setOf("left", "right"),
            setOf(state.comparisonLeftCaseId, state.comparisonRightCaseId),
        )
        assertFalse(state.comparisonLoading)
        assertNull(state.comparisonError)
        assertTrue(
            repository.searchRequests.any {
                it.visibility == CaseVisibility.ACTIVE && it.query.isBlank()
            },
        )
    }

    @Test
    fun `命例不足两个时对比保持零推断并给出行动提示`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["only"] = sampleStoredCase("only")
        }
        val viewModel = createViewModel(repository)

        viewModel.openCaseComparison()

        assertNull(viewModel.state.value.comparisonReport)
        assertTrue(viewModel.state.value.comparisonError?.contains("至少需要两个") == true)
    }

    @Test
    fun `排盘首页只读取三个已查看的活动命例`() = runTest {
        val repository = FakeCaseRepository().apply {
            repeat(5) { index ->
                stored["case-$index"] = sampleStoredCase("case-$index").copy(
                    lastViewedAt = if (index == 4) {
                        null
                    } else {
                        FixedInstant.plusSeconds(index.toLong())
                    },
                )
            }
        }

        val viewModel = createViewModel(repository)

        assertEquals(3, viewModel.state.value.recentCases.size)
        assertTrue(viewModel.state.value.recentCases.all { it.lastViewedAt != null })
        assertEquals(1, repository.searchRequests.size)
        assertEquals(CaseVisibility.ALL, repository.searchRequests.single().visibility)
    }

    @Test
    fun `表单错误保留在新建页且保存成功后直接进入专业细盘`() = runTest {
        val repository = FakeCaseRepository()
        val viewModel = createViewModel(repository)
        viewModel.openCreate()

        viewModel.submitCase()

        assertEquals(AppDestination.CreateCase, viewModel.state.value.destination)
        assertEquals("请填写命例别名。", viewModel.state.value.formError)

        viewModel.updateForm { validForm() }
        viewModel.submitCase()

        assertEquals(
            AppDestination.CaseDetail(repository.stored.keys.single()),
            viewModel.state.value.destination,
        )
        assertEquals(CaseDetailSection.FORTUNE, viewModel.state.value.detailSection)
        assertEquals(repository.stored.keys.single(), viewModel.state.value.detail?.id)
        assertEquals(1, viewModel.state.value.cases.size)
        assertTrue(repository.findByIdRequests.isEmpty())
        assertEquals("命例已完成排盘并保存。", viewModel.state.value.message)
    }

    @Test
    fun `保存命例在数据库写入期间立即进入正在保存的专业细盘`() = runTest {
        val saveGate = CompletableDeferred<Unit>()
        val repository = FakeCaseRepository().apply {
            beforeSave = { saveGate.await() }
        }
        val engine = RecordingEngine()
        val viewModel = createViewModel(repository, engine = engine)
        viewModel.openCreate()
        viewModel.updateForm { validForm() }

        viewModel.submitCase()

        assertEquals(
            AppDestination.CaseDetail("pending-manual-save"),
            viewModel.state.value.destination,
        )
        assertEquals(CaseDetailSection.FORTUNE, viewModel.state.value.detailSection)
        assertEquals("pending-manual-save", viewModel.state.value.detail?.id)
        assertTrue(viewModel.state.value.detailIsTransient)
        assertTrue(viewModel.state.value.detailSavePending)
        assertTrue(viewModel.state.value.saving)
        assertNull(viewModel.state.value.message)
        assertEquals(1, engine.calls)
        assertTrue(repository.stored.isEmpty())

        saveGate.complete(Unit)

        assertEquals(1, repository.stored.size)
        assertFalse(viewModel.state.value.detailIsTransient)
        assertFalse(viewModel.state.value.detailSavePending)
        assertFalse(viewModel.state.value.saving)
        assertEquals(repository.stored.keys.single(), viewModel.state.value.detail?.id)
        assertEquals(1, engine.calls)
    }

    @Test
    fun `首页可创建新分组并将后续保存命例归入该分组`() = runTest {
        val repository = FakeCaseRepository()
        val viewModel = createViewModel(repository)
        viewModel.openCreate()

        viewModel.createAndSelectCaseGroup("研究案例")

        val selectedGroupId = requireNotNull(viewModel.state.value.form.groupId)
        assertEquals("研究案例", repository.groupCatalog.single().name)
        assertEquals(selectedGroupId, repository.groupCatalog.single().id)
        viewModel.updateForm { validForm().copy(groupId = it.groupId) }
        viewModel.submitCase()

        assertEquals(
            listOf(repository.groupCatalog.single()),
            repository.stored.values.single().groups,
        )
    }

    @Test
    fun `关闭保存时开始排盘直接进入未保存专业细盘且不写库`() = runTest {
        val repository = FakeCaseRepository()
        val viewModel = createViewModel(repository)
        viewModel.openCreate()
        viewModel.updateForm { validForm().copy(alias = "") }

        viewModel.previewCase()

        assertEquals(
            AppDestination.CaseDetail("instant-manual-preview"),
            viewModel.state.value.destination,
        )
        assertEquals(CaseDetailSection.FORTUNE, viewModel.state.value.detailSection)
        assertEquals(true, viewModel.state.value.detailIsTransient)
        assertEquals("即时排盘案例", viewModel.state.value.detail?.alias)
        assertNotNull(viewModel.state.value.instantCalculation)
        assertTrue(repository.stored.isEmpty())

        viewModel.closeTransientDetail()
        viewModel.updateForm { it.copy(minute = "38") }

        assertNull(viewModel.state.value.instantCalculation)
    }

    @Test
    fun `夏令时重叠在表单展示候选并于选择后保存`() = runTest {
        val repository = FakeCaseRepository()
        val engine = BaziEngine { input, _ ->
            val selected = input.resolvedUtcOffsetSeconds
                ?: throw TimeZoneChoiceRequiredException(
                    "America/New_York",
                    listOf(-14_400, -18_000),
                    "tzdb:test",
                )
            calculationResult(
                input.copy(
                    resolvedUtcOffsetSeconds = selected,
                    timeZoneDataVersion = "tzdb:test",
                ),
            )
        }
        val viewModel = createViewModel(repository, engine = engine)
        viewModel.openCreate()
        viewModel.updateForm {
            validForm().copy(
                year = "2024",
                month = "11",
                day = "3",
                hour = "1",
                minute = "30",
                timeZoneId = "America/New_York",
            )
        }

        viewModel.submitCase()

        assertEquals(
            listOf(-14_400, -18_000),
            viewModel.state.value.form.availableUtcOffsetSeconds,
        )
        assertTrue(viewModel.state.value.formError.orEmpty().contains("出现两次"))
        assertTrue(repository.stored.isEmpty())

        viewModel.updateForm {
            it.copy(resolvedUtcOffsetSeconds = -18_000)
        }
        viewModel.submitCase()

        assertTrue(viewModel.state.value.destination is AppDestination.CaseDetail)
        assertEquals(CaseDetailSection.FORTUNE, viewModel.state.value.detailSection)
        assertEquals(-18_000, repository.stored.values.single().birthInput.resolvedUtcOffsetSeconds)
        assertEquals("tzdb:test", repository.stored.values.single().birthInput.timeZoneDataVersion)
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
        assertEquals(listOf("case-detail"), repository.findByIdRequests)
        assertEquals(FixedInstant, viewModel.state.value.detail?.lastViewedAt)
    }

    @Test
    fun `再次打开同一命例会保留已有详情且后台刷新失败不清空页面`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-retained"] = sampleStoredCase("case-retained")
        }
        val viewModel = createViewModel(repository)

        viewModel.openDetailFromList("case-retained")
        viewModel.backToList()
        repository.readFailure = IllegalStateException("模拟后台读取失败")
        viewModel.openDetailFromList("case-retained")

        assertEquals(
            AppDestination.CaseDetail("case-retained"),
            viewModel.state.value.destination,
        )
        assertEquals("case-retained", viewModel.state.value.detail?.id)
        assertEquals(false, viewModel.state.value.detailLoading)
        assertNull(viewModel.state.value.detailError)
        assertEquals("详情刷新失败，已保留上次打开的内容。", viewModel.state.value.message)
    }

    @Test
    fun `列表点击命例默认进入专业细盘`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-professional-default"] = sampleStoredCase("case-professional-default")
        }
        val viewModel = createViewModel(repository)

        viewModel.openDetailFromList("case-professional-default")

        assertEquals(CaseDetailSection.FORTUNE, viewModel.state.value.detailSection)
        assertEquals(
            AppDestination.CaseDetail("case-professional-default"),
            viewModel.state.value.destination,
        )
        assertNotNull(viewModel.state.value.detail)
    }

    @Test
    fun `列表点击立即进入专业细盘且不等待仓储读取`() = runTest {
        val readGate = CompletableDeferred<Unit>()
        val repository = FakeCaseRepository().apply {
            stored["case-immediate"] = sampleStoredCase("case-immediate")
            beforeFindById = { id ->
                if (id == "case-immediate") readGate.await()
            }
        }
        val viewModel = createViewModel(repository)

        viewModel.openDetailFromList("case-immediate")

        assertEquals(
            AppDestination.CaseDetail("case-immediate"),
            viewModel.state.value.destination,
        )
        assertEquals(CaseDetailSection.FORTUNE, viewModel.state.value.detailSection)
        assertTrue(viewModel.state.value.detailLoading)

        readGate.complete(Unit)

        assertEquals("case-immediate", viewModel.state.value.detail?.id)
        assertFalse(viewModel.state.value.detailLoading)
    }

    @Test
    fun `快速点击多个案例只允许最后一次详情读取回写`() = runTest {
        val firstGate = CompletableDeferred<Unit>()
        val repository = FakeCaseRepository().apply {
            stored["case-first"] = sampleStoredCase("case-first")
            stored["case-last"] = sampleStoredCase("case-last")
            beforeFindById = { id ->
                if (id == "case-first") firstGate.await()
            }
        }
        val viewModel = createViewModel(repository)

        viewModel.openDetailFromList("case-first")
        viewModel.openDetailFromList("case-last")

        assertEquals(
            AppDestination.CaseDetail("case-last"),
            viewModel.state.value.destination,
        )
        assertEquals("case-last", viewModel.state.value.detail?.id)

        firstGate.complete(Unit)

        assertEquals(
            AppDestination.CaseDetail("case-last"),
            viewModel.state.value.destination,
        )
        assertEquals("case-last", viewModel.state.value.detail?.id)
    }

    @Test
    fun `专业细盘进入默认定位当前时刻，已故或百年命例定位三十六岁`() = runTest {
        val currentCase = sampleStoredCase("current")
        val historicalInput = BirthInput(
            calendarInput = BirthCalendarInput.Solar(CivilDateTime(1920, 3, 4, 5, 6, 0)),
            sexForFortuneDirection = SexForFortuneDirection.MAN,
            timePrecision = TimePrecision.EXACT_TO_SECOND,
        )
        val historicalCase = sampleStoredCase("historical").copy(
            birthInput = historicalInput,
            calculationSnapshots = sampleStoredCase("historical").calculationSnapshots.map { snapshot ->
                snapshot.copy(result = snapshot.result.copy(normalizedInput = historicalInput))
            },
        )
        val deceasedCase = sampleStoredCase("deceased").copy(
            profile = CaseProfile(health = ExplicitText.present("已故")),
        )
        val forecastCase = sampleStoredCase("forecast").copy(
            profile = CaseProfile(health = ExplicitText.present("预测2082年是去世的一个时间点")),
        )
        val repository = FakeCaseRepository().apply {
            stored[currentCase.id] = currentCase
            stored[historicalCase.id] = historicalCase
            stored[deceasedCase.id] = deceasedCase
            stored[forecastCase.id] = forecastCase
        }
        val viewModel = createViewModel(repository)

        viewModel.openDetailFromList(currentCase.id)
        assertEquals("2026-07-30", viewModel.state.value.fortuneObservationDate)
        assertEquals("08:00", viewModel.state.value.fortuneObservationTime)

        viewModel.updateFortuneObservationDate("2001-01-01")
        viewModel.selectDetailSection(CaseDetailSection.BASIC_INFO)
        viewModel.selectDetailSection(CaseDetailSection.FORTUNE)
        assertEquals("2026-07-30", viewModel.state.value.fortuneObservationDate)
        assertEquals("08:00", viewModel.state.value.fortuneObservationTime)

        viewModel.openDetailFromList(historicalCase.id)
        assertEquals("1956-03-04", viewModel.state.value.fortuneObservationDate)
        assertEquals("05:06", viewModel.state.value.fortuneObservationTime)

        viewModel.openDetailFromList(deceasedCase.id)
        assertEquals("2036-02-29", viewModel.state.value.fortuneObservationDate)
        assertEquals("10:30", viewModel.state.value.fortuneObservationTime)

        viewModel.openDetailFromList(forecastCase.id)
        assertEquals("2026-07-30", viewModel.state.value.fortuneObservationDate)
        assertEquals("08:00", viewModel.state.value.fortuneObservationTime)
    }

    @Test
    fun `编辑命例成功后返回详情并刷新修订`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-edit"] = sampleStoredCase("case-edit")
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("case-edit")
        viewModel.openEditCase()
        viewModel.updateEditForm { it.copy(alias = "合成命例已编辑", hour = "12") }

        viewModel.saveEditedCase()

        assertEquals(
            AppDestination.CaseDetail("case-edit"),
            viewModel.state.value.destination,
        )
        assertEquals("合成命例已编辑", viewModel.state.value.detail?.alias)
        assertEquals(2L, viewModel.state.value.detail?.revision)
        assertEquals(2, viewModel.state.value.detail?.calculationSnapshots?.size)
    }

    @Test
    fun `列表编辑入口保存更新原命例而不创建副本`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-edit-from-list"] = sampleStoredCase("case-edit-from-list")
        }
        val viewModel = createViewModel(repository)
        viewModel.openRecordHub()
        viewModel.openEditCase("case-edit-from-list")
        viewModel.updateEditForm {
            it.copy(alias = "列表编辑后姓名", name = "列表编辑后姓名")
        }

        viewModel.saveEditedCase()

        assertEquals(AppDestination.CaseList, viewModel.state.value.destination)
        assertEquals(setOf("case-edit-from-list"), repository.stored.keys)
        assertEquals("列表编辑后姓名", repository.stored.getValue("case-edit-from-list").alias)
        assertEquals(2L, repository.stored.getValue("case-edit-from-list").revision)
    }

    @Test
    fun `创建副本保留原命例并打开新命例详情`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-copy-source"] = sampleStoredCase("case-copy-source")
        }
        val viewModel = createViewModel(repository)
        viewModel.openRecordHub()
        viewModel.openEditCase("case-copy-source")
        viewModel.updateEditForm {
            it.copy(alias = "命例副本姓名", name = "命例副本姓名")
        }

        viewModel.createEditedCaseCopy()

        assertEquals(2, repository.stored.size)
        assertEquals("合成命例甲", repository.stored.getValue("case-copy-source").alias)
        val copy = repository.stored.values.single { it.id != "case-copy-source" }
        assertEquals("命例副本姓名", copy.alias)
        assertEquals(AppDestination.CaseDetail(copy.id), viewModel.state.value.destination)
    }

    @Test
    fun `新增出生时间候选不改采用盘且可在详情明确切换`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-time-candidates"] = sampleStoredCase("case-time-candidates")
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("case-time-candidates")
        viewModel.openBirthTimeCandidate()
        viewModel.updateCandidateLabel("家人回忆 12 点")
        viewModel.updateCandidateForm {
            it.copy(
                hour = "12",
                timePrecision = TimePrecision.APPROXIMATE,
                timeSourceType = TimeSourceType.FAMILY_REPORTED,
            )
        }

        viewModel.saveBirthTimeCandidate()

        assertEquals(
            AppDestination.CaseDetail("case-time-candidates"),
            viewModel.state.value.destination,
        )
        val afterAdd = checkNotNull(viewModel.state.value.detail)
        assertEquals(2, afterAdd.birthTimeCandidates.size)
        assertEquals(1, afterAdd.birthTimeCandidates.count { it.adopted })
        assertEquals(
            10,
            (afterAdd.birthInput.calendarInput as BirthCalendarInput.Solar).dateTime.hour,
        )
        val added = afterAdd.birthTimeCandidates.single {
            it.label == "家人回忆 12 点"
        }
        assertFalse(added.adopted)

        viewModel.adoptBirthTimeCandidate(added.id)

        val afterAdopt = checkNotNull(viewModel.state.value.detail)
        assertEquals(
            AppDestination.CaseDetail("case-time-candidates"),
            viewModel.state.value.destination,
        )
        assertEquals(
            12,
            (afterAdopt.birthInput.calendarInput as BirthCalendarInput.Solar).dateTime.hour,
        )
        assertEquals(added.id, afterAdopt.birthTimeCandidates.single { it.adopted }.id)
        assertEquals(
            added.calculationSnapshotId,
            afterAdopt.calculationSnapshots.single { it.adopted }.id,
        )
    }

    @Test
    fun `农历命例进入编辑器时保留历法与闰月`() = runTest {
        val base = sampleStoredCase("case-lunar")
        val lunarInput = base.birthInput.copy(
            calendarInput = BirthCalendarInput.Lunar(
                LunarDateTime(2023, 2, 1, 10, 30, 0, isLeapMonth = true),
            ),
            timePrecision = TimePrecision.APPROXIMATE,
            timeSourceType = TimeSourceType.FAMILY_REPORTED,
            sourceNote = "家人回忆",
        )
        val lunarCase = base.copy(
            birthInput = lunarInput,
            calculationSnapshots = base.calculationSnapshots.map {
                it.copy(
                    result = it.result.copy(
                        normalizedInput = lunarInput,
                        profile = it.result.profile.copy(
                            ratHourRule = RatHourRule.LATE_RAT_SAME_DAY,
                        ),
                    ),
                )
            },
        )
        val repository = FakeCaseRepository().apply {
            stored[lunarCase.id] = lunarCase
        }
        val viewModel = createViewModel(repository)

        viewModel.openDetail(lunarCase.id)
        viewModel.openEditCase()

        assertEquals(AppDestination.EditCase(lunarCase.id), viewModel.state.value.destination)
        assertEquals(CalendarSystem.LUNAR, viewModel.state.value.editForm.calendarSystem)
        assertTrue(viewModel.state.value.editForm.isLeapMonth)
        assertEquals("2", viewModel.state.value.editForm.month)
        assertEquals(TimePrecision.APPROXIMATE, viewModel.state.value.editForm.timePrecision)
        assertEquals(
            TimeSourceType.FAMILY_REPORTED,
            viewModel.state.value.editForm.timeSourceType,
        )
        assertEquals("家人回忆", viewModel.state.value.editForm.sourceNote)
        assertEquals(
            RatHourRule.LATE_RAT_SAME_DAY,
            viewModel.state.value.editForm.ratHourRule,
        )
    }

    @Test
    fun `记录与事件保存后读取同一详情事实`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-records"] = sampleStoredCase("case-records")
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("case-records")
        viewModel.openTextRecord()
        viewModel.updateRecordDraft {
            TextRecordDraft(CaseTextRecordType.OWNER_FEEDBACK, "合成命主反馈")
        }
        viewModel.saveTextRecord(null)

        assertEquals(1, viewModel.state.value.detail?.textRecords?.size)
        assertEquals(1, viewModel.state.value.detail?.textRecordRevisions?.size)
        assertEquals(
            AppDestination.CaseDetail("case-records"),
            viewModel.state.value.destination,
        )

        viewModel.openEvent()
        viewModel.updateEventDraft {
            EventDraft(
                year = "2024",
                month = "6",
                status = "待核对",
                rawText = "合成关键事件",
                title = "合成事件标题",
                category = CaseEventCategory.EDUCATION,
            )
        }
        viewModel.saveEvent(null)

        assertEquals(1, viewModel.state.value.detail?.events?.size)
        assertEquals(1, viewModel.state.value.detail?.eventRevisions?.size)
        assertEquals("合成事件标题", viewModel.state.value.detail?.events?.single()?.title)
        assertEquals(
            CaseEventCategory.EDUCATION,
            viewModel.state.value.detail?.events?.single()?.category,
        )
        assertEquals(3L, viewModel.state.value.detail?.revision)
    }

    @Test
    fun `点评候选可编辑拒绝采用且采用后原点评不变`() = runTest {
        val commentary = masterCommentary()
        val stored = sampleStoredCase("case-commentary-candidates").copy(
            textRecords = listOf(commentary),
            textRecordRevisions = listOf(commentaryRevision(commentary, 1)),
        )
        val repository = FakeCaseRepository().apply { this.stored[stored.id] = stored }
        val viewModel = createViewModel(repository)

        viewModel.openDetail(stored.id)
        viewModel.openMasterCommentaryCandidates(commentary.id)
        val initial = requireNotNull(viewModel.state.value.commentaryCandidateSet)
        assertEquals(2, initial.candidates.size)
        val first = initial.candidates.first()
        val second = initial.candidates.last()
        viewModel.updateMasterCommentaryCandidateContent(first.id, "编辑后的事业观点")
        viewModel.updateMasterCommentaryCandidateCategory(
            first.id,
            AnalysisCategory.WEALTH,
        )
        viewModel.rejectMasterCommentaryCandidate(second.id)
        viewModel.adoptMasterCommentaryCandidate(first.id)

        assertEquals(
            AppDestination.MasterCommentaryCandidates(stored.id, commentary.id),
            viewModel.state.value.destination,
        )
        val decisions = requireNotNull(viewModel.state.value.commentaryCandidateSet).candidates
        assertEquals(MasterCommentaryCandidateStatus.ADOPTED, decisions.first().status)
        assertEquals(MasterCommentaryCandidateStatus.REJECTED, decisions.last().status)
        val refreshed = repository.stored.getValue(stored.id)
        assertEquals(commentary, refreshed.textRecords.first())
        assertEquals(
            "编辑后的事业观点",
            refreshed.textRecords.single { it.type == CaseTextRecordType.ANALYSIS }.content,
        )
        assertEquals(
            AnalysisCategory.WEALTH,
            refreshed.textRecords.single { it.type == CaseTextRecordType.ANALYSIS }
                .analysisCategory,
        )
    }

    @Test
    fun `点评在候选打开后修改会结构化拒绝旧候选`() = runTest {
        val commentary = masterCommentary()
        val stored = sampleStoredCase("case-commentary-stale").copy(
            textRecords = listOf(commentary),
            textRecordRevisions = listOf(commentaryRevision(commentary, 1)),
        )
        val repository = FakeCaseRepository().apply { this.stored[stored.id] = stored }
        val viewModel = createViewModel(repository)
        viewModel.openDetail(stored.id)
        viewModel.openMasterCommentaryCandidates(commentary.id)
        val candidateId = requireNotNull(viewModel.state.value.commentaryCandidateSet)
            .candidates
            .first()
            .id
        TextRecordUseCase(repository).save(
            stored.id,
            stored.revision,
            commentary.id,
            TextRecordDraft(
                CaseTextRecordType.MASTER_COMMENTARY,
                "事业原文已经修改。财运也需核对",
            ),
        )

        viewModel.adoptMasterCommentaryCandidate(candidateId)

        assertEquals(
            MasterCommentaryCandidateAdoptionErrorCode.SOURCE_REVISION_STALE,
            viewModel.state.value.commentaryCandidateAdoptionFailure?.code,
        )
        assertEquals(1, repository.stored.getValue(stored.id).textRecords.size)
    }

    @Test
    fun `断事笔记停止输入两秒后自动聚合保存且不会逐字写库`() = runTest {
        val stored = sampleStoredCase("case-notes-autosave")
        val repository = FakeCaseRepository().apply { this.stored[stored.id] = stored }
        val viewModel = createViewModel(repository)
        viewModel.openDetail(stored.id)

        viewModel.updateOwnerFeedback("自动保存后的命主反馈")

        assertTrue(repository.stored.getValue(stored.id).textRecords.isEmpty())
        dispatcher.scheduler.advanceTimeBy(1_999)
        dispatcher.scheduler.runCurrent()
        assertTrue(repository.stored.getValue(stored.id).textRecords.isEmpty())

        dispatcher.scheduler.advanceTimeBy(2)
        dispatcher.scheduler.runCurrent()

        assertEquals(
            "自动保存后的命主反馈",
            repository.stored.getValue(stored.id).textRecords.single().content,
        )
        assertEquals(
            viewModel.state.value.caseNotesSavedDraft,
            viewModel.state.value.caseNotesDraft,
        )
        assertFalse(viewModel.state.value.caseNotesSaving)
    }

    @Test
    fun `同修订命例预取命中并快速切换时笔记始终归属当前命例`() = runTest {
        val caseARecord = masterCommentary().copy(id = "notes-a", content = "甲命例点评")
        val caseBRecord = masterCommentary().copy(id = "notes-b", content = "乙命例点评")
        val caseA = sampleStoredCase("case-notes-a").copy(
            revision = 7,
            textRecords = listOf(caseARecord),
        )
        val caseB = sampleStoredCase("case-notes-b").copy(
            revision = 7,
            textRecords = listOf(caseBRecord),
        )
        val repository = FakeCaseRepository().apply {
            stored[caseA.id] = caseA
            stored[caseB.id] = caseB
        }
        val viewModel = createViewModel(repository)

        viewModel.prefetchCaseDetail(caseA.id)
        viewModel.prefetchCaseDetail(caseB.id)
        dispatcher.scheduler.runCurrent()

        viewModel.openDetailFromList(caseA.id)
        assertEquals(caseA.id, viewModel.state.value.caseNotesCaseId)
        assertEquals(7L, viewModel.state.value.caseNotesRevision)
        assertEquals("甲命例点评", viewModel.state.value.caseNotesDraft.masterCommentary)

        viewModel.openDetailFromList(caseB.id)
        assertEquals(caseB.id, viewModel.state.value.detail?.id)
        assertEquals(caseB.id, viewModel.state.value.caseNotesCaseId)
        assertEquals(7L, viewModel.state.value.caseNotesRevision)
        assertEquals("乙命例点评", viewModel.state.value.caseNotesDraft.masterCommentary)

        viewModel.openDetailFromList(caseA.id)
        assertEquals(caseA.id, viewModel.state.value.detail?.id)
        assertEquals(caseA.id, viewModel.state.value.caseNotesCaseId)
        assertEquals("甲命例点评", viewModel.state.value.caseNotesDraft.masterCommentary)
    }

    @Test
    fun `父修订未变但后台更新子笔记时重新读取聚合而不沿用缓存`() = runTest {
        val oldRecord = masterCommentary().copy(id = "notes-sync", content = "同步前点评")
        val stored = sampleStoredCase("case-notes-sync").copy(
            revision = 9,
            textRecords = listOf(oldRecord),
        )
        val repository = FakeCaseRepository().apply { this.stored[stored.id] = stored }
        val viewModel = createViewModel(repository)
        viewModel.prefetchCaseDetail(stored.id)
        dispatcher.scheduler.runCurrent()

        repository.stored[stored.id] = stored.copy(
            textRecords = listOf(oldRecord.copy(content = "后台同步后的点评")),
        )
        viewModel.openDetailFromList(stored.id)
        dispatcher.scheduler.runCurrent()

        assertEquals(stored.id, viewModel.state.value.caseNotesCaseId)
        assertEquals(9L, viewModel.state.value.caseNotesRevision)
        assertEquals("后台同步后的点评", viewModel.state.value.caseNotesDraft.masterCommentary)
    }

    @Test
    fun `空预取草稿遇到数据库非空时保持不可渲染并强制重读`() = runTest {
        val stored = sampleStoredCase("case-notes-empty-cache").copy(revision = 10)
        val repository = FakeCaseRepository().apply { this.stored[stored.id] = stored }
        val viewModel = createViewModel(repository)
        viewModel.prefetchCaseDetail(stored.id)
        dispatcher.scheduler.runCurrent()

        val databaseRecord = masterCommentary().copy(
            id = "notes-after-empty-cache",
            content = "数据库实际存在的点评",
        )
        repository.stored[stored.id] = stored.copy(textRecords = listOf(databaseRecord))
        val readGate = CompletableDeferred<Unit>()
        repository.beforeFindById = { caseId ->
            if (caseId == stored.id) readGate.await()
        }

        viewModel.openDetailFromList(stored.id)

        assertTrue(viewModel.state.value.caseNotesHydrating)
        assertTrue(viewModel.state.value.caseNotesDraft.masterCommentary.isEmpty())

        readGate.complete(Unit)
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.state.value.caseNotesHydrating)
        assertEquals(stored.id, viewModel.state.value.caseNotesCaseId)
        assertEquals(10L, viewModel.state.value.caseNotesRevision)
        assertEquals(
            "数据库实际存在的点评",
            viewModel.state.value.caseNotesDraft.masterCommentary,
        )
    }

    @Test
    fun `杀进程恢复详情时原子恢复笔记所有权与内容`() = runTest {
        val record = masterCommentary().copy(id = "notes-restored", content = "进程恢复点评")
        val stored = sampleStoredCase("case-notes-restored").copy(
            revision = 12,
            textRecords = listOf(record),
        )
        val repository = FakeCaseRepository().apply { this.stored[stored.id] = stored }
        val restored = createViewModel(
            repository = repository,
            restoredUiStateOverride = StageTwoUiState(
                destination = AppDestination.CaseDetail(stored.id),
                detailSection = CaseDetailSection.RECORDS,
            ),
        )
        dispatcher.scheduler.runCurrent()

        assertEquals(stored.id, restored.state.value.detail?.id)
        assertEquals(stored.id, restored.state.value.caseNotesCaseId)
        assertEquals(12L, restored.state.value.caseNotesRevision)
        assertEquals("进程恢复点评", restored.state.value.caseNotesDraft.masterCommentary)
    }

    @Test
    fun `千例目录中打开末端命例不会串用上一命例笔记`() = runTest {
        val repository = FakeCaseRepository()
        repeat(1_001) { index ->
            val record = masterCommentary().copy(
                id = "notes-scale-$index",
                content = "规模点评-$index",
            )
            val stored = sampleStoredCase("case-scale-$index").copy(
                revision = 3,
                textRecords = listOf(record),
            )
            repository.stored[stored.id] = stored
        }
        val viewModel = createViewModel(repository)

        viewModel.openDetailFromList("case-scale-0")
        viewModel.openDetailFromList("case-scale-1000")

        assertEquals("case-scale-1000", viewModel.state.value.detail?.id)
        assertEquals("case-scale-1000", viewModel.state.value.caseNotesCaseId)
        assertEquals("规模点评-1000", viewModel.state.value.caseNotesDraft.masterCommentary)
    }

    @Test
    fun `反馈主题候选可编辑拒绝采用且采用只追加正式标签`() = runTest {
        val feedback = ownerFeedback()
        val stored = sampleStoredCase("case-feedback-theme-candidates").copy(
            textRecords = listOf(feedback),
            textRecordRevisions = listOf(commentaryRevision(feedback, 1)),
        )
        val repository = FakeCaseRepository().apply { this.stored[stored.id] = stored }
        val viewModel = createViewModel(repository)

        viewModel.openDetail(stored.id)
        viewModel.openFeedbackThemeCandidates(feedback.id)
        val initial = requireNotNull(viewModel.state.value.feedbackThemeCandidateSet)
        assertEquals(2, initial.candidates.size)
        val first = initial.candidates.first()
        val second = initial.candidates.last()
        viewModel.updateFeedbackThemeCandidateTag(first.id, "事业复盘")
        viewModel.rejectFeedbackThemeCandidate(second.id)
        viewModel.adoptFeedbackThemeCandidate(first.id)

        assertEquals(
            AppDestination.FeedbackThemeCandidates(stored.id, feedback.id),
            viewModel.state.value.destination,
        )
        val decisions = requireNotNull(
            viewModel.state.value.feedbackThemeCandidateSet,
        ).candidates
        assertEquals(FeedbackThemeCandidateStatus.ADOPTED, decisions.first().status)
        assertEquals(FeedbackThemeCandidateStatus.REJECTED, decisions.last().status)
        val refreshed = repository.stored.getValue(stored.id)
        assertEquals(listOf(feedback), refreshed.textRecords)
        assertEquals(stored.textRecordRevisions, refreshed.textRecordRevisions)
        assertEquals(stored.events, refreshed.events)
        assertEquals(listOf("事业复盘"), refreshed.tags.map { it.name })
    }

    @Test
    fun `反馈在主题候选打开后修改会结构化拒绝旧候选且零写入`() = runTest {
        val feedback = ownerFeedback()
        val stored = sampleStoredCase("case-feedback-theme-stale").copy(
            textRecords = listOf(feedback),
            textRecordRevisions = listOf(commentaryRevision(feedback, 1)),
        )
        val repository = FakeCaseRepository().apply { this.stored[stored.id] = stored }
        val viewModel = createViewModel(repository)
        viewModel.openDetail(stored.id)
        viewModel.openFeedbackThemeCandidates(feedback.id)
        val candidateId = requireNotNull(viewModel.state.value.feedbackThemeCandidateSet)
            .candidates
            .first()
            .id
        TextRecordUseCase(repository).save(
            stored.id,
            stored.revision,
            feedback.id,
            TextRecordDraft(
                CaseTextRecordType.OWNER_FEEDBACK,
                "工作反馈已经修改。健康也需复查",
            ),
        )
        val afterSourceEdit = repository.stored.getValue(stored.id)

        viewModel.adoptFeedbackThemeCandidate(candidateId)

        assertEquals(
            FeedbackThemeAdoptionErrorCode.SOURCE_REVISION_STALE,
            viewModel.state.value.feedbackThemeAdoptionFailure?.code,
        )
        assertEquals(afterSourceEdit, repository.stored.getValue(stored.id))
        assertTrue(repository.stored.getValue(stored.id).tags.isEmpty())
    }

    @Test
    fun `保存冲突保留编辑输入并停留当前页`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-conflict"] = sampleStoredCase("case-conflict")
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("case-conflict")
        viewModel.openTextRecord()
        viewModel.updateRecordDraft { it.copy(content = "未保存但必须保留的合成输入") }
        repository.writeOverride = CaseWriteResult.RevisionConflict(
            "case-conflict",
            1,
            2,
        )

        viewModel.saveTextRecord(null)

        assertEquals(
            AppDestination.EditTextRecord("case-conflict", null),
            viewModel.state.value.destination,
        )
        assertEquals(
            "未保存但必须保留的合成输入",
            viewModel.state.value.recordDraft.content,
        )
        assertNotNull(viewModel.state.value.mutationError)
    }

    @Test
    fun `分组标签筛选与排序直接复用已加载目录`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-filter"] = sampleStoredCase("case-filter").copy(
                groups = listOf(CaseGroup("group-1", "家人")),
                tags = listOf(CaseTag("tag-1", "已核对")),
            )
        }
        val viewModel = createViewModel(repository)
        val catalogReadCount = repository.searchRequests.size

        viewModel.selectGroup("group-1")
        viewModel.selectTag("tag-1")
        viewModel.selectSortOrder(CaseSortOrder.LAST_VIEWED_DESC)
        viewModel.applyAdvancedFilter(
            CaseAdvancedFilter(
                ganZhi = setOf('甲', '子'),
                fourPillars = FourPillarsSearchFilter(
                    year = PillarCharacterFilter(
                        stem = '甲',
                        branch = '子',
                        stemTenGod = "比肩",
                        branchTenGod = "正印",
                    ),
                ),
                birthRegion = "北京",
                seasonalWuxingStates = setOf("木旺"),
                shenSha = setOf("天乙贵人"),
            ),
        )

        assertEquals(catalogReadCount, repository.searchRequests.size)
        assertEquals("group-1", viewModel.state.value.selectedGroupId)
        assertEquals("tag-1", viewModel.state.value.selectedTagId)
        assertEquals(CaseSortOrder.LAST_VIEWED_DESC, viewModel.state.value.sortOrder)
        assertEquals(setOf('甲', '子'), viewModel.state.value.advancedFilter.ganZhi)
        assertEquals(
            PillarCharacterFilter(
                stem = '甲',
                branch = '子',
                stemTenGod = "比肩",
                branchTenGod = "正印",
            ),
            viewModel.state.value.advancedFilter.fourPillars.year,
        )
        assertEquals("北京", viewModel.state.value.advancedFilter.birthRegion)
        assertEquals(setOf("木旺"), viewModel.state.value.advancedFilter.seasonalWuxingStates)
        assertEquals(setOf("天乙贵人"), viewModel.state.value.advancedFilter.shenSha)
    }

    @Test
    fun `用户列表与名人案例通过独立案例库类型切换`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["user-case"] = sampleStoredCase("user-case")
            stored["celebrity-case"] = sampleStoredCase("celebrity-case").copy(
                libraryType = com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType.CELEBRITY,
            )
        }
        val viewModel = createViewModel(repository)
        val catalogReadCount = repository.searchRequests.size

        viewModel.selectCaseLibrary(
            com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType.CELEBRITY,
        )

        assertEquals(
            com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType.CELEBRITY,
            viewModel.state.value.libraryType,
        )
        assertEquals(listOf("celebrity-case"), viewModel.state.value.cases.map { it.id })
        assertEquals(catalogReadCount, repository.searchRequests.size)
        assertEquals(1, viewModel.state.value.libraryCaseCounts[CaseLibraryType.USER])
        assertEquals(1, viewModel.state.value.libraryCaseCounts[CaseLibraryType.CELEBRITY])
    }

    @Test
    fun `空回收站切换不重读数据库也不显示加载`() = runTest {
        val repository = FakeCaseRepository()
        val viewModel = createViewModel(repository)
        val catalogReadCount = repository.searchRequests.size

        viewModel.selectVisibility(CaseVisibility.TRASHED)

        assertEquals(catalogReadCount, repository.searchRequests.size)
        assertFalse(viewModel.state.value.listLoading)
        assertTrue(viewModel.state.value.cases.isEmpty())
        assertEquals(0, viewModel.state.value.trashedCaseCount)
    }

    @Test
    fun `置顶操作精确更新缓存且不重新读取整库`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-1"] = sampleStoredCase("case-1")
            stored["case-2"] = sampleStoredCase("case-2").copy(alias = "另一案例")
        }
        val viewModel = createViewModel(repository)
        val catalogReadCount = repository.searchRequests.size

        viewModel.updatePinnedCases(setOf("case-2"))

        assertEquals(catalogReadCount, repository.searchRequests.size)
        assertFalse(viewModel.state.value.listLoading)
        assertTrue(viewModel.state.value.cases.first { it.id == "case-2" }.isPinned)
        assertTrue(repository.stored.getValue("case-2").isPinned)
        assertEquals("已更新 1 个命例的星标置顶状态。", viewModel.state.value.message)
    }

    @Test
    fun `活动案例批量删除精确移入回收站且列表保持可见状态`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-delete"] = sampleStoredCase("case-delete")
            stored["case-keep"] = sampleStoredCase("case-keep").copy(alias = "保留案例")
        }
        val viewModel = createViewModel(repository)
        val catalogReadCount = repository.searchRequests.size

        viewModel.batchDeleteCases(setOf("case-delete"))

        assertEquals(catalogReadCount, repository.searchRequests.size)
        assertFalse(viewModel.state.value.listLoading)
        assertEquals(listOf("case-keep"), viewModel.state.value.cases.map { it.id })
        assertEquals(1, viewModel.state.value.trashedCaseCount)
        assertNotNull(repository.stored.getValue("case-delete").deletedAt)
    }

    @Test
    fun `批量删除等待数据库提交时先更新列表且不触发整库读取`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-delete"] = sampleStoredCase("case-delete")
            stored["case-keep"] = sampleStoredCase("case-keep").copy(alias = "保留案例")
        }
        val deleteStarted = CompletableDeferred<Unit>()
        val allowDeleteToFinish = CompletableDeferred<Unit>()
        repository.beforeMoveCasesToTrash = {
            deleteStarted.complete(Unit)
            allowDeleteToFinish.await()
        }
        val viewModel = createViewModel(repository)
        val catalogReadCount = repository.searchRequests.size

        viewModel.batchDeleteCases(setOf("case-delete"))
        deleteStarted.await()

        assertEquals(listOf("case-keep"), viewModel.state.value.cases.map { it.id })
        assertEquals(1, viewModel.state.value.trashedCaseCount)
        assertTrue(viewModel.state.value.mutationSaving)
        assertEquals(catalogReadCount, repository.searchRequests.size)

        allowDeleteToFinish.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.mutationSaving)
        assertNotNull(repository.stored.getValue("case-delete").deletedAt)
        assertEquals(catalogReadCount, repository.searchRequests.size)
    }

    @Test
    fun `批量删除写库失败会恢复乐观列表且不显示全屏加载`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-delete"] = sampleStoredCase("case-delete")
            stored["case-keep"] = sampleStoredCase("case-keep").copy(alias = "保留案例")
            deleteFailure = IllegalStateException("模拟删除失败")
        }
        val viewModel = createViewModel(repository)
        val catalogReadCount = repository.searchRequests.size

        viewModel.batchDeleteCases(setOf("case-delete"))
        testScheduler.advanceUntilIdle()

        assertEquals(
            setOf("case-delete", "case-keep"),
            viewModel.state.value.cases.map { it.id }.toSet(),
        )
        assertFalse(viewModel.state.value.mutationSaving)
        assertFalse(viewModel.state.value.listLoading)
        assertEquals("删除失败，列表已恢复，请稍后重试。", viewModel.state.value.mutationError)
        assertEquals(catalogReadCount, repository.searchRequests.size)
    }

    @Test
    fun `新增名人案例入口预设名人归属并同步名人分组`() = runTest {
        val repository = FakeCaseRepository().apply {
            groupCatalog += CaseGroup(
                id = "user-group",
                name = "用户分组",
                libraryType = CaseLibraryType.USER,
            )
            groupCatalog += CaseGroup(
                id = "celebrity-group",
                name = "名人分组",
                libraryType = CaseLibraryType.CELEBRITY,
            )
        }
        val viewModel = createViewModel(repository)
        viewModel.selectGroup("user-group")

        viewModel.openCreateCelebrityCase()

        assertEquals(AppDestination.CreateCase, viewModel.state.value.destination)
        assertEquals(
            com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType.CELEBRITY,
            viewModel.state.value.form.libraryType,
        )
        assertEquals(CaseLibraryType.CELEBRITY, viewModel.state.value.libraryType)
        assertNull(viewModel.state.value.selectedGroupId)
        assertEquals(
            listOf("celebrity-group"),
            viewModel.state.value.availableGroups.map { it.id },
        )

        viewModel.openCreate()

        assertEquals(CaseLibraryType.USER, viewModel.state.value.form.libraryType)
        assertEquals(
            listOf("user-group"),
            viewModel.state.value.availableGroups.map { it.id },
        )
    }

    @Test
    fun `详情分类保存后返回详情并保留最近查看不提升修订`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-meta"] = sampleStoredCase("case-meta")
        }
        val viewModel = createViewModel(repository)

        viewModel.openDetail("case-meta")
        assertEquals(1L, viewModel.state.value.detail?.revision)
        assertNotNull(viewModel.state.value.detail?.lastViewedAt)
        viewModel.openMetadata()
        viewModel.updateMetadataDraft {
            it.copy(groupNames = "家人", isFavorite = true)
        }
        viewModel.saveMetadata()

        assertNull(viewModel.state.value.mutationError)
        assertFalse(viewModel.state.value.mutationSaving)
        assertEquals(
            AppDestination.CaseDetail("case-meta"),
            viewModel.state.value.destination,
        )
        assertEquals(2L, viewModel.state.value.detail?.revision)
        assertEquals("家人", viewModel.state.value.detail?.groups?.single()?.name)
        assertEquals(true, viewModel.state.value.detail?.isFavorite)
    }

    @Test
    fun `重复候选原位提示且明确确认后才保存`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["existing"] = sampleStoredCase("existing")
        }
        val viewModel = createViewModel(repository)
        viewModel.openCreate()
        viewModel.updateForm { validForm() }

        viewModel.submitCase()

        assertEquals(AppDestination.CreateCase, viewModel.state.value.destination)
        assertEquals(1, viewModel.state.value.duplicateCandidates.size)
        assertEquals(setOf("existing"), repository.stored.keys)

        viewModel.submitCase(allowDuplicate = true)

        assertTrue(viewModel.state.value.destination is AppDestination.CaseDetail)
        assertEquals(CaseDetailSection.FORTUNE, viewModel.state.value.detailSection)
        assertEquals(2, repository.stored.size)
    }

    @Test
    fun `命例移入回收站后主列表隐藏并可恢复`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-trash"] = sampleStoredCase("case-trash")
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("case-trash")
        viewModel.requestMoveToTrash()

        assertEquals(true, viewModel.state.value.deleteConfirmationVisible)
        viewModel.confirmMoveToTrash()

        assertEquals(AppDestination.CaseList, viewModel.state.value.destination)
        assertEquals(emptyList<String>(), viewModel.state.value.cases.map { it.id })

        viewModel.selectVisibility(CaseVisibility.TRASHED)
        assertEquals(listOf("case-trash"), viewModel.state.value.cases.map { it.id })
        viewModel.openDetail("case-trash")
        viewModel.restoreCase()

        assertEquals(CaseVisibility.ACTIVE, viewModel.state.value.visibility)
        assertEquals(listOf("case-trash"), viewModel.state.value.cases.map { it.id })
        assertNull(repository.stored.getValue("case-trash").deletedAt)
    }

    @Test
    fun `回收站批量删除会永久移除案例且不再提示移入回收站`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["trashed"] = sampleStoredCase("trashed").copy(deletedAt = FixedInstant)
            stored["active"] = sampleStoredCase("active")
        }
        val viewModel = createViewModel(repository)
        val catalogReadCount = repository.searchRequests.size

        viewModel.selectVisibility(CaseVisibility.TRASHED)
        viewModel.batchDeleteCases(setOf("trashed", "active"))

        assertEquals(catalogReadCount, repository.searchRequests.size)
        assertFalse(viewModel.state.value.listLoading)
        assertNull(repository.stored["trashed"])
        assertNotNull(repository.stored["active"])
        assertEquals(0, viewModel.state.value.trashedCaseCount)
        assertEquals("已永久删除 1 个命例。", viewModel.state.value.message)
    }

    @Test
    fun `复制命例后打开新副本详情且保留来源`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-source"] = sampleStoredCase("case-source")
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("case-source")

        viewModel.duplicateCase()

        val copied = repository.stored.values.single { it.id != "case-source" }
        assertEquals(AppDestination.CaseDetail(copied.id), viewModel.state.value.destination)
        assertEquals("case-source", viewModel.state.value.detail?.copiedFromCaseId)
    }

    @Test
    fun `单命例明文确认后导出预览并保留两份导入`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-exchange"] = sampleStoredCase("case-exchange")
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("case-exchange")
        viewModel.requestSingleCaseExport()

        assertTrue(viewModel.state.value.singleCaseExportConfirmationVisible)
        val request = viewModel.confirmSingleCaseExport()
        assertEquals("合成命例甲_南枫八字命例.json", request?.fileName)
        assertEquals(SingleCaseExportDocumentKind.JSON, request?.kind)

        val output = ByteArrayOutputStream()
        viewModel.exportCurrentCase { output }

        assertTrue(output.size() > 0)
        assertEquals(
            "单命例 JSON 已导出，图片仅保留引用信息。",
            viewModel.state.value.message,
        )
        val beforePreview = repository.stored.toMap()

        viewModel.previewSingleCase { ByteArrayInputStream(output.toByteArray()) }

        assertEquals("case-exchange", viewModel.state.value.singleCasePreview?.document?.caseData?.id)
        assertTrue(viewModel.state.value.singleCasePreview?.conflicts?.isNotEmpty() == true)
        assertEquals(beforePreview, repository.stored)
        assertNull(viewModel.state.value.singleCaseExchangeError)

        viewModel.commitSingleCaseImport(SingleCaseImportDecision.KEEP_BOTH)

        assertNull(viewModel.state.value.singleCasePreview)
        val imported = repository.stored.values.single { it.id != "case-exchange" }
        assertEquals("case-exchange", imported.copiedFromCaseId)
        assertEquals(
            "单命例已作为新命例导入，原有本地命例未被覆盖。",
            viewModel.state.value.message,
        )
    }

    @Test
    fun `单命例逐字段采用只更新明确选择的目标值`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-merge"] = sampleStoredCase("case-merge")
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("case-merge")
        val output = ByteArrayOutputStream()
        viewModel.exportCurrentCase { output }
        repository.stored["case-merge"] = repository.stored.getValue("case-merge").copy(
            alias = "本地修改别名",
            revision = 2,
        )

        viewModel.previewSingleCase { ByteArrayInputStream(output.toByteArray()) }
        viewModel.prepareSingleCaseMerge("case-merge")

        assertNotNull(viewModel.state.value.singleCaseMergePreparation)
        assertEquals(
            SingleCaseValueChoice.LOCAL,
            viewModel.state.value.singleCaseFieldChoices[SingleCaseFieldKey.ALIAS],
        )
        viewModel.chooseSingleCaseMergeField(
            SingleCaseFieldKey.ALIAS,
            SingleCaseValueChoice.IMPORTED,
        )
        viewModel.commitSingleCaseMerge()

        assertEquals("合成命例甲", repository.stored.getValue("case-merge").alias)
        assertNull(viewModel.state.value.singleCaseMergePreparation)
        assertEquals(
            "单命例差异已合并到“本地修改别名”。",
            viewModel.state.value.message,
        )
    }

    @Test
    fun `回收站命例不能进入导出或命盘图片交付`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["trashed-export"] = sampleStoredCase("trashed-export").copy(
                deletedAt = FixedInstant,
            )
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("trashed-export")

        viewModel.requestSingleCaseExport()
        viewModel.requestCaseImageDelivery(CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE)

        assertFalse(viewModel.state.value.singleCaseExportConfirmationVisible)
        assertNull(viewModel.state.value.caseImageConfirmationMode)
    }

    @Test
    fun `密码加密导出后错误密码保留重试且正确密码进入预览`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-password"] = sampleStoredCase("case-password")
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("case-password")
        viewModel.requestSingleCaseExport()
        viewModel.requestPasswordSingleCaseExport()
        val password = "合成测试密码123".toCharArray()

        val request = viewModel.confirmPasswordSingleCaseExport(password.copyOf())
        assertEquals("合成命例甲_南枫八字命例_加密.json", request?.fileName)
        assertEquals(SingleCaseExportDocumentKind.ENCRYPTED_JSON, request?.kind)
        val output = ByteArrayOutputStream()
        viewModel.exportCurrentCase { output }

        assertFalse(output.toByteArray().decodeToString().contains("合成命例甲"))
        assertEquals(
            "密码加密单命例已导出；请另行安全保存密码。",
            viewModel.state.value.message,
        )
        viewModel.previewSingleCase { ByteArrayInputStream(output.toByteArray()) }
        assertTrue(viewModel.state.value.singleCasePasswordImportVisible)

        viewModel.previewSingleCaseWithPassword("错误密码".toCharArray()) {
            ByteArrayInputStream(output.toByteArray())
        }
        assertTrue(
            viewModel.state.value.singleCasePasswordError?.contains("DECRYPTION_FAILED") == true,
        )

        viewModel.previewSingleCaseWithPassword(password.copyOf()) {
            ByteArrayInputStream(output.toByteArray())
        }
        assertFalse(viewModel.state.value.singleCasePasswordImportVisible)
        assertEquals(
            SingleCaseDocumentProtection.PASSWORD_PROTECTED,
            viewModel.state.value.singleCasePreview?.protection,
        )
    }

    @Test
    fun `密码流程提前结束时清零调用方字符数组`() = runTest {
        val viewModel = createViewModel(FakeCaseRepository())
        val password = "临时密码123".toCharArray()

        assertEquals(
            null,
            viewModel.confirmPasswordSingleCaseExport(password),
        )
        assertTrue(password.all { it == '\u0000' })

        val emptyPassword = CharArray(0)
        viewModel.previewSingleCaseWithPassword(emptyPassword) {
            error("空密码不应打开文件")
        }
        assertTrue(emptyPassword.all { it == '\u0000' })
    }

    @Test
    fun `单命例导出密码最少允许六位`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-password-length"] = sampleStoredCase("case-password-length")
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("case-password-length")
        viewModel.requestSingleCaseExport()
        viewModel.requestPasswordSingleCaseExport()

        val shortPassword = "12345".toCharArray()
        assertNull(viewModel.confirmPasswordSingleCaseExport(shortPassword))
        assertTrue(shortPassword.all { it == '\u0000' })
        assertEquals("密码至少需要 6 个字符。", viewModel.state.value.singleCasePasswordError)

        val minimumPassword = "123456".toCharArray()
        val request = viewModel.confirmPasswordSingleCaseExport(minimumPassword)
        assertTrue(minimumPassword.all { it == '\u0000' })
        assertEquals(SingleCaseExportDocumentKind.ENCRYPTED_JSON, request?.kind)
    }

    @Test
    fun `带附件命例默认选择命例包并经专用入口预览提交`() = runTest {
        val attachment = SourceAttachment(
            id = "attachment-ui",
            relativePath = "case-bundle/source/image.png",
            originalFileName = "合成图片.png",
            mimeType = "image/png",
            sha256 = "0".repeat(64),
            byteSize = 0,
            createdAt = FixedInstant,
        )
        val caseData = sampleStoredCase("case-bundle").copy(
            attachments = listOf(attachment),
        )
        val repository = FakeCaseRepository().apply {
            stored[caseData.id] = caseData
        }
        val bundle = RecordingSingleCaseBundleOperations(caseData)
        val root = Files.createTempDirectory("nanfeng-viewmodel-bundle-")
        val viewModel = createViewModel(
            repository = repository,
            bundleOperations = bundle,
            backupRoot = root,
        )
        viewModel.openDetail(caseData.id)
        viewModel.requestSingleCaseExport()

        assertFalse(viewModel.state.value.singleCaseExportIncludesAttachments)
        viewModel.chooseSingleCaseExportAttachments(true)
        val request = viewModel.confirmSingleCaseExport()
        assertEquals("合成命例甲_南枫八字命例包.nfbcase", request?.fileName)
        assertEquals(SingleCaseExportDocumentKind.BUNDLE, request?.kind)
        val output = ByteArrayOutputStream()
        viewModel.exportCurrentCase { output }
        assertTrue(bundle.exportCalled)
        assertEquals(
            "单命例附件包已导出，图片二进制和引用均已校验。",
            viewModel.state.value.message,
        )

        viewModel.previewSingleCase {
            ByteArrayInputStream(output.toByteArray())
        }
        assertTrue(bundle.previewCalled)
        assertTrue(viewModel.state.value.singleCasePreview?.containsAttachmentBinaries == true)
        viewModel.commitSingleCaseImport(
            decision = SingleCaseImportDecision.KEEP_BOTH,
            openInput = { ByteArrayInputStream(output.toByteArray()) },
        )
        assertTrue(bundle.commitImportCalled)
        assertNull(viewModel.state.value.singleCasePreview)
    }

    @Test
    fun `完整备份确认导出与只读预览均通过备份唯一入口`() = runTest {
        val backup = RecordingBackupOperations()
        val root = Files.createTempDirectory("nanfeng-viewmodel-backup-")
        val viewModel = createViewModel(
            repository = FakeCaseRepository(),
            backupOperations = backup,
            backupRoot = root,
        )

        viewModel.requestFullBackupExport()
        assertTrue(viewModel.state.value.fullBackupExportConfirmationVisible)
        assertEquals("南枫八字备份_测试.zip", viewModel.confirmFullBackupExport())
        val output = ByteArrayOutputStream()
        viewModel.exportFullBackup { output }

        assertTrue(backup.exportCalled)
        assertEquals("zip", output.toByteArray().decodeToString())
        assertTrue(viewModel.state.value.message?.contains("完整未加密备份已导出") == true)

        viewModel.previewFullBackup { ByteArrayInputStream(output.toByteArray()) }
        assertTrue(backup.previewCalled)
        assertEquals(2, viewModel.state.value.fullBackupPreview?.manifest?.counts?.cases)
        assertNull(viewModel.state.value.fullBackupError)
    }

    @Test
    fun `完整备份密码导出后错误密码可重试且正确密码进入预览`() = runTest {
        val backup = RecordingBackupOperations()
        val root = Files.createTempDirectory("nanfeng-viewmodel-encrypted-backup-")
        val viewModel = createViewModel(
            repository = FakeCaseRepository(),
            backupOperations = backup,
            backupRoot = root,
        )
        val password = "完整备份密码123".toCharArray()

        viewModel.requestFullBackupExport()
        viewModel.requestPasswordFullBackupExport()
        assertNull(viewModel.confirmPasswordFullBackupExport("12345".toCharArray()))
        assertTrue(viewModel.state.value.fullBackupPasswordError?.contains("至少需要 6") == true)
        assertEquals(
            "南枫八字备份_测试_加密.nfbak",
            viewModel.confirmPasswordFullBackupExport(password.copyOf()),
        )
        val output = ByteArrayOutputStream()
        viewModel.exportFullBackup { output }
        assertEquals("encrypted", output.toByteArray().decodeToString())
        assertTrue(viewModel.state.value.message?.contains("密码加密完整备份已导出") == true)

        viewModel.previewFullBackup { ByteArrayInputStream(output.toByteArray()) }
        assertTrue(viewModel.state.value.fullBackupPasswordImportVisible)
        viewModel.previewFullBackupWithPassword("错误密码".toCharArray()) {
            ByteArrayInputStream(output.toByteArray())
        }
        assertTrue(viewModel.state.value.fullBackupPasswordError?.contains("DECRYPTION_FAILED") == true)
        viewModel.previewFullBackupWithPassword(password.copyOf()) {
            ByteArrayInputStream(output.toByteArray())
        }
        assertFalse(viewModel.state.value.fullBackupPasswordImportVisible)
        assertTrue(viewModel.state.value.fullBackupPreview?.manifest?.encrypted == true)
    }

    @Test
    fun `完整备份逐例决策与合并范围生成零写入计划`() = runTest {
        val backup = RecordingBackupOperations()
        val root = Files.createTempDirectory("nanfeng-viewmodel-restore-plan-")
        val viewModel = createViewModel(
            repository = FakeCaseRepository(),
            backupOperations = backup,
            backupRoot = root,
        )
        viewModel.previewFullBackup { ByteArrayInputStream("zip".encodeToByteArray()) }
        viewModel.skipAllFullBackupCases()
        assertEquals(2, viewModel.state.value.fullBackupDecisions.size)
        assertTrue(
            viewModel.state.value.fullBackupDecisions.values.all {
                it.action == BackupCaseRestoreAction.SKIP
            },
        )
        viewModel.chooseFullBackupDecision("backup-new", BackupCaseRestoreAction.IMPORT_AS_IS)
        viewModel.prepareFullBackupCaseMerge("backup-conflict", "local-target")
        assertEquals("local-target", viewModel.state.value.fullBackupMergePreparation?.targetCaseId)
        viewModel.toggleFullBackupMergeModule(SingleCaseMergeModule.TEXT_RECORDS)
        viewModel.chooseFullBackupMergeField(
            SingleCaseFieldKey.ALIAS,
            SingleCaseValueChoice.IMPORTED,
        )
        viewModel.confirmFullBackupMergeDecision()
        viewModel.prepareFullBackupRestorePlan()

        assertEquals(2, viewModel.state.value.fullBackupDecisions.size)
        assertEquals(
            BackupCaseRestoreAction.MERGE,
            viewModel.state.value.fullBackupDecisions["backup-conflict"]?.action,
        )
        assertNotNull(viewModel.state.value.fullBackupRestorePlan)
        assertTrue(viewModel.state.value.message?.contains("尚未执行写入") == true)

        viewModel.requestFullBackupRestore()
        assertTrue(viewModel.state.value.fullBackupRestoreConfirmationVisible)
        assertTrue(viewModel.confirmFullBackupRestore())
        viewModel.executeFullBackupRestore {
            ByteArrayInputStream("zip".encodeToByteArray())
        }

        assertTrue(backup.executeCalled)
        assertNull(viewModel.state.value.fullBackupRestorePlan)
        assertNull(viewModel.state.value.fullBackupPreview)
        assertTrue(viewModel.state.value.message?.contains("完整备份恢复完成") == true)
    }

    @Test
    fun `岁运页按观察时刻定位当前流年并拒绝无效日期时间`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-fortune"] = sampleStoredCase("case-fortune")
        }
        val observations =
            mutableListOf<com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime>()
        val resolver = FortunePositionResolver { result, observedAt ->
            observations += observedAt
            FortunePosition(
                observedAt = observedAt,
                annualFortune = AnnualFortune(
                    name = "丙午年",
                    calendarYear = observedAt.year,
                    nominalAge = observedAt.year - 1999,
                    decadeIndex = 0,
                    decadeName = result.decadeFortunes.first().name,
                ),
                decadeFortune = result.decadeFortunes.first(),
                status = FortunePositionStatus.WITHIN_DECADE,
            )
        }
        val viewModel = createViewModel(
            repository = repository,
            fortunePositionResolver = resolver,
        )

        viewModel.openDetail("case-fortune")
        viewModel.selectDetailSection(CaseDetailSection.BASIC_CHART)

        assertEquals("丙午年", viewModel.state.value.fortunePosition?.annualFortune?.name)
        assertTrue(observations.isNotEmpty())

        viewModel.selectDetailSection(CaseDetailSection.FORTUNE)

        assertEquals("2026-07-30", viewModel.state.value.fortuneObservationDate)
        assertEquals("08:00", viewModel.state.value.fortuneObservationTime)
        assertEquals("丙午年", viewModel.state.value.fortunePosition?.annualFortune?.name)
        assertEquals(8, observations.last().hour)

        viewModel.updateFortuneObservationTime("23:15")

        assertEquals(23, observations.last().hour)
        assertEquals(15, observations.last().minute)

        viewModel.updateFortuneObservationTime("25:00")

        assertNull(viewModel.state.value.fortunePosition)
        assertEquals(
            "观察时间请按 HH:mm 填写。",
            viewModel.state.value.fortunePositionError,
        )

        viewModel.updateFortuneObservationTime("12:00")
        viewModel.updateFortuneObservationDate("2026-02-30")

        assertNull(viewModel.state.value.fortunePosition)
        assertEquals(
            "观察日期请按 YYYY-MM-DD 填写。",
            viewModel.state.value.fortunePositionError,
        )

        viewModel.updateFortuneObservationDate("2026-02-03")

        assertEquals(2026, viewModel.state.value.fortunePosition?.annualFortune?.calendarYear)
        assertNull(viewModel.state.value.fortunePositionError)
        assertEquals(2, observations.last().month)
        assertEquals(3, observations.last().day)
    }

    @Test
    fun `专业细盘反复切换复用默认定位结果且不重新计算`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-fortune-cache"] = sampleStoredCase("case-fortune-cache")
        }
        var resolveCount = 0
        val resolver = FortunePositionResolver { result, observedAt ->
            resolveCount += 1
            FortunePosition(
                observedAt = observedAt,
                annualFortune = AnnualFortune(
                    name = "丙午年",
                    calendarYear = observedAt.year,
                    nominalAge = observedAt.year - 1999,
                    decadeIndex = 0,
                    decadeName = result.decadeFortunes.first().name,
                ),
                decadeFortune = result.decadeFortunes.first(),
                status = FortunePositionStatus.WITHIN_DECADE,
            )
        }
        val viewModel = createViewModel(
            repository = repository,
            fortunePositionResolver = resolver,
        )

        viewModel.openDetailFromList("case-fortune-cache")
        assertEquals(1, resolveCount)
        assertNotNull(viewModel.state.value.fortunePosition)

        viewModel.selectDetailSection(CaseDetailSection.BASIC_INFO)
        viewModel.selectDetailSection(CaseDetailSection.FORTUNE)

        assertEquals(1, resolveCount)
        assertNotNull(viewModel.state.value.fortunePosition)
        assertEquals("2026-07-30", viewModel.state.value.fortuneObservationDate)
        assertEquals("08:00", viewModel.state.value.fortuneObservationTime)
    }

    @Test
    fun `观察日期时间一次确认只触发一次岁运定位`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-fortune-atomic"] = sampleStoredCase("case-fortune-atomic")
        }
        val observations =
            mutableListOf<com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime>()
        val resolver = FortunePositionResolver { result, observedAt ->
            observations += observedAt
            FortunePosition(
                observedAt = observedAt,
                annualFortune = AnnualFortune(
                    name = "丙午年",
                    calendarYear = observedAt.year,
                    nominalAge = observedAt.year - 1999,
                    decadeIndex = 0,
                    decadeName = result.decadeFortunes.first().name,
                ),
                decadeFortune = result.decadeFortunes.first(),
                status = FortunePositionStatus.WITHIN_DECADE,
            )
        }
        val viewModel = createViewModel(
            repository = repository,
            fortunePositionResolver = resolver,
        )
        viewModel.openDetailFromList("case-fortune-atomic")
        val before = observations.size

        viewModel.updateFortuneObservation("2026-02-03", "23:15")

        assertEquals(before + 1, observations.size)
        assertEquals(23, observations.last().hour)
        assertEquals(15, observations.last().minute)
    }

    @Test
    fun `四柱反查表单通过公开接口查询并保留解释证据`() = runTest {
        val recordedQueries =
            mutableListOf<com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupQuery>()
        val lookup = FourPillarsLookup { query ->
            recordedQueries += query
            FourPillarsLookupResult.Completed(
                query = query,
                candidates = listOf(
                    FourPillarsLookupCandidate(
                        civilDateTime =
                            com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime(
                                1949,
                                10,
                                1,
                                16,
                                0,
                                0,
                            ),
                        timeZoneId = "Asia/Shanghai",
                        resolvedUtcOffsetSeconds = 28_800,
                        timeZoneDataVersion = "tzdb:test",
                        instant = Instant.parse("1949-10-01T08:00:00Z"),
                        fourPillars =
                            com.nanzhufeng.nanfengbazi.domain.model.FourPillars(
                                "己丑",
                                "癸酉",
                                "甲子",
                                "壬申",
                            ),
                        ratHourRule = RatHourRule.TYME_DEFAULT,
                        engineVersion = "1.5.1",
                        ruleVersion = "stage0-v1",
                    ),
                ),
                evidence = FourPillarsLookupEvidence(
                    engineName = "Tyme4j",
                    engineVersion = "1.5.1",
                    ruleVersion = "stage0-v1",
                    lookupMethod = "test",
                ),
            )
        }
        val viewModel = createViewModel(
            repository = FakeCaseRepository(),
            fourPillarsLookup = lookup,
        )

        viewModel.openCreate()
        viewModel.confirmFourPillarsLookup(
            FourPillarsLookupSelection(
                pillars = listOf("己丑", "癸酉", "甲子", "壬申"),
                startYear = 1949,
                endYear = 1949,
            ),
        )

        assertEquals(AppDestination.FourPillarsLookup, viewModel.state.value.destination)
        assertEquals(1, recordedQueries.size)
        assertEquals(1, viewModel.state.value.fourPillarsLookupCandidates.size)
        assertEquals("Tyme4j", viewModel.state.value.fourPillarsLookupEvidence?.engineName)
        assertNull(viewModel.state.value.fourPillarsLookupError)

        viewModel.useFourPillarsLookupCandidate(
            viewModel.state.value.fourPillarsLookupCandidates.single(),
        )

        assertEquals(AppDestination.CreateCase, viewModel.state.value.destination)
        assertEquals("1949", viewModel.state.value.form.year)
        assertEquals("10", viewModel.state.value.form.month)
        assertEquals("1", viewModel.state.value.form.day)
        assertEquals("16", viewModel.state.value.form.hour)
        assertEquals("0", viewModel.state.value.form.minute)
        assertEquals("Asia/Shanghai", viewModel.state.value.form.timeZoneId)
        assertEquals(28_800, viewModel.state.value.form.resolvedUtcOffsetSeconds)
        assertEquals(TimePrecision.DOUBLE_HOUR_ONLY, viewModel.state.value.form.timePrecision)
        assertEquals("", viewModel.state.value.form.locationName)
    }

    @Test
    fun `四柱反查年份输入错误不会调用引擎且原表单保留`() = runTest {
        var called = false
        val viewModel = createViewModel(
            repository = FakeCaseRepository(),
            fourPillarsLookup = FourPillarsLookup {
                called = true
                error("不应调用")
            },
        )
        viewModel.openCreate()
        viewModel.openFourPillarsLookup()
        viewModel.updateFourPillarsLookupForm {
            it.copy(
                yearPillar = "甲子",
                monthPillar = "丙寅",
                dayPillar = "甲子",
                hourPillar = "甲子",
                startYear = "十九四九",
            )
        }

        viewModel.searchFourPillars()

        assertFalse(called)
        assertEquals("十九四九", viewModel.state.value.fourPillarsLookupForm.startYear)
        assertEquals(
            "起始年份必须是整数。",
            viewModel.state.value.fourPillarsLookupError,
        )
    }

    @Test
    fun `客观摘要读取当前采用快照并复制同一文本且保持详情返回链`() = runTest {
        val repository = FakeCaseRepository()
        val stored = sampleStoredCase("case-objective-summary")
        repository.stored[stored.id] = stored
        val viewModel = createViewModel(repository)
        viewModel.openDetail(stored.id)

        viewModel.openObjectiveSummary()

        val summary = requireNotNull(viewModel.state.value.objectiveSummary)
        assertEquals(
            AppDestination.CaseObjectiveSummary(stored.id),
            viewModel.state.value.destination,
        )
        var copiedText: String? = null
        viewModel.copyObjectiveSummary {
            copiedText = it
            true
        }
        assertEquals(summary.copyText, copiedText)
        assertTrue(viewModel.state.value.objectiveSummaryCopied)
        assertNull(viewModel.state.value.objectiveSummaryFailure)

        viewModel.navigateBack()
        assertEquals(
            AppDestination.CaseDetail(stored.id),
            viewModel.state.value.destination,
        )
        assertEquals(stored.id, viewModel.state.value.detail?.id)
    }

    @Test
    fun `剪贴板不可用和写入异常返回不同结构化失败且摘要保留`() = runTest {
        val repository = FakeCaseRepository()
        val stored = sampleStoredCase("case-objective-copy-error")
        repository.stored[stored.id] = stored
        val viewModel = createViewModel(repository)
        viewModel.openDetail(stored.id)
        viewModel.openObjectiveSummary()
        val summary = requireNotNull(viewModel.state.value.objectiveSummary)

        viewModel.copyObjectiveSummary { false }
        assertEquals(
            com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryErrorCode
                .CLIPBOARD_UNAVAILABLE,
            viewModel.state.value.objectiveSummaryFailure?.code,
        )
        assertEquals(summary, viewModel.state.value.objectiveSummary)

        viewModel.copyObjectiveSummary { error("synthetic clipboard failure") }
        assertEquals(
            com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryErrorCode.COPY_FAILED,
            viewModel.state.value.objectiveSummaryFailure?.code,
        )
        assertEquals(summary, viewModel.state.value.objectiveSummary)
        assertFalse(viewModel.state.value.objectiveSummaryCopied)
    }

    @Test
    fun `外部分析桥接默认脱敏并经两次主动确认保存带来源分析`() = runTest {
        val repository = FakeCaseRepository()
        val stored = sampleStoredCase("case-external-analysis")
        repository.stored[stored.id] = stored
        val viewModel = createViewModel(repository)
        viewModel.openDetail(stored.id)

        viewModel.openExternalAnalysisBridge()

        val payload = requireNotNull(viewModel.state.value.externalAnalysisPayload)
        assertEquals(
            AppDestination.ExternalAnalysisBridge(stored.id),
            viewModel.state.value.destination,
        )
        assertTrue(viewModel.state.value.externalAnalysisDraft.redactionEnabled)
        assertFalse(payload.copyText.contains(stored.alias))
        assertTrue(payload.copyText.contains("App 不联网、不自动发送"))

        var copied: String? = null
        viewModel.copyExternalAnalysisPayload {
            copied = it
            true
        }
        assertNull(copied)
        assertEquals(
            com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisBridgeErrorCode
                .CONFIRMATION_REQUIRED,
            viewModel.state.value.externalAnalysisFailure?.code,
        )

        viewModel.setExternalAnalysisExportConfirmed(true)
        viewModel.copyExternalAnalysisPayload {
            copied = it
            true
        }
        assertEquals(payload.copyText, copied)
        assertTrue(viewModel.state.value.externalAnalysisCopied)

        viewModel.updateExternalAnalysisProvider("合成外部服务")
        viewModel.updateExternalAnalysisModel("离线验收模型")
        viewModel.updateExternalAnalysisResult("这是一段仅用于测试的外部分析结果。")
        viewModel.saveExternalAnalysisResult()
        assertEquals(
            com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisBridgeErrorCode
                .CONFIRMATION_REQUIRED,
            viewModel.state.value.externalAnalysisFailure?.code,
        )

        viewModel.setExternalAnalysisImportConfirmed(true)
        viewModel.saveExternalAnalysisResult()

        assertEquals(AppDestination.CaseDetail(stored.id), viewModel.state.value.destination)
        assertEquals(CaseDetailSection.RECORDS, viewModel.state.value.detailSection)
        val saved = repository.stored.getValue(stored.id).textRecords.single()
        assertEquals(CaseTextRecordType.ANALYSIS, saved.type)
        assertTrue(saved.content.contains("来源：合成外部服务"))
        assertTrue(saved.content.contains("不是南枫八字本机算法真值"))
        assertTrue(saved.content.endsWith("这是一段仅用于测试的外部分析结果。"))
    }

    @Test
    fun `外部分析字段和脱敏变化重建材料并撤销旧确认`() = runTest {
        val repository = FakeCaseRepository()
        val stored = sampleStoredCase("case-external-selection")
        repository.stored[stored.id] = stored
        val viewModel = createViewModel(repository)
        viewModel.openDetail(stored.id)
        viewModel.openExternalAnalysisBridge()
        viewModel.setExternalAnalysisExportConfirmed(true)
        viewModel.setExternalAnalysisGroupSelected(
            com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisFieldGroup.FORTUNE_FACTS,
            false,
        )

        assertFalse(viewModel.state.value.externalAnalysisDraft.exportConfirmed)
        assertFalse(
            requireNotNull(viewModel.state.value.externalAnalysisPayload)
                .copyText.contains("【起运与大运】"),
        )

        viewModel.setExternalAnalysisRedaction(false)
        assertFalse(viewModel.state.value.externalAnalysisDraft.redactionEnabled)
        assertTrue(
            requireNotNull(viewModel.state.value.externalAnalysisPayload)
                .copyText.contains(stored.alias),
        )
    }

    @Test
    fun `图片导出与分享只接收真实页面快照且不调用字段渲染器`() = runTest {
        val repository = FakeCaseRepository()
        val stored = sampleStoredCase("case-image")
        repository.stored[stored.id] = stored
        val viewModel = createViewModel(repository)
        viewModel.openDetail(stored.id)

        var exportFileName: String? = null
        viewModel.requestCaseImageDelivery(
            com.nanzhufeng.nanfengbazi.domain.CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE,
        )
        val savedCapture = capturedPageImage(byteArrayOf(1, 2, 3, 4))
        viewModel.confirmCaseImageDelivery { mode, facts, name ->
            viewModel.completeCaseDetailPageCapture(
                mode,
                facts,
                name.first(),
                savedCapture,
            ) { _, completedName -> exportFileName = completedName }
        }
        val exported = ByteArrayOutputStream()
        viewModel.exportPreparedCaseImage(openOutput = { exported })

        var sharePrepared = false
        viewModel.requestCaseImageDelivery(
            com.nanzhufeng.nanfengbazi.domain.CaseImageDeliveryMode.SHARE_LONG_IMAGE,
        )
        val sharedCapture = capturedPageImage(byteArrayOf(5, 6, 7, 8))
        viewModel.confirmCaseImageDelivery { mode, facts, name ->
            viewModel.completeCaseDetailPageCapture(
                mode,
                facts,
                name.first(),
                sharedCapture,
            ) { _, _ -> sharePrepared = true }
        }
        val shared = ByteArrayOutputStream()
        var shareFileReady = false
        viewModel.copyPreparedCaseImageForShare(
            openOutput = { shared },
            onReady = { shareFileReady = true },
        )

        assertTrue(exportFileName?.endsWith("_南枫八字命盘.png") == true)
        assertTrue(sharePrepared)
        assertTrue(shareFileReady)
        assertArrayEquals(savedCapture.bytes, exported.toByteArray())
        assertArrayEquals(sharedCapture.bytes, shared.toByteArray())
        assertEquals(
            AppDestination.CaseDetail(stored.id),
            viewModel.state.value.destination,
        )
    }

    @Test
    fun `系统输出不可用返回结构化失败且详情不丢失`() = runTest {
        val repository = FakeCaseRepository()
        val stored = sampleStoredCase("case-image-output")
        repository.stored[stored.id] = stored
        val viewModel = createViewModel(repository)
        viewModel.openDetail(stored.id)
        viewModel.requestCaseImageDelivery(
            com.nanzhufeng.nanfengbazi.domain.CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE,
        )
        viewModel.confirmCaseImageDelivery { mode, facts, name ->
            viewModel.completeCaseDetailPageCapture(
                mode,
                facts,
                name.first(),
                capturedPageImage(byteArrayOf(9, 10, 11)),
            ) { _, _ -> }
        }

        viewModel.exportPreparedCaseImage(openOutput = { null })

        assertEquals(
            com.nanzhufeng.nanfengbazi.domain.CaseImageExportErrorCode.OUTPUT_UNAVAILABLE,
            viewModel.state.value.caseImageLastResultCode,
        )
        assertEquals(
            AppDestination.CaseDetail(stored.id),
            viewModel.state.value.destination,
        )
    }

    @Test
    fun `取消系统保存保留已生成图片与详情`() = runTest {
        val repository = FakeCaseRepository()
        val stored = sampleStoredCase("case-image-cancel")
        repository.stored[stored.id] = stored
        val viewModel = createViewModel(repository)
        viewModel.openDetail(stored.id)
        viewModel.requestCaseImageDelivery(
            com.nanzhufeng.nanfengbazi.domain.CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE,
        )
        viewModel.confirmCaseImageDelivery { mode, facts, name ->
            viewModel.completeCaseDetailPageCapture(
                mode,
                facts,
                name.first(),
                capturedPageImage(byteArrayOf(12, 13, 14)),
            ) { _, _ -> }
        }

        viewModel.cancelPreparedCaseImageDelivery(
            com.nanzhufeng.nanfengbazi.domain.CaseImageDeliveryMode.SAVE_TO_SYSTEM_FILE,
        )
        viewModel.requestCaseImageDelivery(
            com.nanzhufeng.nanfengbazi.domain.CaseImageDeliveryMode.SHARE_LONG_IMAGE,
        )
        viewModel.confirmCaseImageDelivery { mode, facts, name ->
            viewModel.completeCaseDetailPageCapture(
                mode,
                facts,
                name.first(),
                capturedPageImage(byteArrayOf(15, 16, 17)),
            ) { _, _ -> }
        }

        assertEquals(
            AppDestination.CaseDetail(stored.id),
            viewModel.state.value.destination,
        )
    }

    @Test
    fun `无分享目标和分享取消均返回稳定结果码且不离开详情`() = runTest {
        val repository = FakeCaseRepository()
        val stored = sampleStoredCase("case-image-share-errors")
        repository.stored[stored.id] = stored
        val viewModel = createViewModel(repository)
        viewModel.openDetail(stored.id)

        viewModel.reportNoCaseImageShareTarget()
        assertEquals(
            com.nanzhufeng.nanfengbazi.domain.CaseImageExportErrorCode.NO_SHARE_TARGET,
            viewModel.state.value.caseImageLastResultCode,
        )
        // A late callback from the Android chooser must not overwrite a real failure.
        viewModel.completeCaseImageShare(cancelled = true)
        assertEquals(
            com.nanzhufeng.nanfengbazi.domain.CaseImageExportErrorCode.NO_SHARE_TARGET,
            viewModel.state.value.caseImageLastResultCode,
        )
        viewModel.dismissCaseImageError()
        viewModel.completeCaseImageShare(cancelled = true)

        assertEquals(
            com.nanzhufeng.nanfengbazi.domain.CaseImageExportErrorCode.USER_CANCELLED,
            viewModel.state.value.caseImageLastResultCode,
        )
        assertEquals(
            AppDestination.CaseDetail(stored.id),
            viewModel.state.value.destination,
        )
    }

    private fun createViewModel(
        repository: FakeCaseRepository,
        caseCatalogStore: CaseCatalogStore = RepositoryCaseCatalogStore(repository, dispatcher),
        bundleOperations: SingleCaseBundleOperations? = null,
        backupOperations: CaseBackupOperations? = null,
        backupRoot: Path? = null,
        engine: BaziEngine = RecordingEngine(),
        fortunePositionResolver: FortunePositionResolver? = null,
        fourPillarsLookup: FourPillarsLookup? = null,
        almanacReader: AlmanacReader? = null,
        calculationPreferenceStore: CalculationPreferenceStore =
            InMemoryCalculationPreferenceStore(),
        savedStateHandle: androidx.lifecycle.SavedStateHandle = androidx.lifecycle.SavedStateHandle(),
        restoredUiStateOverride: StageTwoUiState? = null,
    ): StageTwoViewModel {
        val fixedClock = Clock.fixed(FixedInstant, ZoneOffset.UTC)
        val ids = generateSequence(1) { it + 1 }
            .map { "generated-$it" }
            .iterator()
        return StageTwoViewModel(
            caseRepository = repository,
            caseCatalogStore = caseCatalogStore,
            createCase = CreateCaseUseCase(
                baziEngine = engine,
                caseRepository = repository,
                clock = fixedClock,
                idGenerator = IdGenerator { ids.next() },
            ),
            editCase = EditCaseUseCase(
                baziEngine = engine,
                caseRepository = repository,
                clock = fixedClock,
                idGenerator = IdGenerator { ids.next() },
            ),
            birthTimeCandidates = BirthTimeCandidateUseCase(
                baziEngine = engine,
                caseRepository = repository,
                clock = fixedClock,
                idGenerator = IdGenerator { ids.next() },
            ),
            caseMetadata = CaseMetadataUseCase(
                caseRepository = repository,
                clock = fixedClock,
                idGenerator = IdGenerator { ids.next() },
            ),
            textRecords = TextRecordUseCase(
                caseRepository = repository,
                clock = fixedClock,
                idGenerator = IdGenerator { ids.next() },
            ),
            caseEvents = CaseEventUseCase(
                caseRepository = repository,
                clock = fixedClock,
                idGenerator = IdGenerator { ids.next() },
            ),
            caseLifecycle = CaseLifecycleUseCase(
                caseRepository = repository,
                clock = fixedClock,
                idGenerator = IdGenerator { ids.next() },
            ),
            clock = fixedClock,
            observationClock = fixedClock,
            fortunePositionResolver = fortunePositionResolver,
            fourPillarsLookup = fourPillarsLookup,
            almanacReader = almanacReader,
            singleCaseExchange = SingleCaseExchangeService(
                repository = repository,
                clock = fixedClock,
                idGenerator = { ids.next() },
                passwordKdfIterations = 100_000,
            ),
            singleCaseBundleService = bundleOperations,
            caseBackupService = backupOperations,
            backupAttachmentRoot = backupRoot?.resolve("attachments"),
            backupWorkRoot = backupRoot?.resolve("work"),
            calculationPreferenceStore = calculationPreferenceStore,
            ioDispatcher = dispatcher,
            fortuneCalculationDispatcher = dispatcher,
            searchDebounceMillis = 0L,
            savedStateHandle = savedStateHandle,
            restoredUiStateOverride = restoredUiStateOverride,
        )
    }

    private fun masterCommentary() = CaseTextRecord(
        id = "commentary-1",
        type = CaseTextRecordType.MASTER_COMMENTARY,
        content = "事业需要核对。财运也需核对",
        createdAt = FixedInstant,
        updatedAt = FixedInstant,
    )

    private class GateableAlmanacReader(
        private val delegate: AlmanacReader = TymeAlmanacReader(),
    ) : AlmanacReader {
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun loadMonth(query: AlmanacMonthQuery): AlmanacResult {
            gate?.await()
            return delegate.loadMonth(query)
        }
    }

    private fun ownerFeedback() = CaseTextRecord(
        id = "feedback-1",
        type = CaseTextRecordType.OWNER_FEEDBACK,
        content = "工作发生变化。健康需要复查",
        createdAt = FixedInstant,
        updatedAt = FixedInstant,
    )

    private fun commentaryRevision(
        commentary: CaseTextRecord,
        version: Int,
    ) = CaseTextRecordRevision(
        id = "${commentary.id}:revision:$version",
        recordId = commentary.id,
        version = version,
        changeType = RecordChangeType.CREATED,
        snapshot = commentary,
        changedAt = FixedInstant,
    )

    private fun capturedPageImage(bytes: ByteArray) = CapturedCaseDetailLongImage(
        bytes = bytes,
        widthPixels = 1080,
        heightPixels = 3200,
        sha256 = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) },
    )

    private class RecordingSingleCaseBundleOperations(
        private val caseData: BaziCase,
    ) : SingleCaseBundleOperations {
        var exportCalled = false
        var previewCalled = false
        var commitImportCalled = false

        override suspend fun export(
            caseId: String,
            output: OutputStream,
            attachmentRoot: Path,
            appVersion: String,
            protection: SingleCaseProtection,
        ): SingleCaseExportResult {
            exportCalled = true
            output.write("PK-bundle".encodeToByteArray())
            return SingleCaseExportResult.Success(
                suggestedFileName = suggestedFileName(caseData),
                byteSize = 9,
                sha256 = "1".repeat(64),
            )
        }

        override fun suggestedFileName(case: BaziCase): String =
            "${case.alias}_南枫八字命例包.nfbcase"

        override fun suggestedEncryptedFileName(case: BaziCase): String =
            "${case.alias}_南枫八字命例包_加密.nfbcase"

        override suspend fun preview(
            input: InputStream,
            workRoot: Path,
            password: CharArray?,
        ): SingleCasePreviewResult {
            previewCalled = true
            input.readBytes()
            return SingleCasePreviewResult.Success(
                SingleCasePreview(
                    document = SingleCaseDocument(
                        formatVersion = 1,
                        appVersion = "0.3.0-test",
                        databaseSchemaVersion = 5,
                        exportedAt = FixedInstant.toString(),
                        attachmentMode = SingleCaseAttachmentMode.BUNDLED_BINARIES,
                        payloadSha256 = "0".repeat(64),
                        caseData = caseData,
                    ),
                    counts = SingleCaseCounts(
                        calculationSnapshots = caseData.calculationSnapshots.size,
                        textRecords = caseData.textRecords.size,
                        textRecordRevisions = caseData.textRecordRevisions.size,
                        events = caseData.events.size,
                        eventRevisions = caseData.eventRevisions.size,
                        attachmentReferences = caseData.attachments.size,
                        fieldEvidence = caseData.fieldEvidence.size,
                    ),
                    conflicts = emptyList(),
                    containsAttachmentBinaries = true,
                    bundleManifestSha256 = "2".repeat(64),
                ),
            )
        }

        override suspend fun commitImport(
            preview: SingleCasePreview,
            decision: SingleCaseImportDecision,
            input: InputStream,
            workRoot: Path,
            attachmentRoot: Path,
            password: CharArray?,
        ): SingleCaseImportResult {
            commitImportCalled = true
            input.readBytes()
            return SingleCaseImportResult.Imported("imported-bundle", 1)
        }

        override suspend fun commitMerge(
            plan: SingleCaseMergePlan,
            input: InputStream,
            workRoot: Path,
            attachmentRoot: Path,
            password: CharArray?,
        ): SingleCaseImportResult {
            error("本测试不执行命例包合并")
        }
    }

    private class RecordingBackupOperations : CaseBackupOperations {
        var exportCalled = false
        var previewCalled = false
        var executeCalled = false
        private var encryptedExport = false

        override suspend fun export(
            output: OutputStream,
            attachmentRoot: Path,
            appVersion: String,
            protection: BackupProtection,
        ): BackupExportResult {
            exportCalled = true
            encryptedExport = protection is BackupProtection.PasswordProtected
            output.write(if (encryptedExport) {
                "encrypted".encodeToByteArray()
            } else {
                "zip".encodeToByteArray()
            })
            return BackupExportResult.Success(
                counts = counts(),
                fileCount = 10,
            )
        }

        override fun suggestedFileName(): String = "南枫八字备份_测试.zip"

        override fun suggestedEncryptedFileName(): String = "南枫八字备份_测试_加密.nfbak"

        override suspend fun preview(
            input: InputStream,
            workRoot: Path,
            password: CharArray?,
        ): BackupPreviewResult {
            previewCalled = true
            input.readBytes()
            if (encryptedExport && password == null) {
                return BackupPreviewResult.Rejected(
                    code = "PASSWORD_REQUIRED",
                    message = "需要密码",
                )
            }
            if (encryptedExport && password?.concatToString() != "完整备份密码123") {
                return BackupPreviewResult.Rejected(
                    code = "DECRYPTION_FAILED",
                    message = "解密失败",
                )
            }
            return BackupPreviewResult.Success(
                RestorePreview(
                    manifest = BackupManifest(
                        formatVersion = 1,
                        appVersion = "0.3.0-test",
                        databaseSchemaVersion = 5,
                        createdAt = FixedInstant.toString(),
                        encrypted = encryptedExport,
                        encryptionParametersVersion = if (encryptedExport) 1 else null,
                        engineVersions = emptyList(),
                        ruleVersions = emptyList(),
                        counts = counts(),
                        files = emptyList(),
                    ),
                    sourceFileCount = 10,
                    cases = backupCases(),
                ),
            )
        }

        override suspend fun prepareRestorePlan(
            preview: RestorePreview,
            decisions: List<BackupCaseRestoreDecision>,
        ): BackupRestorePlanResult = if (decisions.size == preview.cases.size) {
            BackupRestorePlanResult.Success(BackupRestorePlan(preview, decisions))
        } else {
            BackupRestorePlanResult.Rejected("DECISIONS_INCOMPLETE", "决策不完整")
        }

        override suspend fun prepareCaseMerge(
            preview: RestorePreview,
            sourceCaseId: String,
            targetCaseId: String,
        ): BackupCaseMergePreparationResult = BackupCaseMergePreparationResult.Success(
            BackupCaseMergePreparation(
                sourceCaseId = sourceCaseId,
                targetCaseId = targetCaseId,
                targetAlias = "本地目标",
                targetRevision = 1,
                analysis = CaseMergeAnalysis(
                    fieldDifferences = listOf(
                        SingleCaseFieldDifference(
                            SingleCaseFieldKey.ALIAS,
                            "别名",
                            "本地目标",
                            "来源冲突",
                        ),
                    ),
                    addableCounts = SingleCaseCounts(
                        calculationSnapshots = 0,
                        textRecords = 1,
                        textRecordRevisions = 0,
                        events = 0,
                        eventRevisions = 0,
                        attachmentReferences = 0,
                        fieldEvidence = 0,
                    ),
                ),
            ),
        )

        override suspend fun executeRestorePlan(
            plan: BackupRestorePlan,
            input: InputStream,
            workRoot: Path,
            attachmentRoot: Path,
            password: CharArray?,
        ): BackupRestoreExecutionResult {
            executeCalled = true
            input.readBytes()
            return BackupRestoreExecutionResult.Success(
                BackupRestoreExecutionSummary(
                    importedCases = 1,
                    keptBothCases = 0,
                    mergedCases = 1,
                    skippedCases = 0,
                    restoredAttachments = 0,
                ),
            )
        }

        private fun backupCases() = listOf(
            BackupCaseRestorePreview(sampleStoredCase("backup-new"), emptyList()),
            BackupCaseRestorePreview(
                sourceCase = sampleStoredCase("backup-conflict").copy(alias = "来源冲突"),
                conflicts = listOf(
                    BackupCaseConflictCandidate(
                        localCaseId = "local-target",
                        localAlias = "本地目标",
                        localRevision = 1,
                        isTrashed = false,
                        reasons = setOf(BackupCaseConflictReason.SAME_BIRTH_INPUT),
                    ),
                ),
            ),
        )

        private fun counts() = BackupCounts(
            cases = 2,
            snapshots = 2,
            textRecords = 3,
            events = 1,
            attachments = 0,
        )
    }
}
