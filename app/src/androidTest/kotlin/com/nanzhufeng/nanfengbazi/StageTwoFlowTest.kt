package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test

class StageTwoFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createSaveSearchAndOpenDetail() {
        val alias = "Stage2合成样例-${System.currentTimeMillis()}"
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeRule.onNodeWithTag("case_list_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("import_single_case_button").assertIsDisplayed()
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
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText("别名：$alias").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("别名：$alias").performClick()

        composeRule.onNodeWithTag("case_detail_screen").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("原始录入信息"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("原始录入信息").performScrollTo().assertIsDisplayed()
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

        composeRule.onNodeWithTag("export_single_case_button").performScrollTo().performClick()
        composeRule.onNodeWithText("导出未加密单命例？").assertIsDisplayed()
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
        composeRule.onNodeWithTag("restore_case_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("visibility_active").assertIsDisplayed()
        composeRule.onNodeWithText("别名：$editedAlias（副本）").assertIsDisplayed()

        composeRule.onNodeWithTag("import_single_case_button").performClick()
        val exportedFileName = "${editedAlias.take(48)}_南枫八字命例.json"
        val exportedFile = device.wait(
            Until.findObject(By.text(exportedFileName)),
            10_000,
        )
        checkNotNull(exportedFile) { "系统打开文档页面未找到刚导出的单命例 JSON" }
        exportedFile.click()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("single_case_preview"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("单命例导入预览").assertIsDisplayed()
        composeRule.onNodeWithText("稳定 ID 已存在", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
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
    }
}
