package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziTimeZoneDefaults
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookupContract
import com.nanzhufeng.nanfengbazi.domain.AlmanacContract
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

internal enum class BirthPickerMode(val label: String) {
    SOLAR("公历"),
    LUNAR("农历"),
    FOUR_PILLARS("四柱"),
}

internal data class BirthDateTimeSelection(
    val mode: BirthPickerMode,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val timePrecision: TimePrecision,
    val isLeapMonth: Boolean,
)

/**
 * 顶部快速定位输入的解析结果。字段为空即表示保留当前滚轮值，不能靠文本猜测历法类型。
 */
internal data class BirthPickerQuickLocate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val hour: Int? = null,
    val minute: Int? = null,
) {
    val hasValue: Boolean
        get() = year != null || month != null || day != null || hour != null || minute != null
}

/**
 * 支持完整紧凑格式、常见分隔格式，以及带中文单位的任意部分输入。
 * 例如：199001020830、1990-01-02 08:30、1990年、8:30、3月15日。
 */
internal fun parseBirthPickerQuickLocate(raw: String): BirthPickerQuickLocate? {
    val input = raw.map { character ->
        if (character in '０'..'９') {
            ('0'.code + (character.code - '０'.code)).toChar()
        } else {
            character
        }
    }.joinToString("").trim()
    if (input.isEmpty()) return null

    fun firstNumber(pattern: String): Int? =
        Regex(pattern).find(input)?.groupValues?.getOrNull(1)?.toIntOrNull()

    val labeledYear = firstNumber("(\\d{1,4})\\s*年")
    val labeledMonth = firstNumber("(\\d{1,2})\\s*月")
    val labeledDay = firstNumber("(\\d{1,2})\\s*[日号]")
    val labeledHour = firstNumber("(\\d{1,2})\\s*[时点]")
    val labeledMinute = firstNumber("(\\d{1,2})\\s*分")
    val clockMatch = Regex("(\\d{1,2})\\s*[:：]\\s*(\\d{1,2})").find(input)
    val explicit = BirthPickerQuickLocate(
        year = labeledYear,
        month = labeledMonth,
        day = labeledDay,
        hour = labeledHour ?: clockMatch?.groupValues?.getOrNull(1)?.toIntOrNull(),
        minute = labeledMinute ?: clockMatch?.groupValues?.getOrNull(2)?.toIntOrNull(),
    )
    if (labeledYear != null || labeledMonth != null || labeledDay != null ||
        labeledHour != null || labeledMinute != null
    ) {
        return explicit
    }

    if (input.all(Char::isDigit)) {
        return when (input.length) {
            4 -> BirthPickerQuickLocate(year = input.toIntOrNull())
            6 -> BirthPickerQuickLocate(
                year = input.take(4).toIntOrNull(),
                month = input.drop(4).take(2).toIntOrNull(),
            )
            8 -> BirthPickerQuickLocate(
                year = input.take(4).toIntOrNull(),
                month = input.drop(4).take(2).toIntOrNull(),
                day = input.drop(6).take(2).toIntOrNull(),
            )
            10 -> BirthPickerQuickLocate(
                year = input.take(4).toIntOrNull(),
                month = input.drop(4).take(2).toIntOrNull(),
                day = input.drop(6).take(2).toIntOrNull(),
                hour = input.drop(8).take(2).toIntOrNull(),
            )
            12 -> BirthPickerQuickLocate(
                year = input.take(4).toIntOrNull(),
                month = input.drop(4).take(2).toIntOrNull(),
                day = input.drop(6).take(2).toIntOrNull(),
                hour = input.drop(8).take(2).toIntOrNull(),
                minute = input.drop(10).take(2).toIntOrNull(),
            )
            else -> null
        }
    }

    val datePart = clockMatch?.let { input.removeRange(it.range) } ?: input
    val values = Regex("\\d+").findAll(datePart).mapNotNull { it.value.toIntOrNull() }.toList()
    if (values.firstOrNull()?.toString()?.length == 4) {
        return BirthPickerQuickLocate(
            year = values.getOrNull(0),
            month = values.getOrNull(1),
            day = values.getOrNull(2),
            hour = explicit.hour,
            minute = explicit.minute,
        )
    }
    return explicit.takeIf(BirthPickerQuickLocate::hasValue)
}

data class BirthPickerTodaySnapshot(
    val solarYear: Int,
    val solarMonth: Int,
    val solarDay: Int,
    val lunarYear: Int,
    val lunarMonth: Int,
    val lunarDay: Int,
    val isLeapMonth: Boolean,
    val hour: Int,
    val minute: Int,
    val pillars: List<String>,
)

/** 公历与农历共用同一份时分精度，切换历法时不能分叉。 */
internal data class BirthPickerClockSelection(
    val hour: Int,
    val minute: Int,
    private val knownPrecision: TimePrecision,
    val isUnknown: Boolean,
) {
    val hourValue: Int? get() = hour.takeUnless { isUnknown }
    val minuteValue: Int? get() = minute.takeUnless { isUnknown }
    val timePrecision: TimePrecision get() = if (isUnknown) TimePrecision.UNKNOWN else knownPrecision

    fun displayLabel(): String = if (isUnknown) "未知:未知" else {
        "${hour.twoDigits()}:${minute.twoDigits()}"
    }

    fun selectHour(value: Int?): BirthPickerClockSelection = when (value) {
        null -> copy(isUnknown = true)
        else -> copy(hour = value, isUnknown = false)
    }

    fun selectMinute(value: Int?): BirthPickerClockSelection = when (value) {
        null -> copy(isUnknown = true)
        else -> copy(
            minute = value,
            knownPrecision = if (value == 0) knownPrecision else TimePrecision.EXACT_TO_MINUTE,
            isUnknown = false,
        )
    }

    fun selectKnown(hour: Int, minute: Int): BirthPickerClockSelection = copy(
        hour = hour,
        minute = minute,
        knownPrecision = TimePrecision.EXACT_TO_MINUTE,
        isUnknown = false,
    )

    companion object {
        fun from(hour: Int, minute: Int, timePrecision: TimePrecision): BirthPickerClockSelection =
            BirthPickerClockSelection(
                hour = hour,
                minute = minute,
                knownPrecision = timePrecision.takeUnless { it == TimePrecision.UNKNOWN }
                    ?: TimePrecision.EXACT_TO_MINUTE,
                isUnknown = timePrecision == TimePrecision.UNKNOWN,
            )
    }
}

