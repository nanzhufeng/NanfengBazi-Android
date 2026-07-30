package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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
        composeRule.onNodeWithText("别名：$alias").assertIsDisplayed().performClick()

        composeRule.onNodeWithTag("case_detail_screen").assertIsDisplayed()
        composeRule.onNodeWithText("原始录入信息").assertIsDisplayed()
        composeRule.onNodeWithText("计算结果").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Tyme4j").performScrollTo().assertIsDisplayed()

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
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(editedAlias).assertIsDisplayed()
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

        composeRule.onNodeWithTag("add_record_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("record_editor_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("record_content").performTextInput("Stage3 合成笔记")
        composeRule.onNodeWithTag("save_record").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Stage3 合成笔记").performScrollTo().assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("记录已保存。"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("记录已保存。")).fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithTag("add_event_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("event_editor_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("event_year").performTextInput("2024")
        composeRule.onNodeWithTag("event_content").performTextInput("Stage3 合成关键事件")
        composeRule.onNodeWithTag("save_event").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Stage3 合成关键事件")
            .performScrollTo()
            .assertIsDisplayed()

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
        composeRule.onNodeWithTag("restore_case_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("visibility_active").assertIsDisplayed()
        composeRule.onNodeWithText("别名：$editedAlias（副本）").assertIsDisplayed()
    }
}
