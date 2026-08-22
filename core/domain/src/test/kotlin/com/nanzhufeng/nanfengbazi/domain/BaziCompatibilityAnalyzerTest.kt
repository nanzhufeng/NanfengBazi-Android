package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaziCompatibilityAnalyzerTest {
    @Test
    fun `日主生克明确说明支持与制约方向`() {
        val born = BaziCompatibilityAnalyzer.analyze(
            chart("left", FourPillars("甲子", "乙丑", "甲寅", "丁卯")),
            chart("right", FourPillars("己丑", "庚寅", "丙卯", "壬辰")),
        ) as BaziCompatibilityResult.Ready
        val controlled = BaziCompatibilityAnalyzer.analyze(
            chart("left-control", FourPillars("甲子", "乙丑", "甲寅", "丁卯")),
            chart("right-controlled", FourPillars("己丑", "庚寅", "戊卯", "壬辰")),
        ) as BaziCompatibilityResult.Ready

        assertEquals("男方日主生女方", born.report.dayMasterRelation.description)
        assertTrue(born.report.dayMasterRelation.explanation.contains("输出支持"))
        assertEquals("男方日主克女方", controlled.report.dayMasterRelation.description)
        assertTrue(controlled.report.dayMasterRelation.explanation.contains("制约与主导"))
        assertNotEquals(
            born.report.relationshipSummary.relationshipJudgement,
            controlled.report.relationshipSummary.relationshipJudgement,
        )
        assertTrue(born.report.relationshipSummary.relationshipJudgement.contains("男方更容易主动承担"))
        assertTrue(controlled.report.relationshipSummary.relationshipJudgement.contains("男方更容易推动规则"))
        assertFalse(born.report.relationshipSummary.relationshipJudgement.contains("日主"))
        assertFalse(born.report.relationshipSummary.advantage.contains("地支"))
    }

    @Test
    fun `跨盘五合 六合 六冲均保留柱位证据`() {
        val result = BaziCompatibilityAnalyzer.analyze(
            chart("left", FourPillars("甲子", "乙卯", "丙寅", "丁辰")),
            chart("right", FourPillars("己丑", "庚酉", "辛午", "壬申")),
        ) as BaziCompatibilityResult.Ready

        assertTrue(result.report.coordinationSignals.any { it.kind == BaziCompatibilitySignalKind.STEM_FIVE_COMBINATION && it.values == "甲·己" })
        assertTrue(result.report.coordinationSignals.any { it.kind == BaziCompatibilitySignalKind.BRANCH_SIX_HARMONY && it.values == "子·丑" })
        assertTrue(result.report.tensionSignals.any { it.kind == BaziCompatibilitySignalKind.BRANCH_SIX_CLASH && it.values == "子·午" })
        val stemEvidence = result.report.coordinationSignals.first { it.kind == BaziCompatibilitySignalKind.STEM_FIVE_COMBINATION }
        assertEquals(2, stemEvidence.pillars.size)
        assertEquals(BaziCompatibilitySide.LEFT, stemEvidence.pillars.first().side)
        assertEquals(BaziCompatibilitySide.RIGHT, stemEvidence.pillars.last().side)
        val clashEvidence = result.report.tensionSignals.first {
            it.kind == BaziCompatibilitySignalKind.BRANCH_SIX_CLASH && it.values == "子·午"
        }
        assertTrue(clashEvidence.explanation.contains("男方年柱（成长背景、家庭根基）"))
        assertTrue(clashEvidence.explanation.contains("女方日柱（伴侣相处、亲密边界）"))
        assertTrue(clashEvidence.explanation.contains("需要注意"))
        assertTrue(clashEvidence.explanation.contains("不同节奏或偏好"))
        assertFalse(result.report.guidance.contains("适合"))
    }

    @Test
    fun `三合必须完整才显示成局 两支只显示候选`() {
        val partial = BaziCompatibilityAnalyzer.analyze(
            chart("left", FourPillars("甲申", "乙寅", "丙卯", "丁午")),
            chart("right", FourPillars("己子", "庚巳", "辛酉", "壬未")),
        ) as BaziCompatibilityResult.Ready
        assertTrue(partial.report.coordinationSignals.any { it.kind == BaziCompatibilitySignalKind.BRANCH_THREE_HARMONY_CANDIDATE && it.values == "申·子·辰" })
        assertFalse(partial.report.coordinationSignals.any { it.kind == BaziCompatibilitySignalKind.BRANCH_THREE_HARMONY && it.values == "申·子·辰" })

        val complete = BaziCompatibilityAnalyzer.analyze(
            chart("left", FourPillars("甲申", "乙寅", "丙卯", "丁午")),
            chart("right", FourPillars("己子", "庚辰", "辛酉", "壬未")),
        ) as BaziCompatibilityResult.Ready
        assertTrue(complete.report.coordinationSignals.any { it.kind == BaziCompatibilitySignalKind.BRANCH_THREE_HARMONY && it.values == "申·子·辰" })
    }

    @Test
    fun `五合 六合 六冲 六害 六破规则表逐项可命中`() {
        listOf("甲" to "己", "乙" to "庚", "丙" to "辛", "丁" to "壬", "戊" to "癸").forEach { (left, right) ->
            val result = analysisForYearPair(left, right, "子", "辰")
            assertTrue("$left·$right", result.coordinationSignals.any { it.kind == BaziCompatibilitySignalKind.STEM_FIVE_COMBINATION && it.values == "$left·$right" })
        }
        assertPairs(BaziCompatibilitySignalKind.BRANCH_SIX_HARMONY, listOf("子" to "丑", "寅" to "亥", "卯" to "戌", "辰" to "酉", "巳" to "申", "午" to "未"))
        assertPairs(BaziCompatibilitySignalKind.BRANCH_SIX_CLASH, listOf("子" to "午", "丑" to "未", "寅" to "申", "卯" to "酉", "辰" to "戌", "巳" to "亥"))
        assertPairs(BaziCompatibilitySignalKind.BRANCH_SIX_HARM, listOf("子" to "未", "丑" to "午", "寅" to "巳", "卯" to "辰", "申" to "亥", "酉" to "戌"))
        assertPairs(BaziCompatibilitySignalKind.BRANCH_SIX_BREAK, listOf("子" to "酉", "丑" to "辰", "寅" to "亥", "卯" to "午", "巳" to "申", "未" to "戌"))
    }

    @Test
    fun `时刻近似与口径不同只降级提示 不重算四柱`() {
        val result = BaziCompatibilityAnalyzer.analyze(
            chart("left", FourPillars("甲子", "乙丑", "丙寅", "丁卯"), TimePrecision.APPROXIMATE),
            chart("right", FourPillars("己丑", "庚寅", "辛卯", "壬辰"), profile = CalculationProfile.tymeDefault(ratHourRule = com.nanzhufeng.nanfengbazi.domain.model.RatHourRule.LATE_RAT_SAME_DAY)),
        ) as BaziCompatibilityResult.Ready

        assertTrue(result.report.warnings.any { it.code == BaziCompatibilityWarningCode.LEFT_TIME_NOT_EXACT })
        assertTrue(result.report.warnings.any { it.code == BaziCompatibilityWarningCode.CALCULATION_PROFILE_DIFFERENT })
        assertEquals("甲子", result.report.left.pillars.year)
    }

    @Test
    fun `合盘快照保留双方展示所需的出生时间与大运投影`() {
        val result = BaziCompatibilityAnalyzer.analyze(
            chart("left", FourPillars("甲子", "乙丑", "丙寅", "丁卯")),
            chart("right", FourPillars("己丑", "庚寅", "辛卯", "壬辰")),
        ) as BaziCompatibilityResult.Ready

        assertEquals("2000-01-02 03:04", result.report.left.solarDateTimeText)
        assertEquals(listOf("2000年", "1月", "2日", "3时"), result.report.left.solarDateValues)
        assertEquals("甲子", result.report.left.decadeFortunes.single().name)
        assertEquals("1–11实岁", result.report.left.decadeFortunes.single().ageRange)
        assertTrue(result.report.left.decadeFortunes.single().stemTenGod.isNotBlank())
        assertTrue(result.report.left.structuralProfileOrAnalyze().selectedPattern.name.isNotBlank())
        assertTrue(result.report.right.structuralProfileOrAnalyze().strengthEvidence.isNotEmpty())
    }

    @Test
    fun `历史合盘会按原快照补齐日期 十神 藏干和生肖`() {
        val left = chart("left", FourPillars("甲子", "乙丑", "丙寅", "丁卯"))
        val right = chart("right", FourPillars("己丑", "庚寅", "辛卯", "壬辰"))
        val report = (BaziCompatibilityAnalyzer.analyze(left, right) as BaziCompatibilityResult.Ready).report
        val legacy = report.copy(
            left = report.left.copy(
                solarDateTimeText = "",
                solarDateValues = emptyList(),
                zodiac = "",
                pillarPresentation = emptyList(),
                decadeFortunes = emptyList(),
            ),
            right = report.right.copy(
                solarDateTimeText = "",
                solarDateValues = emptyList(),
                zodiac = "",
                pillarPresentation = emptyList(),
                decadeFortunes = emptyList(),
            ),
        )

        val hydrated = BaziCompatibilityAnalyzer.hydrateHistoricalReport(legacy, left, right)

        assertEquals("2000-01-02 03:04", hydrated.left.solarDateTimeText)
        assertEquals(listOf("2000年", "1月", "2日", "3时"), hydrated.left.solarDateValues)
        assertEquals("鼠", hydrated.left.zodiac)
        assertEquals(4, hydrated.left.pillarPresentation.size)
        assertTrue(hydrated.left.pillarPresentation.all { it.primaryTenGod.isNotBlank() })
        assertTrue(hydrated.left.pillarPresentation.all { it.hiddenStemSummary.isNotBlank() })
        assertEquals("甲子", hydrated.left.decadeFortunes.single().name)
        assertEquals("1–11实岁", hydrated.left.decadeFortunes.single().ageRange)
    }

    @Test
    fun `历史合盘会将泛化关系说明更新为宫位含义`() {
        val left = chart("left", FourPillars("甲子", "乙丑", "丙寅", "丁卯"))
        val right = chart("right", FourPillars("己午", "庚申", "辛申", "壬辰"))
        val report = (BaziCompatibilityAnalyzer.analyze(left, right) as BaziCompatibilityResult.Ready).report
        val legacy = report.copy(
            tensionSignals = report.tensionSignals.map { signal ->
                signal.copy(explanation = "传统命理中的地支六冲结构，提示可提前沟通。")
            },
        )

        val hydrated = BaziCompatibilityAnalyzer.hydrateHistoricalReport(legacy, left, right)

        assertTrue(hydrated.tensionSignals.all { signal -> signal.explanation.contains("形成${signal.kind.title}") })
        assertFalse(hydrated.tensionSignals.any { signal -> signal.explanation.contains("传统命理中的") })
        assertTrue(hydrated.tensionSignals.any { signal -> signal.explanation.contains("亲密边界") })
    }

    @Test
    fun `合盘快照保留双方断事笔记供 AI 指令核验`() {
        val left = chart("left", FourPillars("甲子", "乙丑", "丙寅", "丁卯")).copy(
            textRecords = listOf(
                CaseTextRecord(
                    id = "left-feedback",
                    type = CaseTextRecordType.OWNER_FEEDBACK,
                    content = "2018 年换岗后收入明显上升。",
                    createdAt = NOW,
                    updatedAt = NOW,
                ),
            ),
        )
        val result = BaziCompatibilityAnalyzer.analyze(
            left,
            chart("right", FourPillars("己丑", "庚寅", "辛卯", "壬辰")),
        ) as BaziCompatibilityResult.Ready

        assertEquals("命主反馈", result.report.left.referenceNotes.single().label)
        assertEquals("2018 年换岗后收入明显上升。", result.report.left.referenceNotes.single().content)
    }

    @Test
    fun `相同命例 名人 回收站和未采用快照会被拒绝`() {
        val source = chart("same", FourPillars("甲子", "乙丑", "丙寅", "丁卯"))
        val same = BaziCompatibilityAnalyzer.analyze(source, source) as BaziCompatibilityResult.Rejected
        assertTrue(same.reasons.any { it.code == BaziCompatibilityRejectionCode.SAME_CASE })

        val celebrity = BaziCompatibilityAnalyzer.analyze(
            source,
            chart("celebrity", FourPillars("己丑", "庚寅", "辛卯", "壬辰")).copy(libraryType = CaseLibraryType.CELEBRITY),
        ) as BaziCompatibilityResult.Rejected
        assertTrue(celebrity.reasons.any { it.code == BaziCompatibilityRejectionCode.RIGHT_NOT_USER_CASE })

        val unavailable = BaziCompatibilityAnalyzer.analyze(
            source,
            chart("missing", FourPillars("己丑", "庚寅", "辛卯", "壬辰")).copy(calculationSnapshots = emptyList()),
        ) as BaziCompatibilityResult.Rejected
        assertTrue(unavailable.reasons.any { it.code == BaziCompatibilityRejectionCode.RIGHT_NO_ADOPTED_SNAPSHOT })
    }

    @Test
    fun `合盘双方位置必须对应男方与女方`() {
        val womanOnLeft = BaziCompatibilityAnalyzer.analyze(
            chart("woman-left", FourPillars("甲子", "乙丑", "丙寅", "丁卯")).copy(
                sexForFortuneDirection = SexForFortuneDirection.WOMAN,
            ),
            chart("right", FourPillars("己丑", "庚寅", "辛卯", "壬辰")).copy(
                sexForFortuneDirection = SexForFortuneDirection.WOMAN,
            ),
        ) as BaziCompatibilityResult.Rejected
        assertTrue(womanOnLeft.reasons.any { it.code == BaziCompatibilityRejectionCode.LEFT_NOT_MAN })

        val manOnRight = BaziCompatibilityAnalyzer.analyze(
            chart("left", FourPillars("甲子", "乙丑", "丙寅", "丁卯")),
            chart("man-right", FourPillars("己丑", "庚寅", "辛卯", "壬辰")),
        ) as BaziCompatibilityResult.Rejected
        assertTrue(manOnRight.reasons.any { it.code == BaziCompatibilityRejectionCode.RIGHT_NOT_WOMAN })
    }

    private fun chart(
        id: String,
        pillars: FourPillars,
        precision: TimePrecision = TimePrecision.EXACT_TO_MINUTE,
        profile: CalculationProfile = CalculationProfile.tymeDefault(),
        sex: SexForFortuneDirection = if (id.startsWith("right")) {
            SexForFortuneDirection.WOMAN
        } else {
            SexForFortuneDirection.MAN
        },
    ): BaziCase {
        val input = BirthInput(
            calendarInput = BirthCalendarInput.Solar(CivilDateTime(2000, 1, 2, 3, 4, 0)),
            sexForFortuneDirection = sex,
            timePrecision = precision,
        )
        return BaziCase(
            id = id,
            alias = id,
            name = ExplicitText.absent(),
            sexForFortuneDirection = sex,
            sourceType = CaseSourceType.MANUAL,
            birthInput = input,
            calculationSnapshots = listOf(
                CaseCalculationSnapshot(
                    id = "$id-snapshot",
                    adopted = true,
                    createdAt = NOW,
                    result = CalculationResult(
                        normalizedInput = input,
                        profile = profile,
                        fourPillars = pillars,
                        ownSign = "戊辰",
                        bodySign = "己巳",
                        fetalOrigin = "庚午",
                        fetalBreath = "辛未",
                        fortuneStart = FortuneStart(FortuneDirection.FORWARD, CivilDateTime(2001, 1, 1, 0, 0, 0), CivilDateTime(2011, 1, 1, 0, 0, 0), 1, 0, 0, 0, 0),
                        decadeFortunes = listOf(
                            DecadeFortune(
                                name = "甲子",
                                startAge = 1,
                                endAge = 10,
                                startYear = 2001,
                                endYear = 2010,
                                startAt = CivilDateTime(2001, 1, 2, 3, 4, 0),
                                endAtExclusive = CivilDateTime(2011, 1, 2, 3, 4, 0),
                            ),
                        ),
                        evidence = CalculationEvidence("test", "1", "test-rule", NOW),
                    ),
                ),
            ),
            createdAt = NOW,
            updatedAt = NOW,
        )
    }

    private fun analysisForYearPair(
        leftStem: String,
        rightStem: String,
        leftBranch: String,
        rightBranch: String,
    ): BaziCompatibilityReport = (
        BaziCompatibilityAnalyzer.analyze(
            chart("left-$leftStem-$leftBranch", FourPillars("$leftStem$leftBranch", "乙巳", "丙酉", "丁亥")),
            chart("right-$rightStem-$rightBranch", FourPillars("$rightStem$rightBranch", "庚午", "辛戌", "壬丑")),
        ) as BaziCompatibilityResult.Ready
        ).report

    private fun assertPairs(kind: BaziCompatibilitySignalKind, pairs: List<Pair<String, String>>) {
        pairs.forEach { (left, right) ->
            val report = analysisForYearPair("甲", "乙", left, right)
            val signals = if (kind.isCoordination) report.coordinationSignals else report.tensionSignals
            assertTrue("${kind.name}: $left·$right", signals.any { it.kind == kind && it.values == "$left·$right" })
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-08-21T00:00:00Z")
    }
}
