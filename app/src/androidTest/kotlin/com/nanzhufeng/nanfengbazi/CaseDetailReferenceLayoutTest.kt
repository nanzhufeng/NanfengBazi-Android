package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.EventDatePrecision
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("case_detail_tabs")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("detail_tab_basic_info").performClick()
        composeRule.onNodeWithTag("case_identity_header", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("出生地区").assertIsDisplayed()
        composeRule.onNodeWithText("前一节气").assertIsDisplayed()
        composeRule.onNodeWithText("后一节气").assertIsDisplayed()
        val birthplace = composeRule.onNodeWithTag("basic_info_birthplace_row")
            .fetchSemanticsNode().boundsInRoot
        val previousTerm = composeRule.onNodeWithTag("basic_info_previous_term_row")
            .fetchSemanticsNode().boundsInRoot
        val nextTerm = composeRule.onNodeWithTag("basic_info_next_term_row")
            .fetchSemanticsNode().boundsInRoot
        val zodiacFact = composeRule.onNodeWithTag("basic_info_zodiac_fact")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(birthplace.bottom <= previousTerm.top)
        assertTrue(previousTerm.bottom <= nextTerm.top)
        assertTrue(nextTerm.bottom <= zodiacFact.top)
        val alignedRightColumn = listOf(
            composeRule.onNodeWithTag("basic_info_sex_fact").fetchSemanticsNode().boundsInRoot.left,
            zodiacFact.left,
            composeRule.onNodeWithTag("basic_info_fetal_breath_fact")
                .fetchSemanticsNode().boundsInRoot.left,
        )
        alignedRightColumn.drop(1).forEach { left ->
            assertEquals(alignedRightColumn.first(), left, 1f)
        }

        composeRule.onNodeWithTag("detail_tab_basic_chart").performClick()
        composeRule.onNodeWithTag("shared_identity_solar_time").assertIsDisplayed()
        composeRule.onNodeWithTag("shared_identity_lunar_time").assertIsDisplayed()
        composeRule.onNodeWithTag("shared_identity_chart_type").assertIsDisplayed()
        composeRule.onNodeWithText("乾造").assertIsDisplayed()
        composeRule.onNodeWithTag("basic_chart_details").assertIsDisplayed()
        composeRule.onNodeWithText("十神").assertIsDisplayed()
        composeRule.onNodeWithText("藏干").assertIsDisplayed()
        composeRule.onNodeWithTag("basic_chart_shensha").performScrollTo().assertIsDisplayed()
        val shenShaText = composeRule.onNodeWithTag(
            "basic_chart_shensha_value_0",
            useUnmergedTree = true,
        ).fetchSemanticsNode().config[SemanticsProperties.Text]
            .joinToString("") { it.text }
        assertTrue("神煞必须逐行显示", "、" !in shenShaText)
        assertTrue("单柱最多显示 5 项", shenShaText.lines().size <= 5)
        composeRule.onNodeWithText("副星").assertDoesNotExist()
        composeRule.onNodeWithText("星运").assertDoesNotExist()

        composeRule.onNodeWithTag("detail_tab_fortune").performClick()
        composeRule.onNodeWithTag("shared_identity_solar_time").assertIsDisplayed()
        composeRule.onNodeWithTag("shared_identity_lunar_time").assertIsDisplayed()
        composeRule.onNodeWithTag("shared_identity_chart_type").assertIsDisplayed()
        composeRule.onNodeWithTag("flow_hour_pillar").assertIsDisplayed()
        val stemSurface = composeRule.onNodeWithTag("flow_hour_stem_surface")
            .fetchSemanticsNode().boundsInRoot
        val stemText = composeRule.onNodeWithTag("flow_hour_stem_text")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(stemSurface.height > stemText.height)
        assertEquals(
            stemText.top - stemSurface.top,
            stemSurface.bottom - stemText.bottom,
            1f,
        )
        val hiddenStemRow = composeRule.onNodeWithTag("professional_hidden_stem_row_0")
            .fetchSemanticsNode().boundsInRoot
        val hiddenStemText = composeRule.onNodeWithTag("flow_hour_hidden_stem_0")
            .fetchSemanticsNode().boundsInRoot
        val hiddenTenGodText = composeRule.onNodeWithTag("flow_hour_hidden_ten_god_0")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(hiddenStemText.top >= hiddenStemRow.top)
        assertTrue(hiddenStemText.bottom <= hiddenStemRow.bottom)
        assertTrue(hiddenTenGodText.top >= hiddenStemRow.top)
        assertTrue(hiddenTenGodText.bottom <= hiddenStemRow.bottom)
        val finalHiddenStemRow = composeRule.onNodeWithTag("professional_hidden_stem_row_2")
            .fetchSemanticsNode().boundsInRoot
        val professionalMatrix = composeRule
            .onNodeWithTag("professional_fortune_position")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(professionalMatrix.bottom > finalHiddenStemRow.bottom)
        composeRule.onNodeWithText("八字排盘").assertIsDisplayed()
        composeRule.onNodeWithTag("professional_transit_natal_divider").assertIsDisplayed()
        composeRule.onNodeWithText("起运", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("交运", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("fortune_completed_age").assertIsDisplayed()
        composeRule.onNodeWithText("今").assertIsDisplayed()
        composeRule.onNodeWithTag("fortune_today").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("daily_fortune_details").performScrollTo().assertIsDisplayed()
        assertTenColumnTimelineGrid("daily_fortune_details")
        composeRule.onNodeWithTag("annual_fortune_details").performScrollTo().assertIsDisplayed()
        assertTenColumnTimelineGrid("annual_fortune_details")
        composeRule.onNodeWithTag("monthly_fortune_details").performScrollTo().assertIsDisplayed()
        assertTenColumnTimelineGrid("monthly_fortune_details")
        composeRule.onNodeWithTag("hourly_fortune_details").performScrollTo().assertIsDisplayed()
        assertTenColumnTimelineGrid("hourly_fortune_details")
        val today = LocalDate.now()
        val dailyList = composeRule.onNodeWithTag("daily_fortune_details_list")
        dailyList.performScrollToNode(hasTestTag("timeline_day_${today.withDayOfMonth(1)}"))
        (1..10).forEach { day ->
            composeRule.onNodeWithTag("timeline_day_${today.withDayOfMonth(day)}")
                .assertIsDisplayed()
        }
        val targetDay = if (today.dayOfMonth == 14) 15 else 14
        val selectedDayTag = "timeline_day_${today.withDayOfMonth(targetDay)}"
        dailyList.performScrollToNode(hasTestTag(selectedDayTag))
        val dayLeftBeforeClick = composeRule.onNodeWithTag(selectedDayTag)
            .fetchSemanticsNode().boundsInRoot.left
        composeRule.onNodeWithTag(selectedDayTag).performClick()
        composeRule.waitForIdle()
        val dayLeftAfterClick = composeRule.onNodeWithTag(selectedDayTag)
            .fetchSemanticsNode().boundsInRoot.left
        assertEquals(dayLeftBeforeClick, dayLeftAfterClick, 1f)
        composeRule.onNodeWithTag("decade_fortune_details").performScrollTo().assertIsDisplayed()
        val decadeList = composeRule.onNodeWithTag("decade_fortune_details_list")
        assertTenColumnTimelineGrid("decade_fortune_details")
        decadeList.performScrollToNode(hasTestTag("timeline_minor_stage"))
        composeRule.onNodeWithTag("timeline_minor_stage").assertIsDisplayed()
        composeRule.onNodeWithTag("timeline_minor_stage_upper", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("timeline_minor_stage_lower", useUnmergedTree = true)
            .assertIsDisplayed()
        (0 until 9).forEach { index ->
            composeRule.onNodeWithTag("timeline_decade_$index").assertIsDisplayed()
        }
        val timelineStem = composeRule.onNodeWithTag(
            "timeline_decade_0_stem",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val timelineStemDetail = composeRule.onNodeWithTag(
            "timeline_decade_0_stem_detail",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val timelineBranch = composeRule.onNodeWithTag(
            "timeline_decade_0_branch",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val timelineBranchDetail = composeRule.onNodeWithTag(
            "timeline_decade_0_branch_detail",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val stemDetailGap = timelineStemDetail.top - timelineStem.bottom
        val branchDetailGap = timelineBranchDetail.top - timelineBranch.bottom
        assertTrue(branchDetailGap >= -1f)
        assertTrue(branchDetailGap <= stemDetailGap + 2f)
        decadeList.performScrollToNode(hasTestTag("timeline_decade_11"))
        composeRule.onNodeWithTag("timeline_decade_11").assertIsDisplayed()
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
        composeRule.onNodeWithTag("decade_fortune_details").performScrollTo().assertIsDisplayed()
        decadeList.performScrollToNode(hasTestTag("timeline_minor_stage"))
        composeRule.onNodeWithTag("timeline_minor_stage").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("timeline_annual_1997").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("annual_fortune_details").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("timeline_annual_1997").assertIsDisplayed()
        composeRule.onNodeWithText("阳历 1992-", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("detail_tab_records").performClick()
        composeRule.onNodeWithTag("notes_identity_header").assertIsDisplayed()
        composeRule.onNodeWithTag("notes_identity_chart_type").assertIsDisplayed()
        composeRule.onNodeWithTag("notes_identity_four_pillars").assertIsDisplayed()
        (0 until 10).forEach { index ->
            composeRule.onNodeWithTag("notes_decade_$index").assertIsDisplayed()
        }
        composeRule.onNodeWithText("关键事件反馈记录").assertIsDisplayed()
        composeRule.onNodeWithText("工作方向发生明显调整。").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("detail_tab_fortune").performClick()
        composeRule.onNodeWithTag("professional_fortune_position").assertIsDisplayed()
    }

    private fun assertTenColumnTimelineGrid(tag: String) {
        val listBounds = composeRule.onNodeWithTag("${tag}_list")
            .fetchSemanticsNode().boundsInRoot
        val columnBounds = composeRule.onAllNodesWithTag(
            "${tag}_column",
            useUnmergedTree = true,
        ).fetchSemanticsNodes().map { it.boundsInRoot }
        assertTrue(columnBounds.size >= 10)
        val expectedWidth = listBounds.width / 10f
        columnBounds.take(10).forEach { bounds ->
            assertEquals(expectedWidth, bounds.width, 1f)
        }
    }
}
