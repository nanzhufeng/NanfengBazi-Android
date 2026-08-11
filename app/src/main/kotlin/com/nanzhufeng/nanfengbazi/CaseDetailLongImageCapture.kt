package com.nanzhufeng.nanfengbazi

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal data class CaseDetailPageCaptureTarget(
    val section: CaseDetailSection,
    val scrollState: ScrollState,
    val viewportBounds: Rect,
)

@Stable
internal class CaseDetailPageCaptureRegistry {
    private val targets = mutableMapOf<CaseDetailSection, CaseDetailPageCaptureTarget>()

    var target: CaseDetailPageCaptureTarget? by mutableStateOf(null)
        private set

    var pageBounds: Rect? by mutableStateOf(null)
        private set

    var notesCaptureActive: Boolean by mutableStateOf(false)
        private set

    /** The pager page that is actually visible in the detail viewport. */
    var displayedSection: CaseDetailSection? by mutableStateOf(null)
        private set

    fun update(
        section: CaseDetailSection,
        scrollState: ScrollState,
        viewportBounds: Rect,
    ) {
        val updated = CaseDetailPageCaptureTarget(section, scrollState, viewportBounds)
        targets[section] = updated
        target = updated
    }

    fun targetFor(section: CaseDetailSection): CaseDetailPageCaptureTarget? = targets[section]

    fun updatePageBounds(bounds: Rect) {
        pageBounds = bounds
    }

    fun updateNotesCaptureState(active: Boolean) {
        notesCaptureActive = active
    }

    fun updateDisplayedSection(section: CaseDetailSection) {
        displayedSection = section
    }
}

internal fun Modifier.registerCaseDetailPageBounds(
    registry: CaseDetailPageCaptureRegistry,
): Modifier = onGloballyPositioned { coordinates ->
    registry.updatePageBounds(coordinates.boundsInRoot())
}

internal fun Modifier.registerCaseDetailPageCaptureTarget(
    registry: CaseDetailPageCaptureRegistry,
    section: CaseDetailSection,
    scrollState: ScrollState,
): Modifier = onGloballyPositioned { coordinates ->
    registry.update(section, scrollState, coordinates.boundsInRoot())
}

internal data class CapturedCaseDetailLongImage(
    val bytes: ByteArray,
    val widthPixels: Int,
    val heightPixels: Int,
    val sha256: String,
)

internal data class CapturedCaseDetailLongImages(
    val chart: CapturedCaseDetailLongImage,
    val notes: CapturedCaseDetailLongImage,
)

internal sealed interface CaseDetailLongImageCaptureResult {
    data class Success(
        val images: CapturedCaseDetailLongImages,
    ) : CaseDetailLongImageCaptureResult

    data class Rejected(
        val message: String,
    ) : CaseDetailLongImageCaptureResult
}

internal suspend fun captureCaseDetailLongImage(
    rootView: View,
    registry: CaseDetailPageCaptureRegistry,
    originalSection: CaseDetailSection,
    selectSection: (CaseDetailSection) -> Unit,
    isSectionContentReady: (CaseDetailSection) -> Boolean = { true },
): CaseDetailLongImageCaptureResult {
    if (rootView.width <= 0 || rootView.height <= 0) {
        return CaseDetailLongImageCaptureResult.Rejected("详情页面尚未完成布局，请稍后重试。")
    }
    val originalScrollPositions = mutableMapOf<CaseDetailSection, Int>()
    try {
        val chart = captureSections(
            rootView = rootView,
            registry = registry,
            sections = listOf(
                CaseDetailSection.BASIC_INFO,
                CaseDetailSection.BASIC_CHART,
                CaseDetailSection.FORTUNE,
            ),
            originalScrollPositions = originalScrollPositions,
            selectSection = selectSection,
            isSectionContentReady = isSectionContentReady,
            label = "命盘三页",
        ) ?: return CaseDetailLongImageCaptureResult.Rejected("命盘三页真实排版超过单张长图上限。")

        selectSection(CaseDetailSection.RECORDS)
        registry.updateNotesCaptureState(true)
        val notes = captureSections(
            rootView = rootView,
            registry = registry,
            sections = listOf(CaseDetailSection.RECORDS),
            originalScrollPositions = originalScrollPositions,
            selectSection = { _ -> },
            isSectionContentReady = isSectionContentReady,
            label = "断事笔记",
        ) ?: return CaseDetailLongImageCaptureResult.Rejected("断事笔记真实排版超过单张长图上限。")

        return CaseDetailLongImageCaptureResult.Success(CapturedCaseDetailLongImages(chart, notes))
    } catch (_: OutOfMemoryError) {
        return CaseDetailLongImageCaptureResult.Rejected("命盘图片过长，设备内存不足。")
    } catch (_: Exception) {
        return CaseDetailLongImageCaptureResult.Rejected("真实页面长图生成失败，当前命例未被修改。")
    } finally {
        registry.updateNotesCaptureState(false)
        selectSection(originalSection)
        awaitStableFrames()
        registry.targetFor(originalSection)
            ?.scrollState
            ?.scrollTo(originalScrollPositions[originalSection] ?: 0)
    }
}

