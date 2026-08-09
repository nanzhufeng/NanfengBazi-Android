package com.nanzhufeng.nanfengbazi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonalWuxingStateResolverTest {
    @Test
    fun `月支按四季稳定映射旺相休囚死`() {
        assertEquals(setOf("木旺", "火相", "水休", "金囚", "土死"), SeasonalWuxingStateResolver.resolve('寅'))
        assertEquals(setOf("火旺", "土相", "木休", "水囚", "金死"), SeasonalWuxingStateResolver.resolve('午'))
        assertEquals(setOf("金旺", "水相", "土休", "火囚", "木死"), SeasonalWuxingStateResolver.resolve('酉'))
        assertEquals(setOf("水旺", "木相", "金休", "土囚", "火死"), SeasonalWuxingStateResolver.resolve('子'))
    }
}
