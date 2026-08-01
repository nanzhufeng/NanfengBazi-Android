package com.nanzhufeng.nanfengbazi

import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nanzhufeng.nanfengbazi.domain.FourPillarsField
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupContract
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupError
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupQuery
import com.nanzhufeng.nanfengbazi.domain.YearRangeBoundary
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.RatHourRule
import java.time.ZoneOffset

data class FourPillarsLookupFormState(
    val yearPillar: String = "",
    val monthPillar: String = "",
    val dayPillar: String = "",
    val hourPillar: String = "",
    val startYear: String = FourPillarsLookupContract.MIN_YEAR.toString(),
    val endYear: String = FourPillarsLookupContract.MAX_YEAR.toString(),
    val timeZoneId: String = "Asia/Shanghai",
    val ratHourRule: RatHourRule = RatHourRule.TYME_DEFAULT,
)

sealed interface FourPillarsLookupFormValidation {
    data class Valid(val query: FourPillarsLookupQuery) : FourPillarsLookupFormValidation
    data class Invalid(val message: String) : FourPillarsLookupFormValidation
}

internal fun FourPillarsLookupFormState.toQuery(): FourPillarsLookupFormValidation {
    val parsedStartYear = startYear.trim().toIntOrNull()
        ?: return FourPillarsLookupFormValidation.Invalid("起始年份必须是整数。")
    val parsedEndYear = endYear.trim().toIntOrNull()
        ?: return FourPillarsLookupFormValidation.Invalid("结束年份必须是整数。")
    return FourPillarsLookupFormValidation.Valid(
        FourPillarsLookupQuery(
            fourPillars = FourPillars(
                year = yearPillar,
                month = monthPillar,
                day = dayPillar,
                hour = hourPillar,
            ),
            startYear = parsedStartYear,
            endYear = parsedEndYear,
            timeZoneId = timeZoneId,
            ratHourRule = ratHourRule,
        ),
    )
}

internal fun FourPillarsLookupError.toUserMessage(): String = when (this) {
    is FourPillarsLookupError.InvalidPillar ->
        "${field.displayName()}“$value”不是有效六十甲子，请填写如“甲子”。"
    is FourPillarsLookupError.YearOutOfBounds ->
        "${boundary.displayName()}必须在 $minimum 至 $maximum 年之间。"
    is FourPillarsLookupError.InvalidYearOrder -> "起始年份不能晚于结束年份。"
    is FourPillarsLookupError.YearRangeTooLarge ->
        "一次最多查询 $maximumInclusiveYearCount 个年份，请缩小范围。"
    is FourPillarsLookupError.InvalidTimeZone ->
        "IANA 时区“$timeZoneId”无效，请填写如 Asia/Shanghai。"
    FourPillarsLookupError.EngineUnavailable ->
        "四柱反查引擎未能完成查询，请核对输入后重试。"
}

private fun FourPillarsField.displayName(): String = when (this) {
    FourPillarsField.YEAR -> "年柱"
    FourPillarsField.MONTH -> "月柱"
    FourPillarsField.DAY -> "日柱"
    FourPillarsField.HOUR -> "时柱"
}

private fun YearRangeBoundary.displayName(): String = when (this) {
    YearRangeBoundary.START -> "起始年份"
    YearRangeBoundary.END -> "结束年份"
}

