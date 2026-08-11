package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SourceAttachment
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.regex.Pattern
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

class SingleCaseBundleFlowTest {
    private val alias = "附件包系统链路-${System.currentTimeMillis()}"
    private val sourceAttachmentBytes =
        "synthetic-wenzhen-screenshot-${UUID.randomUUID()}".encodeToByteArray()
    private lateinit var sourceCaseId: String
    private lateinit var sourceAttachmentId: String

    private val seedRule = object : ExternalResource() {
        override fun before() {
            val container = application().container
            runBlocking {
                val created = CreateCaseUseCase(
                    baziEngine = container.baziEngine,
                    caseRepository = container.caseRepository,
                )(
                    form = CaseFormState(
                        alias = alias,
                        name = "合成附件命例",
                        sex = SexForFortuneDirection.MAN,
                        year = "2000",
                        month = "2",
                        day = "29",
                        hour = "10",
                        minute = "30",
                        locationName = "江苏省苏州市",
                    ),
                    allowDuplicate = true,
                )
                check(created is CreateCaseResult.Created) {
                    "无法创建附件包系统测试命例：$created"
                }
                sourceCaseId = created.caseId
                val sourceCase = checkNotNull(
                    container.caseRepository.findById(sourceCaseId),
                )
                sourceAttachmentId = "attachment-${UUID.randomUUID()}"
                val relativePath =
                    "bundle-system-e2e/$sourceCaseId/wenzhen-synthetic.jpg"
                val attachmentPath =
                    container.backupAttachmentRoot.resolve(relativePath)
                Files.createDirectories(checkNotNull(attachmentPath.parent))
                Files.write(attachmentPath, sourceAttachmentBytes)
                val now = Instant.now()
                val attachment = SourceAttachment(
                    id = sourceAttachmentId,
                    relativePath = relativePath,
                    originalFileName = "wenzhen-synthetic.jpg",
                    mimeType = "image/jpeg",
                    sha256 = sha256(sourceAttachmentBytes),
                    byteSize = sourceAttachmentBytes.size.toLong(),
                    createdAt = now,
                )
                val write = container.caseRepository.save(
                    case = sourceCase.copy(
                        textRecords = sourceCase.textRecords + CaseTextRecord(
                            id = "record-${UUID.randomUUID()}",
                            type = CaseTextRecordType.OWNER_FEEDBACK,
                            content = "由合成问真截图录入的反馈",
                            sourceAttachmentId = sourceAttachmentId,
                            createdAt = now,
                            updatedAt = now,
                        ),
                        attachments = listOf(attachment),
                        updatedAt = now,
                    ),
                    expectedRevision = sourceCase.revision,
                )
                check(write is CaseWriteResult.Updated) {
                    "无法写入附件包系统测试证据：$write"
                }
            }
        }
    }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(seedRule).around(composeRule)

    @Test
    fun exportAndKeepBothThroughSystemDocumentsPreservesAttachmentBytesAndReferences() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val exportedFileName = "${alias.take(48)}_南枫八字命例包.nfbcase"

        openSourceDetail()

        composeRule.onNodeWithTag("toggle_case_management").performClick()
        composeRule.onNodeWithTag("export_single_case_menu").performClick()
        composeRule.onNodeWithText("导出当前命例").assertIsDisplayed()
        composeRule.onNodeWithTag("single_case_export_with_attachments")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag("confirm_single_case_export").performClick()

