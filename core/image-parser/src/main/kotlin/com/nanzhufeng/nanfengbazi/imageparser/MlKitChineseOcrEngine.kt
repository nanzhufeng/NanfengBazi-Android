package com.nanzhufeng.nanfengbazi.imageparser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
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
import kotlin.math.max
import kotlin.math.min
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class MlKitChineseOcrEngine(
    private val clock: Clock = Clock.systemUTC(),
    private val maxDecodedPixels: Long = DEFAULT_MAX_DECODED_PIXELS,
    private val segmentOverlapPx: Int = DEFAULT_SEGMENT_OVERLAP_PX,
) : OcrEngine, Closeable {
    init {
        require(maxDecodedPixels > 0) { "OCR 解码像素上限必须为正数" }
        require(segmentOverlapPx >= 0) { "长图分段重叠高度不能为负数" }
    }

    override val engineId: String = "mlkit-chinese-bundled"
    override val engineVersion: String = "16.0.1+long-segment-v1"
    override val executionMode: OcrExecutionMode = OcrExecutionMode.OFFLINE

    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build(),
    )

    override suspend fun recognize(input: OcrImageInput): OcrDocument {
        val dimensions = readDimensions(input.bytes)
        val pixels = dimensions.first.toLong() * dimensions.second.toLong()
        val recognition = if (pixels <= maxDecodedPixels) {
            recognizeWholeImage(input.bytes)
        } else {
            recognizeLongImage(
                bytes = input.bytes,
                width = dimensions.first,
                height = dimensions.second,
            )
        }
        require(recognition.rawText.isNotBlank()) { "图片中未识别到文字" }
        return OcrDocument(
            imageId = input.image.id,
            rawText = recognition.rawText,
            blocks = recognition.lines.mapIndexed { index, line ->
                OcrTextBlock(
                    id = "line-${index + 1}",
                    text = line.text,
                    confidence = line.confidence,
                    boundingBox = line.boundingBox,
                )
            },
            engineId = engineId,
            engineVersion = engineVersion,
            recognizedAt = clock.instant(),
        )
    }

    override fun close() {
        recognizer.close()
    }

    private fun readDimensions(bytes: ByteArray): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法解析图片尺寸" }
        return bounds.outWidth to bounds.outHeight
    }

    private suspend fun recognizeWholeImage(bytes: ByteArray): RecognitionResult {
        val bitmap = requireNotNull(
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                },
            ),
        ) { "无法解码图片" }
        return try {
            recognizeBitmap(bitmap, verticalOffset = 0)
        } finally {
            bitmap.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun recognizeLongImage(
        bytes: ByteArray,
        width: Int,
        height: Int,
    ): RecognitionResult {
        val maxSegmentHeight = (maxDecodedPixels / width).toInt()
        require(maxSegmentHeight >= MIN_SEGMENT_HEIGHT_PX) {
            "图片宽度过大，无法在安全内存范围内分段识别"
        }
        val overlap = min(segmentOverlapPx, maxSegmentHeight / 4)
        val decoder = requireNotNull(
            BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false),
        ) { "无法建立长图分段解码器" }
        val chunks = mutableListOf<RecognitionResult>()
        try {
            var top = 0
            while (top < height) {
                val bottom = min(height, top + maxSegmentHeight)
                val bitmap = requireNotNull(
                    decoder.decodeRegion(
                        Rect(0, top, width, bottom),
                        BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.RGB_565
                        },
                    ),
                ) { "无法解码长图分段" }
                try {
                    chunks += recognizeBitmap(bitmap, verticalOffset = top)
                } finally {
                    bitmap.recycle()
                }
                if (bottom == height) break
                top = bottom - overlap
            }
        } finally {
            decoder.recycle()
        }
        val mergedLines = mergeOverlappingLines(chunks.flatMap(RecognitionResult::lines))
        return RecognitionResult(
            rawText = mergedLines.joinToString("\n", transform = RecognizedLine::text),
            lines = mergedLines,
        )
    }

    private suspend fun recognizeBitmap(
        bitmap: Bitmap,
        verticalOffset: Int,
    ): RecognitionResult {
        val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        return RecognitionResult(
            rawText = result.text,
            lines = result.toRecognizedLines(verticalOffset),
        )
    }

    private fun Text.toRecognizedLines(verticalOffset: Int): List<RecognizedLine> =
        textBlocks.flatMap { block -> block.lines }
            .mapNotNull { line ->
                val text = line.text.trim()
                if (text.isEmpty()) return@mapNotNull null
                RecognizedLine(
                    text = text,
                    confidence = line.confidence.takeIf { it in 0f..1f },
                    boundingBox = line.boundingBox?.let { rect ->
                        EvidenceBoundingBox(
                            left = rect.left,
                            top = rect.top + verticalOffset,
                            right = rect.right,
                            bottom = rect.bottom + verticalOffset,
                        )
                    },
                )
            }

    private fun mergeOverlappingLines(lines: List<RecognizedLine>): List<RecognizedLine> {
        val sorted = lines.sortedWith(
            compareBy<RecognizedLine> { it.boundingBox?.top ?: Int.MAX_VALUE }
                .thenBy { it.boundingBox?.left ?: Int.MAX_VALUE },
        )
        return buildList {
            sorted.forEach { candidate ->
                if (none { existing -> existing.isSameOverlapLine(candidate) }) {
                    add(candidate)
                }
            }
        }
    }

    private fun RecognizedLine.isSameOverlapLine(other: RecognizedLine): Boolean {
        if (text.normalizedForOverlap() != other.text.normalizedForOverlap()) return false
        val first = boundingBox ?: return false
        val second = other.boundingBox ?: return false
        val intersectionWidth = max(0, min(first.right, second.right) - max(first.left, second.left))
        val intersectionHeight = max(0, min(first.bottom, second.bottom) - max(first.top, second.top))
        val intersectionArea = intersectionWidth.toLong() * intersectionHeight.toLong()
        val firstArea = max(1, first.right - first.left).toLong() *
            max(1, first.bottom - first.top).toLong()
        val secondArea = max(1, second.right - second.left).toLong() *
            max(1, second.bottom - second.top).toLong()
        return intersectionArea * 2 >= min(firstArea, secondArea)
    }

    private fun String.normalizedForOverlap(): String =
        filterNot(Char::isWhitespace)

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
        const val DEFAULT_SEGMENT_OVERLAP_PX = 320
        const val MIN_SEGMENT_HEIGHT_PX = 256
    }

    private data class RecognitionResult(
        val rawText: String,
        val lines: List<RecognizedLine>,
    )

    private data class RecognizedLine(
        val text: String,
        val confidence: Float?,
        val boundingBox: EvidenceBoundingBox?,
    )
}
