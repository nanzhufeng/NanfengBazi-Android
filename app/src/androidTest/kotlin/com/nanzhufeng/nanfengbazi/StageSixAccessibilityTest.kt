package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StageSixAccessibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun primaryAndCaseWorkflowPagesExposeNamedTouchTargets() {
        auditPage("create_case_screen")
        composeRule.onNodeWithTag("nav_records").performClick()
        auditPage("case_list_screen")
        composeRule.onNodeWithTag("nav_settings").performClick()
        auditPage("settings_home_screen")
        composeRule.onNodeWithTag("settings_restore_backup").performScrollTo()
        auditPage("settings_home_screen")
        composeRule.onNodeWithTag("nav_chart").performClick()
        auditPage("create_case_screen")

        val alias = "Stage6无障碍-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("case_alias").performTextInput(alias)
        composeRule.onNodeWithTag("sex_man")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("birth_year").performTextInput("1992")
        composeRule.onNodeWithTag("birth_month").performTextInput("8")
        composeRule.onNodeWithTag("birth_day").performTextInput("24")
        composeRule.onNodeWithTag("birth_hour").performTextInput("12")
        composeRule.onNodeWithTag("birth_minute").performTextInput("0")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextInput("江苏省宿迁市泗阳县")
        composeRule.onNodeWithTag("save_case").performScrollTo()
        auditPage("create_case_screen")
        composeRule.onNodeWithTag("save_case").performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                    .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("form_error"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        val formErrors = composeRule.onAllNodes(hasTestTag("form_error"))
            .fetchSemanticsNodes()
        val formErrorText = composeRule.onAllNodes(
            hasAnyAncestor(hasTestTag("form_error")),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().flatMap {
            it.config.getOrNull(SemanticsProperties.Text).orEmpty()
        }.joinToString { it.text }
        val hasDuplicateCandidates =
            composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                .fetchSemanticsNodes().isNotEmpty()
        assertTrue(
            "命例表单保存失败：$formErrorText",
            formErrors.isEmpty() || hasDuplicateCandidates,
        )
        if (hasDuplicateCandidates) {
            composeRule.onNodeWithTag("confirm_duplicate_save")
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.OnClick)
        }
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("case_search").performTextInput(alias)
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText("别名：$alias"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$alias").performClick()
        auditPage("case_detail_screen")

        listOf(
            "detail_tab_basic_chart",
            "detail_tab_fortune",
            "detail_tab_records",
            "detail_tab_basic_info",
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).performClick()
            auditPage("case_detail_screen")
        }

        composeRule.onNodeWithTag("toggle_case_management").performClick()
        composeRule.onNodeWithTag("edit_metadata_button").performClick()
        auditPage("metadata_editor_screen")
        composeRule.onNodeWithTag("save_metadata").performScrollTo()
        auditPage("metadata_editor_screen")
        composeRule.onNodeWithText("返回").performClick()

        composeRule.onNodeWithTag("edit_case_button").performScrollTo().performClick()
        auditPage("edit_case_screen")
        composeRule.onNodeWithTag("save_case").performScrollTo()
        auditPage("edit_case_screen")
        composeRule.onNodeWithText("返回").performClick()

        composeRule.onNodeWithTag("detail_tab_records").performClick()
        composeRule.onNodeWithTag("owner_feedback_input").performScrollTo()
        auditPage("case_detail_screen")
        composeRule.onNodeWithTag("add_event_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("notes_time_picker").assertIsDisplayed()
        composeRule.onNodeWithText("确定").performClick()
        composeRule.onNodeWithTag("save_case_notes_button").performScrollTo()
        auditPage("case_detail_screen")
    }

    private fun auditPage(screenTag: String) {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(screenTag).assertIsDisplayed()
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val actions = composeRule.onAllNodes(
            hasClickAction(),
            useUnmergedTree = false,
        ).fetchSemanticsNodes()
        assertTrue("$screenTag 至少应有一个可操作节点", actions.isNotEmpty())
        val issues = mutableListOf<String>()
        actions.forEach { node ->
            val label = buildList {
                node.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?.filter(String::isNotBlank)
                    ?.let(::addAll)
                node.config.getOrNull(SemanticsProperties.Text)
                    ?.map { it.text }
                    ?.filter(String::isNotBlank)
                    ?.let(::addAll)
                node.config.getOrNull(SemanticsProperties.EditableText)
                    ?.text
                    ?.takeIf(String::isNotBlank)
                    ?.let(::add)
                node.config.getOrNull(SemanticsProperties.StateDescription)
                    ?.takeIf(String::isNotBlank)
                    ?.let(::add)
            }.joinToString()
            val diagnostic = node.config.getOrNull(SemanticsProperties.TestTag)
                ?: node.config.toString()
            if (label.isBlank()) {
                issues += "缺少 TalkBack 名称：$diagnostic"
            }
            val widthDp = node.size.width / density
            val heightDp = node.size.height / density
            if (
                widthDp < MIN_TOUCH_DP - ROUNDING_TOLERANCE_DP ||
                heightDp < MIN_TOUCH_DP - ROUNDING_TOLERANCE_DP
            ) {
                issues += "触控目标不足 48dp：" +
                    "$diagnostic (${widthDp}x${heightDp}dp)"
            }
            if (node.config.getOrNull(SemanticsActions.OnClick) == null) {
                issues += "缺少点击语义：$diagnostic"
            }
        }
        assertTrue(
            "$screenTag 无障碍审计失败：\n${issues.joinToString("\n")}",
            issues.isEmpty(),
        )
    }

    private companion object {
        const val MIN_TOUCH_DP = 48f
        const val ROUNDING_TOLERANCE_DP = 0.5f
    }
}
