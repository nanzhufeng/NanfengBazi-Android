package com.nanzhufeng.nanfengbazi.data

import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationResultCompatibilityTest {
    @Test
    fun `缺少历法转换证据的旧计算快照仍可读取`() {
        val current = sampleCase().calculationSnapshots.single().result
        val currentJson = DomainJson.encodeToString(current)
        val legacyJson = currentJson
            .replace(",\"calendarConversion\":null", "")
            .replace("\"calendarConversion\":null,", "")

        assertFalse(legacyJson.contains("\"calendarConversion\""))
        val decoded = DomainJson.decodeFromString<CalculationResult>(legacyJson)

        assertNull(decoded.calendarConversion)
    }

    @Test
    fun `旧出生输入缺少坐标来源和时区解析证据仍可读取`() {
        val current = sampleCase().birthInput
        val currentJson = DomainJson.encodeToString(current)
        val legacyJson = currentJson
            .replace(",\"coordinateSource\":\"USER_ENTERED\"", "")
            .replace(",\"resolvedUtcOffsetSeconds\":32400", "")
            .replace(",\"timeZoneDataVersion\":\"tzdb:fixture\"", "")
            .replace(",\"timeSourceType\":\"OFFICIAL_RECORD\"", "")

        val decoded = DomainJson.decodeFromString<BirthInput>(legacyJson)

        assertEquals(current.longitude, decoded.longitude)
        assertEquals(current.latitude, decoded.latitude)
        assertNull(decoded.coordinateSource)
        assertNull(decoded.resolvedUtcOffsetSeconds)
        assertNull(decoded.timeZoneDataVersion)
        assertEquals(TimeSourceType.UNKNOWN, decoded.timeSourceType)
    }
}
