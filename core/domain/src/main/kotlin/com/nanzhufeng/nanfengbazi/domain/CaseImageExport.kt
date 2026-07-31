package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
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

const val CASE_IMAGE_DOCUMENT_VERSION: Int = 1

enum class CaseImageExportScope {
    ADOPTED_CHART_AND_FORMAL_RECORDS,
}

enum class CaseImageDeliveryMode {
    SAVE_TO_SYSTEM_FILE,
    SHARE_LONG_IMAGE,
}

data class CaseImageExportInput(
    val caseData: BaziCase,
    val documentVersion: Int = CASE_IMAGE_DOCUMENT_VERSION,
    val scope: CaseImageExportScope =
        CaseImageExportScope.ADOPTED_CHART_AND_FORMAL_RECORDS,
)

data class CaseImageRenderRow(
    val label: String,
    val value: String,
) {
    init {
        require(label.isNotBlank()) { "导出字段标签不能为空" }
        require(value.isNotBlank()) { "导出字段值不能为空" }
    }
}

sealed interface CaseImageRenderBlock {
    val title: String

    data class Rows(
        override val title: String,
        val rows: List<CaseImageRenderRow>,
    ) : CaseImageRenderBlock {
        init {
            require(title.isNotBlank()) { "导出区块标题不能为空" }
            require(rows.isNotEmpty()) { "导出字段区块不能为空" }
        }
    }

    data class Paragraphs(
        override val title: String,
        val paragraphs: List<String>,
    ) : CaseImageRenderBlock {
        init {
            require(title.isNotBlank()) { "导出区块标题不能为空" }
            require(paragraphs.isNotEmpty()) { "导出段落区块不能为空" }
            require(paragraphs.none(String::isBlank)) { "导出段落不能为空" }
        }
    }
}

data class CaseImageRenderFacts(
    val documentVersion: Int,
    val scope: CaseImageExportScope,
    val caseId: String,
    val caseRevision: Long,
    val adoptedSnapshotId: String,
    val title: String,
    val subtitle: String,
    val suggestedFileStem: String,
    val blocks: List<CaseImageRenderBlock>,
    val provenanceNotice: String,
    val privacyNotice: String,
) {
    init {
        require(documentVersion > 0) { "图片文档版本必须大于零" }
        require(caseId.isNotBlank()) { "图片导出必须关联命例" }
        require(adoptedSnapshotId.isNotBlank()) { "图片导出必须关联采用快照" }
        require(title.isNotBlank() && subtitle.isNotBlank()) { "图片标题不能为空" }
        require(suggestedFileStem.isNotBlank()) { "建议文件名不能为空" }
        require(blocks.isNotEmpty()) { "图片导出内容不能为空" }
        require(provenanceNotice.isNotBlank() && privacyNotice.isNotBlank()) {
            "图片导出必须声明来源与隐私边界"
        }
    }
}

enum class CaseImageExportErrorCode {
    UNSUPPORTED_DOCUMENT_VERSION,
    NO_ADOPTED_SNAPSHOT,
    MULTIPLE_ADOPTED_SNAPSHOTS,
    CONTENT_TOO_LARGE,
    RENDER_FAILED,
    OUTPUT_UNAVAILABLE,
    WRITE_FAILED,
    USER_CANCELLED,
    NO_SHARE_TARGET,
    SHARE_LAUNCH_FAILED,
}

data class CaseImageExportFailure(
    val code: CaseImageExportErrorCode,
    val message: String,
) {
    init {
        require(message.isNotBlank()) { "图片导出失败说明不能为空" }
    }
}

sealed interface CaseImageFactsResult {
    data class Prepared(
        val facts: CaseImageRenderFacts,
    ) : CaseImageFactsResult

    data class Rejected(
        val failure: CaseImageExportFailure,
    ) : CaseImageFactsResult
}

