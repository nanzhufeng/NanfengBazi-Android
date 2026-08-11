package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BasicChartDetails
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FieldValueState
import com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.LunarDateTime
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import java.time.LocalDate

const val CASE_OBJECTIVE_SUMMARY_VERSION: Int = 1

enum class CaseObjectiveSummarySource {
    CASE_IDENTITY,
    ADOPTED_SNAPSHOT_INPUT,
    ADOPTED_CALCULATION,
    CALCULATION_EVIDENCE,
    FORMAL_RECORD_INDEX,
}

enum class CaseObjectiveSummaryValueState {
    PRESENT,
    NOT_RECORDED,
    EXPLICITLY_CLEARED,
}

enum class CaseObjectiveSummarySensitivity {
    PUBLIC,
    IDENTITY,
    LOCATION,
    DEMOGRAPHIC,
    PRECISE_BIRTH_TIME,
}

data class CaseObjectiveSummaryObservation(
    val referenceDate: LocalDate,
    val professionalPosition: ProfessionalFortunePosition,
)

data class CaseObjectiveSummaryInput(
    val caseData: BaziCase,
    val summaryVersion: Int = CASE_OBJECTIVE_SUMMARY_VERSION,
    val observation: CaseObjectiveSummaryObservation? = null,
)

data class CaseObjectiveSummaryField(
    val label: String,
    val value: String,
    val source: CaseObjectiveSummarySource,
    val state: CaseObjectiveSummaryValueState = CaseObjectiveSummaryValueState.PRESENT,
    val sensitivity: CaseObjectiveSummarySensitivity = CaseObjectiveSummarySensitivity.PUBLIC,
) {
    init {
        require(label.isNotBlank()) { "客观摘要字段标签不能为空" }
        require(value.isNotBlank()) { "客观摘要字段值不能为空" }
    }
}

data class CaseObjectiveSummarySection(
    val id: String,
    val title: String,
    val fields: List<CaseObjectiveSummaryField>,
) {
    init {
        require(id.isNotBlank()) { "客观摘要章节 id 不能为空" }
        require(title.isNotBlank()) { "客观摘要章节标题不能为空" }
        require(fields.isNotEmpty()) { "客观摘要章节不能为空" }
    }
}

data class CaseObjectiveSummary(
    val version: Int,
    val caseId: String,
    val caseRevision: Long,
    val adoptedSnapshotId: String,
    val title: String,
    val sections: List<CaseObjectiveSummarySection>,
    val provenanceNotice: String,
    val interpretationNotice: String,
    val copyText: String,
) {
    init {
        require(version > 0) { "客观摘要版本必须大于零" }
        require(caseId.isNotBlank()) { "客观摘要必须关联命例" }
        require(adoptedSnapshotId.isNotBlank()) { "客观摘要必须关联采用快照" }
        require(title.isNotBlank()) { "客观摘要标题不能为空" }
        require(sections.isNotEmpty()) { "客观摘要章节不能为空" }
        require(sections.map { it.id }.distinct().size == sections.size) {
            "客观摘要章节 id 不能重复"
        }
        require(provenanceNotice.isNotBlank() && interpretationNotice.isNotBlank()) {
            "客观摘要必须声明事实与解释边界"
        }
        require(copyText.isNotBlank()) { "客观摘要复制文本不能为空" }
    }
}

enum class CaseObjectiveSummaryErrorCode {
    UNSUPPORTED_SUMMARY_VERSION,
    NO_ADOPTED_SNAPSHOT,
    MULTIPLE_ADOPTED_SNAPSHOTS,
    SUMMARY_UNAVAILABLE,
    CLIPBOARD_UNAVAILABLE,
    COPY_FAILED,
}

data class CaseObjectiveSummaryFailure(
    val code: CaseObjectiveSummaryErrorCode,
    val message: String,
) {
    init {
        require(message.isNotBlank()) { "客观摘要失败说明不能为空" }
    }
}

sealed interface CaseObjectiveSummaryResult {
    data class Success(
        val summary: CaseObjectiveSummary,
    ) : CaseObjectiveSummaryResult

    data class Rejected(
        val failure: CaseObjectiveSummaryFailure,
    ) : CaseObjectiveSummaryResult
}

fun interface CaseObjectiveSummaryGenerator {
    fun generate(input: CaseObjectiveSummaryInput): CaseObjectiveSummaryResult
}

