package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.LunarDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaseIdentityZodiacTest {
    private val birth = BirthCalendarInput.Solar(CivilDateTime(2000, 2, 4, 8, 0, 0))

    @Test fun `旧排盘缺星座时详情身份栏按规范公历显示水瓶`() {
        assertEquals("水瓶", resolveCaseWesternZodiac(birth, birth.dateTime, null))
    }

    @Test fun `无排盘快照时仍可从出生日期得到星座`() {
        assertEquals("水瓶", resolveCaseWesternZodiac(birth, null, null))
    }

    @Test fun `空白字段也视为缺失且规范公历优先于原始日期`() {
        assertEquals("狮子", resolveCaseWesternZodiac(birth, CivilDateTime(2000, 8, 1, 0, 0, 0), " "))
    }

    @Test fun `已有采用值保留`() {
        assertEquals("双鱼座", resolveCaseWesternZodiac(birth, birth.dateTime, "双鱼座"))
    }

    @Test fun `农历旧命例使用同一历法库补齐`() {
        val lunar = BirthCalendarInput.Lunar(LunarDateTime(2000, 1, 1, 0, 0, 0, false))
        assertEquals("水瓶", resolveCaseWesternZodiac(lunar, null, null))
    }

    @Test fun `无效日期不伪造星座`() {
        val invalid = BirthCalendarInput.Solar(CivilDateTime(2000, 2, 30, 0, 0, 0))
        assertNull(resolveCaseWesternZodiac(invalid, null, null))
    }
}
