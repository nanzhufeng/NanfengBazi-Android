package com.nanzhufeng.nanfengbazi

import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.ViewModelProvider
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotShareFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val syntheticImageUris = mutableListOf<Uri>()

    @After
    fun tearDown() {
        syntheticImageUris.forEach {
            ApplicationProvider.getApplicationContext<android.content.Context>()
                .contentResolver
                .delete(it, null, null)
        }
    }

    @Test
    fun 八张图片批次通过前台识别并保留可恢复结果() {
        val uris = List(8) { createSyntheticWenzhenListImage() }
        syntheticImageUris += uris
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/png"
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(uris),
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.consumeSharedImages(intent)
        }
        composeRule.waitUntil(timeoutMillis = 180_000) {
            var finished = false
            composeRule.activityRule.scenario.onActivity { activity ->
                val container = (activity.application as NanfengBaziApplication).container
                runBlocking {
                    finished = container.importSessionRepository
                        .list()
                        .singleOrNull { it.images.size == 8 }
                        ?.status == ImportStatus.NEEDS_REVIEW
                }
            }
            finished
        }
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        assertTrue(
            "大批次应创建前台识别通知渠道",
            notificationManager.getNotificationChannel(
                ScreenshotRecognitionWorker.NOTIFICATION_CHANNEL_ID,
            ) != null,
        )
        composeRule.activityRule.scenario.onActivity { activity ->
            val container = (activity.application as NanfengBaziApplication).container
            runBlocking {
                val session = container.importSessionRepository
                    .list()
                    .single { it.images.size == 8 }
                session.images.forEach { image ->
                    java.nio.file.Files.deleteIfExists(
                        activity.filesDir.toPath()
                            .resolve("import-images")
                            .resolve(image.relativePath),
                    )
                }
                container.importSessionRepository.delete(session.id, session.revision)
            }
        }
    }

    @Test
    fun 系统分享合成问真列表图后私有复制并完成离线识别() {
        val uri = createSyntheticWenzhenListImage()
        syntheticImageUris += uri
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.consumeSharedImages(intent)
        }
        composeRule.waitUntil(timeoutMillis = 90_000) {
            composeRule
                .onAllNodesWithTag("screenshot_import_summary")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 90_000) {
            runCatching {
                composeRule.onNodeWithText("截图识别结果待核对").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("原图 1 张", substring = true).assertIsDisplayed()
        var pageType: WenzhenPageType? = null
        var syntheticOcrText = ""
        var syntheticOcrBlocks = ""
        var formalCaseCount = -1
        var fingerprint: String? = null
        var dimensions: Pair<Int?, Int?>? = null
        var privateImagePath: Path? = null
        var candidateCount = -1
        var extractedFieldCount = -1
        var allFieldsUnadopted = false
        var missingFourPillarsFieldId = ""
        var incompleteCandidateId = ""
        composeRule.activityRule.scenario.onActivity { activity ->
            val container = (activity.application as NanfengBaziApplication).container
            runBlocking {
                val session = container.importSessionRepository
                    .list()
                    .first()
                val image = session.images.single()
                pageType = image.pageType
                fingerprint = image.perceptualHash
                dimensions = image.widthPx to image.heightPx
                candidateCount = session.caseCandidates.size
                extractedFieldCount = session.extractedFields.size
                allFieldsUnadopted = session.extractedFields.all { it.adoptedValue == null }
                val missingFourPillars = session.extractedFields.single {
                    it.fieldKey == "chart.four_pillars" && it.normalizedValue == null
                }
                missingFourPillarsFieldId = missingFourPillars.id
                incompleteCandidateId = session.caseCandidates.single {
                    missingFourPillars.id in it.fieldEvidenceIds
                }.id
                privateImagePath = activity.filesDir.toPath()
                    .resolve("import-images")
                    .resolve(image.relativePath)
                syntheticOcrText = session.ocrDocuments.single().rawText
                syntheticOcrBlocks = session.ocrDocuments.single().blocks.joinToString(" | ") {
                    "${it.text}@${it.boundingBox}"
                }
                formalCaseCount = container.caseRepository.search().size
            }
        }
        assertEquals(syntheticOcrText, WenzhenPageType.USER_LIST, pageType)
        assertTrue("私有图片必须保存 64 位感知哈希", fingerprint?.length == 16)
        assertEquals(1080 to 1600, dimensions)
        assertEquals("用户列表一张图应拆成两个待核对候选", 2, candidateCount)
        assertEquals(
            "首候选四柱应恢复，第二候选错字应保留为可修正字段；OCR=$syntheticOcrBlocks",
            8,
            extractedFieldCount,
        )
        assertTrue("OCR 字段未经确认不得产生采用值", allFieldsUnadopted)
        assertTrue("截图识别不得直接写入正式命例", formalCaseCount == 0)
        val storedImagePath = requireNotNull(privateImagePath)
        assertTrue("识别完成后私有原图必须存在", java.nio.file.Files.exists(storedImagePath))

        composeRule.onNodeWithTag("review_screenshot_import_button").performClick()
        composeRule.onNodeWithText("核对问真导入").assertIsDisplayed()
        composeRule.onAllNodesWithText("采用本候选全部内容").onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            var adoptedCount = 0
            composeRule.activityRule.scenario.onActivity { activity ->
                val container = (activity.application as NanfengBaziApplication).container
                runBlocking {
                    adoptedCount = container.importSessionRepository
                        .list()
                        .first()
                        .extractedFields
                        .count { it.adoptedValue != null }
                }
            }
            adoptedCount == 4
        }
        composeRule.onAllNodesWithText("复算一致后写入正式命例").onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            var committedCount = 0
            var committedCandidateCount = 0
            var sessionStillNeedsReview = false
            composeRule.activityRule.scenario.onActivity { activity ->
                val container = (activity.application as NanfengBaziApplication).container
                runBlocking {
                    committedCount = container.caseRepository.search().size
                    val session = container.importSessionRepository.list().first()
                    committedCandidateCount =
                        session.caseCandidates.count { it.targetCaseId != null }
                    sessionStillNeedsReview = session.status == ImportStatus.NEEDS_REVIEW
                }
            }
            committedCount == 1 &&
                committedCandidateCount == 1 &&
                sessionStillNeedsReview
        }
        composeRule
            .onNodeWithTag("screenshot_review_list")
            .performScrollToNode(
                hasTestTag("candidate_blocking_issues_$incompleteCandidateId"),
            )
        composeRule
            .onNodeWithTag("candidate_blocking_issues_$incompleteCandidateId")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("• 四柱无法规范化，请人工修正")
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("screenshot_review_list")
            .performScrollToNode(hasTestTag("screenshot_candidate_$incompleteCandidateId"))
        composeRule
            .onNodeWithTag("screenshot_review_list")
            .performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag("preview_screenshot_field_$missingFourPillarsFieldId")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithTag("screenshot_field_preview_$missingFourPillarsFieldId")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule
            .onNodeWithTag("preview_screenshot_field_$missingFourPillarsFieldId")
            .performClick()
        composeRule
            .onNodeWithTag("edit_screenshot_field_$missingFourPillarsFieldId")
            .performScrollTo()
            .performTextReplacement("庚辰 癸未 乙未 甲申")
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag("edit_screenshot_field_$missingFourPillarsFieldId")
            .assertTextContains("庚辰 癸未 乙未 甲申")
        composeRule.activityRule.scenario.onActivity { activity ->
            val inputMethodManager =
                activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(
                activity.currentFocus?.windowToken,
                0,
            )
            activity.currentFocus?.clearFocus()
        }
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag("screenshot_review_list")
            .performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag("save_screenshot_field_$missingFourPillarsFieldId")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        var correctionDebug = "尚未读取修正状态"
        val correctionPersisted = runCatching {
            composeRule.waitUntil(timeoutMillis = 10_000) {
                var corrected = false
                composeRule.activityRule.scenario.onActivity { activity ->
                    val container = (activity.application as NanfengBaziApplication).container
                    val screenshotImportViewModel =
                        ViewModelProvider(activity)[ScreenshotImportViewModel::class.java]
                    runBlocking {
                        val persistedField = container.importSessionRepository
                            .list()
                            .first()
                            .extractedFields
                            .single { it.id == missingFourPillarsFieldId }
                        corrected =
                            persistedField.userEdited &&
                                persistedField.normalizedValue != null &&
                                persistedField.adoptedValue == null
                        correctionDebug =
                            "message=${screenshotImportViewModel.state.value.message}, " +
                                "busy=${screenshotImportViewModel.state.value.busy}, " +
                                "activeSession=${screenshotImportViewModel.state.value.activeSessionId}, " +
                                "field=$persistedField"
                    }
                }
                corrected
            }
        }.isSuccess
        assertTrue(correctionDebug, correctionPersisted)
        composeRule
            .onNodeWithTag("adopt_screenshot_candidate_$incompleteCandidateId")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("commit_screenshot_candidate_$incompleteCandidateId")
            .performScrollTo()
            .assertIsEnabled()
        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithTag("delete_screenshot_import_button").performClick()
        composeRule.onNodeWithTag("confirm_delete_screenshot_import").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("screenshot_import_summary")
                .fetchSemanticsNodes()
                .isEmpty()
        }
        composeRule.activityRule.scenario.onActivity { activity ->
            val container = (activity.application as NanfengBaziApplication).container
            runBlocking {
                assertTrue("确认删除后导入会话应移除", container.importSessionRepository.list().isEmpty())
                assertEquals("删除导入会话不得删除已提交命例", 1, container.caseRepository.search().size)
            }
        }
        assertTrue("确认删除后私有原图应移除", !java.nio.file.Files.exists(storedImagePath))
        composeRule.onAllNodesWithText("案例甲").onFirst().performClick()
        composeRule
            .onNodeWithText("导入证据对照", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "来源原文不会被人工修正覆盖；规范值、采用值与本机计算结果分别留存。",
                useUnmergedTree = true,
            )
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun createSyntheticWenzhenListImage(): Uri {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "synthetic-wenzhen-list.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NanfengBaziTests")
        }
        val uri = requireNotNull(
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values,
            ),
        )
        val bitmap = Bitmap.createBitmap(1080, 1600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 62f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText("问真八字", 80f, 130f, paint)
        canvas.drawText("用户列表", 80f, 270f, paint)
        canvas.drawText("名人库", 420f, 270f, paint)
        canvas.drawText("筛选", 80f, 410f, paint)
        canvas.drawText("案例甲 男", 80f, 650f, paint)
        canvas.drawText("阳历1992年8月24日", 80f, 760f, paint)
        canvas.drawText("壬 戊 壬 丙", 700f, 650f, paint)
        canvas.drawText("申 申 申 午", 700f, 750f, paint)
        canvas.drawText("案例乙 女", 80f, 1050f, paint)
        canvas.drawText("阳历2000年8月5日", 80f, 1160f, paint)
        canvas.drawText("庚 癸 乙 甲", 700f, 1050f, paint)
        canvas.drawText("辰 未 未 申", 700f, 1150f, paint)
        requireNotNull(context.contentResolver.openOutputStream(uri, "w")).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "合成测试图片写入失败"
            }
        }
        bitmap.recycle()
        return uri
    }
}
