package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanzhufeng.nanfengbazi.domain.ExternalAnalysisFieldGroup
import com.nanzhufeng.nanfengbazi.domain.EXTERNAL_ANALYSIS_RESULT_MAX_LENGTH

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExternalAnalysisBridgeScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onSetGroupSelected: (ExternalAnalysisFieldGroup, Boolean) -> Unit,
    onSetRedaction: (Boolean) -> Unit,
    onSetExportConfirmed: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onProviderChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onResultChange: (String) -> Unit,
    onSetImportConfirmed: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("external_analysis_screen"),
    ) {
        TopAppBar(
            title = { Text("外部分析桥接") },
            navigationIcon = {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("返回")
                }
            },
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("external_analysis_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("external_analysis_boundary"),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            "纯本地手动桥接",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "南枫八字不会联网、不会调用外部 AI，也不会自动发送命例。" +
                                "这里只生成你可以手动复制的材料，并接收你主动粘贴的结果。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "四柱和岁运本身仍属于敏感资料；默认脱敏只隐藏身份、出生时间、" +
                                "地点和时区，不能让命盘完全匿名。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "1. 选择要复制的字段",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("默认脱敏")
                                Text(
                                    "隐藏身份、精确出生时间、地点和时区",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = state.externalAnalysisDraft.redactionEnabled,
                                onCheckedChange = onSetRedaction,
                                modifier = Modifier.testTag("external_analysis_redaction_switch"),
                            )
                        }
                        ExternalAnalysisFieldGroup.entries.forEach { group ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = group in
                                        state.externalAnalysisDraft.selectedGroups,
                                    onCheckedChange = { onSetGroupSelected(group, it) },
                                    modifier = Modifier.testTag(
                                        "external_analysis_group_${group.name.lowercase()}",
                                    ),
                                )
                                Text(group.displayName)
                            }
                        }
                    }
                }
            }

            state.externalAnalysisPayload?.let { payload ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("external_analysis_preview"),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "精确复制预览",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "材料 ${payload.id} · ${payload.fields.size} 个字段 · " +
                                    "脱敏 ${payload.fields.count { it.redacted }} 个",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            SelectionContainer {
                                Text(
                                    payload.copyText,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = state.externalAnalysisDraft.exportConfirmed,
                            role = Role.Checkbox,
                            onValueChange = onSetExportConfirmed,
                        )
                        .testTag("external_analysis_export_confirm"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.externalAnalysisDraft.exportConfirmed,
                        onCheckedChange = null,
                    )
                    Text("我已核对上方全部字段和脱敏状态，确认复制到系统剪贴板。")
                }
            }

            item {
                Button(
                    onClick = onCopy,
                    enabled = state.externalAnalysisPayload != null &&
                        state.externalAnalysisDraft.exportConfirmed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("copy_external_analysis_button"),
                ) {
                    Text(if (state.externalAnalysisCopied) "已复制材料" else "确认并复制材料")
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "2. 手动回填外部结果",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "请从外部工具复制结果，再在这里填写真实来源并粘贴。" +
                                "应用不会自动解析结果或修改排盘。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = state.externalAnalysisDraft.providerName,
                            onValueChange = onProviderChange,
                            label = { Text("外部来源（必填）") },
                            supportingText = { Text("例如服务名、分析者或机构名称") },
                            singleLine = true,
                            enabled = !state.externalAnalysisSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("external_analysis_provider"),
                        )
                        OutlinedTextField(
                            value = state.externalAnalysisDraft.modelName,
                            onValueChange = onModelChange,
                            label = { Text("模型/版本（选填）") },
                            singleLine = true,
                            enabled = !state.externalAnalysisSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("external_analysis_model"),
                        )
                        OutlinedTextField(
                            value = state.externalAnalysisDraft.resultText,
                            onValueChange = onResultChange,
                            label = { Text("外部分析结果") },
                            supportingText = {
                                Text(
                                    "${state.externalAnalysisDraft.resultText.length}/" +
                                        EXTERNAL_ANALYSIS_RESULT_MAX_LENGTH,
                                )
                            },
                            minLines = 6,
                            enabled = !state.externalAnalysisSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("external_analysis_result"),
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = state.externalAnalysisDraft.importConfirmed,
                            enabled = !state.externalAnalysisSaving,
                            role = Role.Checkbox,
                            onValueChange = {
                                focusManager.clearFocus(force = true)
                                onSetImportConfirmed(it)
                            },
                        )
                        .testTag("external_analysis_import_confirm"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.externalAnalysisDraft.importConfirmed,
                        onCheckedChange = null,
                        enabled = !state.externalAnalysisSaving,
                    )
                    Text(
                        "我确认这是外部内容，只保存为带来源的研究记录，" +
                            "不把它当作本机算法真值。",
                    )
                }
            }

            state.externalAnalysisFailure?.let { failure ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("external_analysis_error"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Text(
                            "${failure.message}（${failure.code}）",
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onSave,
                    enabled = !state.externalAnalysisSaving &&
                        state.externalAnalysisDraft.importConfirmed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("save_external_analysis_button"),
                ) {
                    Text(if (state.externalAnalysisSaving) "正在保存…" else "保存为外部分析记录")
                }
            }
            item {
                Spacer(
                    Modifier
                        .height(128.dp)
                        .testTag("external_analysis_end"),
                )
            }
        }
    }
}
