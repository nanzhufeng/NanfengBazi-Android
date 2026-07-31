package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test

class FeedbackThemeCandidateFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createFeedbackReviewThemesAdoptRejectAndRestore() {
        val alias = "VX05API35-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("nav_chart").performClick()
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextReplacement(alias)
        composeRule.onNodeWithTag("case_name").performTextReplacement("合成反馈主题验收")
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("birth_year").performTextReplacement("1994")
        composeRule.onNodeWithTag("birth_month").performTextReplacement("3")
        composeRule.onNodeWithTag("birth_day").performTextReplacement("8")
        composeRule.onNodeWithTag("birth_hour").performTextReplacement("10")
        composeRule.onNodeWithTag("birth_minute").performTextReplacement("10")
        composeRule.onNodeWithTag("birth_second").performTextReplacement("0")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextReplacement("合成反馈主题地区")
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
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
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$alias").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("detail_tab_records").performClick()
        composeRule.onNodeWithTag("add_record_button").performScrollTo().performClick()
        composeRule.onNodeWithText("命主反馈").performClick()
        composeRule.onNodeWithTag("record_content").performTextReplacement(
            "工作有变化。健康需要复查",
        )
        composeRule.onNodeWithTag("save_record").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("open_feedback_theme_candidates_button")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("feedback_theme_candidates_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("feedback_theme_candidates_notice").assertIsDisplayed()
        composeRule.onNodeWithText(
            "候选只是标签建议",
            substring = true,
        ).assertIsDisplayed()

        composeRule.onAllNodesWithTag("reject_feedback_theme_candidate")[1]
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("feedback_theme_candidates_list")
            .performScrollToNode(hasStateDescription("已拒绝"))
        composeRule.onNodeWithTag("feedback_theme_status_REJECTED").assertIsDisplayed()

        composeRule.onAllNodesWithTag("feedback_theme_tag_input")[0]
            .performScrollTo()
            .performTextReplacement("事业复盘")
        composeRule.onAllNodesWithTag("adopt_feedback_theme_candidate")[0]
            .performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText("已采用为正式标签"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("feedback_theme_candidates_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("feedback_theme_candidates_list")
            .performScrollToNode(hasStateDescription("已采用为正式标签"))
        composeRule.onNodeWithTag("feedback_theme_status_ADOPTED").assertIsDisplayed()
        composeRule.onNodeWithTag("feedback_theme_candidates_list")
            .performScrollToNode(hasStateDescription("已拒绝"))
        composeRule.onNodeWithTag("feedback_theme_status_REJECTED").assertIsDisplayed()
        composeRule.onNodeWithText("返回").performClick()
        composeRule.onAllNodes(hasText("工作有变化。健康需要复查"))[0]
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("detail_tab_basic_info").performClick()
        composeRule.onAllNodes(hasText("事业复盘"))[0]
            .performScrollTo()
            .assertIsDisplayed()
    }
}