        val saveButton = device.wait(
            Until.findObject(By.text(Pattern.compile("(?i)save|保存"))),
            SYSTEM_UI_TIMEOUT_MILLIS,
        )
        checkNotNull(saveButton) { "命例附件包未进入 Android 系统创建文档页面" }
        saveButton.click()
        waitForAppWindow(device)
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(
                hasText("单命例附件包已导出，图片二进制和引用均已校验。"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        waitForMessageToDismiss("单命例附件包已导出，图片二进制和引用均已校验。")

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        openSingleCaseImport()
        val exportedFile = device.wait(
            Until.findObject(By.textContains(alias.take(32))),
            SYSTEM_UI_TIMEOUT_MILLIS,
        )
        checkNotNull(exportedFile) {
            "Android 系统打开文档页面未找到刚导出的命例附件包：$exportedFileName"
        }
        exportedFile.click()
        waitForAppWindow(device)

        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasTestTag("single_case_preview"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("单命例导入预览").assertIsDisplayed()
        composeRule.onNodeWithText("附件包已校验 1 个图片附件", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("keep_both_single_case")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(
                hasText("单命例已作为新命例导入，原有本地命例未被覆盖。"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        verifyImportedCopy()
    }

    @Test
    fun encryptedBundleRequiresPreviewAndCommitPasswordsThroughSystemDocuments() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val password = "Bundle-System-Pass123"

        openSourceDetail()
        composeRule.onNodeWithTag("toggle_case_management").performClick()
        composeRule.onNodeWithTag("export_single_case_menu").performClick()
        composeRule.onNodeWithTag("single_case_export_with_attachments")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag("choose_password_single_case_export").performClick()
        composeRule.onNodeWithTag("single_case_password").performTextInput(password)
        composeRule.onNodeWithTag("confirm_password_single_case_export").performClick()

        val saveButton = device.wait(
            Until.findObject(By.text(Pattern.compile("(?i)save|保存"))),
            SYSTEM_UI_TIMEOUT_MILLIS,
        )
        checkNotNull(saveButton) { "密码加密命例附件包未进入 Android 系统创建文档页面" }
        saveButton.click()
        waitForAppWindow(device)
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(
                hasText(
                    "密码加密单命例附件包已导出；图片二进制已包含，请另行保存密码。",
                ),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        waitForMessageToDismiss(
            "密码加密单命例附件包已导出；图片二进制已包含，请另行保存密码。",
        )

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        openSingleCaseImport()
        check(clickSystemDocument(device, alias.take(32))) {
            "Android 系统打开文档页面未找到刚导出的密码加密命例附件包"
        }
        waitForAppWindow(device)

        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasText("输入单命例解密密码"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("single_case_password").performTextInput(password)
        composeRule.onNodeWithTag("confirm_password_single_case_import").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasTestTag("single_case_preview"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("文件保护：密码加密")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("keep_both_single_case").performClick()
        composeRule.onNodeWithText("再次输入命例包密码").assertIsDisplayed()
        composeRule.onNodeWithTag("single_case_commit_password")
            .performTextInput(password)
        composeRule.onNodeWithTag("confirm_password_single_case_commit").performClick()

        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(
                hasText("单命例已作为新命例导入，原有本地命例未被覆盖。"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        verifyImportedCopy()
    }

    private fun openSourceDetail() {
        if (
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isEmpty()
        ) {
            composeRule.onNodeWithTag("nav_records").performClick()
        }
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_search").performTextInput(alias)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText(alias))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("case_$sourceCaseId").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("case_detail_screen").assertIsDisplayed()
    }

    private fun openSingleCaseImport() {
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("settings_home_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_import_case")
            .performScrollTo()
            .performClick()
    }

    private fun waitForMessageToDismiss(message: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText(message))
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private fun verifyImportedCopy() {
        val container = application().container
        val imported = runBlocking {
            container.caseRepository.search(
                CaseSearchRequest(
                    query = alias,
                    visibility = CaseVisibility.ALL,
                ),
            )
                .filter { it.alias == alias && it.id != sourceCaseId }
                .mapNotNull { container.caseRepository.findById(it.id) }
                .single()
        }
        check(imported.copiedFromCaseId == sourceCaseId) {
            "保留两份导入未记录来源命例身份"
        }
        val importedAttachment = imported.attachments.single()
        check(importedAttachment.id != sourceAttachmentId) {
            "导入附件未生成新的稳定身份"
        }
        check(
            imported.textRecords.single { it.content == "由合成问真截图录入的反馈" }
                .sourceAttachmentId == importedAttachment.id,
        ) {
            "导入文本记录没有重映射到新附件身份"
        }
        val restoredBytes = Files.readAllBytes(
            container.backupAttachmentRoot.resolve(importedAttachment.relativePath),
        )
        check(restoredBytes.contentEquals(sourceAttachmentBytes)) {
            "系统文件往返后的附件字节与来源不一致"
        }
        check(importedAttachment.sha256 == sha256(sourceAttachmentBytes)) {
            "系统文件往返后的附件 SHA-256 与来源不一致"
        }
    }

    private fun application(): NanfengBaziApplication =
        InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as NanfengBaziApplication

    private fun waitForAppWindow(device: UiDevice) {
        check(
            device.wait(
                Until.hasObject(By.pkg(APPLICATION_ID)),
                SYSTEM_UI_TIMEOUT_MILLIS,
            ),
        ) {
            "Android 系统文档页面没有返回南枫八字"
        }
    }

    private fun clickSystemDocument(device: UiDevice, text: String): Boolean {
        repeat(3) {
            val document = device.wait(
                Until.findObject(By.textContains(text)),
                SYSTEM_UI_TIMEOUT_MILLIS,
            ) ?: return@repeat
            if (runCatching { document.click() }.isSuccess) {
                return true
            }
            device.waitForIdle()
        }
        return false
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val APPLICATION_ID = "com.nanzhufeng.nanfengbazi"
        const val SYSTEM_UI_TIMEOUT_MILLIS = 30_000L
    }
}
