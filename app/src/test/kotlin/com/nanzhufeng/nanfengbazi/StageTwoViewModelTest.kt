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
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.ZoneOffset
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.TimeZoneChoiceRequiredException
import com.nanzhufeng.nanfengbazi.domain.CaseSortOrder
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.LunarDateTime
import com.nanzhufeng.nanfengbazi.domain.model.SourceAttachment
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

        assertEquals(AppDestination.CaseList, viewModel.state.value.destination)
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
    fun `农历命例进入编辑器时保留历法与闰月`() = runTest {
        val base = sampleStoredCase("case-lunar")
        val lunarInput = base.birthInput.copy(
            calendarInput = BirthCalendarInput.Lunar(
                LunarDateTime(2023, 2, 1, 10, 30, 0, isLeapMonth = true),
            ),
        )
        val lunarCase = base.copy(
            birthInput = lunarInput,
            calculationSnapshots = base.calculationSnapshots.map {
                it.copy(result = it.result.copy(normalizedInput = lunarInput))
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
    fun `分组标签筛选与排序通过结构化仓储请求刷新`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-filter"] = sampleStoredCase("case-filter").copy(
                groups = listOf(CaseGroup("group-1", "家人")),
                tags = listOf(CaseTag("tag-1", "已核对")),
            )
        }
        val viewModel = createViewModel(repository)

        viewModel.selectGroup("group-1")
        viewModel.selectTag("tag-1")
        viewModel.selectSortOrder(CaseSortOrder.LAST_VIEWED_DESC)

        val request = repository.searchRequests.last()
        assertEquals("group-1", request.groupId)
        assertEquals("tag-1", request.tagId)
        assertEquals(CaseSortOrder.LAST_VIEWED_DESC, request.sortOrder)
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

        assertEquals(AppDestination.CaseList, viewModel.state.value.destination)
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
        val fileName = viewModel.confirmSingleCaseExport()
        assertEquals("合成命例甲_南枫八字命例.json", fileName)

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
    fun `密码加密导出后错误密码保留重试且正确密码进入预览`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-password"] = sampleStoredCase("case-password")
        }
        val viewModel = createViewModel(repository)
        viewModel.openDetail("case-password")
        viewModel.requestSingleCaseExport()
        viewModel.requestPasswordSingleCaseExport()
        val password = "合成测试密码123".toCharArray()

        val fileName = viewModel.confirmPasswordSingleCaseExport(
            password.copyOf(),
            password.copyOf(),
        )
        assertEquals("合成命例甲_南枫八字命例_加密.json", fileName)
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
        val confirmation = password.copyOf()

        assertEquals(
            null,
            viewModel.confirmPasswordSingleCaseExport(password, confirmation),
        )
        assertTrue(password.all { it == '\u0000' })
        assertTrue(confirmation.all { it == '\u0000' })

        val emptyPassword = CharArray(0)
        viewModel.previewSingleCaseWithPassword(emptyPassword) {
            error("空密码不应打开文件")
        }
        assertTrue(emptyPassword.all { it == '\u0000' })
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

        assertTrue(viewModel.state.value.singleCaseExportIncludesAttachments)
        val fileName = viewModel.confirmSingleCaseExport()
        assertEquals("合成命例甲_南枫八字命例包.nfbcase", fileName)
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
        assertEquals(
            "南枫八字备份_测试_加密.nfbak",
            viewModel.confirmPasswordFullBackupExport(password.copyOf(), password.copyOf()),
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

    private fun createViewModel(
        repository: FakeCaseRepository,
        bundleOperations: SingleCaseBundleOperations? = null,
        backupOperations: CaseBackupOperations? = null,
        backupRoot: Path? = null,
        engine: BaziEngine = RecordingEngine(),
    ): StageTwoViewModel {
        val fixedClock = Clock.fixed(FixedInstant, ZoneOffset.UTC)
        val ids = generateSequence(1) { it + 1 }
            .map { "generated-$it" }
            .iterator()
        return StageTwoViewModel(
            caseRepository = repository,
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
            ioDispatcher = dispatcher,
        )
    }

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
