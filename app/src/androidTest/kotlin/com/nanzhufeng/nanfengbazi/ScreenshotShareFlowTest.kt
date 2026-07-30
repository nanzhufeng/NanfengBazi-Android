package com.nanzhufeng.nanfengbazi

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    private var syntheticImageUri: Uri? = null

    @After
    fun tearDown() {
        syntheticImageUri?.let {
            ApplicationProvider.getApplicationContext<android.content.Context>()
                .contentResolver
                .delete(it, null, null)
        }
    }

    @Test
    fun 系统分享合成问真列表图后私有复制并完成离线识别() {
        val uri = createSyntheticWenzhenListImage()
        syntheticImageUri = uri
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
            "首候选四柱应从紧凑 OCR 块恢复，第二候选错字保持待核对；OCR=$syntheticOcrBlocks",
            7,
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
