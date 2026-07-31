package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.FortuneStart
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseImageExportContractTest {
    @Test
    fun `导出事实只读取唯一采用快照并保留正式长记录`() {
        val longRecord = "研究记录".repeat(600)
        val source = caseWithSnapshots(
            snapshots = listOf(snapshot("old", adopted = false), snapshot("adopted", adopted = true)),
        ).copy(
            name = ExplicitText.absent(),
            textRecords = listOf(
                CaseTextRecord(
                    id = "record-1",
                    type = CaseTextRecordType.MASTER_COMMENTARY,
                    content = longRecord,
                    createdAt = NOW,
                    updatedAt = NOW,
                ),
            ),
        )

        val result = CaseImageExportContract.prepare(CaseImageExportInput(source))

        assertTrue(result is CaseImageFactsResult.Prepared)
        val facts = (result as CaseImageFactsResult.Prepared).facts
        assertEquals(CASE_IMAGE_DOCUMENT_VERSION, facts.documentVersion)
        assertEquals("adopted", facts.adoptedSnapshotId)
        assertEquals(source.revision, facts.caseRevision)
        assertTrue(
            facts.blocks
                .filterIsInstance<CaseImageRenderBlock.Paragraphs>()
                .flatMap { it.paragraphs }
                .any { longRecord in it },
        )
        assertTrue(facts.provenanceNotice.contains("未采用候选和旧快照未作为计算真值"))
        assertFalse(facts.blocks.toString().contains("old"))
    }

    @Test
    fun `缺失姓名按明确空值渲染而不是制造姓名`() {
        val result = CaseImageExportContract.prepare(
            CaseImageExportInput(caseWithSnapshots(listOf(snapshot("adopted", true)))),
        ) as CaseImageFactsResult.Prepared

        val identity = result.facts.blocks
            .filterIsInstance<CaseImageRenderBlock.Rows>()
            .first { it.title == "采用资料" }
        assertEquals("未提供", identity.rows.single { it.label == "姓名" }.value)
    }

    @Test
    fun `没有采用快照返回结构化失败`() {
        val result = CaseImageExportContract.prepare(
            CaseImageExportInput(caseWithSnapshots(listOf(snapshot("old", false)))),
        )

        assertEquals(
            CaseImageExportErrorCode.NO_ADOPTED_SNAPSHOT,
            (result as CaseImageFactsResult.Rejected).failure.code,
        )
    }

    @Test
    fun `多个采用快照拒绝而不猜测最新项`() {
        val result = CaseImageExportContract.prepare(
            CaseImageExportInput(
                caseWithSnapshots(
                    listOf(snapshot("first", true), snapshot("second", true)),
                ),
            ),
        )

        assertEquals(
            CaseImageExportErrorCode.MULTIPLE_ADOPTED_SNAPSHOTS,
            (result as CaseImageFactsResult.Rejected).failure.code,
        )
    }

    @Test
    fun `未知文档版本明确拒绝`() {
        val result = CaseImageExportContract.prepare(
            CaseImageExportInput(
                caseData = caseWithSnapshots(listOf(snapshot("adopted", true))),
                documentVersion = 999,
            ),
        )

        assertEquals(
            CaseImageExportErrorCode.UNSUPPORTED_DOCUMENT_VERSION,
            (result as CaseImageFactsResult.Rejected).failure.code,
        )
    }

    private fun caseWithSnapshots(
        snapshots: List<CaseCalculationSnapshot>,
    ): BaziCase {
        val input = birthInput()
        return BaziCase(
            id = "case-image-export",
            alias = "合成命例",
            name = ExplicitText.absent(),
            sexForFortuneDirection = SexForFortuneDirection.WOMAN,
            sourceType = CaseSourceType.MANUAL,
            birthInput = input,
            calculationSnapshots = snapshots,
            createdAt = NOW,
            updatedAt = NOW,
            revision = 7,
        )
    }

    private fun snapshot(id: String, adopted: Boolean): CaseCalculationSnapshot =
        CaseCalculationSnapshot(
            id = id,
            result = calculationResult(),
            adopted = adopted,
            createdAt = NOW,
        )

    private fun calculationResult(): CalculationResult {
        val input = birthInput()
        return CalculationResult(
            normalizedInput = input,
            profile = CalculationProfile.tymeDefault(),
            fourPillars = FourPillars("甲子", "乙丑", "丙寅", "丁卯"),
            ownSign = "戊辰",
            bodySign = "己巳",
            fetalOrigin = "庚午",
            fetalBreath = "辛未",
            fortuneStart = FortuneStart(
                direction = FortuneDirection.FORWARD,
                startAt = CivilDateTime(2003, 1, 2, 3, 4, 5),
                endAt = CivilDateTime(2013, 1, 2, 3, 4, 5),
                years = 3,
                months = 0,
                days = 0,
                hours = 0,
                minutes = 0,
            ),
            decadeFortunes = listOf(
                DecadeFortune("戊辰", 4, 13, 2003, 2012),
            ),
            evidence = CalculationEvidence(
                engineName = "TestEngine",
                engineVersion = "test-1",
                ruleVersion = "test-rule-1",
                calculatedAt = NOW,
            ),
        )
    }

    private fun birthInput(): BirthInput = BirthInput(
        calendarInput = BirthCalendarInput.Solar(
            CivilDateTime(2000, 1, 2, 3, 4, 5),
        ),
        sexForFortuneDirection = SexForFortuneDirection.WOMAN,
        timePrecision = TimePrecision.EXACT_TO_SECOND,
        resolvedUtcOffsetSeconds = 28_800,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-07-31T00:00:00Z")
    }
}
