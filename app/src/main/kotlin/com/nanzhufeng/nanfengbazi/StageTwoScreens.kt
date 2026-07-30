package com.nanzhufeng.nanfengbazi

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FieldValueState
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection

@Composable
fun NanfengBaziApp(viewModel: StageTwoViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }
    BackHandler(enabled = state.destination != AppDestination.CaseList) {
        viewModel.navigateBack()
    }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                when (state.destination) {
                    AppDestination.CaseList -> CaseListScreen(
                        state = state,
                        onQueryChange = viewModel::updateQuery,
                        onRefresh = viewModel::refreshCases,
                        onCreate = viewModel::openCreate,
                        onOpenCase = viewModel::openDetail,
                        modifier = Modifier.padding(padding),
                    )
                    AppDestination.CreateCase -> CreateCaseScreen(
                        state = state,
                        onBack = viewModel::backToList,
                        onFormChange = viewModel::updateForm,
                        onSubmit = viewModel::submitCase,
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.CaseDetail -> CaseDetailScreen(
                        state = state,
                        onBack = viewModel::backToList,
                        onEditCase = viewModel::openEditCase,
                        onAddRecord = { viewModel.openTextRecord() },
                        onEditRecord = viewModel::openTextRecord,
                        onAddEvent = { viewModel.openEvent() },
                        onEditEvent = viewModel::openEvent,
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.EditCase -> CaseFormScreen(
                        title = "编辑命例",
                        screenTag = "edit_case_screen",
                        form = state.editForm,
                        error = state.mutationError,
                        saving = state.mutationSaving,
                        submitLabel = "重新排盘并保存",
                        onBack = viewModel::navigateBack,
                        onFormChange = viewModel::updateEditForm,
                        onSubmit = viewModel::saveEditedCase,
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.EditTextRecord -> TextRecordEditorScreen(
                        state = state,
                        destination = state.destination as AppDestination.EditTextRecord,
                        onBack = viewModel::navigateBack,
                        onDraftChange = viewModel::updateRecordDraft,
                        onSave = viewModel::saveTextRecord,
                        onDelete = viewModel::deleteTextRecord,
                        modifier = Modifier.padding(padding),
                    )
                    is AppDestination.EditEvent -> EventEditorScreen(
                        state = state,
                        destination = state.destination as AppDestination.EditEvent,
                        onBack = viewModel::navigateBack,
                        onDraftChange = viewModel::updateEventDraft,
                        onSave = viewModel::saveEvent,
                        onDelete = viewModel::deleteEvent,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseListScreen(
    state: StageTwoUiState,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreate: () -> Unit,
    onOpenCase: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("case_list_screen"),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("南枫八字", fontWeight = FontWeight.SemiBold)
                    Text(
                        "本地命例",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                Button(
                    onClick = onCreate,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag("new_case_button"),
                ) {
                    Text("新建命例")
                }
            },
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("case_search"),
            label = { Text("搜索姓名或别名") },
            singleLine = true,
        )
        when {
            state.listLoading -> LoadingBox("正在读取命例…")
            state.listError != null -> ErrorBox(
                message = state.listError,
                actionLabel = "重试",
                onAction = onRefresh,
            )
            state.cases.isEmpty() -> EmptyCaseList(onCreate)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.cases, key = { it.id }) { summary ->
                    CaseSummaryCard(summary, onClick = { onOpenCase(summary.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyCaseList(onCreate: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                "还没有命例",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "先手动录入出生资料，应用会完成排盘并保存到本机。",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onCreate,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text("新建第一个命例")
            }
        }
    }
}

@Composable
private fun CaseSummaryCard(
    summary: CaseSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("case_${summary.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                summary.name.value ?: summary.alias,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "别名：${summary.alias}",
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "${summary.sexForFortuneDirection.displayName()} · " +
                    summary.birthInput.displayDateTime(),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "四柱：${summary.fourPillars?.display() ?: "暂无计算结果"}",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCaseScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onFormChange: ((CaseFormState) -> CaseFormState) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CaseFormScreen(
        title = "新建命例",
        screenTag = "create_case_screen",
        form = state.form,
        error = state.formError,
        saving = state.saving,
        submitLabel = "排盘并保存",
        onBack = onBack,
        onFormChange = onFormChange,
        onSubmit = onSubmit,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaseFormScreen(
    title: String,
    screenTag: String,
    form: CaseFormState,
    error: String?,
    saving: Boolean,
    submitLabel: String,
    onBack: () -> Unit,
    onFormChange: ((CaseFormState) -> CaseFormState) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(screenTag),
    ) {
        TopAppBar(
            title = { Text(title) },
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
            SectionHeading("身份信息", "别名用于本地识别；姓名可以留空。")
            OutlinedTextField(
                value = form.alias,
                onValueChange = { value -> onFormChange { it.copy(alias = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("case_alias"),
                label = { Text("命例别名 *") },
                singleLine = true,
                enabled = !saving,
            )
            OutlinedTextField(
                value = form.name,
                onValueChange = { value -> onFormChange { it.copy(name = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("case_name"),
                label = { Text("姓名（可选）") },
                singleLine = true,
                enabled = !saving,
            )
            Text(
                "性别 *",
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SexButton(
                    text = "男",
                    selected = form.sex == SexForFortuneDirection.MAN,
                    enabled = !saving,
                    tag = "sex_man",
                    onClick = {
                        onFormChange { it.copy(sex = SexForFortuneDirection.MAN) }
                    },
                )
                SexButton(
                    text = "女",
                    selected = form.sex == SexForFortuneDirection.WOMAN,
                    enabled = !saving,
                    tag = "sex_woman",
                    onClick = {
                        onFormChange { it.copy(sex = SexForFortuneDirection.WOMAN) }
                    },
                )
            }

            SectionHeading(
                "出生时间",
                "当前阶段支持公历、北京时间民用时；不静默转换农历或真太阳时。",
            )
            NumericFieldRow(
                values = listOf(
                    NumericField("年", form.year, "birth_year") {
                        onFormChange { form -> form.copy(year = it) }
                    },
                    NumericField("月", form.month, "birth_month") {
                        onFormChange { form -> form.copy(month = it) }
                    },
                    NumericField("日", form.day, "birth_day") {
                        onFormChange { form -> form.copy(day = it) }
                    },
                ),
                enabled = !saving,
            )
            NumericFieldRow(
                values = listOf(
                    NumericField("时", form.hour, "birth_hour") {
                        onFormChange { form -> form.copy(hour = it) }
                    },
                    NumericField("分", form.minute, "birth_minute") {
                        onFormChange { form -> form.copy(minute = it) }
                    },
                    NumericField("秒", form.second, "birth_second") {
                        onFormChange { form -> form.copy(second = it) }
                    },
                ),
                enabled = !saving,
            )
            if (error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .testTag("form_error"),
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
            Button(
                onClick = onSubmit,
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 28.dp)
                    .height(52.dp)
                    .testTag("save_case"),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(22.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("正在排盘并保存…")
                } else {
                    Text(submitLabel)
                }
            }
        }
    }
}

private data class NumericField(
    val label: String,
    val value: String,
    val tag: String,
    val onChange: (String) -> Unit,
)

@Composable
private fun NumericFieldRow(
    values: List<NumericField>,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        values.forEach { field ->
            OutlinedTextField(
                value = field.value,
                onValueChange = field.onChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag(field.tag),
                label = { Text(field.label) },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

@Composable
private fun SexButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.testTag(tag),
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.testTag(tag),
        ) {
            Text(text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseDetailScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onEditCase: () -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (String) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("case_detail_screen"),
    ) {
        TopAppBar(
            title = { Text(state.detail?.name?.value ?: state.detail?.alias ?: "命例详情") },
            navigationIcon = {
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            },
        )
        when {
            state.detailLoading -> LoadingBox("正在读取命例详情…")
            state.detailError != null -> ErrorBox(
                message = state.detailError,
                actionLabel = "返回列表",
                onAction = onBack,
            )
            state.detail != null -> CaseDetailContent(
                case = state.detail,
                onEditCase = onEditCase,
                onAddRecord = onAddRecord,
                onEditRecord = onEditRecord,
                onAddEvent = onAddEvent,
                onEditEvent = onEditEvent,
            )
        }
    }
}

@Composable
private fun CaseDetailContent(
    case: BaziCase,
    onEditCase: () -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (String) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
) {
    val adopted = case.calculationSnapshots.asReversed().firstOrNull { it.adopted }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Button(
            onClick = onEditCase,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .testTag("edit_case_button"),
        ) {
            Text("编辑资料并重新排盘")
        }
        DetailSection("原始录入信息") {
            DetailRow("命例别名", case.alias)
            DetailRow(
                "姓名",
                when (case.name.state) {
                    FieldValueState.PRESENT -> case.name.value.orEmpty()
                    FieldValueState.ABSENT -> "未提供"
                    FieldValueState.CLEARED -> "已清空"
                },
            )
            DetailRow("性别", case.sexForFortuneDirection.displayName())
            DetailRow("历法与时间", case.birthInput.displayDateTime())
            DetailRow("时区", case.birthInput.timeZoneId)
            DetailRow("时间精度", case.birthInput.timePrecision.name)
            DetailRow("来源", case.sourceType.displayName())
        }
        DetailSection("计算结果") {
            if (adopted == null) {
                Text("当前命例没有已采用的计算快照。")
            } else {
                DetailRow("四柱", adopted.result.fourPillars.display())
                DetailRow("计算配置", adopted.result.profile.id)
                DetailRow("引擎", adopted.result.evidence.engineName)
                DetailRow("引擎版本", adopted.result.evidence.engineVersion)
                DetailRow("规则版本", adopted.result.evidence.ruleVersion)
                DetailRow(
                    "起运方向",
                    if (adopted.result.fortuneStart.direction.name == "FORWARD") {
                        "顺排"
                    } else {
                        "逆排"
                    },
                )
                DetailRow(
                    "起运年龄",
                    "${adopted.result.fortuneStart.years} 年 " +
                        "${adopted.result.fortuneStart.months} 月 " +
                        "${adopted.result.fortuneStart.days} 日",
                )
                DetailRow(
                    "大运",
                    adopted.result.decadeFortunes.joinToString("、") { it.name },
                )
            }
        }
        DetailSection("分析与记录") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onAddRecord,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_record_button"),
                ) {
                    Text("新增记录")
                }
                OutlinedButton(
                    onClick = onAddEvent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_event_button"),
                ) {
                    Text("新增事件")
                }
            }
            if (case.textRecords.isEmpty()) {
                Text(
                    "暂无笔记、反馈或点评。",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                case.textRecords.forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clickable { onEditRecord(record.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                record.type.displayName(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                record.content,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 4,
                            )
                        }
                    }
                }
            }
            if (case.events.isEmpty()) {
                Text(
                    "暂无关键事件。",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                case.events.forEach { event ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clickable { onEditEvent(event.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                event.displayDate(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                event.rawText,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 4,
                            )
                            if (event.status != null) {
                                Text(
                                    "状态：${event.status}",
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SectionHeading(title: String, description: String) {
    Text(
        title,
        modifier = Modifier.padding(top = 10.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        description,
        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LoadingBox(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(message, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun ErrorBox(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(message, color = MaterialTheme.colorScheme.error)
            OutlinedButton(
                onClick = onAction,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

private fun SexForFortuneDirection.displayName(): String = when (this) {
    SexForFortuneDirection.MAN -> "男"
    SexForFortuneDirection.WOMAN -> "女"
}

private fun CaseSourceType.displayName(): String = when (this) {
    CaseSourceType.MANUAL -> "手动录入"
    CaseSourceType.WENZHEN_SCREENSHOT -> "问真截图迁移"
    CaseSourceType.BACKUP_RESTORE -> "备份恢复"
}

internal fun CaseTextRecordType.displayName(): String = when (this) {
    CaseTextRecordType.NOTE -> "普通笔记"
    CaseTextRecordType.OWNER_FEEDBACK -> "命主反馈"
    CaseTextRecordType.MASTER_COMMENTARY -> "师傅点评"
    CaseTextRecordType.ANALYSIS -> "分析记录"
}

private fun CaseEvent.displayDate(): String = when {
    year == null -> "日期待核对"
    month == null -> "${year}年"
    day == null -> "${year}年${month}月"
    else -> "${year}年${month}月${day}日"
}

private fun com.nanzhufeng.nanfengbazi.domain.model.BirthInput.displayDateTime(): String =
    when (val calendar = calendarInput) {
        is BirthCalendarInput.Solar -> "公历 ${calendar.dateTime.display()}"
        is BirthCalendarInput.Lunar -> {
            val date = calendar.dateTime
            "农历 ${date.year}年${date.month}月${date.day}日 " +
                "%02d:%02d:%02d".format(date.hour, date.minute, date.second)
        }
    }

private fun CivilDateTime.display(): String =
    "%04d-%02d-%02d %02d:%02d:%02d".format(year, month, day, hour, minute, second)

private fun FourPillars.display(): String = "$year $month $day $hour"
