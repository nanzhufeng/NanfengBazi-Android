package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.AnnualFortune
import com.nanzhufeng.nanfengbazi.domain.model.BasicChartDetails
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
import com.nanzhufeng.nanfengbazi.domain.model.HiddenStemDetail
import com.nanzhufeng.nanfengbazi.domain.model.PillarDetail
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermPoint
import com.nanzhufeng.nanfengbazi.domain.model.SolarTermType
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseObjectiveSummaryContractTest {
    @Test
    fun `唯一采用快照生成稳定章节且精确交运读取 endAt`() {
        val source = caseWithSnapshots(
            listOf(snapshot("old", adopted = false), snapshot("adopted", adopted = true)),
        )

        val first = CaseObjectiveSummaryContract.generate(
            CaseObjectiveSummaryInput(source),
        ) as CaseObjectiveSummaryResult.Success
        val second = CaseObjectiveSummaryContract.generate(
            CaseObjectiveSummaryInput(source),
        ) as CaseObjectiveSummaryResult.Success

        assertEquals(first, second)
        assertEquals(
            listOf(
                "birth_facts",
                "chart_facts",
                "fortune_facts",
                "calculation_evidence",
                "formal_record_index",
            ),
            first.summary.sections.map { it.id },
        )
        assertEquals("adopted", first.summary.adoptedSnapshotId)
        assertEquals(
            "2013-01-02 03:04:05",
            first.summary.field("fortune_facts", "精确交运时间").value,
        )
        assertFalse(first.summary.copyText.contains("old"))
        assertTrue(first.summary.copyText.contains("采用快照：adopted"))
        assertTrue(
            CaseObjectiveSummaryPolicy
                .findProhibitedGeneratedPhrases(first.summary)
                .isEmpty(),
        )
    }

    @Test
    fun `旧快照缺字段显式标记未记录且不反推补造`() {
        val legacySnapshot = snapshot("adopted", true).copy(
            result = calculationResult().copy(
                calendarConversion = null,
                basicChartDetails = null,
            ),
        )
        val result = CaseObjectiveSummaryContract.generate(
            CaseObjectiveSummaryInput(caseWithSnapshots(listOf(legacySnapshot))),
        ) as CaseObjectiveSummaryResult.Success

        listOf(
            "换算公历",
            "换算农历",
            "生肖",
            "星座",
            "日主",
            "前后节",
        ).forEach { label ->
            val field = result.summary.sections
                .flatMap { it.fields }
                .single { it.label == label }
            assertEquals(CaseObjectiveSummaryValueState.NOT_RECORDED, field.state)
            assertTrue(field.value.contains("未记录"))
        }
        listOf("当前大运", "当前流年").forEach { label ->
            val field = result.summary.sections
                .flatMap { it.fields }
                .single { it.label == label }
            assertEquals(CaseObjectiveSummaryValueState.NOT_RECORDED, field.state)
            assertTrue(field.value.contains("未生成"))
        }
    }

    @Test
    fun `研究资料只进入计数不把主观记录改写进摘要`() {
        val privateRecord = "这是一段只应保留在正式记录里的原文"
        val source = caseWithSnapshots(listOf(snapshot("adopted", true))).copy(
            textRecords = listOf(
                CaseTextRecord(
                    id = "record-1",
                    type = CaseTextRecordType.MASTER_COMMENTARY,
                    content = privateRecord,
                    createdAt = NOW,
                    updatedAt = NOW,
                ),
            ),
        )

        val result = CaseObjectiveSummaryContract.generate(
            CaseObjectiveSummaryInput(source),
        ) as CaseObjectiveSummaryResult.Success

        assertEquals(
            "1 条",
            result.summary.field("formal_record_index", "正式文本记录").value,
        )
        assertFalse(result.summary.copyText.contains(privateRecord))
    }

    @Test
    fun `观察时刻补齐年龄当前岁运前后五年和小运且原局关系可追溯`() {
        val source = caseWithSnapshots(listOf(snapshot("adopted", true)))
        val result = source.calculationSnapshots.single().result
        val currentDecade = result.decadeFortunes.last()
        val currentAnnual = result.annualFortunes.single { it.calendarYear == 2026 }
        val observedAt = CivilDateTime(2026, 8, 9, 12, 0, 0)
        val professional = ProfessionalFortunePosition(
            position = FortunePosition(
                observedAt = observedAt,
                annualFortune = currentAnnual.copy(
                    decadeIndex = result.decadeFortunes.size,
                    decadeName = currentDecade.name,
                ),
                decadeFortune = currentDecade,
                status = FortunePositionStatus.WITHIN_DECADE,
            ),
            flowPillars = FourPillars("丙午", "丙申", "乙巳", "壬午"),
            minorTimeline = listOf(
                ProfessionalTimelineItem(
                    key = "minor_0",
                    label = "2000",
                    subtitle = "0岁",
                    observedAt = CivilDateTime(2000, 1, 2, 3, 5, 0),
                    pillar = "戊辰",
                    stemTenGod = "食神",
                    heavenStemElement = "土",
                    earthBranchElement = "土",
                    selected = false,
                ),
            ),
            completedAge = 26,
            previousSolarTerm = term("立秋", CivilDateTime(2026, 8, 7, 12, 0, 0)),
            nextSolarTerm = term("处暑", CivilDateTime(2026, 8, 23, 12, 0, 0)),
            observationTimeMode = SolarTimeMode.CIVIL_TIME,
            profileId = result.profile.id,
            ruleVersion = result.profile.ruleVersion,
        )

        val summary = (
            CaseObjectiveSummaryContract.generate(
                CaseObjectiveSummaryInput(
                    caseData = source,
                    observation = CaseObjectiveSummaryObservation(
                        referenceDate = java.time.LocalDate.of(2026, 8, 9),
                        professionalPosition = professional,
                    ),
                ),
            ) as CaseObjectiveSummaryResult.Success
            ).summary

        assertEquals("丑（土）", summary.field("chart_facts", "月令").value)
        assertEquals("寅（木）", summary.field("chart_facts", "日支").value)
        assertTrue(summary.field("chart_facts", "表层五行计数").value.contains("木4"))
        assertTrue(summary.field("chart_facts", "表层五行缺失").value.contains("金"))
        assertTrue(summary.field("chart_facts", "原局地支关系").value.contains("子丑合"))
        assertEquals("26岁（截至2026-08-09）", summary.field("chart_facts", "当前实岁").value)
        assertTrue(summary.field("fortune_facts", "当前大运").value.contains(currentDecade.name))
        assertTrue(summary.field("fortune_facts", "当前流年").value.contains("2026"))
        assertTrue(summary.field("fortune_facts", "流年 2021").value.isNotBlank())
        assertTrue(summary.field("fortune_facts", "流年 2031").value.isNotBlank())
        assertTrue(summary.field("fortune_facts", "小运序列").value.contains("2000 戊辰 0岁"))
    }

    @Test
    fun `字段敏感级别不依赖中文标签`() {
        val summary = (
            CaseObjectiveSummaryContract.generate(
                CaseObjectiveSummaryInput(caseWithSnapshots(listOf(snapshot("adopted", true)))),
            ) as CaseObjectiveSummaryResult.Success
            ).summary

        assertEquals(
            CaseObjectiveSummarySensitivity.IDENTITY,
            summary.field("birth_facts", "命例别名").sensitivity,
        )
        assertEquals(
            CaseObjectiveSummarySensitivity.DEMOGRAPHIC,
            summary.field("birth_facts", "性别口径").sensitivity,
        )
        assertEquals(
            CaseObjectiveSummarySensitivity.PRECISE_BIRTH_TIME,
            summary.field("birth_facts", "出生历法与时间").sensitivity,
        )
    }

    @Test
    fun `没有采用快照返回结构化失败`() {
        val result = CaseObjectiveSummaryContract.generate(
            CaseObjectiveSummaryInput(caseWithSnapshots(listOf(snapshot("old", false)))),
        )

        assertEquals(
            CaseObjectiveSummaryErrorCode.NO_ADOPTED_SNAPSHOT,
            (result as CaseObjectiveSummaryResult.Rejected).failure.code,
        )
    }

    @Test
    fun `多个采用快照拒绝而不猜测最新项`() {
        val result = CaseObjectiveSummaryContract.generate(
            CaseObjectiveSummaryInput(
                caseWithSnapshots(
                    listOf(snapshot("first", true), snapshot("second", true)),
                ),
            ),
        )

        assertEquals(
            CaseObjectiveSummaryErrorCode.MULTIPLE_ADOPTED_SNAPSHOTS,
            (result as CaseObjectiveSummaryResult.Rejected).failure.code,
        )
    }

    @Test
    fun `未知摘要版本明确拒绝`() {
        val result = CaseObjectiveSummaryContract.generate(
            CaseObjectiveSummaryInput(
                caseData = caseWithSnapshots(listOf(snapshot("adopted", true))),
                summaryVersion = 999,
            ),
        )

        assertEquals(
            CaseObjectiveSummaryErrorCode.UNSUPPORTED_SUMMARY_VERSION,
            (result as CaseObjectiveSummaryResult.Rejected).failure.code,
        )
    }

    @Test
    fun `禁止性文案门禁扫描模板标签而不依赖页面`() {
        val valid = (
            CaseObjectiveSummaryContract.generate(
                CaseObjectiveSummaryInput(
                    caseWithSnapshots(listOf(snapshot("adopted", true))),
                ),
            ) as CaseObjectiveSummaryResult.Success
            ).summary
        val invalid = valid.copy(
            sections = valid.sections + CaseObjectiveSummarySection(
                id = "invalid",
                title = "合婚结论",
                fields = listOf(
                    CaseObjectiveSummaryField(
                        label = "测试",
                        value = "仅用于门禁测试",
                        source = CaseObjectiveSummarySource.CALCULATION_EVIDENCE,
                    ),
                ),
            ),
        )

        assertEquals(
            listOf("合婚"),
            CaseObjectiveSummaryPolicy.findProhibitedGeneratedPhrases(invalid),
        )
    }

    private fun CaseObjectiveSummary.field(
        sectionId: String,
        label: String,
    ): CaseObjectiveSummaryField = sections
        .single { it.id == sectionId }
        .fields
        .single { it.label == label }

    private fun caseWithSnapshots(
        snapshots: List<CaseCalculationSnapshot>,
    ): BaziCase = BaziCase(
        id = "case-objective-summary",
        alias = "合成命例",
        name = ExplicitText.absent(),
        sexForFortuneDirection = SexForFortuneDirection.WOMAN,
        sourceType = CaseSourceType.MANUAL,
        birthInput = birthInput(),
        calculationSnapshots = snapshots,
        createdAt = NOW,
        updatedAt = NOW,
        revision = 7,
    )

    private fun snapshot(id: String, adopted: Boolean): CaseCalculationSnapshot =
        CaseCalculationSnapshot(
            id = id,
            result = calculationResult(),
            adopted = adopted,
            createdAt = NOW,
        )

    private fun calculationResult(): CalculationResult = CalculationResult(
        normalizedInput = birthInput(),
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
            DecadeFortune("己巳", 14, 23, 2013, 2022),
            DecadeFortune("庚午", 24, 33, 2023, 2032),
        ),
        annualFortunes = (2021..2031).mapIndexed { index, year ->
            AnnualFortune(
                name = listOf(
                    "辛丑", "壬寅", "癸卯", "甲辰", "乙巳", "丙午",
                    "丁未", "戊申", "己酉", "庚戌", "辛亥",
                )[index],
                calendarYear = year,
                nominalAge = year - 2000 + 1,
            )
        },
        evidence = CalculationEvidence(
            engineName = "TestEngine",
            engineVersion = "test-1",
            ruleVersion = "test-rule-1",
            calculatedAt = NOW,
        ),
        basicChartDetails = basicChartDetails(),
    )

    private fun basicChartDetails(): BasicChartDetails = BasicChartDetails(
        zodiac = "龙",
        westernZodiac = "摩羯座",
        dayMaster = "丙",
        pillars = listOf(
            pillar(PillarPosition.YEAR, "甲子", "木", "水"),
            pillar(PillarPosition.MONTH, "乙丑", "木", "土"),
            pillar(PillarPosition.DAY, "丙寅", "火", "木"),
            pillar(PillarPosition.HOUR, "丁卯", "火", "木"),
        ),
        previousSolarTerm = term("冬至", CivilDateTime(1999, 12, 22, 0, 0, 0)),
        nextSolarTerm = term("小寒", CivilDateTime(2000, 1, 6, 0, 0, 0)),
    )

    private fun pillar(
        position: PillarPosition,
        name: String,
        stemElement: String,
        branchElement: String,
    ): PillarDetail = PillarDetail(
        position = position,
        name = name,
        heavenStem = name.take(1),
        earthBranch = name.takeLast(1),
        heavenStemElement = stemElement,
        earthBranchElement = branchElement,
        primaryTenGod = "测试十神",
        hiddenStems = listOf(HiddenStemDetail("甲", "本气", "测试十神", "木")),
        terrain = "长生",
        selfSittingTerrain = "长生",
        voidEarthBranches = listOf("戌", "亥"),
        naYin = "测试纳音",
    )

    private fun term(name: String, at: CivilDateTime): SolarTermPoint = SolarTermPoint(
        name = name,
        type = SolarTermType.JIE,
        at = at,
    )

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
