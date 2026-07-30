package com.nanzhufeng.nanfengbazi.imageparser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.nanzhufeng.nanfengbazi.domain.model.EvidenceBoundingBox
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.OcrTextBlock
import java.io.Closeable
import java.time.Clock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class MlKitChineseOcrEngine(
    private val clock: Clock = Clock.systemUTC(),
    private val maxDecodedPixels: Long = DEFAULT_MAX_DECODED_PIXELS,
) : OcrEngine, Closeable {
    init {
        require(maxDecodedPixels > 0) { "OCR 解码像素上限必须为正数" }
    }

    override val engineId: String = "mlkit-chinese-bundled"
    override val engineVersion: String = "16.0.1"
    override val executionMode: OcrExecutionMode = OcrExecutionMode.OFFLINE

    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build(),
    )

    override suspend fun recognize(input: OcrImageInput): OcrDocument {
        val bitmap = decodeBitmap(input.bytes)
        return try {
            val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
            require(result.text.isNotBlank()) { "图片中未识别到文字" }
            OcrDocument(
                imageId = input.image.id,
                rawText = result.text,
                blocks = result.toDomainBlocks(),
                engineId = engineId,
                engineVersion = engineVersion,
                recognizedAt = clock.instant(),
            )
        } finally {
            bitmap.recycle()
        }
    }

    override fun close() {
        recognizer.close()
    }

    private fun decodeBitmap(bytes: ByteArray): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法解析图片尺寸" }
        val pixels = bounds.outWidth.toLong() * bounds.outHeight.toLong()
        require(pixels <= maxDecodedPixels) {
            "图片尺寸过大，需要先进行长图分段"
        }
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)) {
            "无法解码图片"
        }
    }

    private fun Text.toDomainBlocks(): List<OcrTextBlock> =
        textBlocks.flatMap { block -> block.lines }
            .mapIndexedNotNull { index, line ->
                val text = line.text.trim()
                if (text.isEmpty()) return@mapIndexedNotNull null
                OcrTextBlock(
                    id = "line-${index + 1}",
                    text = text,
                    confidence = line.confidence.takeIf { it in 0f..1f },
                    boundingBox = line.boundingBox?.let { rect ->
                        EvidenceBoundingBox(
                            left = rect.left,
                            top = rect.top,
                            right = rect.right,
                            bottom = rect.bottom,
                        )
                    },
                )
            }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }

    private companion object {
        const val DEFAULT_MAX_DECODED_PIXELS = 32_000_000L
    }
}
