package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
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
    }
}
