package com.nanzhufeng.nanfengbazi.domain.model

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportModelsTest {
    @Test
    fun `导入状态只允许显式前进失败恢复和取消`() {
        assertTrue(ImportStatus.WAITING.canTransitionTo(ImportStatus.COPYING_IMAGES))
        assertTrue(ImportStatus.RECOGNIZING.canTransitionTo(ImportStatus.FAILED))
        assertTrue(ImportStatus.FAILED.canTransitionTo(ImportStatus.RECOGNIZING))
        assertTrue(ImportStatus.NEEDS_REVIEW.canTransitionTo(ImportStatus.CANCELLED))
        assertFalse(ImportStatus.COPYING_IMAGES.canTransitionTo(ImportStatus.READY_TO_COMMIT))
        assertFalse(ImportStatus.COMPLETED.canTransitionTo(ImportStatus.NEEDS_REVIEW))
        assertFalse(ImportStatus.CANCELLED.canTransitionTo(ImportStatus.COPYING_IMAGES))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `字段证据不得引用会话之外的图片`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        ImportSession(
            id = "session-1",
            sourceApp = ImportSourceApp.WENZHEN_BAZI,
            status = ImportStatus.NEEDS_REVIEW,
            images = listOf(image("image-1", now)),
            extractedFields = listOf(
                CaseFieldEvidence(
                    id = "field-1",
                    attachmentId = "image-outside",
                    fieldKey = "name",
                    rawText = "合成姓名",
                    parserConfidence = 0.9f,
                    parserRuleId = "fixture-v1",
                    userEdited = false,
                    createdAt = now,
                ),
            ),
            parserVersion = "fixture-v1",
            createdAt = now,
            updatedAt = now,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `完成会话必须记录结束时间`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        ImportSession(
            id = "session-1",
            sourceApp = ImportSourceApp.WENZHEN_BAZI,
            status = ImportStatus.COMPLETED,
            parserVersion = "fixture-v1",
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun image(
        id: String,
        now: Instant,
    ) = ImportImageRef(
        id = id,
        originalFileName = "synthetic.png",
        mimeType = "image/png",
        relativePath = "session-1/$id.png",
        sha256 = "a".repeat(64),
        byteSize = 16,
        createdAt = now,
    )
}
