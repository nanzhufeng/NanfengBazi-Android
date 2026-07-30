package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaseMetadataEditorScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onDraftChange: ((CaseMetadataDraft) -> CaseMetadataDraft) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("metadata_editor_screen"),
    ) {
        TopAppBar(
            title = { Text("管理命例分类") },
            navigationIcon = {
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                "分组和标签可用中文逗号、英文逗号或换行分隔；同名项会自动复用。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.metadataDraft.groupNames,
                onValueChange = { value ->
                    onDraftChange { it.copy(groupNames = value) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .testTag("metadata_groups"),
                label = { Text("分组（最多 10 个）") },
                enabled = !state.mutationSaving,
                minLines = 2,
            )
            OutlinedTextField(
                value = state.metadataDraft.tagNames,
                onValueChange = { value ->
                    onDraftChange { it.copy(tagNames = value) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("metadata_tags"),
                label = { Text("标签（最多 10 个）") },
                enabled = !state.mutationSaving,
                minLines = 2,
            )
            MetadataCheckRow(
                label = "收藏命例",
                checked = state.metadataDraft.isFavorite,
                enabled = !state.mutationSaving,
                tag = "metadata_favorite",
                onCheckedChange = { checked ->
                    onDraftChange { it.copy(isFavorite = checked) }
                },
            )
            MetadataCheckRow(
                label = "置顶命例",
                checked = state.metadataDraft.isPinned,
                enabled = !state.mutationSaving,
                tag = "metadata_pinned",
                onCheckedChange = { checked ->
                    onDraftChange { it.copy(isPinned = checked) }
                },
            )
            MutationError(state.mutationError)
            Button(
                onClick = onSave,
                enabled = !state.mutationSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 24.dp)
                    .height(52.dp)
                    .testTag("save_metadata"),
            ) {
                SaveButtonContent(state.mutationSaving, "保存分类与标记")
            }
        }
    }
}

@Composable
private fun MetadataCheckRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.padding(top = 12.dp))
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(tag),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TextRecordEditorScreen(
    state: StageTwoUiState,
    destination: AppDestination.EditTextRecord,
    onBack: () -> Unit,
    onDraftChange: ((TextRecordDraft) -> TextRecordDraft) -> Unit,
    onSave: (String?) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("record_editor_screen"),
    ) {
        TopAppBar(
            title = {
                Text(if (destination.recordId == null) "新增记录" else "编辑记录")
            },
            navigationIcon = {
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                "记录类型",
                style = MaterialTheme.typography.titleMedium,
            )
            RecordTypeRow(
                types = listOf(
                    CaseTextRecordType.NOTE,
                    CaseTextRecordType.OWNER_FEEDBACK,
                ),
                selected = state.recordDraft.type,
                enabled = !state.mutationSaving,
                onSelected = { type ->
                    onDraftChange { it.copy(type = type) }
                },
            )
            RecordTypeRow(
                types = listOf(
                    CaseTextRecordType.MASTER_COMMENTARY,
                    CaseTextRecordType.ANALYSIS,
                ),
                selected = state.recordDraft.type,
                enabled = !state.mutationSaving,
                onSelected = { type ->
                    onDraftChange { it.copy(type = type) }
                },
            )
            OutlinedTextField(
                value = state.recordDraft.content,
                onValueChange = { value ->
                    onDraftChange { it.copy(content = value) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(220.dp)
                    .testTag("record_content"),
                label = { Text("记录内容 *") },
                enabled = !state.mutationSaving,
            )
            MutationError(state.mutationError)
            Button(
                onClick = { onSave(destination.recordId) },
                enabled = !state.mutationSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .height(52.dp)
                    .testTag("save_record"),
            ) {
                SaveButtonContent(state.mutationSaving, "保存记录")
            }
            if (destination.recordId != null) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !state.mutationSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 24.dp)
                        .testTag("delete_record"),
                ) {
                    Text("删除这条记录", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmDelete && destination.recordId != null) {
        DeleteConfirmation(
            title = "删除记录？",
            message = "该操作会从当前命例移除这条记录，并生成新的命例修订。",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete(destination.recordId)
            },
        )
    }
}

@Composable
private fun RecordTypeRow(
    types: List<CaseTextRecordType>,
    selected: CaseTextRecordType,
    enabled: Boolean,
    onSelected: (CaseTextRecordType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        types.forEach { type ->
            if (type == selected) {
                Button(
                    onClick = { onSelected(type) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(type.displayName())
                }
            } else {
                OutlinedButton(
                    onClick = { onSelected(type) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(type.displayName())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventEditorScreen(
    state: StageTwoUiState,
    destination: AppDestination.EditEvent,
    onBack: () -> Unit,
    onDraftChange: ((EventDraft) -> EventDraft) -> Unit,
    onSave: (String?) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("event_editor_screen"),
    ) {
        TopAppBar(
            title = {
                Text(if (destination.eventId == null) "新增关键事件" else "编辑关键事件")
            },
            navigationIcon = {
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                "事件日期可以留空；只填写年份或年月也会保留对应精度。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EventNumberField(
                    value = state.eventDraft.year,
                    label = "年",
                    tag = "event_year",
                    enabled = !state.mutationSaving,
                    onValueChange = { value ->
                        onDraftChange { it.copy(year = value) }
                    },
                    modifier = Modifier.weight(1.4f),
                )
                EventNumberField(
                    value = state.eventDraft.month,
                    label = "月",
                    tag = "event_month",
                    enabled = !state.mutationSaving,
                    onValueChange = { value ->
                        onDraftChange { it.copy(month = value) }
                    },
                    modifier = Modifier.weight(1f),
                )
                EventNumberField(
                    value = state.eventDraft.day,
                    label = "日",
                    tag = "event_day",
                    enabled = !state.mutationSaving,
                    onValueChange = { value ->
                        onDraftChange { it.copy(day = value) }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = state.eventDraft.status,
                onValueChange = { value ->
                    onDraftChange { it.copy(status = value) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("event_status"),
                label = { Text("状态（可选，如：已确认、待核对）") },
                singleLine = true,
                enabled = !state.mutationSaving,
            )
            OutlinedTextField(
                value = state.eventDraft.rawText,
                onValueChange = { value ->
                    onDraftChange { it.copy(rawText = value) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(190.dp)
                    .testTag("event_content"),
                label = { Text("事件原文 *") },
                enabled = !state.mutationSaving,
            )
            MutationError(state.mutationError)
            Button(
                onClick = { onSave(destination.eventId) },
                enabled = !state.mutationSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .height(52.dp)
                    .testTag("save_event"),
            ) {
                SaveButtonContent(state.mutationSaving, "保存事件")
            }
            if (destination.eventId != null) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !state.mutationSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 24.dp)
                        .testTag("delete_event"),
                ) {
                    Text("删除这个事件", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmDelete && destination.eventId != null) {
        DeleteConfirmation(
            title = "删除事件？",
            message = "该操作会从当前命例移除这个事件，并生成新的命例修订。",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete(destination.eventId)
            },
        )
    }
}

@Composable
private fun EventNumberField(
    value: String,
    label: String,
    tag: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.testTag(tag),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun MutationError(error: String?) {
    if (error == null) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag("mutation_error"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            error,
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun SaveButtonContent(saving: Boolean, label: String) {
    if (saving) {
        CircularProgressIndicator(strokeWidth = 2.dp)
        Spacer(Modifier.padding(horizontal = 5.dp))
        Text("正在保存…")
    } else {
        Text(label)
    }
}

@Composable
private fun DeleteConfirmation(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
