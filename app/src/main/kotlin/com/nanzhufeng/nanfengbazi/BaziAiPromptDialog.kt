package com.nanzhufeng.nanfengbazi

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisPrompt
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisPromptContract
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisPromptRequest
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisPromptResult
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisTopic
import com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityReport
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryContract
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryInput
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryObservation
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryResult
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortunePosition
import com.nanzhufeng.nanfengbazi.domain.structuralProfileOrAnalyze
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import java.time.LocalDate

@Composable
internal fun BasicChartAiPromptSection(
    case: BaziCase,
    observation: ProfessionalFortunePosition?,
    onCopied: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var dialogVisible by rememberSaveable(case.id) { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "AI 指令",
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            onClick = { dialogVisible = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .testTag("open_basic_chart_ai_prompt"),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFCFBF8),
            border = BorderStroke(1.dp, NanfengGold.copy(alpha = 0.24f)),
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = NanfengGold.copy(alpha = 0.14f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "AI",
                            color = NanfengGold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "生成命盘分析提示词",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = NanfengGold,
                )
            }
        }
    }
    if (dialogVisible) {
        BaziAiPromptDialog(
            case = case,
            observation = observation,
            onDismiss = { dialogVisible = false },
            onCopied = onCopied,
        )
    }
}

/** 合盘页只复制本次冻结报告，不读取或上传其他命例资料。 */
@Composable
internal fun CompatibilityAiPromptSection(
    report: BaziCompatibilityReport,
    modifier: Modifier = Modifier,
) {
    var dialogVisible by rememberSaveable(report.left.adoptedSnapshotId, report.right.adoptedSnapshotId) { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text("AI 合盘指令", modifier = Modifier.padding(start = 2.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Surface(
            onClick = { dialogVisible = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).testTag("open_compatibility_ai_prompt"),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFCFBF8),
            border = BorderStroke(1.dp, NanfengGold.copy(alpha = 0.24f)),
            shadowElevation = 1.dp,
        ) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = NanfengGold.copy(alpha = 0.14f)) {
                    Box(contentAlignment = Alignment.Center) { Text("AI", color = NanfengGold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("生成双方合盘分析提示词", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("含命盘、大运、断事笔记与关系证据", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(22.dp), tint = NanfengGold)
            }
        }
    }
    if (dialogVisible) CompatibilityAiPromptDialog(report, onDismiss = { dialogVisible = false })
}

@Composable
private fun CompatibilityAiPromptDialog(
    report: BaziCompatibilityReport,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var previewVisible by remember { mutableStateOf(false) }
    var copyError by remember { mutableStateOf<String?>(null) }
    val prompt = remember(report) { report.compatibilityAiPrompt() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).widthIn(max = 460.dp).testTag("compatibility_ai_prompt_dialog"),
            shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 12.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI 合盘指令", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("命盘、大运、关系命中与断事笔记", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("关闭") }
                }
                TextButton(onClick = { previewVisible = !previewVisible }, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp).testTag("toggle_compatibility_ai_prompt_preview")) { Text(if (previewVisible) "收起提示词" else "预览提示词") }
                if (previewVisible) CompatibilityAiPromptPreview(prompt)
                copyError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        if (copyPromptToClipboard(context, prompt)) onDismiss() else copyError = "复制失败，请确认系统剪贴板当前可用。"
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 52.dp).testTag("copy_compatibility_ai_prompt"),
                    shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = NanfengGold, contentColor = Color.White),
                ) { Text("复制到剪贴板") }
            }
        }
    }
}

