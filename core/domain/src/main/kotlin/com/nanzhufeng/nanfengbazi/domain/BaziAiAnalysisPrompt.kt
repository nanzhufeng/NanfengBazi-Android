package com.nanzhufeng.nanfengbazi.domain

import java.security.MessageDigest
import java.time.LocalDate

const val BAZI_AI_ANALYSIS_PROMPT_VERSION: Int = 1

enum class BaziAiAnalysisTopic(
    val displayName: String,
    val taskText: String,
) {
    ALL(
        "全项",
        "事业、财运、婚恋、子女、六亲、健康和学业七个维度",
    ),
    CAREER("事业", "事业方向、职业适配、岗位变化、创业与合作风险"),
    WEALTH("财运", "收入结构、积累能力、耗财风险与财富节奏"),
    RELATIONSHIP("婚恋", "关系模式、适配倾向、重要阶段与相处风险"),
    CHILDREN("子女", "子女缘分、生育阶段与亲子关系倾向"),
    FAMILY("六亲", "父母、兄弟姐妹及重要亲属的关系与支持边界"),
    HEALTH("健康", "传统命理中的体质象意、生活习惯与健康关注点"),
    EDUCATION("学业", "学习方式、优势方向、考试进修与阶段节奏"),
}

data class BaziAiAnalysisPromptRequest(
    val summary: CaseObjectiveSummary,
    val topic: BaziAiAnalysisTopic = BaziAiAnalysisTopic.ALL,
    val referenceDate: LocalDate,
    val hideIdentityAndLocation: Boolean = true,
    val promptVersion: Int = BAZI_AI_ANALYSIS_PROMPT_VERSION,
)

data class BaziAiAnalysisPrompt(
    val version: Int,
    val id: String,
    val topic: BaziAiAnalysisTopic,
    val referenceDate: LocalDate,
    val includedFieldCount: Int,
    val hiddenFieldCount: Int,
    val copyText: String,
)

enum class BaziAiAnalysisPromptErrorCode {
    UNSUPPORTED_VERSION,
    REQUIRED_SECTION_MISSING,
    NO_USABLE_FIELDS,
}

data class BaziAiAnalysisPromptFailure(
    val code: BaziAiAnalysisPromptErrorCode,
    val message: String,
)

sealed interface BaziAiAnalysisPromptResult {
    data class Success(val prompt: BaziAiAnalysisPrompt) : BaziAiAnalysisPromptResult
    data class Rejected(val failure: BaziAiAnalysisPromptFailure) : BaziAiAnalysisPromptResult
}

fun interface BaziAiAnalysisPromptBuilder {
    fun prepare(request: BaziAiAnalysisPromptRequest): BaziAiAnalysisPromptResult
}

object BaziAiAnalysisPromptContract : BaziAiAnalysisPromptBuilder {
    override fun prepare(request: BaziAiAnalysisPromptRequest): BaziAiAnalysisPromptResult {
        if (request.promptVersion != BAZI_AI_ANALYSIS_PROMPT_VERSION) {
            return rejected(
                BaziAiAnalysisPromptErrorCode.UNSUPPORTED_VERSION,
                "暂不支持 AI 指令版本 ${request.promptVersion}。",
            )
        }
        val sectionsById = request.summary.sections.associateBy { it.id }
        val requiredSections = listOf("birth_facts", "chart_facts", "fortune_facts")
        val missingSection = requiredSections.firstOrNull { it !in sectionsById }
        if (missingSection != null) {
            return rejected(
                BaziAiAnalysisPromptErrorCode.REQUIRED_SECTION_MISSING,
                "客观摘要缺少生成 AI 指令所需的命盘资料。",
            )
        }

        val fields = requiredSections.flatMap { id -> sectionsById.getValue(id).fields }
        val hiddenSensitivities = if (request.hideIdentityAndLocation) {
            AI_PRIVATE_SENSITIVITIES
        } else {
            emptySet()
        }
        val visibleFields = fields.filterNot { it.sensitivity in hiddenSensitivities }
        if (visibleFields.isEmpty()) {
            return rejected(
                BaziAiAnalysisPromptErrorCode.NO_USABLE_FIELDS,
                "当前命例没有可用于生成 AI 指令的字段。",
            )
        }
        val hiddenCount = fields.count { it.sensitivity in hiddenSensitivities }
        val identity = listOf(
            request.promptVersion.toString(),
            request.summary.caseId,
            request.summary.caseRevision.toString(),
            request.summary.adoptedSnapshotId,
            request.topic.name,
            request.referenceDate.toString(),
            request.hideIdentityAndLocation.toString(),
            visibleFields.joinToString("\u0000") {
                "${it.label}|${it.value}|${it.source}|${it.sensitivity}"
            },
        ).joinToString("\u0001")
        val promptId = "ai-prompt-${identity.sha256().take(16)}"
        return BaziAiAnalysisPromptResult.Success(
            BaziAiAnalysisPrompt(
                version = request.promptVersion,
                id = promptId,
                topic = request.topic,
                referenceDate = request.referenceDate,
                includedFieldCount = visibleFields.size,
                hiddenFieldCount = hiddenCount,
                copyText = buildPromptText(
                    request,
                    promptId,
                    sectionsById,
                    hiddenSensitivities,
                ),
            ),
        )
    }

