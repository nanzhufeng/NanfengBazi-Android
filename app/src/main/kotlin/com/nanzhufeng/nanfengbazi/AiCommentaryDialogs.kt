package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun AiCommentaryDialogs(
    state: AiCommentaryUiState,
    onDismissCommentary: () -> Unit,
    onDismissServiceMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onDismissHistory: () -> Unit,
    onSaveProvider: (AiCommentaryProviderConfig, String?) -> Unit,
    onSelectProvider: (AiCommentaryProviderId) -> Unit,
    onPrivacyConfirmed: (Boolean) -> Unit,
    onGenerate: () -> Unit,
    onContentChange: (String) -> Unit,
    onSaveCommentary: () -> Unit,
) {
    if (state.settingsVisible) {
        AiCommentarySettingsDialog(
            configs = state.configs,
            savedApiKeys = state.apiKeys,
            initialProvider = state.selectedProvider,
            error = state.error,
            onDismiss = onDismissSettings,
            onSave = onSaveProvider,
        )
    } else if (state.historyVisible) {
        AiCommentaryCallHistoryDialog(
            records = state.callRecords,
            onDismiss = onDismissHistory,
        )
    } else if (state.serviceMenuVisible) {
        AiCommentaryServiceMenuDialog(
            configs = state.configs,
            records = state.callRecords,
            onOpenSettings = onOpenSettings,
            onOpenHistory = onOpenHistory,
            onDismiss = onDismissServiceMenu,
        )
    } else if (state.dialogVisible) {
        AiCommentaryGenerationDialog(
            state = state,
            onDismiss = onDismissCommentary,
            onOpenSettings = onOpenSettings,
            onSelectProvider = onSelectProvider,
            onPrivacyConfirmed = onPrivacyConfirmed,
            onGenerate = onGenerate,
            onContentChange = onContentChange,
            onSave = onSaveCommentary,
        )
    }
}

@Composable
private fun AiCommentaryServiceMenuDialog(
    configs: Map<AiCommentaryProviderId, AiCommentaryProviderConfig>,
    records: List<AiCommentaryCallRecord>,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)
                .testTag("ai_service_menu_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AI 模型服务",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭 AI 模型服务",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                AiServiceMenuEntry(
                    title = "模型设置",
                    description = "已配置 ${configs.values.count { it.enabled && it.hasApiKey }} / ${AiCommentaryProviderPresets.providerIds.size} 个服务",
                    tag = "ai_service_open_settings",
                    onClick = onOpenSettings,
                )
                AiServiceMenuEntry(
                    title = "调用记录",
                    description = if (records.isEmpty()) "暂无记录" else "最近 ${records.size} 条",
                    tag = "ai_service_open_history",
                    onClick = onOpenHistory,
                )
            }
        }
    }
}