data class RenderedCaseImage(
    val facts: CaseImageRenderFacts,
    val mimeType: String,
    val fileExtension: String,
    val bytes: ByteArray,
    val widthPixels: Int,
    val heightPixels: Int,
    val sha256: String,
) {
    init {
        require(mimeType.isNotBlank() && fileExtension.isNotBlank())
        require(bytes.isNotEmpty()) { "渲染图片字节不能为空" }
        require(widthPixels > 0 && heightPixels > 0) { "渲染图片尺寸无效" }
        require(sha256.matches(Regex("[0-9a-f]{64}"))) { "渲染图片哈希无效" }
    }
}

sealed interface CaseImageRenderResult {
    data class Success(
        val image: RenderedCaseImage,
    ) : CaseImageRenderResult

    data class Rejected(
        val failure: CaseImageExportFailure,
    ) : CaseImageRenderResult
}

interface CaseImageRenderer {
    suspend fun render(input: CaseImageExportInput): CaseImageRenderResult
}

object CaseImageExportContract {
    fun prepare(input: CaseImageExportInput): CaseImageFactsResult {
        if (input.documentVersion != CASE_IMAGE_DOCUMENT_VERSION) {
            return CaseImageFactsResult.Rejected(
                CaseImageExportFailure(
                    CaseImageExportErrorCode.UNSUPPORTED_DOCUMENT_VERSION,
                    "暂不支持图片文档版本 ${input.documentVersion}。",
                ),
            )
        }
        val adopted = input.caseData.calculationSnapshots.filter { it.adopted }
        if (adopted.isEmpty()) {
            return CaseImageFactsResult.Rejected(
                CaseImageExportFailure(
                    CaseImageExportErrorCode.NO_ADOPTED_SNAPSHOT,
                    "该命例没有已采用的计算快照，无法生成可复算命盘图片。",
                ),
            )
        }
        if (adopted.size > 1) {
            return CaseImageFactsResult.Rejected(
                CaseImageExportFailure(
                    CaseImageExportErrorCode.MULTIPLE_ADOPTED_SNAPSHOTS,
                    "该命例存在多个已采用快照，请先修复计算档案。",
                ),
            )
        }

        val snapshot = adopted.single()
        val result = snapshot.result
        val inputFacts = result.normalizedInput
        val identityRows = buildList {
            add(CaseImageRenderRow("命例别名", input.caseData.alias))
            add(
                CaseImageRenderRow(
                    "姓名",
                    when (input.caseData.name.state) {
                        FieldValueState.PRESENT -> input.caseData.name.value.orEmpty()
                        FieldValueState.ABSENT -> "未提供"
                        FieldValueState.CLEARED -> "已明确清空"
                    },
                ),
            )
            add(
                CaseImageRenderRow(
                    "性别口径",
                    input.caseData.sexForFortuneDirection.displayName(),
                ),
            )
            add(CaseImageRenderRow("出生历法与时间", inputFacts.calendarInput.displayText()))
            add(CaseImageRenderRow("IANA 时区", inputFacts.timeZoneId))
            add(
                CaseImageRenderRow(
                    "UTC offset",
                    inputFacts.resolvedUtcOffsetSeconds?.toOffsetText() ?: "未记录",
                ),
            )
            add(CaseImageRenderRow("时间精度", inputFacts.timePrecision.displayName()))
            add(CaseImageRenderRow("时间来源", inputFacts.timeSourceType.displayName()))
            add(
                CaseImageRenderRow(
                    "地点",
                    inputFacts.locationName?.takeIf(String::isNotBlank) ?: "未记录",
                ),
            )
            add(
                CaseImageRenderRow(
                    "经纬度",
                    if (inputFacts.longitude != null && inputFacts.latitude != null) {
                        "${inputFacts.longitude}, ${inputFacts.latitude}"
                    } else {
                        "未记录"
                    },
                ),
            )
        }
        val pillars = result.fourPillars
        val chartRows = buildList {
            add(
                CaseImageRenderRow(
                    "四柱",
                    "${pillars.year}　${pillars.month}　${pillars.day}　${pillars.hour}",
                ),
            )
            result.basicChartDetails?.let { details ->
                add(CaseImageRenderRow("生肖", details.zodiac))
                add(CaseImageRenderRow("星座", details.westernZodiac))
                add(CaseImageRenderRow("日主", details.dayMaster))
            }
            add(CaseImageRenderRow("胎元", result.fetalOrigin))
            add(CaseImageRenderRow("胎息", result.fetalBreath))
            add(CaseImageRenderRow("命宫", result.ownSign))
            add(CaseImageRenderRow("身宫", result.bodySign))
        }
        val pillarRows = result.basicChartDetails?.pillars
            ?.sortedBy { it.position.ordinal }
            ?.flatMap { pillar ->
                listOf(
                    CaseImageRenderRow(
                        pillar.position.displayName(),
                        "${pillar.heavenStem}${pillar.earthBranch}｜主星 ${pillar.primaryTenGod}｜" +
                            "星运 ${pillar.terrain}｜自坐 ${pillar.selfSittingTerrain}｜" +
                            "空亡 ${pillar.voidEarthBranches.joinToString("、")}｜纳音 ${pillar.naYin}",
                    ),
                    CaseImageRenderRow(
                        "${pillar.position.displayName()}藏干",
                        pillar.hiddenStems.joinToString("；") {
                            "${it.heavenStem} ${it.type} ${it.tenGod} ${it.element}"
                        }.ifEmpty { "未记录" },
                    ),
                )
            }
            .orEmpty()
        val profile = result.profile
        val evidenceRows = listOf(
            CaseImageRenderRow("图片文档版本", "v${input.documentVersion}"),
            CaseImageRenderRow("命例修订号", input.caseData.revision.toString()),
            CaseImageRenderRow("采用快照", snapshot.id),
            CaseImageRenderRow("引擎", "${result.evidence.engineName} ${result.evidence.engineVersion}"),
            CaseImageRenderRow("规则版本", result.evidence.ruleVersion),
            CaseImageRenderRow("计算配置", profile.id),
            CaseImageRenderRow("子时口径", profile.ratHourRule.displayName()),
            CaseImageRenderRow("时间口径", profile.solarTimeMode.displayName()),
        )
        val fortuneRows = buildList {
            val start = result.fortuneStart
            add(CaseImageRenderRow("起运方向", start.direction.displayName()))
            add(CaseImageRenderRow("精确交运", start.startAt.displayText()))
            add(
                CaseImageRenderRow(
                    "起运年龄",
                    "${start.years}年 ${start.months}月 ${start.days}日 " +
                        "${start.hours}时 ${start.minutes}分",
                ),
            )
            result.decadeFortunes.forEachIndexed { index, decade ->
                add(
                    CaseImageRenderRow(
                        "大运 ${index + 1}",
                        "${decade.name}｜${decade.startAge}–${decade.endAge}岁｜" +
                            "${decade.startYear}–${decade.endYear}",
                    ),
                )
            }
        }
        val blocks = buildList {
            add(CaseImageRenderBlock.Rows("采用资料", identityRows))
            add(CaseImageRenderBlock.Rows("基础命盘", chartRows))
            if (pillarRows.isNotEmpty()) {
                add(CaseImageRenderBlock.Rows("四柱明细", pillarRows))
            }
            add(CaseImageRenderBlock.Rows("起运与大运", fortuneRows))
            add(CaseImageRenderBlock.Rows("计算档案", evidenceRows))
            input.caseData.textRecords
                .takeIf(List<CaseTextRecord>::isNotEmpty)
                ?.let { records ->
                    add(
                        CaseImageRenderBlock.Paragraphs(
                            "正式分析记录",
                            records.map(CaseTextRecord::displayText),
                        ),
                    )
                }
            input.caseData.events
                .takeIf(List<CaseEvent>::isNotEmpty)
                ?.let { events ->
                    add(
                        CaseImageRenderBlock.Paragraphs(
                            "关键事件",
                            events.map(CaseEvent::displayText),
                        ),
                    )
                }
        }
        return CaseImageFactsResult.Prepared(
            CaseImageRenderFacts(
                documentVersion = input.documentVersion,
                scope = input.scope,
                caseId = input.caseData.id,
                caseRevision = input.caseData.revision,
                adoptedSnapshotId = snapshot.id,
                title = input.caseData.name.value ?: input.caseData.alias,
                subtitle = "南枫八字 · 已采用命盘",
                suggestedFileStem = input.caseData.alias.take(48),
                blocks = blocks,
                provenanceNotice =
                    "本图只使用当前已采用的本机计算快照与正式记录；" +
                        "问真来源截图值、未采用候选和旧快照未作为计算真值。",
                privacyNotice =
                    "图片包含出生资料及研究记录。保存后请妥善保管；分享后由目标应用负责传输与存储。",
            ),
        )
    }
}

