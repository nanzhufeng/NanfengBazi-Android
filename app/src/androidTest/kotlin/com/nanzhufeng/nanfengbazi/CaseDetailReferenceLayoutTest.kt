package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.EventDatePrecision
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CaseDetailReferenceLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val alias = "命例详情验收-${System.currentTimeMillis()}"

    @Before
    fun seedReferenceCase(): Unit = runBlocking {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as NanfengBaziApplication
        val createCase = CreateCaseUseCase(
            baziEngine = application.container.baziEngine,
            caseRepository = application.container.caseRepository,
        )
        val result = createCase(
            form = CaseFormState(
                alias = alias,
                name = "席瑞",
                sex = SexForFortuneDirection.MAN,
                year = "1992",
                month = "8",
                day = "24",
                hour = "12",
                minute = "0",
                locationName = "北京市东城区",
                longitude = "116.4167",
                latitude = "39.9167",
                timeZoneId = "Asia/Shanghai",
            ),
            allowDuplicate = true,
        )
        val caseId = when (result) {
            is CreateCaseResult.Created -> result.caseId
            is CreateCaseResult.AlreadyExists -> result.caseId
            else -> error("测试命例写入失败：$result")
        }
        val repository = application.container.caseRepository
        val current = requireNotNull(repository.findById(caseId))
        if (current.textRecords.isEmpty()) {
            val now = Instant.parse("2026-08-08T12:00:00Z")
            val write = repository.save(
                current.copy(
                    textRecords = listOf(
                        CaseTextRecord(
                            id = "reference-owner-feedback-$caseId",
                            type = CaseTextRecordType.OWNER_FEEDBACK,
                            content = "工作方向有调整，希望重点核对事业节奏。",
                            sourceType = TextRecordSourceType.USER,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    ),
                    events = listOf(
                        CaseEvent(
                            id = "reference-event-$caseId",
                            year = 2017,
                            datePrecision = EventDatePrecision.YEAR,
                            rawText = "工作方向发生明显调整。",
                            createdAt = now,
                        ),
                    ),
                ),
                expectedRevision = current.revision,
            )
            check(write is CaseWriteResult.Updated) { "测试笔记写入失败：$write" }
        }
        composeRule.activityRule.scenario.recreate()
        Unit
    }

    @Test
    fun fourReferencePanelsKeepSharedHeaderAndDirectContent() {
        composeRule.onNodeWithTag("nav_records").performClick()
        composeRule.onNodeWithTag("case_search").performTextReplacement(alias)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("别名：$alias")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$alias").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("case_detail_screen")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("case_identity_header").assertIsDisplayed()
        composeRule.onNodeWithText("出生地区").assertIsDisplayed()
        composeRule.onNodeWithText("前一节气").assertIsDisplayed()
        composeRule.onNodeWithText("后一节气").assertIsDisplayed()

        composeRule.onNodeWithTag("detail_tab_basic_chart").performClick()
        composeRule.onNodeWithTag("basic_chart_details").assertIsDisplayed()
        composeRule.onNodeWithText("十神").assertIsDisplayed()
        composeRule.onNodeWithText("藏干").assertIsDisplayed()
        composeRule.onNodeWithTag("basic_chart_shensha").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("副星").assertDoesNotExist()
        composeRule.onNodeWithText("星运").assertDoesNotExist()

        composeRule.onNodeWithTag("detail_tab_fortune").performClick()
        composeRule.onNodeWithTag("flow_hour_pillar").assertIsDisplayed()
        composeRule.onNodeWithText("八字排盘").assertIsDisplayed()
        composeRule.onNodeWithText("起运", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("交运", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("周岁", substring = true)[0].assertIsDisplayed()
        composeRule.onNodeWithTag("fortune_today").assertIsDisplayed()
        composeRule.onNodeWithTag("decade_fortune_details").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("annual_fortune_details").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("timeline_annual_2017").performClick()
        composeRule.onNodeWithText("阳历 2017-", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("monthly_fortune_details").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("daily_fortune_details").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("hourly_fortune_details").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("fortune_selected_datetime").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("定位今天").assertIsDisplayed()
        composeRule.onNodeWithTag("fortune_interactions").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("fortune_shensha").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("detail_tab_records").performClick()
        composeRule.onNodeWithText("关键事件反馈记录").assertIsDisplayed()
        composeRule.onNodeWithText("工作方向发生明显调整。").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("detail_tab_fortune").performClick()
        composeRule.onNodeWithTag("professional_fortune_position").assertIsDisplayed()
    }
}
