package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanzhufeng.nanfengbazi.domain.AlmanacDate
import com.nanzhufeng.nanfengbazi.domain.AlmanacDayDetails
import com.nanzhufeng.nanfengbazi.domain.AlmanacDaySummary
import com.nanzhufeng.nanfengbazi.domain.AlmanacDoubleHours
import com.nanzhufeng.nanfengbazi.domain.AlmanacMonthView
import com.nanzhufeng.nanfengbazi.domain.AlmanacPillarDetail
import com.nanzhufeng.nanfengbazi.domain.FolkBoneWeight
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun AlmanacHomeEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "打开万年历" }
            .testTag("open_almanac"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(0xFFF6EFE2),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = NanfengGold,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    "万年历",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NanfengInk,
                )
                Text(
                    "公历、农历、节气与每日宜忌",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
internal fun AlmanacScreen(
    state: StageTwoUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDate: (AlmanacDate) -> Unit,
    onSelectDateTime: (AlmanacDate, Int) -> Unit,
    onSelectDoubleHour: (Int) -> Unit,
    onAdjustFourPillars: (List<String>) -> Unit,
    onUseForChart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHapticFeedback()
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("almanac_screen"),
    ) {
        AlmanacTopBar(onBack = onBack)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val expanded = maxWidth >= 760.dp
            val month = state.almanacView
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (expanded) 16.dp else 12.dp),
            ) {
                AlmanacCalendarCard(
                    year = state.almanacYear,
                    month = state.almanacMonth,
                    selectedDay = state.almanacSelectedDay,
                    view = month,
                    loading = state.almanacLoading,
                    error = state.almanacError,
                    expanded = expanded,
                    onPreviousMonth = {
                        haptics.perform(AppHapticEvent.SELECTION)
                        onPreviousMonth()
                    },
                    onNextMonth = {
                        haptics.perform(AppHapticEvent.SELECTION)
                        onNextMonth()
                    },
                    onToday = {
                        haptics.perform(AppHapticEvent.SELECTION)
                        onToday()
                    },
                    onOpenDateTimePicker = {
                        haptics.perform(AppHapticEvent.SELECTION)
                        showDateTimePicker = true
                    },
                    onSelectDate = {
                        haptics.perform(AppHapticEvent.SELECTION)
                        onSelectDate(it)
                    },
                )
                AlmanacDetailsCard(
                    details = month?.selected,
                    selectedDoubleHourIndex = state.almanacSelectedDoubleHourIndex,
                    onSelectDoubleHour = onSelectDoubleHour,
                    onAdjustFourPillars = onAdjustFourPillars,
                    onUseForChart = {
                        haptics.perform(AppHapticEvent.CONFIRM)
                        onUseForChart()
                    },
                )
            }
        }
    }
    if (showDateTimePicker) {
        ObservationDateTimePickerSheet(
            currentDate = "%04d-%02d-%02d".format(
                state.almanacYear,
                state.almanacMonth,
                state.almanacSelectedDay,
            ),
            currentTime = "%02d:00".format(
                AlmanacDoubleHours.fromIndex(state.almanacSelectedDoubleHourIndex).representativeHour,
            ),
            onDismiss = { showDateTimePicker = false },
            onConfirm = { selectedDate, selectedTime ->
                val date = runCatching { LocalDate.parse(selectedDate) }.getOrNull()
                val time = runCatching { LocalTime.parse(selectedTime) }.getOrNull()
                if (date != null && time != null) {
                    haptics.perform(AppHapticEvent.CONFIRM)
                    onSelectDateTime(AlmanacDate(date.year, date.monthValue, date.dayOfMonth), time.hour)
                }
                showDateTimePicker = false
            },
            title = "快速跳转日期与时间",
            supportingText = "时间会定位到对应时辰",
            confirmTag = "confirm_almanac_date_time",
            sheetTag = "almanac_date_time_picker_sheet",
        )
    }
}

@Composable
private fun AlmanacTopBar(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                "万年历",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = NanfengInk,
            )
        }
    }
}

