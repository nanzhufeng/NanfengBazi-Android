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
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
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
        viewModel.backToList()
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("create_case_screen"),
    ) {
        TopAppBar(
            title = { Text("新建命例") },
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
                value = state.form.alias,
                onValueChange = { value -> onFormChange { it.copy(alias = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("case_alias"),
                label = { Text("命例别名 *") },
                singleLine = true,
                enabled = !state.saving,
            )
            OutlinedTextField(
                value = state.form.name,
                onValueChange = { value -> onFormChange { it.copy(name = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("case_name"),
                label = { Text("姓名（可选）") },
                singleLine = true,
                enabled = !state.saving,
            )
            Text(
                "性别 *",
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SexButton(
                    text = "男",
                    selected = state.form.sex == SexForFortuneDirection.MAN,
                    enabled = !state.saving,
                    tag = "sex_man",
                    onClick = {
                        onFormChange { it.copy(sex = SexForFortuneDirection.MAN) }
                    },
                )
                SexButton(
                    text = "女",
                    selected = state.form.sex == SexForFortuneDirection.WOMAN,
                    enabled = !state.saving,
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
                    NumericField("年", state.form.year, "birth_year") {
                        onFormChange { form -> form.copy(year = it) }
                    },
                    NumericField("月", state.form.month, "birth_month") {
                        onFormChange { form -> form.copy(month = it) }
                    },
                    NumericField("日", state.form.day, "birth_day") {
                        onFormChange { form -> form.copy(day = it) }
                    },
                ),
                enabled = !state.saving,
            )
            NumericFieldRow(
                values = listOf(
                    NumericField("时", state.form.hour, "birth_hour") {
                        onFormChange { form -> form.copy(hour = it) }
                    },
                    NumericField("分", state.form.minute, "birth_minute") {
                        onFormChange { form -> form.copy(minute = it) }
                    },
                    NumericField("秒", state.form.second, "birth_second") {
                        onFormChange { form -> form.copy(second = it) }
                    },
                ),
                enabled = !state.saving,
            )
            if (state.formError != null) {
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
                        state.formError,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Button(
                onClick = onSubmit,
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 28.dp)
                    .height(52.dp)
                    .testTag("save_case"),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(22.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("正在排盘并保存…")
                } else {
                    Text("排盘并保存")
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
            state.detail != null -> CaseDetailContent(state.detail)
        }
    }
}

@Composable
private fun CaseDetailContent(case: BaziCase) {
    val adopted = case.calculationSnapshots.asReversed().firstOrNull { it.adopted }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
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
