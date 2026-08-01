package com.nanzhufeng.nanfengbazi

import android.content.ClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CaseObjectiveSummaryFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createCaseCopyObjectiveSummaryAndSurviveActivityRecreation() {
        val alias = "VX10API35-${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("nav_chart").performClick()
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextReplacement(alias)
        composeRule.onNodeWithTag("case_name").performTextReplacement("合成摘要验收")
        composeRule.onNodeWithTag("sex_woman").performClick()
        composeRule.onNodeWithTag("birth_year").performTextReplacement("1996")
        composeRule.onNodeWithTag("birth_month").performTextReplacement("8")
        composeRule.onNodeWithTag("birth_day").performTextReplacement("15")
        composeRule.onNodeWithTag("birth_hour").performTextReplacement("9")
        composeRule.onNodeWithTag("birth_minute").performTextReplacement("20")
        composeRule.onNodeWithTag("birth_second").performTextReplacement("0")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextReplacement("合成摘要地区")
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
        composeRule.onNodeWithTag("case_search").performTextReplacement(alias)
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText("别名：$alias"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("别名：$alias")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("toggle_case_management").performClick()
        composeRule.onNodeWithTag("open_objective_summary_button")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("objective_summary_section_birth_facts"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("objective_summary_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("objective_summary_notice").assertIsDisplayed()
        composeRule.onNodeWithTag("copy_objective_summary_button").performClick()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val copied = clipboard.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            .orEmpty()
        assertTrue(copied.contains("南枫八字 · 客观命盘摘要 v1"))
        assertTrue(copied.contains("命例：合成摘要验收"))
        assertTrue(copied.contains("【四柱与基础盘】"))
        assertTrue(copied.contains("来源边界："))
        assertFalse(copied.contains("合婚"))
        assertFalse(copied.contains("命好"))
        assertFalse(copied.contains("命坏"))
        composeRule.onNodeWithTag("objective_summary_list")
            .performScrollToNode(hasTestTag("objective_summary_section_calculation_evidence"))
        composeRule.onNodeWithTag("objective_summary_section_calculation_evidence")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("objective_summary_list")
            .performScrollToNode(hasTestTag("objective_summary_section_formal_record_index"))
        composeRule.onNodeWithTag("objective_summary_section_formal_record_index")
            .assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("objective_summary_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("objective_summary_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("objective_summary_list")
            .performScrollToNode(hasTestTag("objective_summary_section_birth_facts"))
        composeRule.onNodeWithTag("objective_summary_section_birth_facts")
            .assertIsDisplayed()
    }
}
