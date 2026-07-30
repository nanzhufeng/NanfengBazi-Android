package com.nanzhufeng.nanfengbazi.data

import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
}