private fun BaziCompatibilityReport.compatibilityAiPrompt(): String {
    fun participant(role: String, participant: com.nanzhufeng.nanfengbazi.domain.BaziCompatibilityParticipant): String = buildString {
        appendLine("## $role · ${participant.alias}")
        if (participant.solarDateTimeText.isNotBlank()) appendLine("- 公历：${participant.solarDateTimeText}")
        if (participant.lunarDateTimeText.isNotBlank()) appendLine("- 农历：${participant.lunarDateTimeText}")
        if (participant.zodiac.isNotBlank()) appendLine("- 生肖：${participant.zodiac}")
        appendLine("- 四柱：年柱 ${participant.pillars.year}｜月柱 ${participant.pillars.month}｜日柱 ${participant.pillars.day}｜时柱 ${participant.pillars.hour}")
        appendLine("- 日主：${participant.dayMaster}")
        appendLine("- 五行表层计数：${participant.pillars.compatibilityElementSummary()}")
        val structuralProfile = participant.structuralProfileOrAnalyze()
        appendLine("- 日主旺衰（算法候选）：${structuralProfile.strength.displayName}（${structuralProfile.strengthConfidence.displayName}置信）")
        appendLine("- 格局（算法候选）：${structuralProfile.selectedPattern.name}")
        appendLine("- 旺衰依据：${structuralProfile.strengthEvidence.joinToString("；") { it.detail }}")
        appendLine("- 格局依据：${structuralProfile.selectedPattern.evidence.joinToString("；") { it.detail }}")
        if (structuralProfile.reviewItems.isNotEmpty()) {
            appendLine("- 结构复核项：${structuralProfile.reviewItems.joinToString("；")}")
        }
        if (participant.sourceStructureText.isNotBlank()) {
            appendLine("- 问真记录格局：${participant.sourceStructureText}（仅作来源对照，不覆盖算法候选）")
        }
        participant.pillarPresentation
            .sortedBy { it.position.ordinal }
            .forEach { pillar ->
                appendLine("- ${pillar.position.compatibilityPromptLabel()}：十神 ${pillar.primaryTenGod}；天干 ${pillar.heavenStem}；地支 ${pillar.earthBranch}；藏干 ${pillar.hiddenStemSummary.ifBlank { "—" }}")
            }
        if (participant.decadeFortunes.isNotEmpty()) {
            appendLine("- 大运：${participant.decadeFortunes.joinToString("｜") { "${it.name}（${it.ageRange}）" }}")
        }
        if (participant.referenceNotes.isNotEmpty()) {
            appendLine("- 断事笔记与已记录应事：")
            participant.referenceNotes.forEachIndexed { index, note ->
                appendLine("  ${index + 1}. 【${note.label}】${note.content}")
            }
        } else {
            appendLine("- 断事笔记与已记录应事：未提供")
        }
    }
    fun signals(title: String, items: List<com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySignal>): String = buildString {
        appendLine("## $title")
        if (items.isEmpty()) {
            appendLine("- 未检出")
        } else {
            items.groupBy { it.kind to it.values }.forEach { (key, grouped) ->
                val leftEvidence = grouped.flatMap { signal ->
                    signal.pillars.filter { it.side == com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySide.LEFT }
                        .map { "${it.position.compatibilityPromptLabel()} ${it.value}" }
                }.distinct().joinToString("、").ifBlank { "未参与" }
                val rightEvidence = grouped.flatMap { signal ->
                    signal.pillars.filter { it.side == com.nanzhufeng.nanfengbazi.domain.BaziCompatibilitySide.RIGHT }
                        .map { "${it.position.compatibilityPromptLabel()} ${it.value}" }
                }.distinct().joinToString("、").ifBlank { "未参与" }
                appendLine("- ${key.first.title}｜${key.second}｜男方：$leftEvidence｜女方：$rightEvidence｜规则含义：${grouped.first().explanation}")
            }
        }
    }
    return buildString {
        appendLine("# 角色")
        appendLine("你是一名同时熟悉盲派断事、子平格局、十神、宫位、合冲刑害、夫妻宫与大运并行判断的八字合盘分析师。请针对以下两份固定资料，完成一次证据充分、信息密度高的双方合盘分析。")
        appendLine()
        appendLine("# 分析要求")
        appendLine("1. 先看四柱组合、宫位、体用和岁运触发，再用月令、旺衰、十神、格局、调候和喜忌交叉核对。")
        appendLine("2. 每一个判断都必须指出具体依据：哪一方的哪一柱、哪个十神、哪组五行、哪条合冲刑害、哪个夫妻宫或哪段大运。")
        appendLine("3. 不要泛泛使用“性格互补、需要沟通、缘分较深”等套话；没有具体命盘依据的内容不要写。")
        appendLine("4. 断事笔记与应事记录是核验材料，不得改写成命盘原始事实。逐条说明它支持、削弱或暂时无法验证哪些判断。")
        appendLine("5. 五行表层计数只用于观察结构分布；旺衰、格局、喜忌必须结合月令、藏干、通根与全局组合判断。")
        appendLine()
        appendLine("# 双方命盘资料")
        append(participant("男方", left))
        appendLine()
        append(participant("女方", right))
        appendLine()
        appendLine("# 双方已计算的关系证据")
        appendLine("日主关系：男方 ${left.dayMaster}（${dayMasterRelation.leftElement}）｜女方 ${right.dayMaster}（${dayMasterRelation.rightElement}）｜${dayMasterRelation.description}｜${dayMasterRelation.explanation}")
        append(signals("相合与呼应", coordinationSignals))
        append(signals("需要留意的关系", tensionSignals))
        if (warnings.isNotEmpty()) appendLine("资料精度：${warnings.joinToString("；") { it.message }}")
        appendLine()
        appendLine("# 输出结构")
        appendLine("1. 资料核对表：列出双方四柱、日主、夫妻宫（日支）、十神、藏干、五行结构和大运；指出资料中真正缺失的字段。")
        appendLine("2. 男方命局判断：分别写盲派断事观察与子平交叉核对，说明日主、月令、十神组合、夫妻宫、配偶星、五行流通、用忌取向及大运重点。")
        appendLine("3. 女方命局判断：使用与男方完全相同的维度，不得只给概括性评价。")
        appendLine("4. 合盘核心对照表：逐项写【对比项目｜男方命盘落点｜女方命盘落点｜作用机制｜对感情/婚姻的具体影响】；至少覆盖日主、夫妻宫、配偶星、天干关系、地支关系、藏干、五行互补与冲突。")
        appendLine("5. 关系中的优势：从共同目标、情感表达、分工协作、事业财务、家庭互动五个方向筛选真正有命盘依据的优势，并按依据强弱排序。")
        appendLine("6. 关系中的矛盾机制：明确最容易出现分歧的触发点、双方各自的反应模式、冲突升级链路，以及可由双方共同执行的化解方式。")
        appendLine("7. 大运并行：按双方年龄段并列分析大运同频、错位、相互支持或相互消耗的阶段；有时间线笔记时，优先用已发生事项回验。")
        appendLine("8. 断事笔记核验：逐条引用双方笔记，写明【笔记内容｜对应命盘/岁运依据｜吻合程度｜还能继续追问的关键事实】。")
        appendLine("9. 最终结论：用 6 条以内给出双方最强的结合基础、最需要面对的结构问题、适合的相处模式、对财务/家庭/事业协同的判断，以及下一步最值得核实的现实问题。")
    }
}

