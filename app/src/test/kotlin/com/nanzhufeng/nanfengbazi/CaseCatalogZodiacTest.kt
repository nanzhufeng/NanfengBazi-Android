package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.*
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeBaziEngine
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeWesternZodiacResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseCatalogZodiacTest {
    private val solar = BirthCalendarInput.Solar(CivilDateTime(2000, 2, 4, 23, 30, 0))

    @Test fun `即时摘要保留采用结果星座`() = runTest {
        val case = sampleStoredCase()
        val result = TymeBaziEngine().calculate(case.birthInput, CalculationProfile.tymeDefault())
        val saved = case.copy(calculationSnapshots = listOf(case.calculationSnapshots.single().copy(result = result)))
        assertEquals(result.basicChartDetails!!.westernZodiac, saved.toCaseSummary().westernZodiac)
        assertEquals(result.basicChartDetails!!.zodiac, saved.toCaseSummary().zodiac)
    }

    @Test fun `旧缓存恢复和增量更新均补齐星座且保留已有采用值`() = runTest {
        val missing = sampleStoredCase().toCaseSummary().let {
            it.copy(birthInput = it.birthInput.copy(calendarInput = solar), canonicalSolarDateTime = null, westernZodiac = null)
        }
        val cache = object : CaseCatalogSnapshotCache {
            override suspend fun read() = CaseCatalogSnapshot(listOf(missing), emptyMap())
            override suspend fun write(snapshot: CaseCatalogSnapshot) = Unit
        }
        val store = RepositoryCaseCatalogStore(FakeCaseRepository(), UnconfinedTestDispatcher(testScheduler), cache)
        assertEquals("水瓶", store.bootstrap().snapshot.cases.single().westernZodiac)
        store.replaceCachedCases(listOf(missing.copy(westernZodiac = "双鱼")))
        assertEquals("双鱼", store.cached()!!.cases.single().westernZodiac)
        store.replaceCachedCases(listOf(missing))
        assertEquals("水瓶", store.cached()!!.cases.single().westernZodiac)
    }

    @Test fun `农历转换规范日期优先与无效日期`() {
        val lunar = BirthCalendarInput.Lunar(LunarDateTime(2000, 1, 1, 0, 0, 0, false))
        assertEquals("水瓶", TymeWesternZodiacResolver.resolve(lunar))
        assertEquals("狮子", TymeWesternZodiacResolver.resolve(lunar, CivilDateTime(2000, 8, 1, 0, 0, 0)))
        assertNull(TymeWesternZodiacResolver.resolve(BirthCalendarInput.Solar(CivilDateTime(2000, 2, 30, 0, 0, 0))))
    }
}
