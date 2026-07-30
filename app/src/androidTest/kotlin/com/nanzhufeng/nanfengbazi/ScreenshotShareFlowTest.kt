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
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
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
        var formalCaseCount = -1
        var fingerprint: String? = null
        var dimensions: Pair<Int?, Int?>? = null
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
                syntheticOcrText = session.ocrDocuments.single().rawText
                formalCaseCount = container.caseRepository.search().size
            }
        }
        assertEquals(syntheticOcrText, WenzhenPageType.USER_LIST, pageType)
        assertTrue("私有图片必须保存 64 位感知哈希", fingerprint?.length == 16)
        assertEquals(1080 to 1600, dimensions)
        assertTrue("截图识别不得直接写入正式命例", formalCaseCount == 0)
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
            textSize = 92f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        listOf(
            "问真八字",
            "用户列表",
            "用户列表",
            "名人库",
            "筛选",
            "阳历",
            "1992年8月24日",
        ).forEachIndexed { index, line ->
            canvas.drawText(line, 80f, 150f + index * 170f, paint)
        }
        requireNotNull(context.contentResolver.openOutputStream(uri, "w")).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "合成测试图片写入失败"
            }
        }
        bitmap.recycle()
        return uri
    }
}
