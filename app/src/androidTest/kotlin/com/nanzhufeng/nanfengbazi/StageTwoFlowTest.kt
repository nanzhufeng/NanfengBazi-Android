package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class StageTwoFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createSaveSearchAndOpenDetail() {
        val alias = "Stage2合成样例-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("new_case_button").performClick()

        composeRule.onNodeWithTag("case_alias").performTextInput(alias)
        composeRule.onNodeWithTag("case_name").performTextInput("自动化甲")
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("birth_year").performTextInput("2000")
        composeRule.onNodeWithTag("birth_month").performTextInput("2")
        composeRule.onNodeWithTag("birth_day").performTextInput("29")
        composeRule.onNodeWithTag("birth_hour").performTextInput("10")
        composeRule.onNodeWithTag("birth_minute").performTextInput("30")
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasText("别名：$alias"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("case_search").performTextInput(alias)
        composeRule.onNodeWithText("别名：$alias").assertIsDisplayed().performClick()

        composeRule.onNodeWithTag("case_detail_screen").assertIsDisplayed()
        composeRule.onNodeWithText("原始录入信息").assertIsDisplayed()
        composeRule.onNodeWithText("计算结果").assertIsDisplayed()
        composeRule.onNodeWithText("Tyme4j").assertIsDisplayed()
    }
}