private fun FourPillars.compatibilityElementSummary(): String =
    listOf(year, month, day, hour)
        .flatMap { listOf(it.firstOrNull(), it.lastOrNull()) }
        .mapNotNull { character -> character?.compatibilityPromptElement() }
        .groupingBy { it }
        .eachCount()
        .let { counts -> listOf("木", "火", "土", "金", "水").joinToString("｜") { "$it ${counts[it] ?: 0}/8" } }

private fun Char.compatibilityPromptElement(): String? = when (this) {
    '甲', '乙', '寅', '卯' -> "木"
    '丙', '丁', '巳', '午' -> "火"
    '戊', '己', '辰', '戌', '丑', '未' -> "土"
    '庚', '辛', '申', '酉' -> "金"
    '壬', '癸', '亥', '子' -> "水"
    else -> null
}

private fun com.nanzhufeng.nanfengbazi.domain.model.PillarPosition.compatibilityPromptLabel(): String = when (this) {
    com.nanzhufeng.nanfengbazi.domain.model.PillarPosition.YEAR -> "年柱"
    com.nanzhufeng.nanfengbazi.domain.model.PillarPosition.MONTH -> "月柱"
    com.nanzhufeng.nanfengbazi.domain.model.PillarPosition.DAY -> "日柱"
    com.nanzhufeng.nanfengbazi.domain.model.PillarPosition.HOUR -> "时柱"
}

