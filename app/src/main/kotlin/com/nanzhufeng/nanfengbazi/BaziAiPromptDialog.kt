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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisPrompt
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisPromptContract
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisPromptRequest
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisPromptResult
import com.nanzhufeng.nanfengbazi.domain.BaziAiAnalysisTopic
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryContract
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryInput
import com.nanzhufeng.nanfengbazi.domain.CaseObjectiveSummaryResult
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import java.time.LocalDate

@Composable
internal fun BasicChartAiPromptSection(
    case: BaziCase,
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
            onDismiss = { dialogVisible = false },
        )
    }
}

@Composable
private fun BaziAiPromptDialog(
    case: BaziCase,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val referenceDate = remember { LocalDate.now() }
    val summaryResult = remember(case.id, case.revision) {
        CaseObjectiveSummaryContract.generate(CaseObjectiveSummaryInput(case))
    }
    var topic by rememberSaveable(case.id) { mutableStateOf(BaziAiAnalysisTopic.ALL) }
    var hideIdentityAndLocation by rememberSaveable(case.id) { mutableStateOf(true) }
    var previewVisible by rememberSaveable(case.id) { mutableStateOf(false) }
    var copyError by rememberSaveable(case.id) { mutableStateOf<String?>(null) }
    val promptResult = remember(summaryResult, topic, hideIdentityAndLocation, referenceDate) {
        when (summaryResult) {
            is CaseObjectiveSummaryResult.Success -> BaziAiAnalysisPromptContract.prepare(
                BaziAiAnalysisPromptRequest(
                    summary = summaryResult.summary,
                    topic = topic,
                    referenceDate = referenceDate,
                    hideIdentityAndLocation = hideIdentityAndLocation,
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
                        modifier = Modifier.testTag("close_basic_chart_ai_prompt"),
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
                        .testTag("ai_prompt_privacy_row"),
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFFFAFAF8),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    ),
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
                            modifier = Modifier.testTag("ai_prompt_privacy_switch"),
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
                        .heightIn(min = 42.dp)
                        .testTag("ai_prompt_topic_${topic.name.lowercase()}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) NanfengGold else NanfengControlSurface.copy(alpha = 0.7f),
                    border = if (active) null else BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    ),
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
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
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
): Boolean = runCatching {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText("南枫八字 AI 指令", text))
    clipboard.primaryClip
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString() == text
}.getOrDefault(false)