@Composable
private fun FourPillarsSelectionCard(pillars: List<String>, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
            .testTag("open_four_pillars_wheel_picker"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("年柱", "月柱", "日柱", "时柱").forEach { label ->
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                pillars.forEachIndexed { index, pillar ->
                    Surface(
                        modifier = Modifier.size(66.dp),
                        shape = CircleShape,
                        color = listOf(
                            Color(0xFFF6F0E5),
                            Color(0xFFFCECED),
                            Color(0xFFEDF5EF),
                            Color(0xFFEEF2FA),
                        )[index],
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                pillar.ifBlank { "选择" },
                                style = MaterialTheme.typography.titleLarge,
                                color = NanfengInk,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Text(
                "点击滑动选择四柱",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = NanfengGold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal fun FourPillarsLookupFormState.toSavedStateBundle(): Bundle = Bundle().apply {
    putString("yearPillar", yearPillar)
    putString("monthPillar", monthPillar)
    putString("dayPillar", dayPillar)
    putString("hourPillar", hourPillar)
    putString("startYear", startYear)
    putString("endYear", endYear)
    putString("timeZoneId", timeZoneId)
    putString("ratHourRule", ratHourRule.name)
}

internal fun Bundle.toFourPillarsLookupFormState(): FourPillarsLookupFormState =
    FourPillarsLookupFormState(
        yearPillar = getString("yearPillar").orEmpty(),
        monthPillar = getString("monthPillar").orEmpty(),
        dayPillar = getString("dayPillar").orEmpty(),
        hourPillar = getString("hourPillar").orEmpty(),
        startYear = getString("startYear")
            ?: FourPillarsLookupContract.MIN_YEAR.toString(),
        endYear = getString("endYear")
            ?: FourPillarsLookupContract.MAX_YEAR.toString(),
        timeZoneId = getString("timeZoneId") ?: "Asia/Shanghai",
        ratHourRule = runCatching {
            RatHourRule.valueOf(getString("ratHourRule").orEmpty())
        }.getOrDefault(RatHourRule.TYME_DEFAULT),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FourPillarsLookupScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onFormChange: ((FourPillarsLookupFormState) -> FourPillarsLookupFormState) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = state.fourPillarsLookupForm
    var showPillarPicker by rememberSaveable { mutableStateOf(false) }
    var showYearRangePicker by rememberSaveable { mutableStateOf(false) }
    var showTimeZonePicker by rememberSaveable { mutableStateOf(false) }
    if (showPillarPicker) {
        FourPillarsWheelPickerSheet(
            current = listOf(form.yearPillar, form.monthPillar, form.dayPillar, form.hourPillar),
            onDismiss = { showPillarPicker = false },
            onConfirm = { pillars ->
                onFormChange {
                    it.copy(
                        yearPillar = pillars[0],
                        monthPillar = pillars[1],
                        dayPillar = pillars[2],
                        hourPillar = pillars[3],
                    )
                }
                showPillarPicker = false
            },
        )
    }
    if (showYearRangePicker) {
        YearRangeWheelPickerSheet(
            startYear = form.startYear.toIntOrNull() ?: FourPillarsLookupContract.MIN_YEAR,
            endYear = form.endYear.toIntOrNull() ?: FourPillarsLookupContract.MAX_YEAR,
            onDismiss = { showYearRangePicker = false },
            onConfirm = { start, end ->
                onFormChange { it.copy(startYear = start.toString(), endYear = end.toString()) }
                showYearRangePicker = false
            },
        )
    }
    if (showTimeZonePicker) {
        IanaTimeZoneWheelPickerSheet(
            current = form.timeZoneId,
            onDismiss = { showTimeZonePicker = false },
            onConfirm = { zone ->
                onFormChange { it.copy(timeZoneId = zone) }
                showTimeZonePicker = false
            },
        )
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("four_pillars_lookup_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TopAppBar(
                title = { Text("四柱反查", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("返回")
                    }
                },
            )
        }
        item {
            Text(
                FourPillarsLookupContract.CANDIDATE_NOTICE,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("four_pillars_lookup_notice"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            FourPillarsSelectionCard(
                pillars = listOf(form.yearPillar, form.monthPillar, form.dayPillar, form.hourPillar),
                onClick = { showPillarPicker = true },
            )
        }
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
            ) {
                HomePickerRow(
                    title = "查找年份范围",
                    value = "${form.startYear}–${form.endYear}",
                    supporting = "点击用双列滚轮选择",
                    onClick = { showYearRangePicker = true },
                    tag = "open_lookup_year_range_picker",
                )
            }
        }
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
            ) {
                HomePickerRow(
                    title = "IANA 时区",
                    value = form.timeZoneId,
                    supporting = "点击滑动选择常用时区",
                    onClick = { showTimeZonePicker = true },
                    tag = "open_lookup_time_zone_picker",
                )
            }
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("子时口径", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatHourRuleButton(
                        label = "23:00 换日",
                        selected = form.ratHourRule == RatHourRule.TYME_DEFAULT,
                        tag = "lookup_rat_default",
                        onClick = {
                            onFormChange {
                                it.copy(ratHourRule = RatHourRule.TYME_DEFAULT)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    RatHourRuleButton(
                        label = "晚子时算当天",
                        selected = form.ratHourRule == RatHourRule.LATE_RAT_SAME_DAY,
                        tag = "lookup_rat_late",
                        onClick = {
                            onFormChange {
                                it.copy(ratHourRule = RatHourRule.LATE_RAT_SAME_DAY)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Button(
                onClick = onSearch,
                enabled = !state.fourPillarsLookupLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 48.dp)
                    .testTag("lookup_search"),
            ) {
                Text(if (state.fourPillarsLookupLoading) "查询中…" else "查询民用时候选")
            }
        }
        when {
            state.fourPillarsLookupLoading -> item {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("lookup_loading"),
                )
            }
            state.fourPillarsLookupError != null -> item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("lookup_error"),
                ) {
                    Text(
                        state.fourPillarsLookupError,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            state.fourPillarsLookupHasSearched &&
                state.fourPillarsLookupCandidates.isEmpty() -> item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("lookup_empty"),
                ) {
                    Text(
                        "所选年份范围内没有可复算候选。请核对四柱、年份和子时口径。",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            state.fourPillarsLookupCandidates.isNotEmpty() -> {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "找到 ${state.fourPillarsLookupCandidates.size} 个候选",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("lookup_result_count"),
                        )
                        state.fourPillarsLookupEvidence?.let { evidence ->
                            Text(
                                "${evidence.engineName} ${evidence.engineVersion}；" +
                                    "每项已通过 BaziEngine.calculate 复算。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                items(
                    items = state.fourPillarsLookupCandidates,
                    key = { "${it.instant}-${it.resolvedUtcOffsetSeconds}" },
                ) { candidate ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("lookup_candidate"),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                candidate.civilDateTime.display(),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${candidate.timeZoneId} " +
                                    ZoneOffset.ofTotalSeconds(
                                        candidate.resolvedUtcOffsetSeconds,
                                    ).id,
                            )
                            Text(
                                "四柱：${candidate.fourPillars.display()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "民用代表时刻 · ${candidate.timeZoneDataVersion} · " +
                                    candidate.ratHourRule.displayName(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    Text(
                        FourPillarsLookupContract.CANDIDATE_NOTICE,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("lookup_result_notice"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Text(
                "支持年份：${FourPillarsLookupContract.MIN_YEAR}–" +
                    "${FourPillarsLookupContract.MAX_YEAR}；结果不会自动保存为命例。",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PillarInputRow(
    firstLabel: String,
    firstValue: String,
    firstTag: String,
    onFirstChange: (String) -> Unit,
    secondLabel: String,
    secondValue: String,
    secondTag: String,
    onSecondChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = firstValue,
            onValueChange = onFirstChange,
            label = { Text(firstLabel) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag(firstTag),
        )
        OutlinedTextField(
            value = secondValue,
            onValueChange = onSecondChange,
            label = { Text(secondLabel) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag(secondTag),
        )
    }
}

@Composable
private fun YearInput(
    label: String,
    value: String,
    tag: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier.testTag(tag),
    )
}

@Composable
private fun RatHourRuleButton(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = 48.dp)
                .testTag(tag),
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = 48.dp)
                .testTag(tag),
        ) {
            Text(label)
        }
    }
}

private fun com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime.display(): String =
    "%04d-%02d-%02d %02d:%02d:%02d".format(
        year,
        month,
        day,
        hour,
        minute,
        second,
    )

private fun FourPillars.display(): String = "$year $month $day $hour"

private fun RatHourRule.displayName(): String = when (this) {
    RatHourRule.TYME_DEFAULT -> "23:00 换日"
    RatHourRule.LATE_RAT_SAME_DAY -> "晚子时算当天"
}