@Composable
private fun AlmanacCalendarCard(
    year: Int,
    month: Int,
    selectedDay: Int,
    view: AlmanacMonthView?,
    loading: Boolean,
    error: String?,
    expanded: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenDateTimePicker: () -> Unit,
    onSelectDate: (AlmanacDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上个月")
                }
                Text(
                    "$year 年 $month 月",
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenDateTimePicker)
                        .semantics { contentDescription = "选择年月与时间" }
                        .testTag("open_almanac_date_time_picker"),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = NanfengInk,
                )
                OutlinedButton(
                    onClick = onToday,
                    modifier = Modifier.height(36.dp),
                    contentPadding = ButtonDefaults.ContentPadding,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text("今天", style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下个月")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 5.dp),
            ) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEachIndexed { index, label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (index == 0 || index == 6) {
                            Color(0xFFB4554F)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            when {
                view != null -> AlmanacGrid(
                    cells = view.cells,
                    selectedDate = AlmanacDate(year, month, selectedDay),
                    expanded = expanded,
                    onSelectDate = onSelectDate,
                )
                loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
                error != null -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AlmanacGrid(
    cells: List<AlmanacDaySummary>,
    selectedDate: AlmanacDate,
    expanded: Boolean,
    onSelectDate: (AlmanacDate) -> Unit,
) {
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { day ->
                    AlmanacDayCell(
                        day = day,
                        selected = day.date == selectedDate,
                        today = day.date.year == today.year &&
                            day.date.month == today.monthValue &&
                            day.date.day == today.dayOfMonth,
                        expanded = expanded,
                        onClick = { onSelectDate(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlmanacDayCell(
    day: AlmanacDaySummary,
    selected: Boolean,
    today: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val solarTermDay = day.solarTerm != null
    val contentColor = when {
        selected -> Color.White
        solarTermDay -> NanfengSolarTermRed
        day.inSelectedMonth -> NanfengInk
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(if (expanded) 80.dp else 62.dp)
            .semantics {
                contentDescription = buildString {
                    append("${day.date.year}年${day.date.month}月${day.date.day}日，${day.lunarDayText}，${day.dayPillar}日")
                    day.solarTerm?.let { append("，节气$it") }
                }
            }
            .testTag("almanac_day_${day.date.year}_${day.date.month}_${day.date.day}"),
        shape = RoundedCornerShape(12.dp),
        color = when {
            selected -> NanfengGreen
            today -> Color(0xFFF6EFE2)
            else -> NanfengPageBackground.copy(alpha = if (day.inSelectedMonth) 0.72f else 0.35f)
        },
        border = when {
            selected -> null
            today -> BorderStroke(1.dp, NanfengGold)
            else -> null
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                day.date.day.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected || today || solarTermDay) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
            )
            Text(
                day.marker ?: day.lunarDayText,
                modifier = Modifier.padding(top = 1.dp),
                fontSize = if (expanded) 12.sp else 10.sp,
                lineHeight = 13.sp,
                color = if (selected) Color.White.copy(alpha = 0.9f) else {
                    if (solarTermDay) NanfengSolarTermRed
                    else if (day.marker != null) NanfengGold
                    else contentColor.copy(alpha = 0.72f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (expanded) {
                Text(
                    day.dayPillar,
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.68f),
                )
            }
        }
    }
}

@Composable
private fun AlmanacDetailsCard(
    details: AlmanacDayDetails?,
    selectedDoubleHourIndex: Int,
    onSelectDoubleHour: (Int) -> Unit,
    onAdjustFourPillars: (List<String>) -> Unit,
    onUseForChart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        if (details == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
            return@Card
        }
        Column(modifier = Modifier.padding(18.dp)) {
            val markers = buildList {
                details.solarTerm?.let(::add)
                addAll(details.festivalNames)
            }.distinct().joinToString(" · ")
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${details.date.day}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = NanfengInk,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, bottom = 3.dp),
                ) {
                    Text(
                        "${details.date.year} 年 ${details.date.month} 月 · 星期${details.weekName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "农历 ${details.lunarDateText}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NanfengGold,
                            maxLines = 1,
                        )
                        Text(
                            markers,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = NanfengGreen,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
            Text(
                "时辰",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            AlmanacDoubleHourRail(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                selectedIndex = selectedDoubleHourIndex,
                onSelect = onSelectDoubleHour,
            )
            Text(
                "${details.selectedDoubleHour.branch}时 · ${details.selectedDoubleHour.timeRangeLabel}；子时按当前口径设置计算。",
                modifier = Modifier.padding(top = 7.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
            Text(
                "八字排盘",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            AlmanacEightCharacterTable(
                pillars = details.pillars,
                modifier = Modifier.padding(top = 10.dp),
            )
            details.folkBoneWeight?.let { bone ->
                FolkBoneWeightSection(
                    bone = bone,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
            AlmanacAdviceSection("宜", details.recommends, NanfengGreen)
            Spacer(modifier = Modifier.height(10.dp))
            AlmanacAdviceSection("忌", details.avoids, Color(0xFFB4554F))
            Button(
                onClick = onUseForChart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .padding(top = 16.dp)
                    .testTag("use_almanac_date_for_chart"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NanfengNavigation,
                    contentColor = Color(0xFFF2D8A5),
                ),
                shape = RoundedCornerShape(25.dp),
            ) {
                Text("用此日期排盘", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = { onAdjustFourPillars(details.pillars.map(AlmanacPillarDetail::value)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("adjust_almanac_four_pillars"),
                shape = RoundedCornerShape(25.dp),
            ) {
                Text("人工调整四柱")
            }
            Text(
                "称骨歌诀与宜忌均属传统民俗资料，不作为事实判断。",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlmanacDoubleHourRail(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        AlmanacDoubleHours.all.forEach { hour ->
            val selected = hour.index == selectedIndex
            Surface(
                onClick = { onSelect(hour.index) },
                modifier = Modifier
                    .size(width = 42.dp, height = 46.dp)
                    .testTag("almanac_double_hour_${hour.branch}"),
                shape = RoundedCornerShape(13.dp),
                color = if (selected) NanfengNavigation else NanfengPageBackground,
                border = if (selected) null else BorderStroke(1.dp, Color(0x1A1B2732)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        hour.branch,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color(0xFFF2D8A5) else baziElementColor(branchElement(hour.branch)),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlmanacEightCharacterTable(
    pillars: List<AlmanacPillarDetail>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("almanac_eight_character_table"),
        shape = RoundedCornerShape(16.dp),
        color = NanfengPageBackground,
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            AlmanacPillarRow("", pillars) { pillar ->
                Text(
                    pillar.label,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AlmanacPillarRow("十神", pillars) { pillar ->
                Text(
                    pillar.tenGod,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = NanfengInk,
                )
            }
            AlmanacPillarRow("天干", pillars) { pillar ->
                Text(
                    pillar.heavenStem,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = baziElementColor(pillar.heavenStemElement),
                )
            }
            AlmanacPillarRow("地支", pillars) { pillar ->
                Text(
                    pillar.earthBranch,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = baziElementColor(pillar.earthBranchElement),
                )
            }
            AlmanacPillarRow("藏干", pillars, topAligned = true) { pillar ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    pillar.hiddenStems.forEach { hidden ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                hidden.heavenStem,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = baziElementColor(hidden.element),
                            )
                            Text(
                                hidden.tenGod,
                                modifier = Modifier.padding(start = 3.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            AlmanacPillarRow("神煞", pillars, topAligned = true) { pillar ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val shenSha = pillar.shenSha.ifEmpty { listOf("—") }
                    shenSha.forEach { name ->
                        Text(
                            name,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            lineHeight = 16.sp,
                            color = if (name == "—") {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            } else {
                                NanfengGold
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlmanacPillarRow(
    label: String,
    pillars: List<AlmanacPillarDetail>,
    topAligned: Boolean = false,
    cell: @Composable (AlmanacPillarDetail) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = if (topAligned) Alignment.Top else Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        pillars.forEach { pillar ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = if (topAligned) Alignment.TopCenter else Alignment.Center,
            ) { cell(pillar) }
        }
    }
}

private fun formatQian(qian: Int): String = "${qian / 10}两${qian % 10}钱"

@Composable
private fun FolkBoneWeightSection(
    bone: FolkBoneWeight,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("folk_bone_weight_section"),
        colors = CardDefaults.cardColors(containerColor = NanfengWarmTint),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "称骨算命",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatQian(bone.totalQian),
                    style = MaterialTheme.typography.titleLarge,
                    color = NanfengGold,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp)
                    .testTag("folk_bone_weight_components"),
                text = "年 ${formatQian(bone.yearQian)} · 月 ${formatQian(bone.monthQian)} · " +
                    "日 ${formatQian(bone.dayQian)} · 时 ${formatQian(bone.hourQian)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FolkBoneVerdict("男命断语", bone.maleVerdict, Modifier.padding(top = 14.dp))
            FolkBoneVerdict("女命断语", bone.femaleVerdict, Modifier.padding(top = 14.dp))
        }
    }
}

@Composable
private fun FolkBoneVerdict(
    label: String,
    verse: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = NanfengGold,
        )
        Text(
            verse,
            modifier = Modifier.padding(top = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 24.sp,
            color = NanfengInk,
        )
    }
}

private fun branchElement(branch: String): String = when (branch) {
    "寅", "卯" -> "木"
    "巳", "午" -> "火"
    "申", "酉" -> "金"
    "亥", "子" -> "水"
    else -> "土"
}

@Composable
private fun AlmanacAdviceSection(title: String, values: List<String>, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(title, color = color, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            values.ifEmpty { listOf("无特别记录") }.joinToString(" · "),
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = NanfengInk,
        )
    }
}
