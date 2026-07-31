package com.nanzhufeng.nanfengbazi

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.nanzhufeng.nanfengbazi.domain.CaseImageExportContract
import com.nanzhufeng.nanfengbazi.domain.CaseImageExportErrorCode
import com.nanzhufeng.nanfengbazi.domain.CaseImageExportFailure
import com.nanzhufeng.nanfengbazi.domain.CaseImageExportInput
import com.nanzhufeng.nanfengbazi.domain.CaseImageFactsResult
import com.nanzhufeng.nanfengbazi.domain.CaseImageRenderBlock
import com.nanzhufeng.nanfengbazi.domain.CaseImageRenderFacts
import com.nanzhufeng.nanfengbazi.domain.CaseImageRenderResult
import com.nanzhufeng.nanfengbazi.domain.CaseImageRenderer
import com.nanzhufeng.nanfengbazi.domain.RenderedCaseImage
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidCaseImageRenderer(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CaseImageRenderer {
    override suspend fun render(input: CaseImageExportInput): CaseImageRenderResult =
        withContext(dispatcher) {
            when (val prepared = CaseImageExportContract.prepare(input)) {
                is CaseImageFactsResult.Rejected ->
                    CaseImageRenderResult.Rejected(prepared.failure)

                is CaseImageFactsResult.Prepared -> renderFacts(prepared.facts)
            }
        }

    private fun renderFacts(facts: CaseImageRenderFacts): CaseImageRenderResult {
        return try {
            val theme = RenderTheme()
            val layout = buildLayout(facts, theme)
            if (layout.height > MAX_IMAGE_HEIGHT || layout.height * IMAGE_WIDTH > MAX_PIXELS) {
                return CaseImageRenderResult.Rejected(
                    CaseImageExportFailure(
                        CaseImageExportErrorCode.CONTENT_TOO_LARGE,
                        "命例内容超过单张长图上限，请减少正式记录后重试。",
                    ),
                )
            }
            val bitmap = Bitmap.createBitmap(
                IMAGE_WIDTH,
                layout.height.coerceAtLeast(MIN_IMAGE_HEIGHT),
                Bitmap.Config.RGB_565,
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            drawHeader(canvas, facts, theme)
            layout.items.forEach { item -> item.draw(canvas, theme) }
            val output = ByteArrayOutputStream()
            val compressed = try {
                bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)
            } finally {
                bitmap.recycle()
            }
            if (!compressed) {
                CaseImageRenderResult.Rejected(
                    CaseImageExportFailure(
                        CaseImageExportErrorCode.RENDER_FAILED,
                        "系统未能编码命盘图片。",
                    ),
                )
            } else {
                val bytes = output.toByteArray()
                CaseImageRenderResult.Success(
                    RenderedCaseImage(
                        facts = facts,
                        mimeType = MIME_TYPE,
                        fileExtension = "png",
                        bytes = bytes,
                        widthPixels = IMAGE_WIDTH,
                        heightPixels = layout.height.coerceAtLeast(MIN_IMAGE_HEIGHT),
                        sha256 = bytes.sha256(),
                    ),
                )
            }
        } catch (_: OutOfMemoryError) {
            CaseImageRenderResult.Rejected(
                CaseImageExportFailure(
                    CaseImageExportErrorCode.CONTENT_TOO_LARGE,
                    "设备内存不足，无法生成这张长图；请减少正式记录后重试。",
                ),
            )
        } catch (_: Exception) {
            CaseImageRenderResult.Rejected(
                CaseImageExportFailure(
                    CaseImageExportErrorCode.RENDER_FAILED,
                    "命盘图片生成失败，当前命例未被修改。",
                ),
            )
        }
    }

    private fun buildLayout(
        facts: CaseImageRenderFacts,
        theme: RenderTheme,
    ): RenderLayout {
        var top = HEADER_HEIGHT
        val items = buildList {
            facts.blocks.forEach { block ->
                val section = SectionItem(
                    top = top,
                    title = block.title,
                    titleLines = wrapText(block.title, theme.sectionPaint, CONTENT_WIDTH),
                )
                add(section)
                top += section.height + ITEM_GAP
                when (block) {
                    is CaseImageRenderBlock.Rows -> block.rows.forEach { row ->
                        val item = RowItem(
                            top = top,
                            labelLines = wrapText(row.label, theme.labelPaint, CONTENT_WIDTH),
                            valueLines = wrapText(row.value, theme.bodyPaint, CONTENT_WIDTH),
                        )
                        add(item)
                        top += item.height + ITEM_GAP
                    }

                    is CaseImageRenderBlock.Paragraphs -> block.paragraphs.forEach { paragraph ->
                        val item = ParagraphItem(
                            top = top,
                            lines = wrapText(paragraph, theme.bodyPaint, CONTENT_WIDTH - 20),
                        )
                        add(item)
                        top += item.height + ITEM_GAP
                    }
                }
                top += SECTION_GAP
            }
            val provenance = NoticeItem(
                top = top,
                label = "来源边界",
                lines = wrapText(facts.provenanceNotice, theme.noticePaint, CONTENT_WIDTH - 24),
                accent = theme.green,
            )
            add(provenance)
            top += provenance.height + ITEM_GAP
            val privacy = NoticeItem(
                top = top,
                label = "隐私提示",
                lines = wrapText(facts.privacyNotice, theme.noticePaint, CONTENT_WIDTH - 24),
                accent = theme.orange,
            )
            add(privacy)
            top += privacy.height + BOTTOM_PADDING
        }
        return RenderLayout(items = items, height = top)
    }

    private fun drawHeader(
        canvas: Canvas,
        facts: CaseImageRenderFacts,
        theme: RenderTheme,
    ) {
        canvas.drawRoundRect(
            RectF(
                PAGE_PADDING.toFloat(),
                PAGE_PADDING.toFloat(),
                (IMAGE_WIDTH - PAGE_PADDING).toFloat(),
                (HEADER_HEIGHT - 28).toFloat(),
            ),
            32f,
            32f,
            theme.headerBackgroundPaint,
        )
        canvas.drawText(
            facts.title.take(MAX_HEADER_CHARS),
            (PAGE_PADDING + 34).toFloat(),
            (PAGE_PADDING + 70).toFloat(),
            theme.titlePaint,
        )
        canvas.drawText(
            facts.subtitle,
            (PAGE_PADDING + 34).toFloat(),
            (PAGE_PADDING + 118).toFloat(),
            theme.subtitlePaint,
        )
        canvas.drawText(
            "v${facts.documentVersion} · 修订 ${facts.caseRevision}",
            (PAGE_PADDING + 34).toFloat(),
            (PAGE_PADDING + 158).toFloat(),
            theme.metaPaint,
        )
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        return text.split('\n').flatMap { paragraph ->
            if (paragraph.isEmpty()) {
                listOf("")
            } else {
                buildList {
                    var remaining = paragraph
                    while (remaining.isNotEmpty()) {
                        val count = paint.breakText(
                            remaining,
                            true,
                            maxWidth.toFloat(),
                            null,
                        ).coerceAtLeast(1)
                        add(remaining.take(count))
                        remaining = remaining.drop(count)
                    }
                }
            }
        }
    }

    private data class RenderLayout(
        val items: List<LayoutItem>,
        val height: Int,
    )

    private sealed interface LayoutItem {
        val top: Int
        val height: Int
        fun draw(canvas: Canvas, theme: RenderTheme)
    }

    private data class SectionItem(
        override val top: Int,
        val title: String,
        val titleLines: List<String>,
    ) : LayoutItem {
        override val height: Int = 60 + titleLines.size * SECTION_LINE_HEIGHT

        override fun draw(canvas: Canvas, theme: RenderTheme) {
            canvas.drawRoundRect(
                RectF(
                    PAGE_PADDING.toFloat(),
                    top.toFloat(),
                    (IMAGE_WIDTH - PAGE_PADDING).toFloat(),
                    (top + height).toFloat(),
                ),
                22f,
                22f,
                theme.sectionBackgroundPaint,
            )
            titleLines.forEachIndexed { index, line ->
                canvas.drawText(
                    line,
                    (PAGE_PADDING + 24).toFloat(),
                    (top + 44 + index * SECTION_LINE_HEIGHT).toFloat(),
                    theme.sectionPaint,
                )
            }
        }
    }

    private data class RowItem(
        override val top: Int,
        val labelLines: List<String>,
        val valueLines: List<String>,
    ) : LayoutItem {
        override val height: Int =
            CARD_VERTICAL_PADDING * 2 +
                labelLines.size * LABEL_LINE_HEIGHT +
                10 +
                valueLines.size * BODY_LINE_HEIGHT

        override fun draw(canvas: Canvas, theme: RenderTheme) {
            drawCard(canvas, theme, top, height)
            var baseline = top + CARD_VERTICAL_PADDING + LABEL_BASELINE
            labelLines.forEach { line ->
                canvas.drawText(
                    line,
                    (PAGE_PADDING + CARD_HORIZONTAL_PADDING).toFloat(),
                    baseline.toFloat(),
                    theme.labelPaint,
                )
                baseline += LABEL_LINE_HEIGHT
            }
            baseline += 10
            valueLines.forEach { line ->
                canvas.drawText(
                    line,
                    (PAGE_PADDING + CARD_HORIZONTAL_PADDING).toFloat(),
                    baseline.toFloat(),
                    theme.bodyPaint,
                )
                baseline += BODY_LINE_HEIGHT
            }
        }
    }

    private data class ParagraphItem(
        override val top: Int,
        val lines: List<String>,
    ) : LayoutItem {
        override val height: Int =
            CARD_VERTICAL_PADDING * 2 + lines.size * BODY_LINE_HEIGHT

        override fun draw(canvas: Canvas, theme: RenderTheme) {
            drawCard(canvas, theme, top, height)
            canvas.drawCircle(
                (PAGE_PADDING + CARD_HORIZONTAL_PADDING).toFloat(),
                (top + CARD_VERTICAL_PADDING + 14).toFloat(),
                6f,
                theme.orangePaint,
            )
            var baseline = top + CARD_VERTICAL_PADDING + BODY_BASELINE
            lines.forEach { line ->
                canvas.drawText(
                    line,
                    (PAGE_PADDING + CARD_HORIZONTAL_PADDING + 20).toFloat(),
                    baseline.toFloat(),
                    theme.bodyPaint,
                )
                baseline += BODY_LINE_HEIGHT
            }
        }
    }

    private data class NoticeItem(
        override val top: Int,
        val label: String,
        val lines: List<String>,
        val accent: Int,
    ) : LayoutItem {
        override val height: Int =
            CARD_VERTICAL_PADDING * 2 + LABEL_LINE_HEIGHT + 8 + lines.size * NOTICE_LINE_HEIGHT

        override fun draw(canvas: Canvas, theme: RenderTheme) {
            drawCard(canvas, theme, top, height)
            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
            canvas.drawRoundRect(
                RectF(
                    PAGE_PADDING.toFloat(),
                    top.toFloat(),
                    (PAGE_PADDING + 10).toFloat(),
                    (top + height).toFloat(),
                ),
                8f,
                8f,
                accentPaint,
            )
            canvas.drawText(
                label,
                (PAGE_PADDING + CARD_HORIZONTAL_PADDING).toFloat(),
                (top + CARD_VERTICAL_PADDING + LABEL_BASELINE).toFloat(),
                theme.labelPaint,
            )
            var baseline =
                top + CARD_VERTICAL_PADDING + LABEL_LINE_HEIGHT + 8 + NOTICE_BASELINE
            lines.forEach { line ->
                canvas.drawText(
                    line,
                    (PAGE_PADDING + CARD_HORIZONTAL_PADDING).toFloat(),
                    baseline.toFloat(),
                    theme.noticePaint,
                )
                baseline += NOTICE_LINE_HEIGHT
            }
        }
    }

    private class RenderTheme {
        val green: Int = Color.rgb(31, 122, 93)
        val orange: Int = Color.rgb(230, 126, 34)
        private val text: Int = Color.rgb(34, 42, 48)
        private val muted: Int = Color.rgb(91, 103, 112)
        private val sans: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        private val sansMedium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        val titlePaint = textPaint(46f, text, sansMedium)
        val subtitlePaint = textPaint(26f, green, sansMedium)
        val metaPaint = textPaint(20f, muted, sans)
        val sectionPaint = textPaint(31f, text, sansMedium)
        val labelPaint = textPaint(23f, green, sansMedium)
        val bodyPaint = textPaint(25f, text, sans)
        val noticePaint = textPaint(21f, muted, sans)
        val headerBackgroundPaint = fillPaint(Color.rgb(240, 249, 245))
        val sectionBackgroundPaint = fillPaint(Color.rgb(247, 249, 250))
        val cardBackgroundPaint = fillPaint(Color.rgb(251, 252, 252))
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 229, 232)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val orangePaint = fillPaint(orange)

        private fun textPaint(size: Float, colorValue: Int, typefaceValue: Typeface): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                color = colorValue
                textSize = size
                typeface = typefaceValue
            }

        private fun fillPaint(colorValue: Int): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorValue
                style = Paint.Style.FILL
            }
    }

    private companion object {
        const val IMAGE_WIDTH = 1080
        const val MAX_IMAGE_HEIGHT = 24_000
        const val MAX_PIXELS = 26_000_000
        const val MIN_IMAGE_HEIGHT = 1350
        const val PAGE_PADDING = 54
        const val BOTTOM_PADDING = 70
        const val CARD_HORIZONTAL_PADDING = 28
        const val CARD_VERTICAL_PADDING = 22
        const val CONTENT_WIDTH = IMAGE_WIDTH - PAGE_PADDING * 2 - CARD_HORIZONTAL_PADDING * 2
        const val HEADER_HEIGHT = 260
        const val ITEM_GAP = 14
        const val SECTION_GAP = 22
        const val SECTION_LINE_HEIGHT = 40
        const val LABEL_LINE_HEIGHT = 32
        const val BODY_LINE_HEIGHT = 38
        const val NOTICE_LINE_HEIGHT = 31
        const val LABEL_BASELINE = 24
        const val BODY_BASELINE = 27
        const val NOTICE_BASELINE = 23
        const val PNG_QUALITY = 100
        const val MAX_HEADER_CHARS = 24
        const val MIME_TYPE = "image/png"

        fun drawCard(canvas: Canvas, theme: RenderTheme, top: Int, height: Int) {
            val rect = RectF(
                PAGE_PADDING.toFloat(),
                top.toFloat(),
                (IMAGE_WIDTH - PAGE_PADDING).toFloat(),
                (top + height).toFloat(),
            )
            canvas.drawRoundRect(rect, 20f, 20f, theme.cardBackgroundPaint)
            canvas.drawRoundRect(rect, 20f, 20f, theme.cardBorderPaint)
        }
    }
}

private fun ByteArray.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(it) }
