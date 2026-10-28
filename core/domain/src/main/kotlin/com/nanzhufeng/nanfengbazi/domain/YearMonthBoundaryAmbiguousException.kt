package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.SolarTermPoint

/** No CalculationResult may be adopted while the supplied time range straddles a jie. */
class YearMonthBoundaryAmbiguousException(
    val boundary: SolarTermPoint,
    val candidateYearPillars: List<String>,
    val candidateMonthPillars: List<String>,
    val candidateZodiacs: List<String>,
) : IllegalArgumentException(
    "年/月柱待确认：年柱${candidateYearPillars.joinToString("/")}，" +
        "生肖${candidateZodiacs.joinToString("/")}，月柱${candidateMonthPillars.joinToString("/")}。" +
        "出生时间范围跨越${boundary.name}交节（北京时间" +
        with(boundary.at) { "%04d-%02d-%02d %02d:%02d:%02d".format(year, month, day, hour, minute, second) } +
        "），请补充更精确的出生时间或保留两个候选。",
)
