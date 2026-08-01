package com.nanzhufeng.nanfengbazi.imageparser

import android.graphics.BitmapFactory
import android.graphics.Rect
import com.nanzhufeng.nanfengbazi.domain.model.EvidenceBoundingBox
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.OcrTextBlock
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType

class WenzhenUserListOcrRefiner(
    private val engine: MlKitChineseOcrEngine,
) : OcrDocumentRefiner {
    override suspend fun refine(
        input: OcrImageInput,
        pageType: WenzhenPageType,
        initialDocument: OcrDocument,
    ): OcrDocument {
        if (pageType != WenzhenPageType.USER_LIST) return initialDocument
        val dimensions = readDimensions(input.bytes)
        val anchors = initialDocument.blocks
            .mapNotNull { block ->
                block.boundingBox
                    ?.takeIf { SOLAR_DATE_PATTERN.containsMatchIn(block.text) }
            }
            .sortedBy(EvidenceBoundingBox::top)
            .deduplicateNearbyAnchors()
        if (anchors.isEmpty()) return initialDocument
        val rowBounds = anchors.mapIndexed { index, anchor ->
            val center = anchor.verticalCenter()
            val previousCenter = anchors.getOrNull(index - 1)?.verticalCenter()
            val nextCenter = anchors.getOrNull(index + 1)?.verticalCenter()
            val top = previousCenter?.let { (it + center) / 2 }
                ?: (center - (nextCenter?.minus(center)?.div(2) ?: FIRST_ROW_HEAD_PX))
            val bottom = nextCenter?.let { (center + it) / 2 }
                ?: center + (previousCenter?.let(center::minus)?.div(2) ?: LAST_ROW_TAIL_PX)
            top.coerceAtLeast(0) to bottom.coerceAtMost(dimensions.second)
        }
        val identityBlocks = engine.recognizeRegions(
            bytes = input.bytes,
            regions = rowBounds.map { (top, bottom) ->
                Rect(
                    0,
                    top,
                    (dimensions.first * CONTENT_RIGHT_RATIO).toInt(),
                    bottom,
                )
            },
            scale = IDENTITY_RECOGNITION_SCALE,
        )
        val pillarRegions = rowBounds.map { (top, bottom) ->
            Rect(
                (dimensions.first * PILLAR_LEFT_RATIO).toInt(),
                top,
                (dimensions.first * PILLAR_RIGHT_RATIO).toInt(),
                bottom,
            )
        }
        val pillarBlocks = PILLAR_WHITE_THRESHOLDS.flatMap { threshold ->
            engine.recognizeRegions(
                bytes = input.bytes,
                regions = pillarRegions,
                scale = PILLAR_RECOGNITION_SCALE,
                documentWhiteThreshold = threshold,
            )
        }
        val pillarColumnRegions = rowBounds.flatMap { (top, bottom) ->
            (0 until PILLAR_COLUMN_COUNT).map { columnIndex ->
                val leftRatio = PILLAR_COLUMN_LEFT_RATIO +
                    PILLAR_COLUMN_WIDTH_RATIO * columnIndex
                Rect(
                    (dimensions.first * leftRatio).toInt(),
                    top,
                    (dimensions.first * (leftRatio + PILLAR_COLUMN_WIDTH_RATIO)).toInt(),
                    bottom,
                )
            }
        }
        val pillarColumnBlocks = PILLAR_COLUMN_WHITE_THRESHOLDS.flatMap { threshold ->
            engine.recognizeRegions(
                bytes = input.bytes,
                regions = pillarColumnRegions,
                scale = PILLAR_COLUMN_RECOGNITION_SCALE,
                documentWhiteThreshold = threshold,
            )
        }
        val refinedBlocks = (identityBlocks + pillarBlocks + pillarColumnBlocks).filterNot { block ->
            SOLAR_DATE_PATTERN.containsMatchIn(block.text)
        }
        if (refinedBlocks.isEmpty()) return initialDocument
        val mergedBlocks = (initialDocument.blocks + refinedBlocks)
            .sortedWith(
                compareBy<OcrTextBlock> { it.boundingBox?.top ?: Int.MAX_VALUE }
                    .thenBy { it.boundingBox?.left ?: Int.MAX_VALUE },
            )
            .mapIndexed { index, block -> block.copy(id = "line-${index + 1}") }
        return initialDocument.copy(
            rawText = mergedBlocks.joinToString("\n", transform = OcrTextBlock::text),
            blocks = mergedBlocks,
        )
    }

    private fun readDimensions(bytes: ByteArray): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法解析精识别图片尺寸" }
        return bounds.outWidth to bounds.outHeight
    }

    private fun List<EvidenceBoundingBox>.deduplicateNearbyAnchors(): List<EvidenceBoundingBox> =
        buildList {
            this@deduplicateNearbyAnchors.forEach { candidate ->
                if (lastOrNull()?.verticalCenter()?.let { center ->
                        kotlin.math.abs(center - candidate.verticalCenter()) <= ANCHOR_DEDUPLICATION_PX
                    } != true
                ) {
                    add(candidate)
                }
            }
        }

    private fun EvidenceBoundingBox.verticalCenter(): Int = (top + bottom) / 2

    private companion object {
        const val CONTENT_RIGHT_RATIO = 0.84f
        const val PILLAR_LEFT_RATIO = 0.60f
        const val PILLAR_RIGHT_RATIO = 0.84f
        const val IDENTITY_RECOGNITION_SCALE = 3f
        const val PILLAR_RECOGNITION_SCALE = 4f
        const val PILLAR_COLUMN_RECOGNITION_SCALE = 6f
        const val PILLAR_COLUMN_COUNT = 4
        const val PILLAR_COLUMN_LEFT_RATIO = 0.60f
        const val PILLAR_COLUMN_WIDTH_RATIO = 0.05f
        const val ANCHOR_DEDUPLICATION_PX = 48
        const val FIRST_ROW_HEAD_PX = 120
        const val LAST_ROW_TAIL_PX = 140
        val PILLAR_WHITE_THRESHOLDS = listOf(220, 238, 248)
        val PILLAR_COLUMN_WHITE_THRESHOLDS = listOf(220, 238, 248)
        val SOLAR_DATE_PATTERN = Regex(
            "(?:阳历|公历)\\s*[:：]?\\s*\\d{4}[年./-]\\d{1,2}[月./-]\\d{1,2}日?",
        )
    }
}
