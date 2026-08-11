package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AutomatedPickerFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun birthTimeAndBirthplaceUsePrefilledWheelSheets() {
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithText("北京市 东城区").assertIsDisplayed()

        composeRule.onNodeWithTag("open_birth_datetime_picker")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("birth_datetime_picker_sheet")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(560.dp)
        composeRule.onNodeWithTag("picker_scrim_dismiss").performTouchInput {
            click(Offset(8f, 8f))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("birth_datetime_picker_sheet")
                .fetchSemanticsNodes()
                .isEmpty()
        }

        composeRule.onNodeWithTag("open_birth_datetime_picker").performClick()
        composeRule.onNodeWithTag("birth_datetime_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_picker_mode_solar").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_picker_mode_lunar").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_picker_mode_four_pillars").assertIsDisplayed()
        composeRule.onNodeWithText("今天").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_picker_mode_four_pillars").performClick()
        composeRule.onNodeWithTag("birth_datetime_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("four_pillars_wheel_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_picker_today").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_picker_mode_lunar").performClick()
        composeRule.onNodeWithTag("birth_datetime_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_picker_mode_solar").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_picker_mode_lunar").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_picker_mode_four_pillars").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_picker_today").assertIsDisplayed()
        composeRule.onNodeWithText("闰月").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_year_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_minute_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_year_wheel").performScrollToIndex(144)
        composeRule.onNodeWithTag("birth_datetime_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_birth_datetime").performClick()

        composeRule.onNodeWithTag("open_birthplace_picker")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("birthplace_picker_sheet")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(640.dp)
        composeRule.onNodeWithTag("birthplace_region_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("birthplace_city_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("birthplace_district_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("birthplace_region_wheel").performScrollToIndex(2)
        composeRule.onNodeWithTag("birthplace_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_birthplace").performClick()

        val groupName = "自动分组-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("open_case_group_picker")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("home_group_picker_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("home_group_option_none").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "已选中"),
        )
        composeRule.onNodeWithTag("home_group_create_and_select").assertIsDisplayed()
        composeRule.onNodeWithTag("home_group_new_name").performTextInput(groupName)
        composeRule.onNodeWithTag("home_group_create_and_select").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(groupName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("完成").performClick()
        composeRule.onNodeWithText(groupName).assertIsDisplayed()
    }

    @Test
    fun fourPillarsAndRangeUseSelectorsInsteadOfFormattedTyping() {
        composeRule.onNodeWithTag("open_four_pillars_lookup")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("four_pillars_wheel_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_four_pillars_wheels").assertIsDisplayed()
        composeRule.onAllNodesWithText("先选柱位，再直接选择天干与可配地支").assertCountEquals(0)
        composeRule.onAllNodesWithText("确认后直接选择时间候选；选中后回填录入页，不会自动保存命例。")
            .assertCountEquals(0)
        composeRule.onNodeWithTag("lookup_pillar_selector").assertIsDisplayed()
        composeRule.onNodeWithTag("lookup_pillar_stem_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("lookup_pillar_branch_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("lookup_year_range_inline").assertIsDisplayed()
        composeRule.onAllNodesWithTag("lookup_time_zone_inline").assertCountEquals(0)
        composeRule.onNodeWithTag("lookup_pillar_option_月柱").performClick()
        composeRule.onNodeWithTag("lookup_pillar_月柱_stem_slot")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "正在编辑"))
        composeRule.onNodeWithTag("lookup_pillar_stem_editor")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "正在编辑"))
        val stemEditorBoundsBefore = composeRule.onNodeWithTag("lookup_pillar_stem_editor")
            .fetchSemanticsNode().boundsInRoot
        val branchEditorBoundsBefore = composeRule.onNodeWithTag("lookup_pillar_branch_editor")
            .fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithTag("lookup_pillar_stem_辛").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lookup_pillar_月柱_branch_slot")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "正在编辑"))
        composeRule.onNodeWithTag("lookup_pillar_branch_editor")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "正在编辑"))
        assertEquals(
            stemEditorBoundsBefore,
            composeRule.onNodeWithTag("lookup_pillar_stem_editor").fetchSemanticsNode().boundsInRoot,
        )
        assertEquals(
            branchEditorBoundsBefore,
            composeRule.onNodeWithTag("lookup_pillar_branch_editor").fetchSemanticsNode().boundsInRoot,
        )
        composeRule.onNodeWithTag("lookup_year_range_inline").performClick()
        composeRule.onNodeWithTag("lookup_year_range_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("lookup_start_year_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("lookup_end_year_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("lookup_start_year_wheel").performScrollToIndex(100)
        composeRule.onNodeWithTag("lookup_end_year_wheel").performScrollToIndex(400)
        composeRule.onNodeWithTag("confirm_lookup_year_range").performClick()
        composeRule.onNodeWithText("1900–2200").assertIsDisplayed()
        composeRule.onNodeWithTag("lookup_pillar_branch_酉").performClick()
        composeRule.onNodeWithTag("confirm_four_pillars_wheels").performClick()
        composeRule.onNodeWithTag("four_pillars_lookup_screen").assertIsDisplayed()
    }

    @Test
    fun almanacOpensFromHomeAndSelectedDateReturnsToChart() {
        composeRule.onNodeWithTag("open_almanac")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("almanac_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("open_almanac_date_time_picker").performClick()
        composeRule.onNodeWithTag("almanac_date_time_picker_sheet").assertIsDisplayed()
            .assertHeightIsAtLeast(640.dp)
        composeRule.onNodeWithText("快速跳转日期与时间").assertIsDisplayed()
        composeRule.onNodeWithTag("fortune_hour_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("fortune_minute_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_almanac_date_time").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("almanac_date_time_picker_sheet")
                .fetchSemanticsNodes()
                .isEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("use_almanac_date_for_chart")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("folk_bone_weight_section")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("称骨算命").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("男命断语").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("女命断语").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("称骨 · 民俗断语").assertCountEquals(0)
        composeRule.onAllNodesWithText("农历年、月、日、时辰四项合计").assertCountEquals(0)
        composeRule.onNodeWithTag("folk_bone_weight_components")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("干支关系").assertCountEquals(0)
        composeRule.onAllNodesWithText("天干留意").assertCountEquals(0)
        composeRule.onAllNodesWithText("地支留意").assertCountEquals(0)
        composeRule.onAllNodesWithText("总重").assertCountEquals(0)
        composeRule.onNodeWithTag("almanac_eight_character_table")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("almanac_hour_pillar_rail")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("almanac_double_hour_子").assertIsDisplayed()
        composeRule.onNodeWithText("十神").assertIsDisplayed()
        composeRule.onNodeWithText("神煞").assertIsDisplayed()
        composeRule.onNodeWithTag("almanac_shensha_row")
            .assertHeightIsAtLeast(90.dp)
        composeRule.onAllNodesWithText("建除").assertCountEquals(0)
        composeRule.onAllNodesWithText("值神").assertCountEquals(0)
        composeRule.onAllNodesWithText("星宿").assertCountEquals(0)
        composeRule.onAllNodesWithText("胎神").assertCountEquals(0)
        composeRule.onNodeWithTag("use_almanac_date_for_chart")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("case_detail_screen")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("detail_tab_fortune").assertIsDisplayed()
        composeRule.onNodeWithText("即时排盘案例").assertIsDisplayed()
        composeRule.onNodeWithText("未保存").assertIsDisplayed()
    }
}
