package com.nanzhufeng.nanfengbazi

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthplaceCatalogTest {
    @Test
    fun bundledCatalogProvidesDomesticProvinceLevelCoverageAndOverseasChoices() {
        val domesticRegions = BirthplaceCatalog.options.filter { it.domestic }.map { it.region }.toSet()
        val overseasRegions = BirthplaceCatalog.options.filterNot { it.domestic }.map { it.region }.toSet()

        assertTrue(domesticRegions.size >= 34)
        assertTrue(overseasRegions.size >= 6)
        assertTrue("北京市" in domesticRegions)
        assertTrue("美国" in overseasRegions)
    }

    @Test
    fun everyBundledChoiceHasValidIanaZoneAndCoordinate() {
        BirthplaceCatalog.options.forEach { option ->
            assertEquals(option.timeZoneId, ZoneId.of(option.timeZoneId).id)
            assertTrue(option.latitude in -90.0..90.0)
            assertTrue(option.longitude in -180.0..180.0)
            assertTrue(option.displayName.isNotBlank())
        }
    }

    @Test
    fun currentLocationIsRestoredInsteadOfResetToFirstChoice() {
        val shanghai = BirthplaceCatalog.bestMatch("上海市 浦东新区", "Asia/Shanghai")
        val london = BirthplaceCatalog.bestMatch("英国 伦敦", "Europe/London")

        assertEquals("浦东新区", shanghai.district)
        assertEquals("Europe/London", london.timeZoneId)
    }
}