private suspend fun captureSections(
    rootView: View,
    registry: CaseDetailPageCaptureRegistry,
    sections: List<CaseDetailSection>,
    originalScrollPositions: MutableMap<CaseDetailSection, Int>,
    selectSection: (CaseDetailSection) -> Unit,
    isSectionContentReady: (CaseDetailSection) -> Boolean,
    beforeEachSection: (Int) -> Boolean = { true },
    label: String,
): CapturedCaseDetailLongImage? {
    val pieces = mutableListOf<Bitmap>()
    var totalHeight = 0
    try {
        sections.forEachIndexed { index, section ->
            selectSection(section)
            if (!beforeEachSection(index)) return null
            var target = awaitCaptureTarget(
                registry = registry,
                section = section,
                isSectionContentReady = isSectionContentReady,
            ) ?: return null
            originalScrollPositions.putIfAbsent(section, target.scrollState.value)
            target.scrollState.scrollTo(0)
            awaitStableFrames()
            target = registry.targetFor(section) ?: target
            val page = registry.pageBounds?.toPixelBounds(rootView) ?: return null
            val viewport = target.viewportBounds.toPixelBounds(rootView) ?: return null
            val outputWidth = page.width.coerceAtMost(MAX_CAPTURE_OUTPUT_WIDTH)
            fun append(piece: Bitmap): Boolean {
                val outputPiece = piece.scaleToWidth(outputWidth)
                if (outputPiece !== piece) piece.recycle()
                if (!appendPiece(pieces, outputPiece, outputWidth, totalHeight)) {
                    outputPiece.recycle()
                    return false
                }
                totalHeight += outputPiece.height
                return true
            }
            val firstRoot = rootView.captureBitmap()
            val firstPiece = firstRoot.crop(page.left, page.top, page.right, viewport.bottom)
            firstRoot.recycle()
            if (!append(firstPiece)) return null
            var previousOffset = 0
            while (previousOffset < target.scrollState.maxValue) {
                val nextOffset = (previousOffset + viewport.bottom - viewport.top)
                    .coerceAtMost(target.scrollState.maxValue)
                target.scrollState.scrollTo(nextOffset)
                awaitStableFrames()
                target = registry.targetFor(section) ?: target
                val currentViewport = target.viewportBounds.toPixelBounds(rootView) ?: viewport
                val root = rootView.captureBitmap()
                val strip = root.crop(
                    page.left,
                    (currentViewport.bottom - (nextOffset - previousOffset)).coerceAtLeast(currentViewport.top),
                    page.right,
                    currentViewport.bottom,
                )
                root.recycle()
                if (!append(strip)) return null
                previousOffset = nextOffset
            }
        }
        return withContext(Dispatchers.Default) {
            stitchAndEncode(pieces, pieces.firstOrNull()?.width ?: 0, totalHeight)
        }
    } finally {
        pieces.forEach { piece -> if (!piece.isRecycled) piece.recycle() }
        if (pieces.isNotEmpty() && totalHeight == 0) {
            error("$label 未捕获到页面内容")
        }
    }
}