    private val AI_PRIVATE_SENSITIVITIES = setOf(
        CaseObjectiveSummarySensitivity.IDENTITY,
        CaseObjectiveSummarySensitivity.LOCATION,
    )
}

private fun buildPromptText(
    request: BaziAiAnalysisPromptRequest,
    promptId: String,
    sectionsById: Map<String, CaseObjectiveSummarySection>,
    hiddenSensitivities: Set<CaseObjectiveSummarySensitivity>,
): String = buildString {
    val startYear = request.referenceDate.year - 5
    val endYear = request.referenceDate.year + 5
    appendLine("# 角色")
    appendLine("你是一名熟悉盲派命理、主流子平命理与常见现代命理分析方法的传统文化研究者。盲派命理须作为独立分析路径，重点用于四柱组合、宫位、体用与岁运触发的实际断事观察；随后再以子平格局、旺衰、十神和喜用忌神进行交叉核对。请基于下方固定命盘资料展开分析，所有判断必须说明依据、流派假设和不确定性，不得把推演写成已经发生的事实。")
    appendLine()
    appendLine("# 分析边界")
    appendLine("1. 开头必须原样声明：本分析为文化娱乐参考，非专业决策依据，具体发展需结合个人努力与客观环境。")
    appendLine("2. 只使用本提示词中的资料；未提供的事实明确写“资料未提供”，不得虚构经历、家庭情况、疾病、资产或关系状态。")
    appendLine("3. 月令旺相、格局、身强身弱、调候用神、喜用忌神、五行权重和事件时间属于模型推演，必须与“输入事实”分开标注，并给出推理链与可能的不同流派结论；不得把表层五行计数直接等同于旺衰或喜忌。")
    appendLine("4. 关键事件只给可能的时间范围、传统命理喜忌属性、影响程度和观察信号，不使用“必然、一定、注定”等确定措辞。")
    appendLine("5. 健康内容不得作疾病诊断、治疗或手术结论；财运内容不构成投资建议；子女数量、性别和生育时间不得写成确定事实。")
    appendLine("6. 不联网补充个人资料。涉及下蛊、伤害他人、破坏他人命运或断人财路等要求时直接拒绝。")
    appendLine()
    appendLine("# 命盘资料")
    appendLine("资料编号：$promptId")
    appendLine("分析参考日期：${request.referenceDate}")
    appendLine("隐私处理：${if (request.hideIdentityAndLocation) "已隐去姓名、别名、地区、经纬度和时区标识；保留排盘所需出生时间与性别口径" else "未隐去命例资料"}")
    listOf("birth_facts", "chart_facts", "fortune_facts").forEach { sectionId ->
        val section = sectionsById.getValue(sectionId)
        appendLine()
        appendLine("## ${section.title}")
        section.fields.filterNot { it.sensitivity in hiddenSensitivities }.forEach { field ->
            appendLine("- ${field.label}：${field.value}")
        }
    }
    appendLine()
    appendLine("# 本次任务")
    appendLine("重点分析：${request.topic.taskText}。")
    appendLine("先完成命盘技法解读，再围绕所选主题给出结构化结论。分析 $startYear—$endYear 的大运流年趋势时，逐年说明依据；若资料不足以支持某年细断，应明确保留判断，不得补造输入字段。")
    appendLine()
    appendLine("# 输出格式")
    appendLine("1. 输入事实核对：简要复述采用的四柱、日主、十神、藏干、起运和大运资料，并列出缺失项。")
    appendLine("2. 命盘技法解读：先单列“盲派断事观察”，再写“子平格局与现代方法交叉核对”；涵盖五行生克、十神组合、月令旺相、格局／身强身弱／调候用神／喜用忌神，并明确区分事实与推演。")
    if (request.topic == BaziAiAnalysisTopic.ALL) {
        appendLine("3. 命盘事项解读：依次分析事业、财运、婚恋、子女、六亲、健康和学业。")
    } else {
        appendLine("3. ${request.topic.displayName}专题解读：现状倾向、优势、风险、适配方向和可执行建议。")
    }
    appendLine("4. $startYear—$endYear 走势：按年份列出主题、依据、喜忌属性、影响程度和不确定性。")
    appendLine("5. 核心建议与风险规避：区分近期行动、长期方向和需结合现实核验的事项。")
    appendLine("6. 结尾再次提醒：传统命理分析仅供文化娱乐参考，不构成医疗、法律、投资、婚姻或职业决策依据。")
    appendLine("7. 最后一行提示：如需继续提问，请说明具体问题，并继续只依据本命盘资料分析。")
}

private fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

private fun rejected(
    code: BaziAiAnalysisPromptErrorCode,
    message: String,
): BaziAiAnalysisPromptResult = BaziAiAnalysisPromptResult.Rejected(
    BaziAiAnalysisPromptFailure(code, message),
)
