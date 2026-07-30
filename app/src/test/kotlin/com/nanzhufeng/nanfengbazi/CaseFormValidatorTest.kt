package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CoordinateSource
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
        assertEquals("Asia/Shanghai", valid.birthInput.timeZoneId)
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
