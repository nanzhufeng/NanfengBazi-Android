package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import org.junit.Rule
import org.junit.Test

class CaseListSwipeAndEditFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun swipeActionsOpenUnifiedEditorPinAndDelete() {
        val suffix = System.currentTimeMillis().toString()
        val originalName = "$ORIGINAL_NAME-$suffix"
        createCase(originalName)

        swipeCase(originalName)
        composeRule.onNodeWithContentDescription("左滑操作：编辑").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("左滑操作：置顶").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("左滑操作：删除").assertIsDisplayed()
        composeRule.onAllNodesWithText("合盘").assertCountEquals(0)

        composeRule.onNodeWithContentDescription("左滑操作：编辑").performClick()
        composeRule.onNodeWithTag("edit_case_screen").assertIsDisplayed()
        composeRule.onAllNodesWithText("命例别名").assertCountEquals(0)
        composeRule.onAllNodesWithText("时间精度").assertCountEquals(0)
        composeRule.onAllNodesWithText("子时换日规则").assertCountEquals(0)
        composeRule.onAllNodesWithText("时间来源").assertCountEquals(0)
        composeRule.onNodeWithTag("create_case_copy").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回").performClick()
        waitForList()
        filterCases(originalName)

        swipeCase(originalName)
        composeRule.onNodeWithContentDescription("左滑操作：置顶").performClick()
        filterCases("")
        filterCases(originalName)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("打开命例：$originalName"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        swipeCase(originalName)
        composeRule.onNodeWithContentDescription("左滑操作：取消置顶").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("左滑操作：删除").performClick()
        composeRule.onNodeWithTag("confirm_swipe_case_delete").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(originalName).fetchSemanticsNodes().size <= 1
        }
        composeRule.onNodeWithContentDescription("打开命例：$originalName").assertDoesNotExist()
    }

    private fun createCase(name: String) {
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextReplacement(name)
        composeRule.onNodeWithTag("sex_man").performClick()
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
            composeRule.onNodeWithTag("confirm_duplicate_save").performClick()
            waitForList()
        }
        filterCases(name)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("打开命例：$name"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("打开命例：$name").assertIsDisplayed()
    }

    private fun filterCases(query: String) {
        composeRule.onNodeWithTag("case_search").performTextReplacement(query)
        composeRule.waitForIdle()
    }

    private fun swipeCase(name: String) {
        composeRule.onNodeWithContentDescription("打开命例：$name")
            .performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
    }

    private fun waitForList() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val ORIGINAL_NAME = "左滑编辑合成原例"
    }
}
