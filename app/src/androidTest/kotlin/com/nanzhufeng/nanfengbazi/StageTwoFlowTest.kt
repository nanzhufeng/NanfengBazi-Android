package com.nanzhufeng.nanfengbazi

import android.content.ClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class StageTwoFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun threePrimaryEntriesAreReachableAndActionable() {
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        val screenWidthDp = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.configuration.screenWidthDp
        if (screenWidthDp >= 840) {
            composeRule.onNodeWithTag("root_navigation_rail").assertIsDisplayed()
        } else {
            composeRule.onNodeWithTag("root_navigation").assertIsDisplayed()
        }
        listOf("nav_chart", "nav_records", "nav_settings").forEach { tag ->
            composeRule.onNodeWithTag(tag)
                .assertHasClickAction()
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
        composeRule.onNodeWithTag("nav_records").performClick()
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_import_screenshots")
            .assertIsEnabled()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("settings_export_backup")
            .assertIsEnabled()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("settings_copy_diagnostics")
            .performScrollTo()
            .assertIsEnabled()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        val clipboard = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getSystemService(ClipboardManager::class.java)
        val diagnosticText = clipboard.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(InstrumentationRegistry.getInstrumentation().targetContext)
            ?.toString()
            .orEmpty()
        assertTrue(diagnosticText.contains("南枫八字诊断包"))
        assertTrue(diagnosticText.contains("privacy=REDACTED"))
        assertTrue(diagnosticText.contains("database.schema=8"))
        assertFalse(diagnosticText.contains("/data/"))
        composeRule.onNodeWithTag("nav_chart").performClick()
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_records").performClick()
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()
    }

    @Test
    fun fourPillarsLookupCompletesAndSurvivesActivityRecreation() {
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("open_four_pillars_lookup")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("lookup_year_pillar_wheel").performScrollToIndex(25)
        composeRule.onNodeWithTag("lookup_month_pillar_wheel").performScrollToIndex(9)
        composeRule.onNodeWithTag("lookup_day_pillar_wheel").performScrollToIndex(0)
        composeRule.onNodeWithTag("lookup_hour_pillar_wheel").performScrollToIndex(8)
        composeRule.onNodeWithTag("confirm_four_pillars_wheels").performClick()
        composeRule.onNodeWithTag("four_pillars_lookup_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("four_pillars_lookup_notice").assertIsDisplayed()
        composeRule.onNodeWithTag("open_lookup_year_range_picker").performScrollTo().performClick()
        composeRule.onNodeWithTag("lookup_start_year_wheel").performScrollToIndex(149)
        composeRule.onNodeWithTag("lookup_end_year_wheel").performScrollToIndex(149)
        composeRule.onNodeWithTag("confirm_lookup_year_range").performClick()
        composeRule.onNodeWithTag("lookup_search")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("lookup_candidate"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("lookup_result_count")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("four_pillars_lookup_screen")
            .performScrollToNode(hasTestTag("lookup_result_notice"))
        composeRule.onNodeWithTag("lookup_result_notice").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("lookup_candidate"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("four_pillars_lookup_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("four_pillars_lookup_screen")
            .performScrollToNode(hasTestTag("lookup_result_count"))
        composeRule.onNodeWithTag("lookup_result_count").assertIsDisplayed()
        composeRule.onNodeWithTag("four_pillars_lookup_screen")
            .performScrollToNode(hasText("返回"))
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
    }

    @Test
    fun savedCaseAppearsInRecordListAndOpensDetail() {
        val alias = "席瑞"
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextInput(alias)
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("open_birth_datetime_picker")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("birth_year_wheel").performScrollToIndex(1992 - 1800)
        composeRule.onNodeWithTag("birth_month_wheel").performScrollToIndex(8 - 1)
        composeRule.onNodeWithTag("birth_day_wheel").performScrollToIndex(24 - 1)
        composeRule.onNodeWithTag("birth_hour_wheel").performScrollToIndex(12)
        composeRule.onNodeWithTag("confirm_birth_datetime").performClick()
        composeRule.onNodeWithTag("open_birthplace_picker")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("confirm_birthplace").performClick()
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (
            composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("confirm_duplicate_save")
                .performScrollTo()
                .performClick()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText(alias))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("case_search").performTextReplacement(alias)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText(alias))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("case_search").performTextReplacement("")
        composeRule.onNodeWithText(alias).performClick()
        composeRule.onNodeWithTag("case_detail_screen").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()
    }

    @Test
    fun expandedLayoutKeepsCaseIndexAlongsideDetail() {
        val screenWidthDp = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.configuration.screenWidthDp
        if (screenWidthDp < 840) {
            composeRule.onNodeWithTag("root_navigation").assertIsDisplayed()
            return
        }

        val alias = "Stage2宽屏样例-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("root_navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextInput(alias)
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("birth_year").performTextInput("1992")
        composeRule.onNodeWithTag("birth_month").performTextInput("8")
        composeRule.onNodeWithTag("birth_day").performTextInput("24")
        composeRule.onNodeWithTag("birth_hour").performTextInput("12")
        composeRule.onNodeWithTag("birth_minute").performTextInput("0")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextInput("江苏省宿迁市泗阳县")
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (
            composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("confirm_duplicate_save")
                .performScrollTo()
                .performClick()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("别名：$alias"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("case_search").performTextInput(alias)
        composeRule.onNodeWithText("别名：$alias").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodes(hasTestTag("expanded_case_index_pane"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("root_navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("expanded_case_index_pane").assertIsDisplayed()
        composeRule.onNodeWithTag("case_detail_screen").assertIsDisplayed()
    }

    @Test
    fun instantChartShowsResultWithoutSavingCase() {
        val alias = "Stage4即时排盘-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextInput(alias)
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("birth_year").performTextInput("1992")
        composeRule.onNodeWithTag("birth_month").performTextInput("8")
        composeRule.onNodeWithTag("birth_day").performTextInput("24")
        composeRule.onNodeWithTag("birth_hour").performTextInput("12")
        composeRule.onNodeWithTag("birth_minute").performTextInput("0")
        composeRule.onNodeWithTag("birth_time_precision_EXACT_TO_SECOND")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("birth_time_source_SELF_REPORTED")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("birth_time_source_note")
            .performScrollTo()
            .performTextInput("本人确认到秒")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextInput("江苏省宿迁市泗阳县")
        composeRule.onNodeWithTag("preview_case").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("instant_calculation_preview"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("instant_calculation_preview")
            .performScrollTo()
            .assertIsDisplayed()
        check(
            composeRule.onAllNodes(hasText("即时排盘结果（未保存）"))
                .fetchSemanticsNodes().isNotEmpty(),
        ) {
            "即时排盘预览应明确标注未保存"
        }
        check(
            composeRule.onAllNodes(hasText("本人确认到秒"))
                .fetchSemanticsNodes().isNotEmpty(),
        ) {
            "即时排盘应保留时间来源说明"
        }
        composeRule.onNodeWithTag("nav_records").performClick()
        composeRule.onNodeWithTag("case_search").performTextInput(alias)
        check(
            composeRule.onAllNodes(hasText("别名：$alias"))
                .fetchSemanticsNodes().isEmpty(),
        ) {
            "即时排盘不应创建可搜索的命例"
        }
    }

    @Test
    fun lateRatHourRuleReachesVersionedInstantChart() {
        val alias = "晚子时-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias")
            .performTextInput(alias)
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("birth_year").performTextInput("2026")
        composeRule.onNodeWithTag("birth_month").performTextInput("7")
        composeRule.onNodeWithTag("birth_day").performTextInput("30")
        composeRule.onNodeWithTag("birth_hour").performTextInput("23")
        composeRule.onNodeWithTag("birth_minute").performTextInput("0")
        composeRule.onNodeWithTag("birth_rat_hour_rule_LATE_RAT_SAME_DAY")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextInput("江苏省宿迁市泗阳县")
        composeRule.onNodeWithTag("preview_case").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("instant_calculation_preview"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("form_error"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        val formError = composeRule.onAllNodes(
            hasAnyAncestor(hasTestTag("form_error")),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().flatMap {
            it.config.getOrNull(SemanticsProperties.Text).orEmpty()
        }.joinToString { it.text }
        check(formError.isBlank()) {
            "晚子时即时排盘失败：$formError"
        }
        composeRule.onNodeWithTag("instant_calculation_preview")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("instant_rat_hour_rule_LATE_RAT_SAME_DAY")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            "instant_calculation_profile_tyme-late-rat-same-day-v1",
        ).fetchSemanticsNode()
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (
            composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("confirm_duplicate_save")
                .performScrollTo()
                .performClick()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("别名：$alias"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$alias").performClick()
        composeRule.onNodeWithTag("detail_tab_fortune").performScrollTo().performClick()
        setFortuneObservation(year = 2026, month = 7, day = 30, hour = 23, minute = 0)
        composeRule.onNode(
            hasTestTag("fortune_profile_id").and(
                hasAnyDescendant(hasText("tyme-late-rat-same-day-v1")),
            ),
        )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("fortune_rule_version").and(
                hasAnyDescendant(hasText("stage7b-rat-hour-v1")),
            ),
        )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("flow_day_pillar")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun lunarInputShowsLeapMonthChoice() {
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("birth_calendar_lunar")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("birth_lunar_leap_month"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("birth_lunar_leap_month")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
    }

    @Test
    fun createLunarCaseAndShowConversionEvidence() {
        val alias = "Stage4农历样例-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextInput(alias)
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("birth_calendar_lunar")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("birth_lunar_leap_month"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("birth_year").performTextInput("2023")
        composeRule.onNodeWithTag("birth_month").performTextInput("1")
        composeRule.onNodeWithTag("birth_day").performTextInput("1")
        composeRule.onNodeWithTag("birth_hour").performTextInput("13")
        composeRule.onNodeWithTag("birth_minute").performTextInput("0")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextInput("江苏省苏州市")
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (
            composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("confirm_duplicate_save")
                .performScrollTo()
                .performClick()
        }
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("别名：$alias"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$alias").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("detail_tab_basic_chart")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("换算公历"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("换算公历").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2023-01-22 13:00:00")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun addAndAdoptBirthTimeCandidateWithoutDuplicatingCase() {
        val alias = "Stage4时间候选-${System.currentTimeMillis()}"
        val candidateLabel = "家人回忆 12 点"
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextInput(alias)
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("birth_year").performTextInput("2000")
        composeRule.onNodeWithTag("birth_month").performTextInput("2")
        composeRule.onNodeWithTag("birth_day").performTextInput("29")
        composeRule.onNodeWithTag("birth_hour").performTextInput("10")
        composeRule.onNodeWithTag("birth_minute").performTextInput("30")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextInput("江苏省苏州市")
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (
            composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("confirm_duplicate_save")
                .performScrollTo()
                .performClick()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("别名：$alias"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$alias").performClick()

        composeRule.onNodeWithTag("add_birth_time_candidate_button")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("add_birth_time_candidate_screen")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("birth_time_candidate_label")
            .performTextInput(candidateLabel)
        composeRule.onNodeWithTag("birth_hour")
            .performScrollTo()
            .performTextReplacement("12")
        composeRule.onNodeWithTag("birth_time_precision_APPROXIMATE")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("birth_time_source_FAMILY_REPORTED")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(candidateLabel)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("采用此时间")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("当前采用：$candidateLabel"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("candidate_mutation_error"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        val candidateErrors = composeRule.onAllNodes(
            hasTestTag("candidate_mutation_error"),
        ).fetchSemanticsNodes()
        check(candidateErrors.isEmpty()) {
            "采用出生时间候选失败：${candidateErrors.joinToString { it.config.toString() }}"
        }
        composeRule.onNodeWithText("当前采用：$candidateLabel")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("公历 2000-02-29 12:30:00")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun dstOverlapRequiresOffsetChoiceAndPersistsEvidence() {
        val alias = "Stage4时区样例-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextInput(alias)
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("birth_year").performTextInput("2024")
        composeRule.onNodeWithTag("birth_month").performTextInput("11")
        composeRule.onNodeWithTag("birth_day").performTextInput("3")
        composeRule.onNodeWithTag("birth_hour").performTextInput("1")
        composeRule.onNodeWithTag("birth_minute").performTextInput("30")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextInput("美国纽约")
        composeRule.onNodeWithTag("birth_time_zone")
            .performScrollTo()
            .performTextReplacement("America/New_York")
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("birth_utc_offset_-18000"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("birth_utc_offset_-14400")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("birth_utc_offset_-18000")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (
            composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("confirm_duplicate_save")
                .performScrollTo()
                .performClick()
        }
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("别名：$alias"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$alias").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("America/New_York")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("UTC-05:00")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("时区数据版本")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun trueSolarTimeCrossesDoubleHourAndShowsAuditEvidence() {
        val alias = "Stage4真太阳时-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextInput(alias)
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("birth_year").performTextInput("1992")
        composeRule.onNodeWithTag("birth_month").performTextInput("8")
        composeRule.onNodeWithTag("birth_day").performTextInput("24")
        composeRule.onNodeWithTag("birth_hour").performTextInput("13")
        composeRule.onNodeWithTag("birth_minute").performTextInput("4")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextInput("江苏省宿迁市泗阳县")
        composeRule.onNodeWithTag("birth_longitude")
            .performScrollTo()
            .performTextInput("118.68")
        composeRule.onNodeWithTag("birth_latitude")
            .performScrollTo()
            .performTextInput("33.73")
        composeRule.onNodeWithTag("birth_true_solar_time")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (
            composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("confirm_duplicate_save")
                .performScrollTo()
                .performClick()
        }
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("别名：$alias"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$alias").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("已启用").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail_tab_basic_chart").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail_tab_basic_chart").assertIsSelected()
        composeRule.onNodeWithText("tyme-true-solar-provisional-v1")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("basic_chart_details")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("生肖 猴 · 处女座 · 日主 壬")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("basic_chart_primary")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodes(hasText("剑锋金"))
            .onFirst()
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("前节气 处暑", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("胎元").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("命宫").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("问真同口径前一节", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("问真同口径后一节", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("detail_tab_fortune").performScrollTo().performClick()
        composeRule.onNodeWithTag("fortune_transfer_time")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("decade_fortune_details")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("fortune_observation_picker")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("current_fortune_position")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("current_annual_fortune")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("current_decade_fortune")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("professional_fortune_position")
            .performScrollTo()
            .assertIsDisplayed()
        listOf(
            "flow_year_pillar",
            "flow_month_pillar",
            "flow_day_pillar",
            "flow_hour_pillar",
            "previous_solar_term",
            "next_solar_term",
            "fortune_profile_id",
            "fortune_rule_version",
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag)
                .performScrollTo()
                .assertIsDisplayed()
        }
        setFortuneObservation(hour = 23, minute = 0)
        composeRule.onNodeWithTag("copy_fortune_diagnostics")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("诊断已复制")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("annual_fortune_details")
            .performScrollTo()
            .assertIsDisplayed()
        setFortuneObservation(year = 2026, month = 2, day = 3)
        composeRule.onNode(
            hasTestTag("current_annual_fortune")
                .and(hasAnyDescendant(hasText("乙巳", substring = true))),
        )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("detail_tab_basic_chart").performScrollTo().performClick()
        composeRule.onNodeWithText("1992-08-24 12:56:23")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("跨时辰")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("nrel-spa-2008+solarpositioning-2.0.12-v1")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun createSaveSearchAndOpenDetail() {
        val alias = "Stage2合成样例-${System.currentTimeMillis()}"
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("case_alias").performTextInput(alias)
        composeRule.onNodeWithTag("case_name").performTextInput("自动化甲")
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("birth_year").performTextInput("2000")
        composeRule.onNodeWithTag("birth_month").performTextInput("2")
        composeRule.onNodeWithTag("birth_day").performTextInput("29")
        composeRule.onNodeWithTag("birth_hour").performTextInput("10")
        composeRule.onNodeWithTag("birth_minute").performTextInput("30")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextInput("江苏省苏州市")
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (
            composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("confirm_duplicate_save")
                .performScrollTo()
                .performClick()
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasText("别名：$alias"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("case_search").performTextInput(alias)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText("别名：$alias").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("别名：$alias").performClick()

        composeRule.onNodeWithTag("case_detail_screen").assertIsDisplayed()
        val screenWidthDp = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.configuration.screenWidthDp
        if (screenWidthDp >= 840) {
            composeRule.onNodeWithTag("expanded_case_index_pane").assertIsDisplayed()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("原始录入信息"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("原始录入信息").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail_tab_basic_chart").performScrollTo().performClick()
        composeRule.onNodeWithTag("basic_chart_details").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Tyme4j").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail_tab_basic_info").performScrollTo().performClick()

        val editedAlias = "$alias-已编辑"
        composeRule.onNodeWithTag("edit_case_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("case_alias").performTextReplacement(editedAlias)
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (
            composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("confirm_duplicate_save")
                .performScrollTo()
                .performClick()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("export_single_case_button"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasText(editedAlias))
            .onFirst()
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasText("命例资料已重新排盘并保存；旧计算快照仍保留。"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasText("命例资料已重新排盘并保存；旧计算快照仍保留。"),
            ).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("detail_tab_basic_chart").performScrollTo().performClick()
        composeRule.onNodeWithTag("calculation_archive_comparison")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("同一输入与同一版本重新计算，可核对结果稳定性。")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("核心排盘结果一致。")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("detail_tab_basic_info").performScrollTo().performClick()

        composeRule.onNodeWithTag("edit_metadata_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("metadata_groups").performTextInput("合成分组")
        composeRule.onNodeWithTag("metadata_tags").performTextInput("自动化标签")
        composeRule.onNodeWithTag("metadata_favorite").performClick()
        composeRule.onNodeWithTag("metadata_pinned").performClick()
        composeRule.onNodeWithTag("save_metadata").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("合成分组").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("自动化标签").performScrollTo().assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("命例分组、标签与标记已保存。"))
                .fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithTag("detail_tab_records").performScrollTo().performClick()
        composeRule.onNodeWithTag("add_record_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("record_editor_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("分析记录").performClick()
        composeRule.onNodeWithText("事业").performClick()
        composeRule.onNodeWithTag("record_content").performTextInput("Stage3 合成分析初稿")
        composeRule.onNodeWithTag("save_record").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("分析记录 · 事业").performScrollTo().assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("记录已保存。"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("记录已保存。")).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("record_card").performScrollTo().performClick()
        composeRule.onNodeWithText("财运").performClick()
        composeRule.onNodeWithTag("record_content")
            .performTextReplacement("Stage3 合成分析修订稿")
        composeRule.onNodeWithTag("save_record").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("分析记录 · 财运").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("版本历史（记录 2 / 事件 0）")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Stage3 合成分析初稿")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("add_event_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("event_editor_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("event_title").performTextInput("Stage3 合成事件标题")
        composeRule.onNodeWithText("事业").performClick()
        composeRule.onNodeWithTag("event_year").performScrollTo().performTextInput("2024")
        composeRule.onNodeWithTag("event_content")
            .performScrollTo()
            .performTextInput("Stage3 合成关键事件")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("save_event")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("mutation_error"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        val eventErrors = composeRule.onAllNodes(hasTestTag("mutation_error"))
            .fetchSemanticsNodes()
        check(eventErrors.isEmpty()) {
            "事件保存失败：${eventErrors.joinToString { it.config.toString() }}"
        }
        composeRule.onAllNodes(hasText("Stage3 合成关键事件"))[0]
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodes(hasText("Stage3 合成事件标题"))[0]
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodes(hasText("2024年 · 事业"))[0]
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("detail_tab_basic_info").performClick()
        composeRule.onNodeWithTag("toggle_case_management").performClick()
        composeRule.onNodeWithTag("export_single_case_button").performScrollTo().performClick()
        composeRule.onNodeWithText("选择单命例导出内容").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_single_case_export").performClick()
        val saveButton = device.wait(
            Until.findObject(
                By.text(Pattern.compile("(?i)save|保存")),
            ),
            10_000,
        )
        checkNotNull(saveButton) { "系统创建文档页面未出现保存按钮" }
        saveButton.click()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasText("单命例 JSON 已导出，图片仅保留引用信息。"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasText("单命例 JSON 已导出，图片仅保留引用信息。"),
            ).fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithTag("duplicate_case_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("复制来源")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("复制来源").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("trash_case_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("confirm_trash_button").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("visibility_trashed").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("别名：$editedAlias（副本）"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$editedAlias（副本）").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("restore_case_button"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("restore_case_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("visibility_active").assertIsDisplayed()
        composeRule.onNodeWithTag("visibility_active").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("别名：$editedAlias（副本）"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$editedAlias（副本）")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("record_more").performClick()
        composeRule.onNodeWithTag("open_case_comparison")
            .performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_comparison_report"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("case_comparison_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_comparison_summary")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "结果仅描述字段异同，不生成吉凶、合婚或关系结论。",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("back_from_case_comparison").performClick()
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("record_more").performClick()
        composeRule.onNodeWithTag("import_single_case_button").performClick()
        val exportedFileName = "${editedAlias.take(48)}_南枫八字命例.json"
        val exportedFile = device.wait(
            Until.findObject(By.text(exportedFileName)),
            10_000,
        )
        checkNotNull(exportedFile) { "系统打开文档页面未找到刚导出的单命例 JSON" }
        exportedFile.click()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasTestTag("single_case_preview"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("单命例导入预览").assertIsDisplayed()
        composeRule.onNodeWithText("稳定 ID 已存在", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodes(hasText("与此命例生成合并差异"))[1]
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("single_case_merge_dialog"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("追加文本记录", substring = true)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("追加事件", substring = true)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("confirm_single_case_merge").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("single_case_merge_dialog"))
                .fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("record_more").performClick()
        composeRule.onNodeWithTag("import_single_case_button").performClick()
        val exportedFileAgain = device.wait(
            Until.findObject(By.text(exportedFileName)),
            10_000,
        )
        checkNotNull(exportedFileAgain) { "系统打开文档页面未找到待二次导入的 JSON" }
        exportedFileAgain.click()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasTestTag("single_case_preview"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("keep_both_single_case").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("single_case_preview"))
                .fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("别名：$editedAlias"))
                .fetchSemanticsNodes().size >= 2
        }

        val encryptedExportAlias = "$editedAlias（副本）"
        composeRule.onNodeWithTag("case_search")
            .performTextReplacement(encryptedExportAlias)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("别名：$encryptedExportAlias"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$encryptedExportAlias")
            .performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("detail_tab_basic_info").performClick()
        composeRule.onNodeWithTag("toggle_case_management").performClick()
        composeRule.onNodeWithTag("export_single_case_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("choose_password_single_case_export").performClick()
        val encryptedPassword = "Stage3B-Pass123"
        composeRule.onNodeWithTag("single_case_password")
            .performTextInput(encryptedPassword)
        composeRule.onNodeWithTag("single_case_password_confirmation")
            .performTextInput(encryptedPassword)
        composeRule.onNodeWithTag("confirm_password_single_case_export").performClick()
        val encryptedSaveButton = device.wait(
            Until.findObject(By.text(Pattern.compile("(?i)save|保存"))),
            10_000,
        )
        checkNotNull(encryptedSaveButton) { "加密导出未进入系统创建文档页面" }
        encryptedSaveButton.click()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(
                hasText("密码加密单命例已导出；请另行安全保存密码。"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        device.pressBack()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("record_more").performClick()
        composeRule.onNodeWithTag("import_single_case_button").performClick()
        val encryptedFileName = "${encryptedExportAlias.take(48)}_南枫八字命例_加密.json"
        val encryptedFile = device.wait(
            Until.findObject(By.text(encryptedFileName)),
            10_000,
        )
        checkNotNull(encryptedFile) { "系统打开文档页面未找到加密单命例 JSON" }
        encryptedFile.click()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("输入单命例解密密码"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("single_case_password")
            .performTextInput(encryptedPassword)
        composeRule.onNodeWithTag("confirm_password_single_case_import").performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("single_case_preview"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("文件保护：密码加密")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("skip_single_case_import").performClick()
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("record_more").performClick()
        composeRule.onNodeWithTag("export_full_backup_button").performClick()
        composeRule.onNodeWithTag("confirm_full_backup_export").performClick()
        val backupSaveButton = device.wait(
            Until.findObject(By.text(Pattern.compile("(?i)save|保存"))),
            10_000,
        )
        checkNotNull(backupSaveButton) { "完整备份导出未进入系统创建文档页面" }
        backupSaveButton.click()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText("完整未加密备份已导出", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("record_more").performClick()
        composeRule.onNodeWithTag("preview_full_backup_button").performClick()
        val backupFile = device.wait(
            Until.findObject(By.text(Pattern.compile("南枫八字备份_.*\\.zip"))),
            10_000,
        )
        checkNotNull(backupFile) { "系统打开文档页面未找到完整备份 ZIP" }
        backupFile.click()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("full_backup_preview"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("文件保护：未加密")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("独立临时数据库预演：通过")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("full_backup_conflict_summary")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("full_backup_case_list")
            .performScrollToNode(hasTestTag("full_backup_merge_candidate"))
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("full_backup_merge_candidate"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasTestTag("full_backup_merge_candidate"))
            .onFirst()
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText("选择完整备份合并范围"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("full_backup_error"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        val mergeErrors = composeRule.onAllNodes(hasTestTag("full_backup_error"))
            .fetchSemanticsNodes()
        check(mergeErrors.isEmpty()) {
            mergeErrors.first().config.getOrNull(SemanticsProperties.Text)
                ?.joinToString(separator = "") { it.text }
                ?: "完整备份合并准备返回未知错误"
        }
        composeRule.onNodeWithText("选择完整备份合并范围").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_full_backup_merge").assertIsNotEnabled()
        composeRule.onNodeWithTag("cancel_full_backup_merge").performClick()
        composeRule.onNodeWithTag("full_backup_preview").assertIsDisplayed()
        composeRule.onNodeWithTag("skip_all_full_backup_cases")
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithTag("prepare_full_backup_plan")
            .assertIsEnabled()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("full_backup_plan_ready"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("request_full_backup_restore")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("confirm_full_backup_restore")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodes(
                    hasText("完整备份恢复完成", substring = true),
                ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("record_more").performClick()
        composeRule.onNodeWithTag("export_full_backup_button").performClick()
        composeRule.onNodeWithTag("choose_password_full_backup_export").performClick()
        val fullBackupPassword = "FullBackup-Pass123"
        composeRule.onNodeWithTag("full_backup_password")
            .performTextInput(fullBackupPassword)
        composeRule.onNodeWithTag("full_backup_password_confirmation")
            .performTextInput(fullBackupPassword)
        composeRule.onNodeWithTag("confirm_password_full_backup_export").performClick()
        val encryptedBackupSaveButton = device.wait(
            Until.findObject(By.text(Pattern.compile("(?i)save|保存"))),
            10_000,
        )
        checkNotNull(encryptedBackupSaveButton) { "加密完整备份未进入系统创建文档页面" }
        encryptedBackupSaveButton.click()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasText("密码加密完整备份已导出", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("record_more").performClick()
        composeRule.onNodeWithTag("preview_full_backup_button").performClick()
        val encryptedBackupFile = device.wait(
            Until.findObject(By.text(Pattern.compile("南枫八字备份_.*_加密\\.nfbak"))),
            10_000,
        )
        checkNotNull(encryptedBackupFile) { "系统打开文档页面未找到加密完整备份" }
        encryptedBackupFile.click()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("输入完整备份解密密码"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("full_backup_password")
            .performTextInput(fullBackupPassword)
        composeRule.onNodeWithTag("confirm_password_full_backup_import").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodes(hasTestTag("full_backup_preview"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("文件保护：密码加密")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("full_backup_conflict_summary")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("close_full_backup_preview").performClick()
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()
    }

    private fun setFortuneObservation(
        year: Int? = null,
        month: Int? = null,
        day: Int? = null,
        hour: Int? = null,
        minute: Int? = null,
    ) {
        composeRule.onNodeWithTag("fortune_observation_picker")
            .performScrollTo()
            .performClick()
        year?.let {
            composeRule.onNodeWithTag("fortune_year_wheel").performScrollToIndex(it - 1800)
        }
        month?.let {
            composeRule.onNodeWithTag("fortune_month_wheel").performScrollToIndex(it - 1)
        }
        day?.let {
            composeRule.onNodeWithTag("fortune_day_wheel").performScrollToIndex(it - 1)
        }
        hour?.let {
            composeRule.onNodeWithTag("fortune_hour_wheel").performScrollToIndex(it)
        }
        minute?.let {
            composeRule.onNodeWithTag("fortune_minute_wheel").performScrollToIndex(it)
        }
        composeRule.onNodeWithTag("confirm_fortune_observation").performClick()
    }
}
