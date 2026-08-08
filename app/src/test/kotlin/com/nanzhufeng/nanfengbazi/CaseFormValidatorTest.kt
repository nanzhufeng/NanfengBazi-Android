package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziTimeZoneDefaults
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CoordinateSource
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseFormValidatorTest {
    @Test
    fun `别名与性别是必填项`() {
        val noAlias = CaseFormValidator.validate(validForm().copy(alias = " "))
        val noSex = CaseFormValidator.validate(validForm().copy(sex = null))

        assertEquals(
            CaseFormValidation.Invalid("请填写命例别名。"),
            noAlias,
        )
        assertEquals(
            CaseFormValidation.Invalid("请选择性别。"),
            noSex,
        )
    }

    @Test
    fun `出生地区必填且经纬度必须成对有效`() {
        assertEquals(
            CaseFormValidation.Invalid("请填写出生地区。"),
            CaseFormValidator.validate(validForm().copy(locationName = " ")),
        )
        assertEquals(
            CaseFormValidation.Invalid("经度和纬度必须同时填写或同时留空。"),
            CaseFormValidator.validate(validForm().copy(longitude = "120.6")),
        )
        assertEquals(
            CaseFormValidation.Invalid("纬度必须在 -90 到 90 之间。"),
            CaseFormValidator.validate(
                validForm().copy(longitude = "120.6", latitude = "91"),
            ),
        )
    }

    @Test
    fun `无效公历日期会给出明确反馈`() {
        val result = CaseFormValidator.validate(
            validForm().copy(year = "2025", month = "2", day = "29"),
        )

        assertEquals(
            CaseFormValidation.Invalid("出生日期或时间无效，请检查年月日和时分秒。"),
            result,
        )
    }

    @Test
    fun `有效表单生成结构化公历输入`() {
        val result = CaseFormValidator.validate(validForm())

        assertTrue(result is CaseFormValidation.Valid)
        val valid = result as CaseFormValidation.Valid
        val solar = valid.birthInput.calendarInput as BirthCalendarInput.Solar
        assertEquals(2000, solar.dateTime.year)
        assertEquals(29, solar.dateTime.day)
        assertEquals("合成命例甲", valid.alias)
        assertEquals("江苏省苏州市", valid.birthInput.locationName)
        assertEquals(BaziTimeZoneDefaults.BEIJING_IANA_ID, valid.birthInput.timeZoneId)
    }

    @Test
    fun `新建表单默认北京时间并保留用户明确时区`() {
        assertEquals(BaziTimeZoneDefaults.BEIJING_IANA_ID, CaseFormState().timeZoneId)
        assertEquals("America/New_York", CaseFormState(timeZoneId = "America/New_York").timeZoneId)
    }

    @Test
    fun `手工经纬度记录来源且保留用户选择的 offset`() {
        val result = CaseFormValidator.validate(
            validForm().copy(
                longitude = "120.5853",
                latitude = "31.2989",
                resolvedUtcOffsetSeconds = 28_800,
            ),
        ) as CaseFormValidation.Valid

        assertEquals(120.5853, result.birthInput.longitude)
        assertEquals(31.2989, result.birthInput.latitude)
        assertEquals(CoordinateSource.USER_ENTERED, result.birthInput.coordinateSource)
        assertEquals(28_800, result.birthInput.resolvedUtcOffsetSeconds)
    }

    @Test
    fun `时间精度和来源由用户选择而不是按秒数猜测`() {
        val valid = CaseFormValidator.validate(
            validForm().copy(
                second = "15",
                timePrecision = TimePrecision.EXACT_TO_SECOND,
                timeSourceType = TimeSourceType.OFFICIAL_RECORD,
                sourceNote = "  出生证明  ",
            ),
        ) as CaseFormValidation.Valid

        assertEquals(TimePrecision.EXACT_TO_SECOND, valid.birthInput.timePrecision)
        assertEquals(TimeSourceType.OFFICIAL_RECORD, valid.birthInput.timeSourceType)
        assertEquals("出生证明", valid.birthInput.sourceNote)
        assertEquals(
            CaseFormValidation.Invalid("当前时间精度要求秒数为 0。"),
            CaseFormValidator.validate(
                validForm().copy(second = "15", timePrecision = TimePrecision.APPROXIMATE),
            ),
        )
    }

    @Test
    fun `时辰未知不伪造唯一命盘`() {
        assertEquals(
            CaseFormValidation.Invalid(
                "时辰未知不能生成唯一命盘，请先录入一个可计算的候选时间。",
            ),
            CaseFormValidator.validate(
                validForm().copy(timePrecision = TimePrecision.UNKNOWN),
            ),
        )
    }

    @Test
    fun `启用真太阳时要求经纬度并写入结构化开关`() {
        assertEquals(
            CaseFormValidation.Invalid("启用真太阳时必须填写出生地经度和纬度。"),
            CaseFormValidator.validate(validForm().copy(useTrueSolarTime = true)),
        )

        val valid = CaseFormValidator.validate(
            validForm().copy(
                longitude = "118.68",
                latitude = "33.73",
                useTrueSolarTime = true,
            ),
        ) as CaseFormValidation.Valid

        assertTrue(valid.birthInput.useTrueSolarTime)
    }

    @Test
    fun `有效农历表单保留闰月语义`() {
        val result = CaseFormValidator.validate(
            validForm().copy(
                calendarSystem = CalendarSystem.LUNAR,
                year = "2023",
                month = "2",
                day = "1",
                isLeapMonth = true,
            ),
        )

        assertTrue(result is CaseFormValidation.Valid)
        val lunar = (result as CaseFormValidation.Valid)
            .birthInput.calendarInput as BirthCalendarInput.Lunar
        assertEquals(2023, lunar.dateTime.year)
        assertEquals(2, lunar.dateTime.month)
        assertTrue(lunar.dateTime.isLeapMonth)
    }

    @Test
    fun `农历基础字段越界会给出明确反馈`() {
        val result = CaseFormValidator.validate(
            validForm().copy(
                calendarSystem = CalendarSystem.LUNAR,
                month = "2",
                day = "31",
            ),
        )

        assertEquals(
            CaseFormValidation.Invalid("农历日期必须在 1..30"),
            result,
        )
    }
}
