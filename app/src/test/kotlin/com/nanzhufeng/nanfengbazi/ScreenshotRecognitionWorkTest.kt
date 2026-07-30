package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotRecognitionWorkTest {
    @Test
    fun `较大图片批次按数量字节或像素进入前台而小批次保持后台`() {
        assertFalse(
            recognitionNeedsForeground(
                List(7) { index -> image("small-$index", 1_000, 1_000, 1_000_000) },
            ),
        )
        assertTrue(
            recognitionNeedsForeground(
                List(8) { index -> image("count-$index", 1_000, 1_000, 1_000_000) },
            ),
        )
        assertTrue(
            recognitionNeedsForeground(
                listOf(image("bytes", 1_000, 1_000, 32L * 1024L * 1024L)),
            ),
        )
        assertTrue(
            recognitionNeedsForeground(
                listOf(
                    image("pixels-1", 6_000, 4_000, 1_000_000),
                    image("pixels-2", 6_000, 4_000, 1_000_000),
                ),
            ),
        )
    }

    private fun image(
        id: String,
        width: Int,
        height: Int,
        byteSize: Long,
    ) = ImportImageRef(
        id = id,
        originalFileName = "$id.png",
        mimeType = "image/png",
        relativePath = "session/$id.png",
        sha256 = "a".repeat(64),
        byteSize = byteSize,
        widthPx = width,
        heightPx = height,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
