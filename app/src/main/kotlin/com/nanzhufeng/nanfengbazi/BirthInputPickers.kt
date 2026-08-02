package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
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
    val isLeapMonth: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BirthDateTimePickerSheet(
    form: CaseFormState,
    onDismiss: () -> Unit,
    onConfirm: (BirthDateTimeSelection) -> Unit,
    onOpenFourPillars: () -> Unit,
    showFourPillarsOption: Boolean = true,
) {
    val haptic = rememberAppHapticFeedback()
    val initial = remember(form) { form.toPickerDateTime() }
    var mode by remember {
        mutableStateOf(
            if (form.calendarSystem == CalendarSystem.LUNAR) {
                BirthPickerMode.LUNAR
            } else {
                BirthPickerMode.SOLAR
            },
        )
    }
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.monthValue) }
    var day by remember { mutableIntStateOf(initial.dayOfMonth) }
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minute by remember { mutableIntStateOf(initial.minute) }
    var leapMonth by remember { mutableStateOf(form.isLeapMonth) }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 440.dp, max = 660.dp)
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .testTag("birth_datetime_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PickerSegmentedControl(
                    values = if (showFourPillarsOption) {
                        BirthPickerMode.entries
                    } else {
                        listOf(BirthPickerMode.SOLAR, BirthPickerMode.LUNAR)
                    },
                    selected = mode,
                    label = BirthPickerMode::label,
                    onSelected = { mode = it },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        val now = LocalDateTime.now()
                        mode = BirthPickerMode.SOLAR
                        year = now.year
                        month = now.monthValue
                        day = now.dayOfMonth
                        hour = now.hour
                        minute = now.minute
                        leapMonth = false
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("birth_picker_now"),
                ) {
                    Text("今", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        haptic.perform(AppHapticEvent.CONFIRM)
                        if (mode == BirthPickerMode.FOUR_PILLARS) {
                            onDismiss()
                            onOpenFourPillars()
                        } else {
                            onConfirm(
                                BirthDateTimeSelection(
                                    mode = mode,
                                    year = year,
                                    month = month,
                                    day = day,
                                    hour = hour,
                                    minute = minute,
                                    isLeapMonth = mode == BirthPickerMode.LUNAR && leapMonth,
                                ),
                            )
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("confirm_birth_datetime"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NanfengNavigation,
                        contentColor = NanfengGoldLight,
                    ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text(if (mode == BirthPickerMode.FOUR_PILLARS) "反查" else "确定")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (mode == BirthPickerMode.FOUR_PILLARS) {
                FourPillarsEntryPreview(onOpenFourPillars)
            } else {
                if (mode == BirthPickerMode.LUNAR) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        SmallToggleChip(
                            text = "普通月",
                            selected = !leapMonth,
                            onClick = { leapMonth = false },
                        )
                        Spacer(Modifier.width(8.dp))
                        SmallToggleChip(
                            text = "闰月",
                            selected = leapMonth,
                            onClick = { leapMonth = true },
                        )
                    }
                }
                WheelSelectionPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BirthWheelViewportHeight),
                ) {
                    ValueWheel(
                        label = "年",
                        values = (1800..2100).toList(),
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
                    )
                    ValueWheel(
                        label = "时",
                        values = (0..23).toList(),
                        selectedValue = hour,
                        display = Int::twoDigits,
                        onSelected = { hour = it },
                        modifier = Modifier.weight(1f),
                        tag = "birth_hour_wheel",
                    )
                    ValueWheel(
                        label = "分",
                        values = (0..59).toList(),
                        selectedValue = minute,
                        display = Int::twoDigits,
                        onSelected = { minute = it },
                        modifier = Modifier.weight(1f),
                        tag = "birth_minute_wheel",
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NanfengWarmTint,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            if (mode == BirthPickerMode.SOLAR) {
                                "%04d-%02d-%02d  %02d:%02d".format(year, month, day, hour, minute)
                            } else {
                                "${year}年${if (leapMonth) "闰" else ""}${lunarMonthName(month)}月" +
                                    "${lunarDayName(day)}  ${hour.twoDigits()}:${minute.twoDigits()}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = NanfengInk,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "历史夏令时由所选 IANA 时区自动解析，不要求手工判断。",
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
private fun FourPillarsEntryPreview(onOpen: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "年柱 · 月柱 · 日柱 · 时柱",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf("年", "月", "日", "时").forEachIndexed { index, label ->
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = listOf(
                        Color(0xFFF5F0E6),
                        Color(0xFFFCEBEC),
                        Color(0xFFECF5EF),
                        Color(0xFFEEF2FB),
                    )[index],
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
        Text(
            "进入四柱反查后滑动选择四柱与年份范围；候选是可解释、可复算的民用时间，不是出生分钟的唯一证明。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("open_four_pillars_from_picker"),
            colors = ButtonDefaults.buttonColors(
                containerColor = NanfengNavigation,
                contentColor = NanfengGoldLight,
            ),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text("开始四柱反查")
        }
    }
}

internal data class BirthplaceOption(
    val region: String,
    val city: String,
    val district: String,
    val timeZoneId: String,
    val latitude: Double,
    val longitude: Double,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 460.dp, max = 650.dp)
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
                    .height(BirthWheelViewportHeight),
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
                    Text(
                        "${selected.timeZoneId} · 城市中心参考坐标 ${selected.latitude}, ${selected.longitude}",
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "首版使用离线地点库；真太阳时默认关闭，启用前请在详细设置核对精确经纬度。",
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
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val haptic = rememberAppHapticFeedback()
    val cycle = remember { sexagenaryCycle() }
    var year by remember { mutableStateOf(current.getOrNull(0).takeIf { it in cycle } ?: "甲子") }
    var month by remember { mutableStateOf(current.getOrNull(1).takeIf { it in cycle } ?: "甲子") }
    var day by remember { mutableStateOf(current.getOrNull(2).takeIf { it in cycle } ?: "甲子") }
    var hour by remember { mutableStateOf(current.getOrNull(3).takeIf { it in cycle } ?: "甲子") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .testTag("four_pillars_wheel_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("选择四柱", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "四列均为有效六十甲子",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = {
                        haptic.perform(AppHapticEvent.CONFIRM)
                        onConfirm(listOf(year, month, day, hour))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NanfengNavigation,
                        contentColor = NanfengGoldLight,
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("confirm_four_pillars_wheels"),
                ) {
                    Text("确定")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            WheelSelectionPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BirthWheelViewportHeight),
            ) {
                ValueWheel("年柱", cycle, year, { it }, { year = it }, Modifier.weight(1f), "lookup_year_pillar_wheel")
                ValueWheel("月柱", cycle, month, { it }, { month = it }, Modifier.weight(1f), "lookup_month_pillar_wheel")
                ValueWheel("日柱", cycle, day, { it }, { day = it }, Modifier.weight(1f), "lookup_day_pillar_wheel")
                ValueWheel("时柱", cycle, hour, { it }, { hour = it }, Modifier.weight(1f), "lookup_hour_pillar_wheel")
            }
            Text(
                "候选是可解释、可复算的民用时间，不是出生分钟的唯一证明。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YearRangeWheelPickerSheet(
    startYear: Int,
    endYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val haptic = rememberAppHapticFeedback()
    val years = remember { (1800..2100).toList() }
    var start by remember { mutableIntStateOf(startYear.coerceIn(1800, 2100)) }
    var end by remember { mutableIntStateOf(endYear.coerceIn(1800, 2100)) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .testTag("lookup_year_range_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("查找年份范围", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = {
                        haptic.perform(AppHapticEvent.CONFIRM)
                        onConfirm(minOf(start, end), maxOf(start, end))
                    },
                    colors = ButtonDefaults.buttonColors(NanfengNavigation, NanfengGoldLight),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("confirm_lookup_year_range"),
                ) { Text("确定") }
            }
            WheelSelectionPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BirthWheelViewportHeight),
            ) {
                ValueWheel("起始年", years, start, Int::toString, { start = it }, Modifier.weight(1f), "lookup_start_year_wheel")
                ValueWheel("结束年", years, end, Int::toString, { end = it }, Modifier.weight(1f), "lookup_end_year_wheel")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IanaTimeZoneWheelPickerSheet(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val haptic = rememberAppHapticFeedback()
    val zones = remember { BirthplaceCatalog.options.map(BirthplaceOption::timeZoneId).distinct() }
    var selected by remember { mutableStateOf(current.takeIf { it in zones } ?: "Asia/Shanghai") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .testTag("lookup_time_zone_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("IANA 时区", style = MaterialTheme.typography.titleLarge)
                    Text("离线常用时区", style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        haptic.perform(AppHapticEvent.CONFIRM)
                        onConfirm(selected)
                    },
                    colors = ButtonDefaults.buttonColors(NanfengNavigation, NanfengGoldLight),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("confirm_lookup_time_zone"),
                ) { Text("确定") }
            }
            WheelSelectionPanel(
                modifier = Modifier.fillMaxWidth().height(BirthWheelViewportHeight),
            ) {
                ValueWheel(
                    label = "时区",
                    values = zones,
                    selectedValue = selected,
                    display = { it },
                    onSelected = { selected = it },
                    modifier = Modifier.weight(1f),
                    tag = "lookup_time_zone_wheel",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObservationDateTimePickerSheet(
    currentDate: String,
    currentTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val now = remember { LocalDateTime.now() }
    val parsedDate = remember(currentDate) {
        runCatching { LocalDate.parse(currentDate) }.getOrDefault(now.toLocalDate())
    }
    val parsedTime = remember(currentTime) {
        runCatching { java.time.LocalTime.parse(currentTime) }.getOrDefault(now.toLocalTime())
    }
    var year by remember { mutableIntStateOf(parsedDate.year.coerceIn(1800, 2100)) }
    var month by remember { mutableIntStateOf(parsedDate.monthValue) }
    var day by remember { mutableIntStateOf(parsedDate.dayOfMonth) }
    var hour by remember { mutableIntStateOf(parsedTime.hour) }
    var minute by remember { mutableIntStateOf(parsedTime.minute) }
    val maxDay = remember(year, month) {
        runCatching { LocalDate.of(year, month, 1).lengthOfMonth() }.getOrDefault(31)
    }
    val haptic = rememberAppHapticFeedback()
    LaunchedEffect(maxDay) {
        if (day > maxDay) day = maxDay
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .testTag("fortune_observation_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("选择观察时刻", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "用于定位当前大运、流年与流月",
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
                    modifier = Modifier.testTag("confirm_fortune_observation"),
                ) { Text("确定") }
            }
            WheelSelectionPanel(
                modifier = Modifier.fillMaxWidth().height(BirthWheelViewportHeight),
            ) {
                ValueWheel("年", (1800..2100).toList(), year, Int::toString, { year = it }, Modifier.weight(1.22f), "fortune_year_wheel")
                ValueWheel("月", (1..12).toList(), month, Int::twoDigits, { month = it }, Modifier.weight(1f), "fortune_month_wheel")
                ValueWheel("日", (1..maxDay).toList(), day.coerceAtMost(maxDay), Int::twoDigits, { day = it }, Modifier.weight(1f), "fortune_day_wheel")
                ValueWheel("时", (0..23).toList(), hour, Int::twoDigits, { hour = it }, Modifier.weight(1f), "fortune_hour_wheel")
                ValueWheel("分", (0..59).toList(), minute, Int::twoDigits, { minute = it }, Modifier.weight(1f), "fortune_minute_wheel")
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
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .clickable {
                            if (!isSelected) {
                                haptic.perform(AppHapticEvent.SELECTION)
                                onSelected(value)
                            }
                        },
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
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
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
private fun <T> ValueWheel(
    label: String,
    values: List<T>,
    selectedValue: T,
    display: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    tag: String,
) {
    val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val currentValues by rememberUpdatedState(values)
    val currentSelected by rememberUpdatedState(selectedValue)
    val currentOnSelected by rememberUpdatedState(onSelected)
    val haptic = rememberAppHapticFeedback()
    var lastCenteredIndex by remember(values) { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(values, selectedValue) {
        if (!listState.isScrollInProgress && selectedIndex in values.indices) {
            listState.scrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(listState, values) {
        snapshotFlow { listState.isScrollInProgress to listState.centeredWheelIndex() }
            .distinctUntilChanged()
            .collect { (scrolling, index) ->
                if (index == null || index !in currentValues.indices) return@collect
                if (index != lastCenteredIndex) {
                    lastCenteredIndex = index
                    val next = currentValues[index]
                    if (scrolling && next != currentSelected) {
                        haptic.perform(AppHapticEvent.SNAP)
                        currentOnSelected(next)
                    }
                }
            }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            modifier = Modifier.height(28.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BirthWheelListHeight),
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = BirthWheelPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = label
                        stateDescription = display(selectedValue)
                    }
                    .testTag(tag),
            ) {
                items(values.size) { index ->
                    val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
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
                            display(values[index]),
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

@Composable
private fun WheelSelectionPanel(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFBFBFA))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = BirthWheelSelectionBandOffset)
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
        return fallback.withYear(year.toIntOrNull()?.coerceIn(1800, 2100) ?: fallback.year)
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

private fun sexagenaryCycle(): List<String> {
    val stems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    val branches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    return List(60) { index -> stems[index % stems.size] + branches[index % branches.size] }
}

private val BirthWheelViewportHeight = 166.dp
private val BirthWheelListHeight = 138.dp
private val BirthWheelItemHeight = 44.dp
private val BirthWheelPadding = (BirthWheelListHeight - BirthWheelItemHeight) / 2
private val BirthWheelSelectionBandOffset = 28.dp + BirthWheelPadding

internal object BirthplaceCatalog {
    val options: List<BirthplaceOption> = listOf(
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

    fun bestMatch(locationName: String, timeZoneId: String): BirthplaceOption =
        options.firstOrNull { locationName.contains(it.district) }
            ?: options.firstOrNull { locationName.contains(it.city) }
            ?: options.firstOrNull { locationName.contains(it.region) }
            ?: options.firstOrNull { it.timeZoneId == timeZoneId }
            ?: options.first()

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
        "Asia/Shanghai",
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