@Composable
private fun CompatibilityAiPromptPreview(copyText: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(max = 190.dp).testTag("compatibility_ai_prompt_preview"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8F8F6),
    ) {
        SelectionContainer {
            Text(
                copyText,
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BaziAiPromptDialog(
    case: BaziCase,
    observation: ProfessionalFortunePosition?,
    onDismiss: () -> Unit,
    onCopied: () -> Unit,
) {
    val context = LocalContext.current
    val referenceDate = observation?.position?.observedAt?.let {
        LocalDate.of(it.year, it.month, it.day)
    } ?: remember { LocalDate.now() }
    val summaryObservation = observation?.let {
        CaseObjectiveSummaryObservation(
            referenceDate = referenceDate,
            professionalPosition = it,
        )
    }
    val summaryResult = remember(case.id, case.revision, summaryObservation) {
        CaseObjectiveSummaryContract.generate(
            CaseObjectiveSummaryInput(
                caseData = case,
                observation = summaryObservation,
            ),
        )
    }
    var topic by rememberSaveable(case.id) { mutableStateOf(BaziAiAnalysisTopic.ALL) }
    var hideIdentityAndLocation by remember(case.id) { mutableStateOf(true) }
    var previewVisible by remember(case.id) { mutableStateOf(false) }
    var copyError by remember(case.id) { mutableStateOf<String?>(null) }
    val ownerFeedback = remember(case.id, case.revision) {
        case.toAiAnalysisOwnerFeedback()
    }
    val promptResult = remember(
        summaryResult,
        topic,
        hideIdentityAndLocation,
        referenceDate,
        ownerFeedback,
    ) {
        when (summaryResult) {
            is CaseObjectiveSummaryResult.Success -> BaziAiAnalysisPromptContract.prepare(
                BaziAiAnalysisPromptRequest(
                    summary = summaryResult.summary,
                    topic = topic,
                    referenceDate = referenceDate,
                    hideIdentityAndLocation = hideIdentityAndLocation,
                    ownerFeedback = ownerFeedback,
                ),
            )
            is CaseObjectiveSummaryResult.Rejected -> null
        }
    }
    val prompt = (promptResult as? BaziAiAnalysisPromptResult.Success)?.prompt
    val generationError = when {
        summaryResult is CaseObjectiveSummaryResult.Rejected -> summaryResult.failure.message
        promptResult is BaziAiAnalysisPromptResult.Rejected -> promptResult.failure.message
        else -> null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .widthIn(max = 460.dp)
                .testTag("basic_chart_ai_prompt_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "AI 指令",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "基于当前已采用命盘生成",
                            modifier = Modifier.padding(top = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("close_basic_chart_ai_prompt"),
                    ) {
                        Text("关闭")
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = NanfengControlSurface.copy(alpha = 0.72f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            "复制后可粘贴到 DeepSeek、ChatGPT、豆包等模型使用。",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "App 只写入系统剪贴板，不联网、不自动发送；提示词要求模型区分命盘事实与推演，并在开头和结尾保留娱乐参考声明。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    "分析主题",
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                BaziAiTopicGrid(
                    selected = topic,
                    onSelect = {
                        topic = it
                        copyError = null
                    },
                )

                Surface(
                    onClick = {
                        hideIdentityAndLocation = !hideIdentityAndLocation
                        copyError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .semantics(mergeDescendants = true) {
                            role = Role.Switch
                            stateDescription = if (hideIdentityAndLocation) "已开启" else "已关闭"
                        }
                        .testTag("ai_prompt_privacy_row"),
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFFFAFAF8),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("隐去姓名与地区", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "推荐开启；出生时间、性别和四柱仍会保留",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = hideIdentityAndLocation,
                            onCheckedChange = null,
                            modifier = Modifier
                                .size(width = 56.dp, height = 48.dp)
                                .testTag("ai_prompt_privacy_switch"),
                        )
                    }
                }

                prompt?.let {
                    Text(
                        "将复制 ${it.includedFieldCount} 个命盘字段" +
                            if (it.hiddenFieldCount > 0) "，已隐去 ${it.hiddenFieldCount} 个身份字段" else "",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { previewVisible = !previewVisible },
                        modifier = Modifier
                            .align(Alignment.End)
                            .heightIn(min = 48.dp)
                            .testTag("toggle_ai_prompt_preview"),
                    ) {
                        Text(if (previewVisible) "收起提示词" else "预览提示词")
                    }
                    if (previewVisible) {
                        AiPromptPreview(prompt)
                    }
                }
                (generationError ?: copyError)?.let { error ->
                    Text(
                        error,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .testTag("ai_prompt_error"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = {
                        val copied = prompt?.let {
                            copyPromptToClipboard(context, it.copyText)
                        } == true
                        if (copied) {
                            onDismiss()
                            onCopied()
                        } else {
                            copyError = "复制失败，请确认系统剪贴板当前可用。"
                        }
                    },
                    enabled = prompt != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .heightIn(min = 52.dp)
                        .testTag("copy_basic_chart_ai_prompt"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NanfengGold,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("复制到剪贴板")
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun BaziAiTopicGrid(
    selected: BaziAiAnalysisTopic,
    onSelect: (BaziAiAnalysisTopic) -> Unit,
) {
    BaziAiAnalysisTopic.entries.chunked(4).forEachIndexed { rowIndex, rowTopics ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (rowIndex == 0) 0.dp else 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rowTopics.forEach { topic ->
                val active = topic == selected
                Surface(
                    onClick = { onSelect(topic) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("ai_prompt_topic_${topic.name.lowercase()}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) NanfengGold else NanfengControlSurface.copy(alpha = 0.7f),
                    shadowElevation = if (active) 1.dp else 0.dp,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            topic.displayName,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiPromptPreview(prompt: BaziAiAnalysisPrompt) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 190.dp)
            .testTag("ai_prompt_preview"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8F8F6),
    ) {
        SelectionContainer {
            Text(
                prompt.copyText,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun copyPromptToClipboard(
    context: android.content.Context,
    text: String,
): Boolean {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    return tryWritePromptToClipboard(
        writer = PromptClipboardWriter { label, value ->
            clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        },
        text = text,
    )
}

internal fun interface PromptClipboardWriter {
    fun write(label: String, text: String)
}

internal fun tryWritePromptToClipboard(
    writer: PromptClipboardWriter,
    text: String,
): Boolean = runCatching {
    require(text.isNotBlank()) { "复制内容不能为空" }
    writer.write("南枫八字 AI 指令", text)
    true
}.getOrDefault(false)
