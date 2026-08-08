package com.nanzhufeng.nanfengbazi.domain

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class BaziTimeZoneDefaultsTest {
    @Test
    fun `北京时间默认值是可解析的 Asia Shanghai`() {
        assertEquals("Asia/Shanghai", BaziTimeZoneDefaults.BEIJING_IANA_ID)
        assertEquals("北京时间", BaziTimeZoneDefaults.displayName("Asia/Shanghai"))
        assertEquals(ZoneId.of("Asia/Shanghai"), BaziTimeZoneDefaults.beijingZoneId)
    }
}