internal data class FourPillarsLookupSelection(
    val pillars: List<String>,
    val startYear: Int,
    val endYear: Int,
) {
    init {
        require(pillars.size == PillarLabels.size) { "四柱必须包含年、月、日、时四项。" }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BirthDateTimePickerSheet(
    form: CaseFormState,
    onDismiss: () -> Unit,
    onConfirm: (BirthDateTimeSelection) -> Unit,
    onConfirmFourPillars: ((FourPillarsLookupSelection) -> Unit)? = null,
    fourPillarsCurrent: List<String> = emptyList(),
    fourPillarsStartYear: Int = FourPillarsLookupContract.MIN_YEAR,
    fourPillarsEndYear: Int = FourPillarsLookupContract.MAX_YEAR,
    initialMode: BirthPickerMode? = null,
    showFourPillarsOption: Boolean = true,
    todaySnapshot: BirthPickerTodaySnapshot? = null,
) {
    val haptic = rememberAppHapticFeedback()
    val initial = remember(form) { form.toPickerDateTime() }
    var mode by remember(form.calendarSystem, initialMode) {
        mutableStateOf(
            initialMode ?: if (form.calendarSystem == CalendarSystem.LUNAR) {
                BirthPickerMode.LUNAR
            } else {
                BirthPickerMode.SOLAR
            },
        )
    }
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.monthValue) }
    var day by remember { mutableIntStateOf(initial.dayOfMonth) }
    var clock by remember(form) {
        mutableStateOf(BirthPickerClockSelection.from(initial.hour, initial.minute, form.timePrecision))
    }
    var leapMonth by remember { mutableStateOf(form.isLeapMonth) }
    var quickLocateText by remember(form) { mutableStateOf("") }
    val maxDay = remember(mode, year, month) {
        if (mode == BirthPickerMode.SOLAR) {
            runCatching { LocalDate.of(year, month, 1).lengthOfMonth() }.getOrDefault(31)
        } else {
            30
        }
    }
    LaunchedEffect(maxDay) {
        if (day > maxDay) day = maxDay
    }
    LaunchedEffect(mode, quickLocateText) {
        if (mode == BirthPickerMode.FOUR_PILLARS) return@LaunchedEffect
        val quickLocate = parseBirthPickerQuickLocate(quickLocateText) ?: return@LaunchedEffect
        quickLocate.year?.takeIf { it in SupportedPickerYears }?.let { year = it }
        quickLocate.month?.takeIf { it in 1..12 }?.let { month = it }
        quickLocate.day?.takeIf { it in 1..31 }?.let { day = it }
        quickLocate.hour?.takeIf { it in 0..23 }?.let { clock = clock.selectHour(it) }
        quickLocate.minute?.takeIf { it in 0..59 }?.let { clock = clock.selectMinute(it) }
    }

    FixedPickerSheet(
        onDismiss = onDismiss,
        targetHeight = BirthPickerPanelHeight,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .testTag("birth_datetime_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (mode == BirthPickerMode.FOUR_PILLARS && onConfirmFourPillars != null) {
                FourPillarsPickerContent(
                    current = fourPillarsCurrent,
                    startYear = fourPillarsStartYear,
                    endYear = fourPillarsEndYear,
                    onConfirm = onConfirmFourPillars,
                    todayPillars = todaySnapshot?.pillars.orEmpty(),
                    contentPadding = PaddingValues(0.dp),
                    header = { onToday, onConfirmPillars ->
                        BirthPickerHeader(
                            selectedMode = mode,
                            availableModes = BirthPickerMode.entries,
                            onModeSelected = { mode = it },
                            onToday = onToday,
                            todayEnabled = todaySnapshot?.pillars?.size == PillarLabels.size,
                            onConfirm = onConfirmPillars,
                            confirmTag = "confirm_four_pillars_wheels",
                        )
                    },
                    modifier = Modifier.testTag("four_pillars_wheel_sheet"),
                )
            } else {
            BirthPickerHeader(
                selectedMode = mode,
                availableModes = if (showFourPillarsOption && onConfirmFourPillars != null) {
                    BirthPickerMode.entries
                } else {
                    listOf(BirthPickerMode.SOLAR, BirthPickerMode.LUNAR)
                },
                onModeSelected = { mode = it },
                onToday = {
                    val today = todaySnapshot
                    if (today != null) {
                        if (mode == BirthPickerMode.LUNAR) {
                            year = today.lunarYear
                            month = today.lunarMonth
                            day = today.lunarDay
                            leapMonth = today.isLeapMonth
                        } else {
                            year = today.solarYear
                            month = today.solarMonth
                            day = today.solarDay
                            leapMonth = false
                        }
                        clock = clock.selectKnown(today.hour, today.minute)
                    } else if (mode == BirthPickerMode.SOLAR) {
                        val now = LocalDateTime.now()
                        year = now.year
                        month = now.monthValue
                        day = now.dayOfMonth
                        clock = clock.selectKnown(now.hour, now.minute)
                        leapMonth = false
                    }
                },
                todayEnabled = todaySnapshot != null || mode == BirthPickerMode.SOLAR,
                onConfirm = {
                    haptic.perform(AppHapticEvent.CONFIRM)
                    onConfirm(
                        BirthDateTimeSelection(
                            mode = mode,
                            year = year,
                            month = month,
                            day = day,
                            hour = clock.hour,
                            minute = clock.minute,
                            timePrecision = clock.timePrecision,
                            isLeapMonth = mode == BirthPickerMode.LUNAR && leapMonth,
                        ),
                    )
                },
                confirmTag = "confirm_birth_datetime",
            )
            BirthPickerQuickLocateInput(
                value = quickLocateText,
                onValueChange = { quickLocateText = it },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            WheelSelectionPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BirthDateTimeWheelViewportHeight),
            ) {
                ValueWheel(
                    label = "年",
                    values = SupportedPickerYears.toList(),
                    selectedValue = year,
                    display = Int::toString,
                    onSelected = { year = it },
                    modifier = Modifier.weight(1.22f),
                    tag = "birth_year_wheel",
                )
                ValueWheel(
                    label = "月",
                    values = (1..12).toList(),
                    selectedValue = month,
                    display = { value ->
                        if (mode == BirthPickerMode.LUNAR) lunarMonthName(value) else value.twoDigits()
                    },
                    onSelected = { month = it },
                    modifier = Modifier.weight(1f),
                    tag = "birth_month_wheel",
                    cyclic = true,
                )
                ValueWheel(
                    label = "日",
                    values = (1..maxDay).toList(),
                    selectedValue = day.coerceAtMost(maxDay),
                    display = { value ->
                        if (mode == BirthPickerMode.LUNAR) lunarDayName(value) else value.twoDigits()
                    },
                    onSelected = { day = it },
                    modifier = Modifier.weight(1f),
                    tag = "birth_day_wheel",
                    cyclic = true,
                )
                ValueWheel(
                    label = "时",
                    values = BirthPickerHourValues,
                    selectedValue = clock.hourValue,
                    display = { value -> value?.twoDigits() ?: "未知" },
                    onSelected = { clock = clock.selectHour(it) },
                    modifier = Modifier.weight(1f),
                    tag = "birth_hour_wheel",
                    cyclic = true,
                )
                ValueWheel(
                    label = "分",
                    values = BirthPickerMinuteValues,
                    selectedValue = clock.minuteValue,
                    display = { value -> value?.twoDigits() ?: "未知" },
                    onSelected = { clock = clock.selectMinute(it) },
                    modifier = Modifier.weight(1f),
                    tag = "birth_minute_wheel",
                    cyclic = true,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (mode == BirthPickerMode.LUNAR) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallToggleChip(
                            text = "普通月",
                            selected = !leapMonth,
                            onClick = { leapMonth = false },
                        )
                        SmallToggleChip(
                            text = "闰月",
                            selected = leapMonth,
                            onClick = { leapMonth = true },
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NanfengWarmTint,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        if (mode == BirthPickerMode.SOLAR) {
                            "%04d-%02d-%02d  %s".format(year, month, day, clock.displayLabel())
                        } else {
                            "${year}年${if (leapMonth) "闰" else ""}${lunarMonthName(month)}月" +
                                "${lunarDayName(day)}  ${clock.displayLabel()}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = NanfengInk,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "历史时区变化会自动解析，不要求手工判断。",
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun BirthPickerQuickLocateInput(
    value: String,
    onValueChange: (String) -> Unit,
    inputContentDescription: String = "快速定位出生年月日时分",
    tag: String = "birth_quick_locate_input",
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(tag),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "快速定位",
                style = MaterialTheme.typography.labelLarge,
                color = NanfengNavigation,
                fontWeight = FontWeight.SemiBold,
            )
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        "1990-01-02 08:30；也可输入 1990年、8:30",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = { candidate ->
                        if (candidate.length <= 24 && candidate.all { character ->
                                character.isDigit() || character in "年月日号时点分:-：/ ."
                            }
                        ) {
                            onValueChange(candidate)
                        }
                    },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = inputContentDescription },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = NanfengInk),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
            }
        }
    }
}