private suspend fun awaitCaptureTarget(
    registry: CaseDetailPageCaptureRegistry,
    section: CaseDetailSection,
    isSectionContentReady: (CaseDetailSection) -> Boolean,
): CaseDetailPageCaptureTarget? {
    repeat(MAX_CONTENT_READY_FRAMES) {
        awaitStableFrames(frameCount = 1)
        registry.targetFor(section)?.takeIf { target ->
            registry.displayedSection == section &&
                isSectionContentReady(section) &&
            target.viewportBounds.width > 0f &&
                target.viewportBounds.height > 0f
        }?.let { return it }
    }
    return null
}

private suspend fun awaitStableFrames(frameCount: Int = 2) {
    repeat(frameCount) { withFrameNanos { } }
}

private data class PixelBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
}

private fun Rect.toPixelBounds(rootView: View): PixelBounds? {
    val leftPx = left.roundToInt().coerceIn(0, rootView.width)
    val topPx = top.roundToInt().coerceIn(0, rootView.height)
    val rightPx = right.roundToInt().coerceIn(0, rootView.width)
    val bottomPx = bottom.roundToInt().coerceIn(0, rootView.height)
    return if (rightPx > leftPx && bottomPx > topPx) {
        PixelBounds(leftPx, topPx, rightPx, bottomPx)
    } else {
        null
    }
}

private fun View.captureBitmap(): Bitmap = Bitmap.createBitmap(
    width,
    height,
    Bitmap.Config.ARGB_8888,
).also { bitmap -> draw(Canvas(bitmap)) }

private fun Bitmap.crop(left: Int, top: Int, right: Int, bottom: Int): Bitmap {
    val safeLeft = left.coerceIn(0, width - 1)
    val safeTop = top.coerceIn(0, height - 1)
    val cropWidth = (right - safeLeft).coerceAtLeast(1).coerceAtMost(width - safeLeft)
    val cropHeight = (bottom - safeTop).coerceAtLeast(1).coerceAtMost(height - safeTop)
    return Bitmap.createBitmap(this, safeLeft, safeTop, cropWidth, cropHeight)
}

private fun Bitmap.scaleToWidth(targetWidth: Int): Bitmap =
    if (width == targetWidth) this else Bitmap.createScaledBitmap(
        this,
        targetWidth,
        (height.toLong() * targetWidth / width).toInt().coerceAtLeast(1),
        true,
    )

private fun appendPiece(
    pieces: MutableList<Bitmap>,
    piece: Bitmap,
    width: Int,
    currentHeight: Int,
): Boolean {
    val nextHeight = currentHeight + piece.height
    if (nextHeight > MAX_IMAGE_HEIGHT || nextHeight.toLong() * width > MAX_IMAGE_PIXELS) {
        return false
    }
    pieces += piece
    return true
}

private fun stitchAndEncode(
    pieces: List<Bitmap>,
    width: Int,
    height: Int,
): CapturedCaseDetailLongImage? {
    if (pieces.isEmpty() || width <= 0 || height <= 0) return null
    val stitched = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(stitched)
    var top = 0f
    pieces.forEach { piece ->
        canvas.drawBitmap(piece, 0f, top, null)
        top += piece.height
        piece.recycle()
    }
    val output = ByteArrayOutputStream()
    return try {
        if (!stitched.compress(Bitmap.CompressFormat.PNG, 100, output)) return null
        val bytes = output.toByteArray()
        CapturedCaseDetailLongImage(
            bytes = bytes,
            widthPixels = width,
            heightPixels = height,
            sha256 = bytes.sha256(),
        )
    } finally {
        stitched.recycle()
    }
}

private fun rejectedAndRecycle(
    pieces: List<Bitmap>,
    message: String,
): CaseDetailLongImageCaptureResult.Rejected {
    pieces.forEach { piece -> if (!piece.isRecycled) piece.recycle() }
    return CaseDetailLongImageCaptureResult.Rejected(message)
}

private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { byte -> "%02x".format(byte) }

private const val MAX_CONTENT_READY_FRAMES = 180
private const val MAX_CAPTURE_OUTPUT_WIDTH = 1_140
private const val MAX_IMAGE_HEIGHT = 24_000
private const val MAX_IMAGE_PIXELS = 26_000_000L
