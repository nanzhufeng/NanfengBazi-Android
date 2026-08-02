package com.nanzhufeng.nanfengbazi

import androidx.test.platform.app.InstrumentationRegistry
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * 仅在显式传入 seedPreviewCases=true 时写入本地模拟器，用于记录列表视觉验收。
 * 正常测试、正式构建和首次启动均不会内置或生成这些数据。
 */
class RecordPreviewFixtureTest {
    @Test
    fun seedNamedPreviewCasesOnExplicitRequest() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("seedPreviewCases") == "true")
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as NanfengBaziApplication
        val createCase = CreateCaseUseCase(
            baziEngine = application.container.baziEngine,
            caseRepository = application.container.caseRepository,
        )
        previewCases.forEach { fixture ->
            val result = createCase(
                form = CaseFormState(
                    alias = fixture.name,
                    name = fixture.name,
                    sex = fixture.sex,
                    year = fixture.year.toString(),
                    month = fixture.month.toString(),
                    day = fixture.day.toString(),
                    hour = "12",
                    minute = "0",
                    locationName = "北京市东城区",
                    longitude = "116.4167",
                    latitude = "39.9167",
                    timeZoneId = "Asia/Shanghai",
                ),
                allowDuplicate = true,
            )
            assertTrue(
                "${fixture.name} 写入失败：$result",
                result is CreateCaseResult.Created || result is CreateCaseResult.AlreadyExists,
            )
        }
    }
}

private data class PreviewCaseFixture(
    val name: String,
    val sex: SexForFortuneDirection,
    val year: Int,
    val month: Int,
    val day: Int,
)

private val previewCases = listOf(
    PreviewCaseFixture("席瑞", SexForFortuneDirection.MAN, 1992, 8, 24),
    PreviewCaseFixture("安倍", SexForFortuneDirection.MAN, 1954, 9, 21),
    PreviewCaseFixture("林清娟", SexForFortuneDirection.WOMAN, 1967, 1, 22),
    PreviewCaseFixture("苏晚晴", SexForFortuneDirection.WOMAN, 1995, 3, 18),
    PreviewCaseFixture("程墨", SexForFortuneDirection.MAN, 2000, 8, 5),
    PreviewCaseFixture("沈星遥", SexForFortuneDirection.WOMAN, 2012, 12, 12),
    PreviewCaseFixture("顾南乔", SexForFortuneDirection.WOMAN, 1982, 9, 8),
    PreviewCaseFixture("江予安", SexForFortuneDirection.MAN, 1990, 1, 1),
)