@Composable
private fun BirthPickerHeader(
    selectedMode: BirthPickerMode,
    availableModes: List<BirthPickerMode>,
    onModeSelected: (BirthPickerMode) -> Unit,
    onToday: () -> Unit,
    todayEnabled: Boolean,
    onConfirm: () -> Unit,
    confirmTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PickerSegmentedControl(
            values = availableModes,
            selected = selectedMode,
            label = BirthPickerMode::label,
            onSelected = onModeSelected,
            itemTag = { "birth_picker_mode_${it.name.lowercase()}" },
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onToday,
            enabled = todayEnabled,
            modifier = Modifier
                .height(48.dp)
                .testTag("birth_picker_today"),
        ) {
            Text("今天", fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .height(48.dp)
                .testTag(confirmTag),
            colors = ButtonDefaults.buttonColors(
                containerColor = NanfengNavigation,
                contentColor = NanfengGoldLight,
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Text("确定")
        }
    }
}

internal data class BirthplaceOption(
    val region: String,
    val city: String,
    val district: String,
    val timeZoneId: String,
    /** 仅在有可信城市中心参考点时提供；缺失时不得以省会或猜测坐标代填。 */
    val latitude: Double?,
    val longitude: Double?,
    val domestic: Boolean,
) {
    val displayName: String get() = listOf(region, city, district).distinct().joinToString(" ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BirthplacePickerSheet(
    form: CaseFormState,
    onDismiss: () -> Unit,
    onConfirm: (BirthplaceOption) -> Unit,
) {
    val haptic = rememberAppHapticFeedback()
    val initial = remember(form.locationName, form.timeZoneId) {
        BirthplaceCatalog.bestMatch(form.locationName, form.timeZoneId)
    }
    var domestic by remember { mutableStateOf(initial.domestic) }
    val scoped = remember(domestic) { BirthplaceCatalog.options.filter { it.domestic == domestic } }
    val regions = remember(scoped) { scoped.map(BirthplaceOption::region).distinct() }
    var region by remember(domestic) {
        mutableStateOf(initial.region.takeIf { it in regions } ?: regions.first())
    }
    val cities = remember(scoped, region) {
        scoped.filter { it.region == region }.map(BirthplaceOption::city).distinct()
    }
    var city by remember(region) {
        mutableStateOf(initial.city.takeIf { it in cities } ?: cities.first())
    }
    val districts = remember(scoped, region, city) {
        scoped.filter { it.region == region && it.city == city }
            .map(BirthplaceOption::district)
            .distinct()
    }
    var district by remember(region, city) {
        mutableStateOf(initial.district.takeIf { it in districts } ?: districts.first())
    }
    val selected = remember(scoped, region, city, district) {
        scoped.first { it.region == region && it.city == city && it.district == district }
    }

    FixedPickerSheet(
        onDismiss = onDismiss,
        targetHeight = BirthPickerPanelHeight,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .testTag("birthplace_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PickerSegmentedControl(
                    values = listOf(true, false),
                    selected = domestic,
                    label = { if (it) "国内" else "海外" },
                    onSelected = { domestic = it },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        haptic.perform(AppHapticEvent.CONFIRM)
                        onConfirm(selected)
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("confirm_birthplace"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NanfengNavigation,
                        contentColor = NanfengGoldLight,
                    ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text("确定")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            WheelSelectionPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PrimaryPickerWheelViewportHeight),
            ) {
                ValueWheel(
                    label = if (domestic) "省份" else "国家/地区",
                    values = regions,
                    selectedValue = region,
                    display = { it },
                    onSelected = { region = it },
                    modifier = Modifier.weight(1f),
                    tag = "birthplace_region_wheel",
                )
                ValueWheel(
                    label = "城市",
                    values = cities,
                    selectedValue = city,
                    display = { it },
                    onSelected = { city = it },
                    modifier = Modifier.weight(1f),
                    tag = "birthplace_city_wheel",
                )
                ValueWheel(
                    label = if (domestic) "区县" else "区域",
                    values = districts,
                    selectedValue = district,
                    display = { it },
                    onSelected = { district = it },
                    modifier = Modifier.weight(1f),
                    tag = "birthplace_district_wheel",
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NanfengWarmTint,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        selected.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = NanfengInk,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val coordinateSummary = if (selected.latitude != null && selected.longitude != null) {
                        "城市中心参考坐标 ${selected.latitude}, ${selected.longitude}"
                    } else {
                        "未内置精确坐标；标准时按${BaziTimeZoneDefaults.displayName(selected.timeZoneId)}计算"
                    }
                    Text(
                        "${BaziTimeZoneDefaults.displayName(selected.timeZoneId)} · $coordinateSummary",
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "离线地点库；真太阳时默认关闭，启用前请在详细设置核对精确经纬度。",
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FourPillarsWheelPickerSheet(
    current: List<String>,
    startYear: Int,
    endYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (FourPillarsLookupSelection) -> Unit,
    onOpenCalendarPicker: ((BirthPickerMode) -> Unit)? = null,
) {
    val pickerHeader: (@Composable ((() -> Unit, () -> Unit) -> Unit))? =
        if (onOpenCalendarPicker != null) {
            { _: () -> Unit, onConfirmPillars: () -> Unit ->
                BirthPickerHeader(
                    selectedMode = BirthPickerMode.FOUR_PILLARS,
                    availableModes = BirthPickerMode.entries,
                    onModeSelected = { selected ->
                        if (selected != BirthPickerMode.FOUR_PILLARS) {
                            onOpenCalendarPicker(selected)
                        }
                    },
                    onToday = {},
                    todayEnabled = false,
                    onConfirm = onConfirmPillars,
                    confirmTag = "confirm_four_pillars_wheels",
                )
            }
        } else {
            null
        }
    FixedPickerSheet(
        onDismiss = onDismiss,
        targetHeight = BirthPickerPanelHeight,
    ) {
        FourPillarsPickerContent(
            current = current,
            startYear = startYear,
            endYear = endYear,
            onConfirm = onConfirm,
            header = pickerHeader,
            modifier = Modifier.testTag("four_pillars_wheel_sheet"),
        )
    }
}

@Composable
private fun FourPillarsPickerContent(
    current: List<String>,
    startYear: Int,
    endYear: Int,
    onConfirm: (FourPillarsLookupSelection) -> Unit,
    todayPillars: List<String> = emptyList(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
    header: (@Composable ((() -> Unit, () -> Unit) -> Unit))? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberAppHapticFeedback()
    val initialPillars = remember(current) {
        List(PillarLabels.size) { index -> current.getOrNull(index).toPillarParts() }
    }
    var stems by remember { mutableStateOf(initialPillars.map(PillarParts::stem)) }
    var branches by remember { mutableStateOf(initialPillars.map(PillarParts::branch)) }
    var selectedPillarIndex by remember { mutableIntStateOf(0) }
    var activeEditPart by remember { mutableStateOf(PillarEditPart.STEM) }
    var selectedStartYear by remember { mutableIntStateOf(startYear.coerceIn(SupportedPickerYears)) }
    var selectedEndYear by remember { mutableIntStateOf(endYear.coerceIn(SupportedPickerYears)) }
    var showYearRangePicker by remember { mutableStateOf(false) }
    val selectedStem = stems[selectedPillarIndex]
    val compatibleBranches = remember(selectedStem) { compatibleBranchesFor(selectedStem) }
    val selectedBranch = branches[selectedPillarIndex]
    val confirmSelection = {
        haptic.perform(AppHapticEvent.CONFIRM)
        onConfirm(
            FourPillarsLookupSelection(
                pillars = stems.indices.map { index -> stems[index] + branches[index] },
                startYear = selectedStartYear,
                endYear = selectedEndYear,
            ),
        )
    }
    val useToday = {
        if (todayPillars.size == PillarLabels.size) {
            val parts = todayPillars.map(String::toPillarParts)
            stems = parts.map(PillarParts::stem)
            branches = parts.map(PillarParts::branch)
            selectedPillarIndex = 0
            activeEditPart = PillarEditPart.STEM
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
            if (header != null) {
                header(useToday, confirmSelection)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = confirmSelection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NanfengNavigation,
                            contentColor = NanfengGoldLight,
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("confirm_four_pillars_wheels"),
                    ) { Text("确定") }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (showYearRangePicker) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("年份范围", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { showYearRangePicker = false }) { Text("返回") }
                    Button(
                        onClick = {
                            val earliest = minOf(selectedStartYear, selectedEndYear)
                            val latest = maxOf(selectedStartYear, selectedEndYear)
                            selectedStartYear = earliest
                            selectedEndYear = latest
                            showYearRangePicker = false
                        },
                        colors = ButtonDefaults.buttonColors(NanfengNavigation, NanfengGoldLight),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("confirm_lookup_year_range"),
                    ) { Text("确定") }
                }
                WheelSelectionPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BirthWheelViewportHeight)
                        .testTag("lookup_year_range_sheet"),
                ) {
                    val years = SupportedPickerYears.toList()
                    ValueWheel("起始年", years, selectedStartYear, Int::toString, { selectedStartYear = it }, Modifier.weight(1f), "lookup_start_year_wheel")
                    ValueWheel("结束年", years, selectedEndYear, Int::toString, { selectedEndYear = it }, Modifier.weight(1f), "lookup_end_year_wheel")
                }
            } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = { showYearRangePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "修改年份范围" }
                        .testTag("lookup_year_range_inline"),
                    color = NanfengWarmTint,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("年份范围", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "$selectedStartYear–$selectedEndYear",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text("修改", style = MaterialTheme.typography.labelMedium, color = NanfengNavigation)
                    }
                }
            }
            PickerSegmentedControl(
                values = PillarLabels.indices.toList(),
                selected = selectedPillarIndex,
                label = { PillarLabels[it] },
                onSelected = {
                    selectedPillarIndex = it
                    activeEditPart = PillarEditPart.STEM
                },
                itemTag = { "lookup_pillar_option_${PillarLabels[it]}" },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lookup_pillar_selector"),
            )
            FourPillarsInputGrid(
                pillars = stems.indices.map { index -> stems[index] + branches[index] },
                selectedPillarIndex = selectedPillarIndex,
                activeEditPart = activeEditPart,
                onEditPartSelected = { pillarIndex, editPart ->
                    haptic.perform(AppHapticEvent.SELECTION)
                    selectedPillarIndex = pillarIndex
                    activeEditPart = editPart
                },
            )
            PillarCharacterEditor(
                title = "天干",
                active = activeEditPart == PillarEditPart.STEM,
                tag = "lookup_pillar_stem_editor",
            ) {
                BaziCharacterChoiceGrid(
                    values = HeavenlyStems,
                    selected = selectedStem,
                    editorActive = activeEditPart == PillarEditPart.STEM,
                    onSelect = { stem ->
                        haptic.perform(AppHapticEvent.SELECTION)
                        stems = stems.replacing(selectedPillarIndex, stem)
                        val branchesForStem = compatibleBranchesFor(stem)
                        if (branches[selectedPillarIndex] !in branchesForStem) {
                            branches = branches.replacing(selectedPillarIndex, branchesForStem.first())
                        }
                        activeEditPart = PillarEditPart.BRANCH
                    },
                    tagPrefix = "lookup_pillar_stem",
                    modifier = Modifier.testTag("lookup_pillar_stem_grid"),
                )
            }
            PillarCharacterEditor(
                title = "地支（仅显示可组成六十甲子的地支）",
                active = activeEditPart == PillarEditPart.BRANCH,
                tag = "lookup_pillar_branch_editor",
            ) {
                BaziCharacterChoiceGrid(
                    values = compatibleBranches,
                    selected = selectedBranch.takeIf { it in compatibleBranches } ?: compatibleBranches.first(),
                    editorActive = activeEditPart == PillarEditPart.BRANCH,
                    onSelect = { branch ->
                        haptic.perform(AppHapticEvent.SELECTION)
                        branches = branches.replacing(selectedPillarIndex, branch)
                    },
                    tagPrefix = "lookup_pillar_branch",
                    modifier = Modifier.testTag("lookup_pillar_branch_grid"),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PillarCharacterEditor(
    title: String,
    active: Boolean,
    tag: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = if (active) "正在编辑" else "等待编辑" }
            .testTag(tag)
            .graphicsLayer(alpha = if (active) 1f else 0.84f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape,
                    ),
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun BaziCharacterChoiceGrid(
    values: List<String>,
    selected: String,
    editorActive: Boolean,
    onSelect: (String) -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (editorActive) 1f else 0.92f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { value ->
                    val character = value.first()
                    val isSelected = value == selected
                    Surface(
                        onClick = { onSelect(value) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("${tagPrefix}_$value"),
                        shape = RoundedCornerShape(13.dp),
                        color = if (isSelected && editorActive) {
                            baziElementSelectedContainerColor(character)
                        } else {
                            baziElementContainerColor(character)
                        },
                        shadowElevation = if (isSelected && editorActive) 1.dp else 0.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                value,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = baziElementColor(character),
                            )
                        }
                    }
                }
                repeat(5 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun FixedPickerSheet(
    onDismiss: () -> Unit,
    targetHeight: Dp? = null,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val maxSheetHeight = maxHeight
            val resolvedHeight = targetHeight?.let { minOf(it, maxSheetHeight) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "关闭选择器" }
                    .clickable(onClick = onDismiss)
                    .testTag("picker_scrim_dismiss"),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PickerSheetBottomClearance),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (resolvedHeight != null) Modifier.height(resolvedHeight)
                            else Modifier.heightIn(max = maxSheetHeight),
                        ),
                    color = surfaceColor,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
internal fun ObservationDateTimePickerSheet(
    currentDate: String,
    currentTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    title: String = "选择观察时刻",
    supportingText: String = "用于定位当前大运、流年与流月",
    confirmTag: String = "confirm_fortune_observation",
    sheetTag: String = "fortune_observation_picker_sheet",
    showQuickLocateInput: Boolean = false,
) {
    val now = remember { LocalDateTime.now() }
    val parsedDate = remember(currentDate) {
        runCatching { LocalDate.parse(currentDate) }.getOrDefault(now.toLocalDate())
    }
    val parsedTime = remember(currentTime) {
        runCatching { java.time.LocalTime.parse(currentTime) }.getOrDefault(now.toLocalTime())
    }
    var year by remember { mutableIntStateOf(parsedDate.year.coerceIn(SupportedPickerYears)) }
    var month by remember { mutableIntStateOf(parsedDate.monthValue) }
    var day by remember { mutableIntStateOf(parsedDate.dayOfMonth) }
    var hour by remember { mutableIntStateOf(parsedTime.hour) }
    var minute by remember { mutableIntStateOf(parsedTime.minute) }
    var quickLocateText by remember(currentDate, currentTime) { mutableStateOf("") }
    val maxDay = remember(year, month) {
        runCatching { LocalDate.of(year, month, 1).lengthOfMonth() }.getOrDefault(31)
    }
    val haptic = rememberAppHapticFeedback()
    LaunchedEffect(maxDay) {
        if (day > maxDay) day = maxDay
    }
    LaunchedEffect(showQuickLocateInput, quickLocateText) {
        if (!showQuickLocateInput) return@LaunchedEffect
        val quickLocate = parseBirthPickerQuickLocate(quickLocateText) ?: return@LaunchedEffect
        quickLocate.year?.takeIf { it in SupportedPickerYears }?.let { year = it }
        quickLocate.month?.takeIf { it in 1..12 }?.let { month = it }
        quickLocate.day?.takeIf { it in 1..31 }?.let { day = it }
        quickLocate.hour?.takeIf { it in 0..23 }?.let { hour = it }
        quickLocate.minute?.takeIf { it in 0..59 }?.let { minute = it }
    }

    FixedPickerSheet(
        onDismiss = onDismiss,
        targetHeight = BirthPickerPanelHeight,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .testTag(sheetTag),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = {
                        haptic.perform(AppHapticEvent.CONFIRM)
                        onConfirm(
                            "%04d-%02d-%02d".format(year, month, day),
                            "%02d:%02d".format(hour, minute),
                        )
                    },
                    colors = ButtonDefaults.buttonColors(NanfengNavigation, NanfengGoldLight),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag(confirmTag),
                ) { Text("确定") }
            }
            if (showQuickLocateInput) {
                BirthPickerQuickLocateInput(
                    value = quickLocateText,
                    onValueChange = { quickLocateText = it },
                    inputContentDescription = "快速定位观察日期与时刻",
                    tag = "fortune_quick_locate_input",
                )
            }
            WheelSelectionPanel(
                modifier = Modifier.fillMaxWidth().height(ObservationWheelViewportHeight),
            ) {
                ValueWheel("年", SupportedPickerYears.toList(), year, Int::toString, { year = it }, Modifier.weight(1.22f), "fortune_year_wheel")
                ValueWheel("月", (1..12).toList(), month, Int::twoDigits, { month = it }, Modifier.weight(1f), "fortune_month_wheel", cyclic = true)
                ValueWheel("日", (1..maxDay).toList(), day.coerceAtMost(maxDay), Int::twoDigits, { day = it }, Modifier.weight(1f), "fortune_day_wheel", cyclic = true)
                ValueWheel("时", (0..23).toList(), hour, Int::twoDigits, { hour = it }, Modifier.weight(1f), "fortune_hour_wheel", cyclic = true)
                ValueWheel("分", (0..59).toList(), minute, Int::twoDigits, { minute = it }, Modifier.weight(1f), "fortune_minute_wheel", cyclic = true)
            }
        }
    }
}

@Composable
private fun <T> PickerSegmentedControl(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    itemTag: ((T) -> String)? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberAppHapticFeedback()
    Surface(
        modifier = modifier,
        color = NanfengControlSurface,
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            values.forEach { value ->
                val isSelected = value == selected
                Surface(
                    onClick = {
                        if (!isSelected) {
                            haptic.perform(AppHapticEvent.SELECTION)
                            onSelected(value)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .then(
                            itemTag?.let { tag -> Modifier.testTag(tag(value)) } ?: Modifier,
                        ),
                    shape = RoundedCornerShape(21.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label(value),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) NanfengInk else NanfengNavigationMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallToggleChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) NanfengGold.copy(alpha = 0.16f) else NanfengControlSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) NanfengGold.copy(alpha = 0.35f) else Color.Transparent,
        ),
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T> ValueWheel(
    label: String,
    values: List<T>,
    selectedValue: T,
    display: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    tag: String,
    cyclic: Boolean = false,
) {
    val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val initialListIndex = remember(values, selectedIndex, cyclic) {
        if (cyclic) circularWheelIndex(selectedIndex, values.size) else selectedIndex
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialListIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val currentValues by rememberUpdatedState(values)
    val currentSelected by rememberUpdatedState(selectedValue)
    val currentOnSelected by rememberUpdatedState(onSelected)
    val haptic = rememberAppHapticFeedback()
    var lastCenteredValueIndex by remember(values, cyclic) { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(values, selectedValue, cyclic) {
        if (!listState.isScrollInProgress && selectedIndex in values.indices) {
            val targetIndex = if (cyclic) {
                circularWheelIndexNear(
                    currentIndex = listState.centeredWheelIndex() ?: listState.firstVisibleItemIndex,
                    selectedIndex = selectedIndex,
                    valueCount = values.size,
                )
            } else {
                selectedIndex
            }
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState, values, cyclic) {
        snapshotFlow { listState.isScrollInProgress to listState.centeredWheelIndex() }
            .distinctUntilChanged()
            .collect { (scrolling, listIndex) ->
                val valueIndex = listIndex?.let {
                    if (cyclic) circularWheelValueIndex(it, currentValues.size) else it
                }
                if (valueIndex == null || valueIndex !in currentValues.indices) return@collect
                if (valueIndex != lastCenteredValueIndex) {
                    lastCenteredValueIndex = valueIndex
                    if (scrolling) {
                        haptic.perform(AppHapticEvent.SNAP)
                    }
                }
                if (!scrolling) {
                    val settled = currentValues[valueIndex]
                    if (settled != currentSelected) currentOnSelected(settled)
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = BirthWheelVerticalInset),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            modifier = Modifier.height(28.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val wheelPadding = ((maxHeight - BirthWheelItemHeight) / 2).coerceAtLeast(0.dp)
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = wheelPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = label
                        stateDescription = display(selectedValue)
                    }
                    .testTag(tag),
            ) {
                items(if (cyclic) Int.MAX_VALUE else values.size) { listIndex ->
                    val valueIndex = if (cyclic) {
                        circularWheelValueIndex(listIndex, values.size)
                    } else {
                        listIndex
                    }
                    val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == listIndex }
                    val center = (
                        listState.layoutInfo.viewportStartOffset +
                            listState.layoutInfo.viewportEndOffset
                        ) / 2f
                    val distance = info?.let {
                        abs(it.offset + it.size / 2f - center) / it.size.coerceAtLeast(1)
                    } ?: 2f
                    val proximity = (1f - distance).coerceIn(0f, 1f)
                    val selected = proximity > 0.62f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BirthWheelItemHeight)
                            .graphicsLayer {
                                alpha = 0.26f + 0.74f * proximity
                                scaleX = 0.88f + 0.12f * proximity
                                scaleY = 0.88f + 0.12f * proximity
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            display(values[valueIndex]),
                            fontSize = if (selected) 20.sp else 15.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) NanfengInk else NanfengNavigationMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private fun circularWheelValueIndex(listIndex: Int, valueCount: Int): Int =
    Math.floorMod(listIndex, valueCount)

private fun circularWheelIndex(selectedIndex: Int, valueCount: Int): Int {
    val midpoint = Int.MAX_VALUE / 2
    return midpoint - Math.floorMod(midpoint, valueCount) + selectedIndex
}

private fun circularWheelIndexNear(
    currentIndex: Int,
    selectedIndex: Int,
    valueCount: Int,
): Int {
    val currentCycle = Math.floorDiv(currentIndex, valueCount)
    return listOf(currentCycle - 1, currentCycle, currentCycle + 1)
        .map { cycle -> cycle * valueCount + selectedIndex }
        .filter { it in 0 until Int.MAX_VALUE }
        .minByOrNull { candidate -> abs(candidate.toLong() - currentIndex.toLong()) }
        ?: circularWheelIndex(selectedIndex, valueCount)
}

@Composable
internal fun WheelSelectionPanel(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFBFBFA))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(
                    y = ((maxHeight - BirthWheelItemHeight) / 2).coerceAtLeast(0.dp) +
                        BirthWheelLabelCenterOffset,
                )
                .padding(horizontal = 8.dp)
                .height(BirthWheelItemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(NanfengGold.copy(alpha = 0.10f))
                .border(
                    1.dp,
                    NanfengGold.copy(alpha = 0.22f),
                    RoundedCornerShape(12.dp),
                ),
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListState.centeredWheelIndex(): Int? {
    val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    return layoutInfo.visibleItemsInfo.minByOrNull {
        abs(it.offset + it.size / 2f - center)
    }?.index
}

private fun CaseFormState.toPickerDateTime(): LocalDateTime {
    val fallback = LocalDateTime.of(1990, 1, 1, 0, 0)
    if (calendarSystem == CalendarSystem.LUNAR) {
        return fallback.withYear(year.toIntOrNull()?.coerceIn(SupportedPickerYears) ?: fallback.year)
            .withMonth(month.toIntOrNull()?.coerceIn(1, 12) ?: 1)
            .withDayOfMonth(day.toIntOrNull()?.coerceIn(1, 28) ?: 1)
            .withHour(hour.toIntOrNull()?.coerceIn(0, 23) ?: 0)
            .withMinute(minute.toIntOrNull()?.coerceIn(0, 59) ?: 0)
    }
    return runCatching {
        LocalDateTime.of(
            year.toIntOrNull() ?: 1990,
            month.toIntOrNull() ?: 1,
            day.toIntOrNull() ?: 1,
            hour.toIntOrNull() ?: 0,
            minute.toIntOrNull() ?: 0,
        )
    }.getOrDefault(fallback)
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

private fun lunarMonthName(month: Int): String = listOf(
    "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊",
).getOrElse(month - 1) { month.toString() }

private fun lunarDayName(day: Int): String = listOf(
    "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
    "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
    "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十",
).getOrElse(day - 1) { day.toString() }

private data class PillarParts(val stem: String, val branch: String)

private fun String?.toPillarParts(): PillarParts {
    val source = this.orEmpty()
    val stem = source.firstOrNull { it.toString() in HeavenlyStems }?.toString()
        ?: HeavenlyStems.first()
    val branch = source.firstOrNull { it.toString() in EarthlyBranches }?.toString()
    val compatible = compatibleBranchesFor(stem)
    return PillarParts(stem, branch.takeIf { it in compatible } ?: compatible.first())
}

private fun compatibleBranchesFor(stem: String): List<String> {
    val parity = HeavenlyStems.indexOf(stem).coerceAtLeast(0) % 2
    return EarthlyBranches.filterIndexed { index, _ -> index % 2 == parity }
}

private fun <T> List<T>.replacing(index: Int, value: T): List<T> =
    toMutableList().also { it[index] = value }

private val HeavenlyStems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
private val EarthlyBranches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
internal val PillarLabels = listOf("年柱", "月柱", "日柱", "时柱")

private val SupportedPickerYears = AlmanacContract.MIN_YEAR..AlmanacContract.MAX_YEAR
private val BirthPickerHourValues: List<Int?> = listOf(null) + (0..23).toList()
private val BirthPickerMinuteValues: List<Int?> = listOf(null) + (0..59).toList()
private val BirthPickerPanelHeight = 680.dp
private val PickerSheetBottomClearance = 24.dp
private val BirthWheelViewportHeight = 166.dp
private val ObservationWheelViewportHeight = 250.dp
private val PrimaryPickerWheelViewportHeight = 390.dp
private val BirthDateTimeWheelViewportHeight = 350.dp
private val BirthWheelItemHeight = 44.dp
private val BirthWheelLabelCenterOffset = 14.dp
private val BirthWheelVerticalInset = 12.dp

internal object BirthplaceCatalog {
    private val curatedOptions: List<BirthplaceOption> = listOf(
        place("北京市", "北京市", "东城区", 39.9288, 116.4160),
        place("北京市", "北京市", "西城区", 39.9123, 116.3659),
        place("北京市", "北京市", "朝阳区", 39.9219, 116.4436),
        place("天津市", "天津市", "和平区", 39.1172, 117.2147),
        place("上海市", "上海市", "黄浦区", 31.2316, 121.4844),
        place("上海市", "上海市", "浦东新区", 31.2215, 121.5447),
        place("重庆市", "重庆市", "渝中区", 29.5527, 106.5686),
        place("河北省", "石家庄市", "长安区", 38.0475, 114.5392),
        place("山西省", "太原市", "迎泽区", 37.8635, 112.5634),
        place("内蒙古自治区", "呼和浩特市", "新城区", 40.8583, 111.6656),
        place("辽宁省", "沈阳市", "和平区", 41.7897, 123.4204),
        place("辽宁省", "大连市", "中山区", 38.9186, 121.6440),
        place("吉林省", "长春市", "南关区", 43.8639, 125.3502),
        place("黑龙江省", "哈尔滨市", "道里区", 45.7559, 126.6168),
        place("江苏省", "南京市", "玄武区", 32.0486, 118.7977),
        place("江苏省", "苏州市", "姑苏区", 31.3114, 120.6173),
        place("浙江省", "杭州市", "西湖区", 30.2592, 120.1303),
        place("安徽省", "合肥市", "蜀山区", 31.8512, 117.2605),
        place("福建省", "福州市", "鼓楼区", 26.0820, 119.3038),
        place("福建省", "厦门市", "思明区", 24.4455, 118.0826),
        place("江西省", "南昌市", "东湖区", 28.6987, 115.9036),
        place("山东省", "济南市", "历下区", 36.6668, 117.0768),
        place("山东省", "青岛市", "市南区", 36.0758, 120.4124),
        place("河南省", "郑州市", "金水区", 34.8004, 113.6606),
        place("湖北省", "武汉市", "武昌区", 30.5542, 114.3167),
        place("湖南省", "长沙市", "岳麓区", 28.2349, 112.9313),
        place("广东省", "广州市", "天河区", 23.1247, 113.3612),
        place("广东省", "深圳市", "福田区", 22.5410, 114.0556),
        place("广西壮族自治区", "南宁市", "青秀区", 22.7858, 108.4952),
        place("海南省", "海口市", "龙华区", 20.0310, 110.3285),
        place("四川省", "成都市", "锦江区", 30.6562, 104.0833),
        place("贵州省", "贵阳市", "南明区", 26.5682, 106.7141),
        place("云南省", "昆明市", "五华区", 25.0434, 102.7040),
        place("西藏自治区", "拉萨市", "城关区", 29.6525, 91.1721),
        place("陕西省", "西安市", "雁塔区", 34.2225, 108.9480),
        place("甘肃省", "兰州市", "城关区", 36.0570, 103.8253),
        place("青海省", "西宁市", "城西区", 36.6283, 101.7658),
        place("宁夏回族自治区", "银川市", "兴庆区", 38.4739, 106.2887),
        place("新疆维吾尔自治区", "乌鲁木齐市", "天山区", 43.7954, 87.6336),
        BirthplaceOption("香港", "香港", "中西区", "Asia/Hong_Kong", 22.2819, 114.1586, true),
        BirthplaceOption("澳门", "澳门", "花地玛堂区", "Asia/Macau", 22.1987, 113.5439, true),
        BirthplaceOption("台湾", "台北市", "中正区", "Asia/Taipei", 25.0324, 121.5199, true),
        abroad("日本", "东京", "千代田区", "Asia/Tokyo", 35.6938, 139.7530),
        abroad("韩国", "首尔", "钟路区", "Asia/Seoul", 37.5735, 126.9790),
        abroad("新加坡", "新加坡", "市中心", "Asia/Singapore", 1.2903, 103.8519),
        abroad("英国", "伦敦", "威斯敏斯特", "Europe/London", 51.4975, -0.1357),
        abroad("法国", "巴黎", "巴黎一区", "Europe/Paris", 48.8640, 2.3311),
        abroad("美国", "纽约", "曼哈顿", "America/New_York", 40.7831, -73.9712),
        abroad("美国", "洛杉矶", "洛杉矶市区", "America/Los_Angeles", 34.0522, -118.2437),
        abroad("澳大利亚", "悉尼", "悉尼市", "Australia/Sydney", -33.8688, 151.2093),
    )

    /** 旧城市级兜底，仅在区县资源没有覆盖对应城市时使用。 */
    private val domesticFallback: List<BirthplaceOption> = buildList {
        addAll(domesticCities("河北省", "唐山市、秦皇岛市、邯郸市、邢台市、保定市、张家口市、承德市、沧州市、廊坊市、衡水市"))
        addAll(domesticCities("山西省", "大同市、阳泉市、长治市、晋城市、朔州市、晋中市、运城市、忻州市、临汾市、吕梁市"))
        addAll(domesticCities("内蒙古自治区", "包头市、乌海市、赤峰市、通辽市、鄂尔多斯市、呼伦贝尔市、巴彦淖尔市、乌兰察布市、兴安盟、锡林郭勒盟、阿拉善盟"))
        addAll(domesticCities("辽宁省", "鞍山市、抚顺市、本溪市、丹东市、锦州市、营口市、阜新市、辽阳市、盘锦市、铁岭市、朝阳市、葫芦岛市"))
        addAll(domesticCities("吉林省", "吉林市、四平市、辽源市、通化市、白山市、松原市、白城市、延边朝鲜族自治州"))
        addAll(domesticCities("黑龙江省", "齐齐哈尔市、鸡西市、鹤岗市、双鸭山市、大庆市、伊春市、佳木斯市、七台河市、牡丹江市、黑河市、绥化市、大兴安岭地区"))
        addAll(domesticCities("江苏省", "无锡市、徐州市、常州市、南通市、连云港市、淮安市、盐城市、扬州市、镇江市、泰州市、宿迁市"))
        addAll(domesticCities("浙江省", "宁波市、温州市、嘉兴市、湖州市、绍兴市、金华市、衢州市、舟山市、台州市、丽水市"))
        addAll(domesticCities("安徽省", "芜湖市、蚌埠市、淮南市、马鞍山市、淮北市、铜陵市、安庆市、黄山市、滁州市、阜阳市、宿州市、六安市、亳州市、池州市、宣城市"))
        addAll(domesticCities("福建省", "莆田市、三明市、泉州市、漳州市、南平市、龙岩市、宁德市"))
        addAll(domesticCities("江西省", "景德镇市、萍乡市、九江市、新余市、鹰潭市、赣州市、吉安市、宜春市、抚州市、上饶市"))
        addAll(domesticCities("山东省", "淄博市、枣庄市、东营市、烟台市、潍坊市、济宁市、泰安市、威海市、日照市、临沂市、德州市、聊城市、滨州市、菏泽市"))
        addAll(domesticCities("河南省", "开封市、洛阳市、平顶山市、安阳市、鹤壁市、新乡市、焦作市、濮阳市、许昌市、漯河市、三门峡市、南阳市、商丘市、信阳市、周口市、驻马店市、济源市"))
        addAll(domesticCities("湖北省", "黄石市、十堰市、宜昌市、襄阳市、鄂州市、荆门市、孝感市、荆州市、黄冈市、咸宁市、随州市、恩施土家族苗族自治州、仙桃市、潜江市、天门市、神农架林区"))
        addAll(domesticCities("湖南省", "株洲市、湘潭市、衡阳市、邵阳市、岳阳市、常德市、张家界市、益阳市、郴州市、永州市、怀化市、娄底市、湘西土家族苗族自治州"))
        addAll(domesticCities("广东省", "珠海市、汕头市、佛山市、韶关市、湛江市、肇庆市、江门市、茂名市、惠州市、梅州市、汕尾市、河源市、阳江市、清远市、东莞市、中山市、潮州市、揭阳市、云浮市"))
        addAll(domesticCities("广西壮族自治区", "柳州市、桂林市、梧州市、北海市、防城港市、钦州市、贵港市、玉林市、百色市、贺州市、河池市、来宾市、崇左市"))
        addAll(domesticCities("海南省", "三亚市、三沙市、儋州市、五指山市、琼海市、文昌市、万宁市、东方市、定安县、屯昌县、澄迈县、临高县、白沙黎族自治县、昌江黎族自治县、乐东黎族自治县、陵水黎族自治县、保亭黎族苗族自治县、琼中黎族苗族自治县"))
        addAll(domesticCities("四川省", "自贡市、攀枝花市、泸州市、德阳市、绵阳市、广元市、遂宁市、内江市、乐山市、南充市、眉山市、宜宾市、广安市、达州市、雅安市、巴中市、资阳市、阿坝藏族羌族自治州、甘孜藏族自治州、凉山彝族自治州"))
        addAll(domesticCities("贵州省", "六盘水市、遵义市、安顺市、毕节市、铜仁市、黔西南布依族苗族自治州、黔东南苗族侗族自治州、黔南布依族苗族自治州"))
        addAll(domesticCities("云南省", "曲靖市、玉溪市、保山市、昭通市、丽江市、普洱市、临沧市、楚雄彝族自治州、红河哈尼族彝族自治州、文山壮族苗族自治州、西双版纳傣族自治州、大理白族自治州、德宏傣族景颇族自治州、怒江傈僳族自治州、迪庆藏族自治州"))
        addAll(domesticCities("西藏自治区", "日喀则市、昌都市、林芝市、山南市、那曲市、阿里地区"))
        addAll(domesticCities("陕西省", "铜川市、宝鸡市、咸阳市、渭南市、延安市、汉中市、榆林市、安康市、商洛市"))
        addAll(domesticCities("甘肃省", "嘉峪关市、金昌市、白银市、天水市、武威市、张掖市、平凉市、酒泉市、庆阳市、定西市、陇南市、临夏回族自治州、甘南藏族自治州"))
        addAll(domesticCities("青海省", "海东市、海北藏族自治州、黄南藏族自治州、海南藏族自治州、果洛藏族自治州、玉树藏族自治州、海西蒙古族藏族自治州"))
        addAll(domesticCities("宁夏回族自治区", "石嘴山市、吴忠市、固原市、中卫市"))
        addAll(domesticCities("新疆维吾尔自治区", "克拉玛依市、吐鲁番市、哈密市、昌吉回族自治州、博尔塔拉蒙古自治州、巴音郭楞蒙古自治州、阿克苏地区、克孜勒苏柯尔克孜自治州、喀什地区、和田地区、伊犁哈萨克自治州、塔城地区、阿勒泰地区"))
        addAll(domesticCities("台湾", "新北市、桃园市、台中市、台南市、高雄市、基隆市、新竹市、嘉义市、新竹县、苗栗县、彰化县、南投县、云林县、嘉义县、屏东县、宜兰县、花莲县、台东县、澎湖县、金门县、连江县"))
    }

    /**
     * 国内区县离线目录。区县条目不附会中心点坐标，避免把行政区名称静默升级为真太阳时证据。
     */
    private val bundledDistricts: List<BirthplaceOption> by lazy {
        val stream = requireNotNull(
            BirthplaceCatalog::class.java.classLoader
                ?.getResourceAsStream("birthplace/china-districts.csv"),
        ) { "缺少离线区县目录 birthplace/china-districts.csv" }
        stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines
                .filterNot { it.isBlank() || it.startsWith('#') || it.startsWith("region|") }
                .map { line ->
                    val parts = line.split('|', limit = 3)
                    require(parts.size == 3) { "无效区县目录行：$line" }
                    BirthplaceOption(
                        region = parts[0],
                        city = parts[1],
                        district = parts[2],
                        timeZoneId = BaziTimeZoneDefaults.BEIJING_IANA_ID,
                        latitude = null,
                        longitude = null,
                        domestic = true,
                    )
                }
                .toList()
        }
    }

    val options: List<BirthplaceOption> by lazy {
        val coveredCities = bundledDistricts.mapTo(mutableSetOf()) { it.region to it.city }
        buildList {
            addAll(curatedOptions)
            addAll(bundledDistricts)
            addAll(domesticFallback.filterNot { (it.region to it.city) in coveredCities })
            addAll(overseasCoverage())
        }.distinctBy { listOf(it.region, it.city, it.district, it.timeZoneId) }
    }

    private fun overseasCoverage(): List<BirthplaceOption> = listOf(
        abroad("日本", "大阪", "大阪市", "Asia/Tokyo", 34.6937, 135.5023), abroad("日本", "京都", "京都市", "Asia/Tokyo", 35.0116, 135.7681), abroad("日本", "横滨", "横滨市", "Asia/Tokyo", 35.4437, 139.6380), abroad("日本", "札幌", "札幌市", "Asia/Tokyo", 43.0618, 141.3545), abroad("日本", "福冈", "福冈市", "Asia/Tokyo", 33.5904, 130.4017),
        abroad("韩国", "釜山", "釜山市", "Asia/Seoul", 35.1796, 129.0756), abroad("韩国", "仁川", "仁川市", "Asia/Seoul", 37.4563, 126.7052), abroad("韩国", "大邱", "大邱市", "Asia/Seoul", 35.8714, 128.6014),
        abroad("英国", "曼彻斯特", "市中心", "Europe/London", 53.4808, -2.2426), abroad("英国", "伯明翰", "市中心", "Europe/London", 52.4862, -1.8904), abroad("英国", "爱丁堡", "市中心", "Europe/London", 55.9533, -3.1883),
        abroad("法国", "里昂", "市中心", "Europe/Paris", 45.7640, 4.8357), abroad("法国", "马赛", "市中心", "Europe/Paris", 43.2965, 5.3698), abroad("法国", "尼斯", "市中心", "Europe/Paris", 43.7102, 7.2620),
        abroad("美国", "旧金山", "市中心", "America/Los_Angeles", 37.7749, -122.4194), abroad("美国", "芝加哥", "市中心", "America/Chicago", 41.8781, -87.6298), abroad("美国", "西雅图", "市中心", "America/Los_Angeles", 47.6062, -122.3321), abroad("美国", "波士顿", "市中心", "America/New_York", 42.3601, -71.0589), abroad("美国", "休斯敦", "市中心", "America/Chicago", 29.7604, -95.3698),
        abroad("澳大利亚", "墨尔本", "市中心", "Australia/Melbourne", -37.8136, 144.9631), abroad("澳大利亚", "布里斯班", "市中心", "Australia/Brisbane", -27.4698, 153.0251), abroad("澳大利亚", "珀斯", "市中心", "Australia/Perth", -31.9505, 115.8605),
        abroad("加拿大", "多伦多", "市中心", "America/Toronto", 43.6532, -79.3832), abroad("加拿大", "温哥华", "市中心", "America/Vancouver", 49.2827, -123.1207), abroad("加拿大", "蒙特利尔", "市中心", "America/Toronto", 45.5019, -73.5674),
        abroad("德国", "柏林", "市中心", "Europe/Berlin", 52.5200, 13.4050), abroad("德国", "慕尼黑", "市中心", "Europe/Berlin", 48.1351, 11.5820), abroad("德国", "法兰克福", "市中心", "Europe/Berlin", 50.1109, 8.6821),
        abroad("意大利", "罗马", "市中心", "Europe/Rome", 41.9028, 12.4964), abroad("意大利", "米兰", "市中心", "Europe/Rome", 45.4642, 9.1900),
        abroad("西班牙", "马德里", "市中心", "Europe/Madrid", 40.4168, -3.7038), abroad("西班牙", "巴塞罗那", "市中心", "Europe/Madrid", 41.3874, 2.1686),
        abroad("泰国", "曼谷", "市中心", "Asia/Bangkok", 13.7563, 100.5018), abroad("泰国", "清迈", "市中心", "Asia/Bangkok", 18.7883, 98.9853),
        abroad("马来西亚", "吉隆坡", "市中心", "Asia/Kuala_Lumpur", 3.1390, 101.6869), abroad("马来西亚", "槟城", "乔治市", "Asia/Kuala_Lumpur", 5.4141, 100.3288),
        abroad("印度尼西亚", "雅加达", "市中心", "Asia/Jakarta", -6.2088, 106.8456), abroad("菲律宾", "马尼拉", "市中心", "Asia/Manila", 14.5995, 120.9842), abroad("越南", "河内", "市中心", "Asia/Ho_Chi_Minh", 21.0278, 105.8342), abroad("越南", "胡志明市", "市中心", "Asia/Ho_Chi_Minh", 10.8231, 106.6297),
        abroad("阿联酋", "迪拜", "市中心", "Asia/Dubai", 25.2048, 55.2708), abroad("阿联酋", "阿布扎比", "市中心", "Asia/Dubai", 24.4539, 54.3773),
        abroad("俄罗斯", "莫斯科", "市中心", "Europe/Moscow", 55.7558, 37.6173), abroad("俄罗斯", "圣彼得堡", "市中心", "Europe/Moscow", 59.9343, 30.3351),
        abroad("印度", "新德里", "市中心", "Asia/Kolkata", 28.6139, 77.2090), abroad("印度", "孟买", "市中心", "Asia/Kolkata", 19.0760, 72.8777),
        abroad("新西兰", "奥克兰", "市中心", "Pacific/Auckland", -36.8509, 174.7645), abroad("新西兰", "惠灵顿", "市中心", "Pacific/Auckland", -41.2866, 174.7756),
        abroad("巴西", "圣保罗", "市中心", "America/Sao_Paulo", -23.5505, -46.6333), abroad("巴西", "里约热内卢", "市中心", "America/Sao_Paulo", -22.9068, -43.1729),
        abroad("墨西哥", "墨西哥城", "市中心", "America/Mexico_City", 19.4326, -99.1332)
    )

    /** 首页新建表单的唯一出生地区默认值；用户后续选择优先。 */
    val defaultBirthplace: BirthplaceOption = options.first { option ->
        option.region == "北京市" && option.city == "北京市" && option.district == "东城区"
    }

    fun bestMatch(locationName: String, timeZoneId: String): BirthplaceOption =
        options.firstOrNull { locationName.contains(it.district) }
            ?: options.firstOrNull { locationName.contains(it.city) }
            ?: options.firstOrNull { locationName.contains(it.region) }
            ?: options.firstOrNull { it.timeZoneId == timeZoneId }
            ?: options.first()

    private fun domesticCities(region: String, cities: String): List<BirthplaceOption> =
        cities.split('、').map { city ->
            BirthplaceOption(
                region = region,
                city = city,
                district = if (city.endsWith("自治州") || city.endsWith("盟") || city.endsWith("地区")) "驻地" else "市辖区",
                timeZoneId = BaziTimeZoneDefaults.BEIJING_IANA_ID,
                latitude = null,
                longitude = null,
                domestic = true,
            )
        }

    private fun place(
        region: String,
        city: String,
        district: String,
        latitude: Double,
        longitude: Double,
    ) = BirthplaceOption(
        region,
        city,
        district,
        BaziTimeZoneDefaults.BEIJING_IANA_ID,
        latitude,
        longitude,
        true,
    )

    private fun abroad(
        region: String,
        city: String,
        district: String,
        timeZoneId: String,
        latitude: Double,
        longitude: Double,
    ) = BirthplaceOption(
        region,
        city,
        district,
        timeZoneId,
        latitude,
        longitude,
        false,
    )
}
