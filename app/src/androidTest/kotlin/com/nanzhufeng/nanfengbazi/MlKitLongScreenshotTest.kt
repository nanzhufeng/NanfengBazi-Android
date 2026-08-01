package com.nanzhufeng.nanfengbazi

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nanzhufeng.nanfengbazi.domain.model.EvidenceBoundingBox
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.OcrTextBlock
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import com.nanzhufeng.nanfengbazi.imageparser.AnchorBasedWenzhenPageClassifier
import com.nanzhufeng.nanfengbazi.imageparser.MlKitChineseOcrEngine
import com.nanzhufeng.nanfengbazi.imageparser.OcrImageInput
import com.nanzhufeng.nanfengbazi.imageparser.WenzhenImageGrouper
import com.nanzhufeng.nanfengbazi.imageparser.WenzhenP0Parser
import com.nanzhufeng.nanfengbazi.imageparser.WenzhenUserListOcrRefiner
import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MlKitLongScreenshotTest {
    @Test
    fun 用户列表日期列二次扫描补回首次OCR遗漏的真实日期锚点() = runBlocking {
        val bytes = createUserListDateRecoveryImage()
        val image = ImportImageRef(
            id = "user-list-date-recovery",
            originalFileName = "synthetic-user-list-date-recovery.png",
            mimeType = "image/png",
            relativePath = "session-1/user-list-date-recovery.png",
            sha256 = "e".repeat(64),
            byteSize = bytes.size.toLong(),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val initialDocument = OcrDocument(
            imageId = image.id,
            rawText = "用户列表\n阳历1992年8月24日",
            blocks = listOf(
                OcrTextBlock(
                    id = "initial-date",
                    text = "阳历1992年8月24日",
                    confidence = 1f,
                    boundingBox = EvidenceBoundingBox(50, 270, 500, 340),
                ),
            ),
            engineId = "fixture",
            engineVersion = "fixture",
            recognizedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val engine = MlKitChineseOcrEngine()
        try {
            val refined = WenzhenUserListOcrRefiner(engine).refine(
                input = OcrImageInput(image, bytes),
                pageType = WenzhenPageType.USER_LIST,
                initialDocument = initialDocument,
            )
            val classifiedImage = image.copy(pageType = WenzhenPageType.USER_LIST)
            val parsed = WenzhenP0Parser().parse(
                images = listOf(classifiedImage),
                documents = listOf(refined),
                groupedCandidates = WenzhenImageGrouper().group(
                    listOf(classifiedImage),
                    listOf(refined),
                ),
            )

            assertEquals(2, parsed.candidates.size)
        } finally {
            engine.close()
        }
    }

    @Test
    fun 基本排盘合成图经真实离线OCR后拆出四柱表格证据() = runBlocking {
        val bytes = createBasicChartSyntheticImage()
        val sourceImage = ImportImageRef(
            id = "basic-chart-image",
            originalFileName = "synthetic-basic-chart.png",
            mimeType = "image/png",
            relativePath = "session-1/basic-chart-image.png",
            sha256 = "d".repeat(64),
            byteSize = bytes.size.toLong(),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val engine = MlKitChineseOcrEngine()
        try {
            val document = engine.recognize(OcrImageInput(sourceImage, bytes))
            val classification = AnchorBasedWenzhenPageClassifier().classify(document)
            assertTrue(
                "合成基本排盘页应被识别；OCR=${document.rawText}",
                classification.pageType == WenzhenPageType.BASIC_CHART,
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
            val chartFields = result.fields.filter {
                it.fieldKey.matches(Regex("chart\\.(year|month|day|hour)\\..+"))
            }
            assertTrue(
                "应从表格行生成至少 16 项分柱证据；OCR=${document.rawText}",
                chartFields.size >= 16,
            )
            assertTrue(chartFields.all { it.adoptedValue == null && it.boundingBox != null })
        } finally {
            engine.close()
        }
    }

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
            assertTrue("应提取前一节；OCR=${document.rawText}", "birth.previous_jie" in fieldKeys)
            assertTrue("应提取后一节；OCR=${document.rawText}", "birth.next_jie" in fieldKeys)
            assertTrue("应提取胎元；OCR=${document.rawText}", "chart.fetal_origin" in fieldKeys)
            assertTrue("应提取胎息；OCR=${document.rawText}", "chart.fetal_breath" in fieldKeys)
            assertTrue("应提取命宫；OCR=${document.rawText}", "chart.own_sign" in fieldKeys)
            assertTrue("应提取身宫；OCR=${document.rawText}", "chart.body_sign" in fieldKeys)
            assertTrue(result.fields.all { it.adoptedValue == null && it.boundingBox != null })
        } finally {
            engine.close()
        }
    }

    @Test
    fun 基本资料来源保真字段经真实离线OCR后不生成计算值() = runBlocking {
        val bytes = createSourceOnlyBasicInfoSyntheticImage()
        val sourceImage = ImportImageRef(
            id = "basic-info-source-only-image",
            originalFileName = "synthetic-basic-info-source-only.png",
            mimeType = "image/png",
            relativePath = "session-1/basic-info-source-only-image.png",
            sha256 = "c".repeat(64),
            byteSize = bytes.size.toLong(),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val engine = MlKitChineseOcrEngine()
        try {
            val document = engine.recognize(OcrImageInput(sourceImage, bytes))
            val classification = AnchorBasedWenzhenPageClassifier().classify(document)
            assertTrue(
                "合成来源保真页应被识别为基本资料；OCR=${document.rawText}",
                classification.pageType == WenzhenPageType.BASIC_INFO,
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
            val fieldsByKey = result.fields.associateBy { it.fieldKey }
            listOf(
                "chart.star_lodge",
                "chart.life_gua",
                "chart.five_element.same_party_percent",
                "chart.five_element.metal_percent",
            ).forEach { fieldKey ->
                assertTrue("应提取 $fieldKey；OCR=${document.rawText}", fieldKey in fieldsByKey)
                assertTrue(fieldsByKey[fieldKey]?.calculatedValue == null)
            }
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

    private fun createUserListDateRecoveryImage(): ByteArray {
        val bitmap = Bitmap.createBitmap(1080, 900, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val primary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 56f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val secondary = Paint(primary).apply {
            color = Color.rgb(150, 150, 150)
            textSize = 48f
        }
        canvas.drawText("用户列表", 50f, 100f, primary)
        canvas.drawText("案例甲 男", 50f, 220f, primary)
        canvas.drawText("壬 戊 壬 丙", 650f, 210f, primary)
        canvas.drawText("申 申 申 午", 650f, 265f, primary)
        canvas.drawText("阳历1992年8月24日", 50f, 330f, secondary)
        canvas.drawText("案例乙 女", 50f, 520f, primary)
        canvas.drawText("乙 己 戊 癸", 650f, 510f, primary)
        canvas.drawText("亥 卯 申 丑", 650f, 565f, primary)
        canvas.drawText("阳历1995年3月18日", 50f, 630f, secondary)
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun createBasicChartSyntheticImage(): ByteArray {
        val bitmap = Bitmap.createBitmap(1080, 1900, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 52f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        listOf(
            "问真八字 基本排盘",
            "年柱 月柱 日柱 时柱",
            "主星 比肩 七杀 元男 偏财",
            "天干 壬 戊 壬 丙",
            "地支 申 申 申 午",
            "藏干 庚金 庚金 庚金 丁火",
            "副星 偏印 偏印 偏印 正财",
            "星运 长生 长生 长生 胎",
            "自坐 长生 病 长生 帝旺",
            "空亡 戌亥 寅卯 戌亥 寅卯",
            "纳音 剑锋金 大驿土 剑锋金 天河水",
            "原局天干 丙壬相冲",
        ).forEachIndexed { index, line ->
            canvas.drawText(line, 52f, 130f + index * 145f, paint)
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

    private fun createBasicInfoSyntheticImage(): ByteArray {
        val bitmap = Bitmap.createBitmap(1080, 2500, Bitmap.Config.ARGB_8888)
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
            "立秋：1992-08-07 14:27:24",
            "白露：1992-09-07 17:18:20",
            "胎元：己亥 胎息：丙寅",
            "命宫：壬寅 身宫：癸卯",
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

    private fun createSourceOnlyBasicInfoSyntheticImage(): ByteArray {
        val bitmap = Bitmap.createBitmap(1080, 2500, Bitmap.Config.ARGB_8888)
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
            "星宿：奎宿 命卦：艮卦",
            "自定旺衰：身旺 自定格局：偏印格",
            "同党 72% 异党 28%",
            "木 0% 火 12% 土 16% 金 43% 水 29%",
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
