package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportImageDuplicateDetectorTest {
    @Test
    fun `SHA 相同优先判定为完全重复`() {
        val matches = ImportImageDuplicateDetector().findMatches(
            listOf(
                image("first", sha = "a".repeat(64), perceptualHash = "0000000000000000"),
                image("second", sha = "a".repeat(64), perceptualHash = "ffffffffffffffff"),
            ),
        )

        assertEquals(1, matches.size)
        assertEquals(DuplicateImageKind.EXACT, matches.single().kind)
        assertEquals(0, matches.single().perceptualDistance)
    }

    @Test
    fun `感知哈希小距离标记为相似而不是自动合并`() {
        val matches = ImportImageDuplicateDetector(similarDistanceThreshold = 2).findMatches(
            listOf(
                image("first", sha = "a".repeat(64), perceptualHash = "0000000000000000"),
                image("second", sha = "b".repeat(64), perceptualHash = "0000000000000003"),
                image("third", sha = "c".repeat(64), perceptualHash = "ffffffffffffffff"),
            ),
        )

        assertEquals(1, matches.size)
        assertEquals(DuplicateImageKind.VISUALLY_SIMILAR, matches.single().kind)
        assertEquals(2, matches.single().perceptualDistance)
    }

    private fun image(
        id: String,
        sha: String,
        perceptualHash: String,
    ) = ImportImageRef(
        id = id,
        originalFileName = "$id.png",
        mimeType = "image/png",
        relativePath = "session/$id.png",
        sha256 = sha,
        perceptualHash = perceptualHash,
        byteSize = 1,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
