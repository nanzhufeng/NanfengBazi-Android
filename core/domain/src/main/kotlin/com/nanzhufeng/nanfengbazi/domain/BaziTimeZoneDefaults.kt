package com.nanzhufeng.nanfengbazi.domain

import java.time.ZoneId

/** The sole default for new, restored-without-a-value, and local import workflows. */
object BaziTimeZoneDefaults {
    const val BEIJING_IANA_ID: String = "Asia/Shanghai"
    /** Almanac wall clocks explicitly denote UTC+08, including historical dates. */
    const val CHINA_STANDARD_IANA_ID: String = "Etc/GMT-8"
    const val BEIJING_DISPLAY_NAME: String = "北京时间"

    val beijingZoneId: ZoneId = ZoneId.of(BEIJING_IANA_ID)

    fun displayName(timeZoneId: String): String =
        if (timeZoneId == BEIJING_IANA_ID || timeZoneId == CHINA_STANDARD_IANA_ID) BEIJING_DISPLAY_NAME else timeZoneId
}
