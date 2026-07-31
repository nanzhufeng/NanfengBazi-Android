package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasStateDescription
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

class MasterCommentaryCandidateFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createCommentaryReviewCandidatesAdoptRejectAndRestore() {
        val alias = "VX04API35-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("nav_chart").performClick()
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextReplacement(alias)
        composeRule.onNodeWithTag("case_name").performTextReplacement("合成观点候选验收")
        composeRule.onNodeWithTag("sex_woman").performClick()
        composeRule.onNodeWithTag("birth_year").performTextReplacement("1997")
        composeRule.onNodeWithTag("birth_month").performTextReplacement("7")
        composeRule.onNodeWithTag("birth_day").performTextReplacement("17")
        composeRule.onNodeWithTag("birth_hour").performTextReplacement("13")
        composeRule.onNodeWithTag("birth_minute").performTextReplacement("24")
        composeRule.onNodeWithTag("birth_second").performTextReplacement("0")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextReplacement("合成观点候选地区")
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
        composeRule.onNodeWithText("师傅点评").performClick()
        composeRule.onNodeWithTag("record_content").performTextReplacement(
            "事业需要用户核对。财运也需要用户核对",
        )
        composeRule.onNodeWithTag("save_record").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("open_commentary_candidates_button")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("master_commentary_candidates_screen")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("master_commentary_candidates_notice")
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "候选只用于人工整理",
            substring = true,
        ).assertIsDisplayed()

        composeRule.onAllNodesWithTag("reject_commentary_candidate")[1]
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("master_commentary_candidates_list")
            .performScrollToNode(hasStateDescription("已拒绝"))
        composeRule.onNodeWithTag("commentary_candidate_status_REJECTED")
            .assertIsDisplayed()

        composeRule.onAllNodesWithTag("commentary_candidate_content")[0]
            .performScrollTo()
            .performTextReplacement("编辑后的正式事业分析")
        composeRule.onAllNodesWithTag("commentary_category_CAREER")[0]
            .performClick()
        composeRule.onAllNodesWithTag("adopt_commentary_candidate")[0]
            .performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText("已采用为正式分析"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("master_commentary_candidates_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("master_commentary_candidates_list")
            .performScrollToNode(hasStateDescription("已采用为正式分析"))
        composeRule.onNodeWithTag("commentary_candidate_status_ADOPTED")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("master_commentary_candidates_list")
            .performScrollToNode(hasStateDescription("已拒绝"))
        composeRule.onNodeWithTag("commentary_candidate_status_REJECTED")
            .assertIsDisplayed()
        composeRule.onNodeWithText("返回").performClick()
        composeRule.onAllNodes(
            hasText("事业需要用户核对。财运也需要用户核对"),
        )[0]
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodes(hasText("编辑后的正式事业分析"))[0]
            .performScrollTo()
            .assertIsDisplayed()
    }
}