private fun BirthCalendarInput.displayText(): String = when (this) {
    is BirthCalendarInput.Solar -> "公历 ${dateTime.displayText()}"
    is BirthCalendarInput.Lunar ->
        "农历 ${if (dateTime.isLeapMonth) "闰" else ""}${dateTime.displayText()}"
}

private fun CivilDateTime.displayText(): String =
    "%04d-%02d-%02d %02d:%02d:%02d".format(year, month, day, hour, minute, second)

private fun LunarDateTime.displayText(): String =
    "%04d-%02d-%02d %02d:%02d:%02d".format(year, month, day, hour, minute, second)

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

private fun CaseTextRecord.displayText(): String {
    val typeText = when (type) {
        CaseTextRecordType.NOTE -> "笔记"
        CaseTextRecordType.OWNER_FEEDBACK -> "命主反馈"
        CaseTextRecordType.MASTER_COMMENTARY -> "师傅点评"
        CaseTextRecordType.ANALYSIS -> "分析"
    }
    val categoryText = analysisCategory?.displayName()?.let { " · $it" }.orEmpty()
    return "【$typeText$categoryText】$content"
}

private fun AnalysisCategory.displayName(): String = when (this) {
    AnalysisCategory.GENERAL -> "综合"
    AnalysisCategory.PERSONALITY -> "性格"
    AnalysisCategory.CAREER -> "事业"
    AnalysisCategory.WEALTH -> "财运"
    AnalysisCategory.RELATIONSHIP -> "婚姻感情"
    AnalysisCategory.HEALTH -> "健康"
    AnalysisCategory.EDUCATION -> "学业"
    AnalysisCategory.FAMILY -> "家庭"
    AnalysisCategory.OTHER -> "其他"
}

private fun CaseEvent.displayText(): String {
    val date = listOfNotNull(
        year?.toString(),
        month?.toString()?.padStart(2, '0'),
        day?.toString()?.padStart(2, '0'),
    ).joinToString("-").ifEmpty { "日期未知" }
    val titleText = title?.let { " · $it" }.orEmpty()
    val statusText = status?.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
    return "【$date · ${category.displayName()}$titleText$statusText】$rawText"
}

private fun CaseEventCategory.displayName(): String = when (this) {
    CaseEventCategory.GENERAL -> "综合"
    CaseEventCategory.EDUCATION -> "学业"
    CaseEventCategory.CAREER -> "事业"
    CaseEventCategory.WEALTH -> "财运"
    CaseEventCategory.RELATIONSHIP -> "婚恋"
    CaseEventCategory.FAMILY -> "家庭"
    CaseEventCategory.HEALTH -> "健康"
    CaseEventCategory.OTHER -> "其他"
}
