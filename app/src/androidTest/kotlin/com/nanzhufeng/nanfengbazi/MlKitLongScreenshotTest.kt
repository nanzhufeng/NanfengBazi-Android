package com.nanzhufeng.nanfengbazi

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.imageparser.MlKitChineseOcrEngine
import com.nanzhufeng.nanfengbazi.imageparser.OcrImageInput
import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MlKitLongScreenshotTest {
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
}
