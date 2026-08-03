package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.nanzhufeng.nanfengbazi.domain.AlmanacMonthView
import java.time.LocalDate

@Composable
internal fun AlmanacHomeEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
    onUseForChart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHapticFeedback()
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
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    AlmanacCalendarCard(
                        year = state.almanacYear,
                        month = state.almanacMonth,
                        selectedDay = state.almanacSelectedDay,
                        view = month,
                        loading = state.almanacLoading,
                        error = state.almanacError,
                        expanded = true,
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
                        onSelectDate = {
                            haptics.perform(AppHapticEvent.SELECTION)
                            onSelectDate(it)
                        },
                        modifier = Modifier.weight(1.65f),
                    )
                    AlmanacDetailsCard(
                        details = month?.selected,
                        onUseForChart = {
                            haptics.perform(AppHapticEvent.CONFIRM)
                            onUseForChart()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AlmanacCalendarCard(
                        year = state.almanacYear,
                        month = state.almanacMonth,
                        selectedDay = state.almanacSelectedDay,
                        view = month,
                        loading = state.almanacLoading,
                        error = state.almanacError,
                        expanded = false,
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
                        onSelectDate = {
                            haptics.perform(AppHapticEvent.SELECTION)
                            onSelectDate(it)
                        },
                    )
                    AlmanacDetailsCard(
                        details = month?.selected,
                        onUseForChart = {
                            haptics.perform(AppHapticEvent.CONFIRM)
                            onUseForChart()
                        },
                    )
                }
            }
        }
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
                    modifier = Modifier.weight(1f),
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
    val contentColor = when {
        selected -> Color.White
        day.inSelectedMonth -> NanfengInk
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Surface(
        modifier = modifier
            .height(if (expanded) 80.dp else 62.dp)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${day.date.year}年${day.date.month}月${day.date.day}日，${day.lunarDayText}，${day.dayPillar}日"
            }
            .testTag("almanac_day_${day.date.year}_${day.date.month}_${day.date.day}"),
        shape = RoundedCornerShape(12.dp),
        color = when {
            selected -> NanfengGreen
            today -> Color(0xFFF6EFE2)
            else -> NanfengPageBackground.copy(alpha = if (day.inSelectedMonth) 0.72f else 0.35f)
        },
        border = if (today && !selected) BorderStroke(1.dp, NanfengGold) else null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                day.date.day.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
            )
            Text(
                day.marker ?: day.lunarDayText,
                modifier = Modifier.padding(top = 1.dp),
                fontSize = if (expanded) 12.sp else 10.sp,
                lineHeight = 13.sp,
                color = if (selected) Color.White.copy(alpha = 0.9f) else {
                    if (day.marker != null) NanfengGold else contentColor.copy(alpha = 0.72f)
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${details.date.day}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = NanfengInk,
                )
                Column(modifier = Modifier.padding(start = 10.dp, bottom = 3.dp)) {
                    Text(
                        "${details.date.year} 年 ${details.date.month} 月 · 星期${details.weekName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "农历 ${details.lunarDateText}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NanfengGold,
                    )
                }
            }
            val markers = buildList {
                details.solarTerm?.let(::add)
                addAll(details.festivalNames)
            }
            if (markers.isNotEmpty()) {
                Text(
                    markers.joinToString(" · "),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NanfengGreen,
                    fontWeight = FontWeight.Medium,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
            Text(
                "当日干支",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillarBlock("年柱", details.yearPillar, Modifier.weight(1f))
                PillarBlock("月柱", details.monthPillar, Modifier.weight(1f))
                PillarBlock("日柱", details.dayPillar, Modifier.weight(1f))
            }
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                AlmanacInfoRow("星座", details.constellation)
                AlmanacInfoRow("建除", details.duty)
                AlmanacInfoRow("值神", details.twelveStar)
                AlmanacInfoRow("星宿", details.twentyEightStar)
                AlmanacInfoRow("胎神", details.fetusPosition)
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
            Text(
                "宜忌、值神等属于传统民俗资料，不作为事实判断。",
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
private fun PillarBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = NanfengPageBackground,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = NanfengInk,
            )
        }
    }
}

@Composable
private fun AlmanacInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.width(46.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = NanfengInk,
        )
    }
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
