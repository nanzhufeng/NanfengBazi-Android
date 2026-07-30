package com.nanzhufeng.nanfengbazi

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import com.nanzhufeng.nanfengbazi.imageparser.AnchorBasedWenzhenPageClassifier
import com.nanzhufeng.nanfengbazi.imageparser.MlKitChineseOcrEngine
import com.nanzhufeng.nanfengbazi.imageparser.OcrImageInput
import com.nanzhufeng.nanfengbazi.imageparser.WenzhenImageGrouper
import com.nanzhufeng.nanfengbazi.imageparser.WenzhenP0Parser
import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MlKitLongScreenshotTest {
    @Test
    fun 命主反馈合成图经真实离线OCR后生成事件候选() = runBlocking {
        val bytes = createFeedbackSyntheticImage()
        val sourceImage = ImportImageRef(
            id = "feedback-image",
            originalFileName = "synthetic-feedback.png",
            mimeType = "image/png",
            relativePath = "session-1/feedback-image.png",
            sha256 = "c".repeat(64),
            byteSize = bytes.size.toLong(),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val engine = MlKitChineseOcrEngine()
        try {
            val document = engine.recognize(OcrImageInput(sourceImage, bytes))
            val classification = AnchorBasedWenzhenPageClassifier().classify(document)
            assertTrue(
                "合成反馈页应被识别；OCR=${document.rawText}",
                classification.pageType == WenzhenPageType.FEEDBACK,
            )
            val image = sourceImage.copy(
                pageType = classification.pageType,
                pageConfidence = classification.confidence,
                classifierVersion = classification.classifierVersion,
            )
            val result = WenzhenP0Parser().parse(
                images = listOf(image),
                documents = listOf(document),
                groupedCandidates = WenzhenImageGrouper().group(
                    listOf(image),
                    listOf(document),
                ),
            )
            val eventFields = result.fields.filter {
                it.fieldKey.startsWith("event.candidate.")
            }
            assertTrue(
                "应从年份标题生成至少两个事件候选；OCR=${document.rawText}",
                eventFields.size >= 2,
            )
            assertTrue(eventFields.all { it.adoptedValue == null && it.boundingBox != null })
            assertTrue(result.longTexts.single().rawText == document.rawText)
        } finally {
            engine.close()
        }
    }

    @Test
    fun 基本资料合成图经真实离线OCR后提取关键出生字段() = runBlocking {
        val bytes = createBasicInfoSyntheticImage()
        val sourceImage = ImportImageRef(
            id = "basic-info-image",
            originalFileName = "synthetic-basic-info.png",
            mimeType = "image/png",
            relativePath = "session-1/basic-info-image.png",
            sha256 = "b".repeat(64),
            byteSize = bytes.size.toLong(),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val engine = MlKitChineseOcrEngine()
        try {
            val document = engine.recognize(OcrImageInput(sourceImage, bytes))
            val classification = AnchorBasedWenzhenPageClassifier().classify(document)
            assertTrue(
                "合成基本资料页应被识别；OCR=${document.rawText}",
                classification.pageType == WenzhenPageType.BASIC_INFO,
            )
            val image = sourceImage.copy(
                pageType = classification.pageType,
                pageConfidence = classification.confidence,
                classifierVersion = classification.classifierVersion,
            )
            val grouped = WenzhenImageGrouper().group(listOf(image), listOf(document))
            val result = WenzhenP0Parser().parse(
                images = listOf(image),
                documents = listOf(document),
                groupedCandidates = grouped,
            )
            val fieldKeys = result.fields.mapTo(mutableSetOf()) { it.fieldKey }
            assertTrue("应提取姓名；OCR=${document.rawText}", "identity.name" in fieldKeys)
            assertTrue("应提取性别；OCR=${document.rawText}", "identity.sex" in fieldKeys)
            assertTrue(
                "应提取公历出生时间；OCR=${document.rawText}",
                "birth.solar_datetime" in fieldKeys,
            )
            assertTrue(
                "应提取真太阳时；OCR=${document.rawText}",
                "birth.true_solar_datetime" in fieldKeys,
            )
            assertTrue("应提取出生地区；OCR=${document.rawText}", "birth.location" in fieldKeys)
            assertTrue(result.fields.all { it.adoptedValue == null && it.boundingBox != null })
        } finally {
            engine.close()
        }
    }

    @Test
    fun 长截图按重叠区分段且边界框回映原图坐标() = runBlocking {
        val bytes = createTallSyntheticImage()
        val image = ImportImageRef(
            id = "long-image-1",
            originalFileName = "synthetic-long.png",
            mimeType = "image/png",
            relativePath = "session-1/long-image-1.png",
            sha256 = "a".repeat(64),
            byteSize = bytes.size.toLong(),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val engine = MlKitChineseOcrEngine(
            maxDecodedPixels = 960_000,
            segmentOverlapPx = 200,
        )
        try {
            val document = engine.recognize(OcrImageInput(image, bytes))
            assertTrue(document.rawText.contains("1992"))
            assertTrue(
                "长图顶部文字应保留原图坐标",
                document.blocks.any { (it.boundingBox?.top ?: Int.MAX_VALUE) < 500 },
            )
            assertTrue(
                "长图底部文字应回映到原图纵坐标",
                document.blocks.any { (it.boundingBox?.bottom ?: 0) > 2_000 },
            )
        } finally {
            engine.close()
        }
    }

    private fun createTallSyntheticImage(): ByteArray {
        val bitmap = Bitmap.createBitmap(800, 2400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 96f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText("问真八字 用户列表", 50f, 220f, paint)
        canvas.drawText("阳历 1992年8月24日", 50f, 2_220f, paint)
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun createBasicInfoSyntheticImage(): ByteArray {
        val bitmap = Bitmap.createBitmap(1080, 1700, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 58f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        listOf(
            "问真八字 基本信息",
            "姓名：席瑞",
            "性别：男",
            "农历：1992年七月廿六 午时 乾造",
            "阳历：1992-08-24 12:00:00",
            "真太阳时：1992-08-24 11:53:00",
            "出生地区：江苏省宿迁市泗阳县",
            "地址经纬：北纬33.72 东经118.68",
            "星座：处女座 属相：猴",
            "壬申 戊申 壬申 丙午",
        ).forEachIndexed { index, line ->
            canvas.drawText(line, 56f, 150f + index * 145f, paint)
        }
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun createFeedbackSyntheticImage(): ByteArray {
        val bitmap = Bitmap.createBitmap(1080, 1300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 64f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        listOf(
            "问真八字 命主反馈",
            "关键事件反馈记录",
            "1999年 己卯",
            "进入大学学习",
            "2005年 乙酉",
            "进入新的工作单位",
        ).forEachIndexed { index, line ->
            canvas.drawText(line, 64f, 150f + index * 170f, paint)
        }
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
