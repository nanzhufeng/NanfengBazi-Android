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
        assertTrue(BirthplaceCatalog.options.count { it.domestic } >= 2_800)
        assertTrue(overseasRegions.size >= 20)
        assertTrue("北京市" in domesticRegions)
        assertTrue("美国" in overseasRegions)
    }

    @Test
    fun everyBundledChoiceHasValidIanaZoneAndCoordinate() {
        BirthplaceCatalog.options.forEach { option ->
            assertEquals(option.timeZoneId, ZoneId.of(option.timeZoneId).id)
            option.latitude?.let { assertTrue(it in -90.0..90.0) }
            option.longitude?.let { assertTrue(it in -180.0..180.0) }
            assertEquals(option.latitude == null, option.longitude == null)
            assertTrue(option.displayName.isNotBlank())
        }
    }

    @Test
    fun catalogIncludesEveryProvinceLevelRegionAndMainstreamOverseasCities() {
        val domesticRegions = BirthplaceCatalog.options.filter { it.domestic }.map { it.region }.toSet()

        assertTrue(setOf("河北省", "广东省", "四川省", "新疆维吾尔自治区", "台湾").all(domesticRegions::contains))
        assertTrue(BirthplaceCatalog.options.any { it.region == "广东省" && it.city == "珠海市" })
        assertTrue(BirthplaceCatalog.options.any { it.region == "湖北省" && it.city == "宜昌市" })
        assertTrue(
            BirthplaceCatalog.options.any {
                it.region == "广东省" && it.city == "深圳市" && it.district == "南山区"
            },
        )
        assertTrue(
            BirthplaceCatalog.options.any {
                it.region == "河北省" && it.city == "保定市" && it.district == "涞水县"
            },
        )
        assertTrue(
            BirthplaceCatalog.options.any {
                it.region == "四川省" && it.city == "成都市" && it.district == "双流区"
            },
        )
        assertTrue(BirthplaceCatalog.options.any { it.region == "加拿大" && it.city == "温哥华" })
        assertTrue(BirthplaceCatalog.options.any { it.region == "泰国" && it.city == "曼谷" })
    }

    @Test
    fun currentLocationIsRestoredInsteadOfResetToFirstChoice() {
        val shanghai = BirthplaceCatalog.bestMatch("上海市 浦东新区", "Asia/Shanghai")
        val london = BirthplaceCatalog.bestMatch("英国 伦敦", "Europe/London")

        assertEquals("浦东新区", shanghai.district)
        assertEquals("Europe/London", london.timeZoneId)
    }
}