@Composable
private fun AiServiceMenuEntry(
    title: String,
    description: String,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(tag),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiCommentaryCallHistoryDialog(
    records: List<AiCommentaryCallRecord>,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.84f).widthIn(max = 720.dp)
                .testTag("ai_call_history_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 16.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("调用记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "仅记录模型、用量、估算金额和运行状态",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("返回") }
                }
                HorizontalDivider()
                if (records.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无 AI 调用记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        records.forEach { record -> AiCallRecordCard(record) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiCallRecordCard(record: AiCommentaryCallRecord) {
    val time = remember(record.requestedAtEpochMillis) {
        Instant.ofEpochMilli(record.requestedAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (record.succeeded) "成功" else "失败",
                    color = if (record.succeeded) NanfengGreen else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${record.providerName} · ${record.model}", style = MaterialTheme.typography.bodyMedium)
            Text(
                buildString {
                    append("耗时 ${record.durationMillis} ms")
                    if (record.inputTokens != null || record.outputTokens != null) {
                        append(" · 输入 ${record.inputTokens ?: "-"} / 输出 ${record.outputTokens ?: "-"} tokens")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (record.succeeded && (record.inputTokens != null || record.outputTokens != null)) {
                val amount = record.estimatedCostOrNull()?.let(AiCommentaryCnyMoneyDisplay::label)
                Text(
                    "估算金额：${amount ?: "暂不可估算"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            record.errorSummary?.takeIf(String::isNotBlank)?.let { error ->
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AiCommentaryGenerationDialog(
    state: AiCommentaryUiState,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectProvider: (AiCommentaryProviderId) -> Unit,
    onPrivacyConfirmed: (Boolean) -> Unit,
    onGenerate: () -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .widthIn(max = 720.dp)
                .testTag("ai_commentary_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 16.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("AI 点评", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "生成后先预览编辑，确认才保存",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onOpenSettings, enabled = !state.generating && !state.saving) {
                        Text("模型设置")
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !state.generating && !state.saving,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭 AI 点评",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("选择模型服务", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiCommentaryProviderPresets.providerIds.forEach { providerId ->
                            val config = state.configs[providerId]
                            val available = config?.enabled == true && config.hasApiKey
                            Surface(
                                onClick = { onSelectProvider(providerId) },
                                modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                                    .testTag("ai_provider_${providerId.name.lowercase()}"),
                                shape = RoundedCornerShape(13.dp),
                                color = if (state.selectedProvider == providerId) {
                                    NanfengGold.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                },
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(providerId.displayName, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        if (available) "可用" else "待配置",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (available) NanfengGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    state.configs[state.selectedProvider]?.let { config ->
                        Text(
                            "当前：${AiCommentaryProviderPresets.displayModel(config.providerId, config.model)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        onClick = { onPrivacyConfirmed(!state.privacyConfirmed) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = state.privacyConfirmed,
                                onCheckedChange = onPrivacyConfirmed,
                                enabled = !state.generating,
                            )
                            Column(Modifier.weight(1f)) {
                                Text("确认发送脱敏命盘资料及命主反馈（如有）", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "隐藏姓名、别名、地区、经纬度和时区；保留四柱、出生时间、性别口径、十神、岁运，以及已保存的反馈总结和时间线事件。资料会发送给所选第三方并可能产生费用。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (state.generating) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(24.dp))
                            Text("正在生成结构化点评…", modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.generatedDraft != null) {
                        Text("点评预览", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${state.generatedDraft.providerName} · ${state.generatedDraft.model} · 提示版本 ${state.generatedDraft.promptVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = state.editedContent,
                            onValueChange = onContentChange,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp)
                                .testTag("ai_commentary_preview_input"),
                            label = { Text("可在保存前修改") },
                        )
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onGenerate,
                        enabled = !state.generating && !state.saving,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                            disabledContentColor = Color.White.copy(alpha = 0.78f),
                        ),
                    ) {
                        Text(if (state.generatedDraft == null) "生成点评" else "重新生成")
                    }
                    Button(
                        onClick = onSave,
                        enabled = state.generatedDraft != null && state.editedContent.isNotBlank() && !state.generating && !state.saving,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("save_ai_commentary"),
                    ) {
                        Text(if (state.saving) "保存中" else "保存点评")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiCommentarySettingsDialog(
    configs: Map<AiCommentaryProviderId, AiCommentaryProviderConfig>,
    savedApiKeys: Map<AiCommentaryProviderId, String>,
    initialProvider: AiCommentaryProviderId,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (AiCommentaryProviderConfig, String?) -> Unit,
) {
    var providerId by rememberSaveable { mutableStateOf(initialProvider) }
    val stored = configs[providerId] ?: AiCommentaryProviderPresets.defaults(providerId)
    var enabled by remember(providerId, stored) { mutableStateOf(stored.enabled) }
    var model by remember(providerId, stored) {
        mutableStateOf(AiCommentaryProviderPresets.selectedModel(providerId, stored.model))
    }
    var apiKey by remember(providerId, savedApiKeys) {
        mutableStateOf(savedApiKeys[providerId].orEmpty())
    }
    var modelPickerExpanded by remember(providerId) { mutableStateOf(false) }
    var revealApiKey by remember(providerId) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 640.dp)
                .testTag("ai_commentary_settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("AI 模型设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "API Key 由 Android Keystore 加密，仅保存在本机",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭 AI 模型设置",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiCommentaryProviderPresets.providerIds.forEach { candidate ->
                        Surface(
                            onClick = { providerId = candidate },
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                            color = if (providerId == candidate) NanfengGold.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                Text(candidate.displayName, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("启用 ${providerId.displayName}")
                        Text(
                            if (stored.hasApiKey) "已保存密钥" else "尚未保存密钥",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Box(Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { modelPickerExpanded = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                            .testTag("ai_model_picker"),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFAFBFA),
                        shadowElevation = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "模型",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    AiCommentaryProviderPresets.displayModel(providerId, model),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "选择模型",
                                tint = NanfengGold,
                            )
                        }
                    }
                    NanfengWhiteDropdownMenu(
                        expanded = modelPickerExpanded,
                        onDismissRequest = { modelPickerExpanded = false },
                        placement = if (providerId == AiCommentaryProviderId.OPEN_ROUTER) {
                            NanfengPopupPlacement.AUTO
                        } else {
                            // The direct-provider lists are deliberately short. Keep them
                            // below the chosen-model surface so that surface remains visible.
                            NanfengPopupPlacement.BELOW_ANCHOR
                        },
                    ) {
                        AiCommentaryProviderPresets.models(providerId).forEach { preset ->
                            val vendorStyle = aiModelVendorStyle(preset)
                            DropdownMenuItem(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                text = {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = vendorStyle.container,
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 9.dp, horizontal = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .widthIn(min = 4.dp, max = 4.dp)
                                                    .height(34.dp)
                                                    .background(
                                                        color = vendorStyle.accent,
                                                        shape = RoundedCornerShape(4.dp),
                                                    ),
                                            )
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    preset.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = vendorStyle.title,
                                                )
                                                Text(
                                                    preset.summary,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = vendorStyle.summary,
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    model = preset.model
                                    modelPickerExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (revealApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { revealApiKey = !revealApiKey }) {
                            Icon(
                                painter = painterResource(
                                    if (revealApiKey) R.drawable.ic_visibility_off
                                    else R.drawable.ic_visibility,
                                ),
                                contentDescription = if (revealApiKey) {
                                    "隐藏 API Key"
                                } else {
                                    "显示 API Key"
                                },
                            )
                        }
                    },
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "应用不会把密钥写入命例、备份、日志或诊断包。模型调用只在你点击“生成点评”并确认发送后发生。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                    Button(
                        onClick = {
                            onSave(
                                stored.copy(
                                    enabled = enabled,
                                    model = model.trim(),
                                    baseUrl = stored.baseUrl,
                                ),
                                apiKey,
                            )
                        },
                        modifier = Modifier.weight(1f).testTag("save_ai_provider_config"),
                    ) { Text("保存并关闭") }
                }
            }
        }
    }
}

private data class AiModelVendorStyle(
    val container: Color,
    val accent: Color,
    val title: Color,
    val summary: Color,
)

private fun aiModelVendorStyle(preset: AiCommentaryModelPreset): AiModelVendorStyle {
    // Keep the model picker visually calm on its pure-white dialog surface. The colored
    // rail identifies a provider; the pale card must never compete with the model label.
    return when {
        preset.model.startsWith("openai/") -> AiModelVendorStyle(
            container = Color(0xFFEAF5F2),
            accent = Color(0xFF22966F),
            title = Color(0xFF23745A),
            summary = Color(0xFF5B9785),
        )
        preset.model.startsWith("anthropic/") -> AiModelVendorStyle(
            container = Color(0xFFF9EDE8),
            accent = Color(0xFFC26738),
            title = Color(0xFF9B512F),
            summary = Color(0xFFB57960),
        )
        preset.model.startsWith("deepseek/") || preset.model.startsWith("deepseek-") -> AiModelVendorStyle(
            container = Color(0xFFEDF3FC),
            accent = Color(0xFF4775C7),
            title = Color(0xFF3B619F),
            summary = Color(0xFF718BBC),
        )
        preset.model.startsWith("qwen/") || preset.model.startsWith("qwen") -> AiModelVendorStyle(
            container = Color(0xFFF3F0FA),
            accent = Color(0xFF875AC3),
            title = Color(0xFF6C4B99),
            summary = Color(0xFF9275B3),
        )
        else -> AiModelVendorStyle(
            container = Color(0xFFF8F2E8),
            accent = Color(0xFFC7A35E),
            title = Color(0xFF92783E),
            summary = Color(0xFFA59166),
        )
    }
}