object CaseObjectiveSummaryContract : CaseObjectiveSummaryGenerator {
    override fun generate(input: CaseObjectiveSummaryInput): CaseObjectiveSummaryResult {
        if (input.summaryVersion != CASE_OBJECTIVE_SUMMARY_VERSION) {
            return CaseObjectiveSummaryResult.Rejected(
                CaseObjectiveSummaryFailure(
                    CaseObjectiveSummaryErrorCode.UNSUPPORTED_SUMMARY_VERSION,
                    "暂不支持客观摘要版本 ${input.summaryVersion}。",
                ),
            )
        }
        val adopted = input.caseData.calculationSnapshots.filter { it.adopted }
        if (adopted.isEmpty()) {
            return CaseObjectiveSummaryResult.Rejected(
                CaseObjectiveSummaryFailure(
                    CaseObjectiveSummaryErrorCode.NO_ADOPTED_SNAPSHOT,
                    "该命例没有已采用的计算快照，无法生成可追溯摘要。",
                ),
            )
        }
        if (adopted.size > 1) {
            return CaseObjectiveSummaryResult.Rejected(
                CaseObjectiveSummaryFailure(
                    CaseObjectiveSummaryErrorCode.MULTIPLE_ADOPTED_SNAPSHOTS,
                    "该命例存在多个已采用快照，请先修复计算档案。",
                ),
            )
        }

        val snapshot = adopted.single()
        val result = snapshot.result
        val normalizedInput = result.normalizedInput
        val identityFields = listOf(
            presentField(
                "命例别名",
                input.caseData.alias,
                CaseObjectiveSummarySource.CASE_IDENTITY,
                CaseObjectiveSummarySensitivity.IDENTITY,
            ),
            when (input.caseData.name.state) {
                FieldValueState.PRESENT -> presentField(
                    "姓名",
                    input.caseData.name.value.orEmpty(),
                    CaseObjectiveSummarySource.CASE_IDENTITY,
                    CaseObjectiveSummarySensitivity.IDENTITY,
                )
                FieldValueState.ABSENT -> missingField(
                    "姓名",
                    CaseObjectiveSummarySource.CASE_IDENTITY,
                    "未提供",
                    CaseObjectiveSummarySensitivity.IDENTITY,
                )
                FieldValueState.CLEARED -> CaseObjectiveSummaryField(
                    label = "姓名",
                    value = "已明确清空",
                    source = CaseObjectiveSummarySource.CASE_IDENTITY,
                    state = CaseObjectiveSummaryValueState.EXPLICITLY_CLEARED,
                    sensitivity = CaseObjectiveSummarySensitivity.IDENTITY,
                )
            },
            presentField(
                "性别口径",
                input.caseData.sexForFortuneDirection.displayName(),
                CaseObjectiveSummarySource.CASE_IDENTITY,
                CaseObjectiveSummarySensitivity.DEMOGRAPHIC,
            ),
            presentField(
                "出生历法与时间",
                normalizedInput.calendarInput.displayText(),
                CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                CaseObjectiveSummarySensitivity.PRECISE_BIRTH_TIME,
            ),
            result.calendarConversion?.let {
                presentField(
                    "换算公历",
                    it.solarDateTime.displayText(),
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    CaseObjectiveSummarySensitivity.PRECISE_BIRTH_TIME,
                )
            } ?: missingField(
                "换算公历",
                CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                sensitivity = CaseObjectiveSummarySensitivity.PRECISE_BIRTH_TIME,
            ),
            result.calendarConversion?.let {
                presentField(
                    "换算农历",
                    it.lunarDateTime.displayText(includeLeapMonth = true),
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    CaseObjectiveSummarySensitivity.PRECISE_BIRTH_TIME,
                )
            } ?: missingField(
                "换算农历",
                CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                sensitivity = CaseObjectiveSummarySensitivity.PRECISE_BIRTH_TIME,
            ),
            presentField(
                "IANA 时区",
                normalizedInput.timeZoneId,
                CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                CaseObjectiveSummarySensitivity.LOCATION,
            ),
            normalizedInput.resolvedUtcOffsetSeconds?.let {
                presentField(
                    "UTC offset",
                    it.toOffsetText(),
                    CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                    CaseObjectiveSummarySensitivity.LOCATION,
                )
            } ?: missingField(
                "UTC offset",
                CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                sensitivity = CaseObjectiveSummarySensitivity.LOCATION,
            ),
            normalizedInput.timeZoneDataVersion?.takeIf(String::isNotBlank)?.let {
                presentField(
                    "时区数据版本",
                    it,
                    CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                    CaseObjectiveSummarySensitivity.LOCATION,
                )
            } ?: missingField(
                "时区数据版本",
                CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                sensitivity = CaseObjectiveSummarySensitivity.LOCATION,
            ),
            presentField(
                "时间精度",
                normalizedInput.timePrecision.displayName(),
                CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                CaseObjectiveSummarySensitivity.PRECISE_BIRTH_TIME,
            ),
            presentField(
                "时间来源",
                normalizedInput.timeSourceType.displayName(),
                CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                CaseObjectiveSummarySensitivity.PRECISE_BIRTH_TIME,
            ),
            normalizedInput.locationName?.takeIf(String::isNotBlank)?.let {
                presentField(
                    "出生地区",
                    it,
                    CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                    CaseObjectiveSummarySensitivity.LOCATION,
                )
            } ?: missingField(
                "出生地区",
                CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                sensitivity = CaseObjectiveSummarySensitivity.LOCATION,
            ),
            if (normalizedInput.longitude != null && normalizedInput.latitude != null) {
                presentField(
                    "经纬度",
                    "${normalizedInput.longitude}, ${normalizedInput.latitude}",
                    CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                    CaseObjectiveSummarySensitivity.LOCATION,
                )
            } else {
                missingField(
                    "经纬度",
                    CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT,
                    sensitivity = CaseObjectiveSummarySensitivity.LOCATION,
                )
            },
        )

        val pillars = result.fourPillars
        val details = result.basicChartDetails
        val chartFields = buildList {
            add(
                presentField(
                    "四柱",
                    "${pillars.year}　${pillars.month}　${pillars.day}　${pillars.hour}",
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                details?.let {
                    presentField(
                        "生肖",
                        it.zodiac,
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    )
                } ?: missingField(
                    "生肖",
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                details?.let {
                    presentField(
                        "星座",
                        it.westernZodiac,
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    )
                } ?: missingField(
                    "星座",
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                details?.let {
                    presentField(
                        "日主",
                        it.dayMaster,
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    )
                } ?: missingField(
                    "日主",
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            val monthPillar = details?.pillars?.firstOrNull {
                it.position == PillarPosition.MONTH
            }
            val dayPillar = details?.pillars?.firstOrNull {
                it.position == PillarPosition.DAY
            }
            add(
                monthPillar?.let {
                    presentField(
                        "月令",
                        "${it.earthBranch}（${it.earthBranchElement}）",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    )
                } ?: missingField(
                    "月令",
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                dayPillar?.let {
                    presentField(
                        "日支",
                        "${it.earthBranch}（${it.earthBranchElement}）",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    )
                } ?: missingField(
                    "日支",
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            val elementCounts = details?.surfaceElementCounts()
            add(
                elementCounts?.let {
                    presentField(
                        "表层五行计数",
                        ELEMENT_ORDER.joinToString("、") { element ->
                            "$element${it.getValue(element)}"
                        } + "（四干四支，不含藏干）",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    )
                } ?: missingField(
                    "表层五行计数",
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                elementCounts?.let {
                    val missing = ELEMENT_ORDER.filter { element -> it.getValue(element) == 0 }
                    presentField(
                        "表层五行缺失",
                        missing.joinToString("、").ifEmpty { "无" } + "（不含藏干）",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    )
                } ?: missingField(
                    "表层五行缺失",
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            val natalRelations = NatalChartRelationResolver.resolve(pillars)
            add(
                presentField(
                    "原局天干关系",
                    natalRelations.heavenStemRelations.joinToString("；").ifEmpty { "未发现合冲" },
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                presentField(
                    "原局地支关系",
                    natalRelations.earthBranchRelations.joinToString("；")
                        .ifEmpty { "未发现合冲刑害或三合三会" },
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                presentField(
                    "原局整柱关系",
                    natalRelations.wholePillarRelations.joinToString("；").ifEmpty { "无重复整柱" },
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            input.observation?.let { observation ->
                add(
                    presentField(
                        "当前实岁",
                        "${observation.professionalPosition.completedAge}岁（截至${observation.referenceDate}）",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                        CaseObjectiveSummarySensitivity.PRECISE_BIRTH_TIME,
                    ),
                )
                add(
                    presentField(
                        "当前虚岁",
                        "${observation.professionalPosition.position.annualFortune.nominalAge}岁",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                        CaseObjectiveSummarySensitivity.PRECISE_BIRTH_TIME,
                    ),
                )
            }
            add(
                presentField(
                    "胎元",
                    result.fetalOrigin,
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                presentField(
                    "胎息",
                    result.fetalBreath,
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                presentField(
                    "命宫",
                    result.ownSign,
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                presentField(
                    "身宫",
                    result.bodySign,
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            if (details == null) {
                PillarPosition.entries.forEach { position ->
                    add(
                        missingField(
                            "${position.displayName()}明细",
                            CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                        ),
                    )
                }
                add(
                    missingField(
                        "相邻节气",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    ),
                )
                add(
                    missingField(
                        "前后节",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    ),
                )
            } else {
                details.pillars
                    .sortedBy { it.position.ordinal }
                    .forEach { pillar ->
                        add(
                            presentField(
                                "${pillar.position.displayName()}明细",
                                "${pillar.heavenStem}${pillar.earthBranch}｜" +
                                    "主星 ${pillar.primaryTenGod}｜" +
                                    "星运 ${pillar.terrain}｜" +
                                    "自坐 ${pillar.selfSittingTerrain}｜" +
                                    "空亡 ${pillar.voidEarthBranches.joinToString("、")}｜" +
                                    "纳音 ${pillar.naYin}｜藏干 " +
                                    pillar.hiddenStems.joinToString("；") {
                                        "${it.heavenStem} ${it.type} ${it.tenGod} ${it.element}"
                                    }.ifEmpty { "未记录" },
                                CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                            ),
                        )
                    }
                add(
                    presentField(
                        "相邻节气",
                        "${details.previousSolarTerm.name} " +
                            "${details.previousSolarTerm.at.displayText()}；" +
                            "${details.nextSolarTerm.name} " +
                            details.nextSolarTerm.at.displayText(),
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    ),
                )
                add(
                    if (details.previousJie != null && details.nextJie != null) {
                        presentField(
                            "前后节",
                            "${details.previousJie.name} " +
                                "${details.previousJie.at.displayText()}；" +
                                "${details.nextJie.name} " +
                                details.nextJie.at.displayText(),
                            CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                        )
                    } else {
                        missingField(
                            "前后节",
                            CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                        )
                    },
                )
            }
            add(
                result.trueSolarTimeEvidence?.let {
                    presentField(
                        "真太阳时证据",
                        "${it.trueSolarDateTime.displayText()}｜" +
                            "校正 ${it.totalCorrectionSeconds} 秒｜" +
                            "算法 ${it.algorithmVersion}",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    )
                } ?: missingField(
                    "真太阳时证据",
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    "未记录（当前快照未包含该证据）",
                ),
            )
        }

        val start = result.fortuneStart
        val fortuneFields = buildList {
            add(
                presentField(
                    "起运方向",
                    start.direction.displayName(),
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                presentField(
                    "精确交运时间",
                    start.endAt.displayText(),
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            add(
                presentField(
                    "起运年龄",
                    "${start.years}年 ${start.months}月 ${start.days}日 " +
                        "${start.hours}时 ${start.minutes}分",
                    CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                ),
            )
            if (result.decadeFortunes.isEmpty()) {
                add(
                    missingField(
                        "大运表",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    ),
                )
            } else {
                result.decadeFortunes.forEachIndexed { index, decade ->
                    val exactBoundary = if (
                        decade.startAt != null && decade.endAtExclusive != null
                    ) {
                        "｜${decade.startAt.displayText()} 至 " +
                            "${decade.endAtExclusive.displayText()}（不含）"
                    } else {
                        "｜精确边界未记录"
                    }
                    add(
                        presentField(
                            "大运 ${index + 1}",
                            "${decade.name}｜${decade.startAge}–${decade.endAge}岁｜" +
                                "${decade.startYear}–${decade.endYear}$exactBoundary",
                            CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                        ),
                    )
                }
            }
            if (result.annualFortunes.isEmpty()) {
                add(
                    missingField(
                        "流年序列",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    ),
                )
            } else {
                val first = result.annualFortunes.first()
                val last = result.annualFortunes.last()
                add(
                    presentField(
                        "流年序列",
                        "${first.calendarYear}–${last.calendarYear}，" +
                            "共 ${result.annualFortunes.size} 年",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    ),
                )
            }
            val observation = input.observation
            if (observation == null) {
                add(
                    missingField(
                        "当前大运",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                        "未生成（客观摘要未携带观察时刻）",
                    ),
                )
                add(
                    missingField(
                        "当前流年",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                        "未生成（客观摘要未携带观察时刻）",
                    ),
                )
            } else {
                val professional = observation.professionalPosition
                val position = professional.position
                add(
                    presentField(
                        "分析参考日期",
                        observation.referenceDate.toString(),
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    ),
                )
                add(
                    presentField(
                        "当前大运",
                        position.decadeFortune?.let { decade ->
                            "${decade.name}｜${decade.startAge}–${decade.endAge}岁｜" +
                                "${decade.startYear}–${decade.endYear}"
                        } ?: when (position.status) {
                            FortunePositionStatus.BEFORE_FIRST_DECADE -> "尚未起运（小运阶段）"
                            FortunePositionStatus.AFTER_TIMELINE -> "已超出当前大运表范围"
                            FortunePositionStatus.WITHIN_DECADE -> "未记录"
                        },
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    ),
                )
                add(
                    presentField(
                        "当前流年",
                        "${position.annualFortune.calendarYear} ${position.annualFortune.name}｜" +
                            "虚岁${position.annualFortune.nominalAge}",
                        CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                    ),
                )
                val yearWindow = (observation.referenceDate.year - 5)..
                    (observation.referenceDate.year + 5)
                result.annualFortunes.filter { it.calendarYear in yearWindow }.forEach { annual ->
                    val decadeName = annual.decadeName
                        ?: result.decadeFortunes.firstOrNull {
                            annual.calendarYear in it.startYear..it.endYear
                        }?.name
                    add(
                        presentField(
                            "流年 ${annual.calendarYear}",
                            "${annual.name}｜虚岁${annual.nominalAge}" +
                                decadeName?.let { "｜大运$it" }.orEmpty(),
                            CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                        ),
                    )
                }
                add(
                    if (professional.minorTimeline.isEmpty()) {
                        missingField(
                            "小运序列",
                            CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                            "未生成（当前计算档案没有可用小运）",
                        )
                    } else {
                        presentField(
                            "小运序列",
                            professional.minorTimeline.joinToString("；") { item ->
                                "${item.label} ${item.pillar} ${item.subtitle}"
                            },
                            CaseObjectiveSummarySource.ADOPTED_CALCULATION,
                        )
                    },
                )
            }
        }

        val profile = result.profile
        val evidenceFields = listOf(
            presentField(
                "摘要版本",
                "v${input.summaryVersion}",
                CaseObjectiveSummarySource.CALCULATION_EVIDENCE,
            ),
            presentField(
                "命例修订号",
                input.caseData.revision.toString(),
                CaseObjectiveSummarySource.CALCULATION_EVIDENCE,
            ),
            presentField(
                "采用快照",
                snapshot.id,
                CaseObjectiveSummarySource.CALCULATION_EVIDENCE,
            ),
            presentField(
                "引擎",
                "${result.evidence.engineName} ${result.evidence.engineVersion}",
                CaseObjectiveSummarySource.CALCULATION_EVIDENCE,
            ),
            presentField(
                "规则版本",
                result.evidence.ruleVersion,
                CaseObjectiveSummarySource.CALCULATION_EVIDENCE,
            ),
            presentField(
                "计算配置",
                profile.id,
                CaseObjectiveSummarySource.CALCULATION_EVIDENCE,
            ),
            presentField(
                "子时口径",
                profile.ratHourRule.displayName(),
                CaseObjectiveSummarySource.CALCULATION_EVIDENCE,
            ),
            presentField(
                "时间口径",
                profile.solarTimeMode.displayName(),
                CaseObjectiveSummarySource.CALCULATION_EVIDENCE,
            ),
            presentField(
                "计算时间",
                result.evidence.calculatedAt.toString(),
                CaseObjectiveSummarySource.CALCULATION_EVIDENCE,
            ),
            presentField(
                "计算警告",
                result.warnings.joinToString("；") { "${it.code}：${it.message}" }
                    .ifEmpty { "无" },
                CaseObjectiveSummarySource.CALCULATION_EVIDENCE,
            ),
        )

        val recordFields = listOf(
            presentField(
                "正式文本记录",
                "${input.caseData.textRecords.size} 条",
                CaseObjectiveSummarySource.FORMAL_RECORD_INDEX,
            ),
            presentField(
                "关键事件",
                "${input.caseData.events.size} 条",
                CaseObjectiveSummarySource.FORMAL_RECORD_INDEX,
            ),
            presentField(
                "来源附件",
                "${input.caseData.attachments.size} 个",
                CaseObjectiveSummarySource.FORMAL_RECORD_INDEX,
            ),
            presentField(
                "字段证据",
                "${input.caseData.fieldEvidence.size} 条",
                CaseObjectiveSummarySource.FORMAL_RECORD_INDEX,
            ),
        )

        val sections = listOf(
            CaseObjectiveSummarySection("birth_facts", "出生资料", identityFields),
            CaseObjectiveSummarySection("chart_facts", "四柱与基础盘", chartFields),
            CaseObjectiveSummarySection("fortune_facts", "起运与大运", fortuneFields),
            CaseObjectiveSummarySection("calculation_evidence", "计算档案", evidenceFields),
            CaseObjectiveSummarySection("formal_record_index", "已有研究资料", recordFields),
        )
        val provenanceNotice =
            "本摘要只读取当前唯一已采用的本机计算快照和正式资料计数；" +
                "来源截图值、未采用候选和旧快照不作为计算事实。"
        val interpretationNotice =
            "本摘要是可追溯的计算事实清单，不提供主观解释或确定性人生判断。"
        val title = input.caseData.name.value ?: input.caseData.alias
        val copyText = buildCopyText(
            version = input.summaryVersion,
            title = title,
            sections = sections,
            provenanceNotice = provenanceNotice,
            interpretationNotice = interpretationNotice,
        )
        return CaseObjectiveSummaryResult.Success(
            CaseObjectiveSummary(
                version = input.summaryVersion,
                caseId = input.caseData.id,
                caseRevision = input.caseData.revision,
                adoptedSnapshotId = snapshot.id,
                title = title,
                sections = sections,
                provenanceNotice = provenanceNotice,
                interpretationNotice = interpretationNotice,
                copyText = copyText,
            ),
        )
    }
}

object CaseObjectiveSummaryPolicy {
    private val prohibitedGeneratedPhrases = listOf(
        "命好",
        "命坏",
        "大吉",
        "大凶",
        "合婚",
        "注定发财",
        "必定发财",
        "婚姻必",
        "事业必",
        "健康必",
    )

    fun findProhibitedGeneratedPhrases(summary: CaseObjectiveSummary): List<String> {
        val generatedText = buildString {
            summary.sections.forEach { section ->
                appendLine(section.title)
                section.fields.forEach { appendLine(it.label) }
            }
            appendLine(summary.provenanceNotice)
            append(summary.interpretationNotice)
        }
        return prohibitedGeneratedPhrases.filter(generatedText::contains)
    }
}

fun CaseObjectiveSummarySource.displayName(): String = when (this) {
    CaseObjectiveSummarySource.CASE_IDENTITY -> "命例身份"
    CaseObjectiveSummarySource.ADOPTED_SNAPSHOT_INPUT -> "采用快照输入"
    CaseObjectiveSummarySource.ADOPTED_CALCULATION -> "采用快照计算"
    CaseObjectiveSummarySource.CALCULATION_EVIDENCE -> "计算档案"
    CaseObjectiveSummarySource.FORMAL_RECORD_INDEX -> "正式资料索引"
}

private fun presentField(
    label: String,
    value: String,
    source: CaseObjectiveSummarySource,
    sensitivity: CaseObjectiveSummarySensitivity = CaseObjectiveSummarySensitivity.PUBLIC,
): CaseObjectiveSummaryField = CaseObjectiveSummaryField(
    label = label,
    value = value,
    source = source,
    sensitivity = sensitivity,
)

private fun missingField(
    label: String,
    source: CaseObjectiveSummarySource,
    value: String = "未记录（旧快照缺少该字段）",
    sensitivity: CaseObjectiveSummarySensitivity = CaseObjectiveSummarySensitivity.PUBLIC,
): CaseObjectiveSummaryField = CaseObjectiveSummaryField(
    label = label,
    value = value,
    source = source,
    state = CaseObjectiveSummaryValueState.NOT_RECORDED,
    sensitivity = sensitivity,
)

private fun buildCopyText(
    version: Int,
    title: String,
    sections: List<CaseObjectiveSummarySection>,
    provenanceNotice: String,
    interpretationNotice: String,
): String = buildString {
    appendLine("南枫八字 · 客观命盘摘要 v$version")
    appendLine("命例：$title")
    sections.forEach { section ->
        appendLine()
        appendLine("【${section.title}】")
        section.fields.forEach { field ->
            val value = field.value.replace("\n", "\n  ")
            appendLine("${field.label}：$value〔${field.source.displayName()}〕")
        }
    }
    appendLine()
    appendLine("来源边界：$provenanceNotice")
    append("说明：$interpretationNotice")
}

private fun BirthCalendarInput.displayText(): String = when (this) {
    is BirthCalendarInput.Solar -> "公历 ${dateTime.displayText()}"
    is BirthCalendarInput.Lunar ->
        "农历 ${dateTime.displayText(includeLeapMonth = true)}"
}

private fun CivilDateTime.displayText(): String =
    "%04d-%02d-%02d %02d:%02d:%02d".format(year, month, day, hour, minute, second)

private fun LunarDateTime.displayText(includeLeapMonth: Boolean): String =
    "%04d-%s%02d-%02d %02d:%02d:%02d".format(
        year,
        if (includeLeapMonth && isLeapMonth) "闰" else "",
        month,
        day,
        hour,
        minute,
        second,
    )

private fun Int.toOffsetText(): String {
    val sign = if (this >= 0) "+" else "-"
    val absolute = kotlin.math.abs(this)
    return "UTC$sign%02d:%02d".format(absolute / 3600, absolute % 3600 / 60)
}

private fun SexForFortuneDirection.displayName(): String = when (this) {
    SexForFortuneDirection.WOMAN -> "女"
    SexForFortuneDirection.MAN -> "男"
}

private fun TimePrecision.displayName(): String = when (this) {
    TimePrecision.EXACT_TO_SECOND -> "精确到秒"
    TimePrecision.EXACT_TO_MINUTE -> "精确到分钟"
    TimePrecision.APPROXIMATE -> "大约时间"
    TimePrecision.HOUR_ONLY -> "仅小时"
    TimePrecision.DOUBLE_HOUR_ONLY -> "仅时辰"
    TimePrecision.UNKNOWN -> "未知"
}

private fun TimeSourceType.displayName(): String = when (this) {
    TimeSourceType.SELF_REPORTED -> "本人提供"
    TimeSourceType.FAMILY_REPORTED -> "家人提供"
    TimeSourceType.OFFICIAL_RECORD -> "正式记录"
    TimeSourceType.WENZHEN_SCREENSHOT -> "问真截图"
    TimeSourceType.WENZHEN_WEB_IMPORT -> "问真网页导入"
    TimeSourceType.OTHER_RECORD -> "其他记录"
    TimeSourceType.UNKNOWN -> "未知"
}

private fun PillarPosition.displayName(): String = when (this) {
    PillarPosition.YEAR -> "年柱"
    PillarPosition.MONTH -> "月柱"
    PillarPosition.DAY -> "日柱"
    PillarPosition.HOUR -> "时柱"
}

private fun RatHourRule.displayName(): String = when (this) {
    RatHourRule.TYME_DEFAULT -> "23:00 起按次日"
    RatHourRule.LATE_RAT_SAME_DAY -> "晚子时仍按当天"
}

private fun SolarTimeMode.displayName(): String = when (this) {
    SolarTimeMode.CIVIL_TIME -> "民用时"
    SolarTimeMode.TRUE_SOLAR_TIME -> "真太阳时"
}

private fun FortuneDirection.displayName(): String = when (this) {
    FortuneDirection.FORWARD -> "顺排"
    FortuneDirection.BACKWARD -> "逆排"
}

private val ELEMENT_ORDER = listOf("木", "火", "土", "金", "水")

private fun BasicChartDetails.surfaceElementCounts(): Map<String, Int> =
    ELEMENT_ORDER.associateWith { element ->
        pillars.count { it.heavenStemElement == element } +
            pillars.count { it.earthBranchElement == element }
    }
