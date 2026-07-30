package com.nanzhufeng.nanfengbazi.domain.model

import java.time.Instant
import org.junit.Assert.assertThrows
import org.junit.Test

class CaseModelsTest {
    @Test
    fun `未提供和用户清空不能携带伪值`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExplicitText(FieldValueState.CLEARED, "不应保留")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExplicitText(FieldValueState.PRESENT, " ")
        }
    }

    @Test
    fun `附件路径拒绝路径穿越和反斜杠`() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceAttachment(
                id = "attachment",
                relativePath = "../outside.png",
                originalFileName = "脱敏.png",
                mimeType = "image/png",
                sha256 = "a".repeat(64),
                byteSize = 1,
                createdAt = Instant.EPOCH,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            SourceAttachment(
                id = "attachment",
                relativePath = "case\\outside.png",
                originalFileName = "脱敏.png",
                mimeType = "image/png",
                sha256 = "a".repeat(64),
                byteSize = 1,
                createdAt = Instant.EPOCH,
            )
        }
    }

    @Test
    fun `字段证据不能引用当前命例之外的附件`() {
        val base = BirthInput(
            calendarInput = BirthCalendarInput.Solar(
                CivilDateTime(2000, 1, 1, 0, 0, 0),
            ),
            sexForFortuneDirection = SexForFortuneDirection.MAN,
            timePrecision = TimePrecision.EXACT_TO_SECOND,
        )
        assertThrows(IllegalArgumentException::class.java) {
            BaziCase(
                id = "case",
                alias = "脱敏",
                name = ExplicitText.absent(),
                sexForFortuneDirection = SexForFortuneDirection.MAN,
                sourceType = CaseSourceType.MANUAL,
                birthInput = base,
                fieldEvidence = listOf(
                    CaseFieldEvidence(
                        id = "evidence",
                        attachmentId = "missing",
                        fieldKey = "name",
                        rawText = "脱敏",
                        parserConfidence = 1f,
                        parserRuleId = "test",
                        userEdited = false,
                        createdAt = Instant.EPOCH,
                    ),
                ),
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            )
        }
    }
}
