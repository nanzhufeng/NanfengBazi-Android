package com.nanzhufeng.nanfengbazi

import android.content.ClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExternalAnalysisBridgeFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun previewCopyRestoreAndBackfillExternalAnalysisOnApi35() {
        val suffix = System.currentTimeMillis()
        val alias = "VX11API35-$suffix"
        val name = "合成外部桥接验收-$suffix"
        val location = "合成外部桥接地区-$suffix"
        createSyntheticCase(alias, name, location)

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
        composeRule.onNodeWithTag("open_external_analysis_button")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("external_analysis_preview"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("external_analysis_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("external_analysis_boundary").assertIsDisplayed()
        composeRule.onNodeWithText("[已脱敏]", substring = true)
            .assertIsDisplayed()

        composeRule.onNodeWithTag("external_analysis_list")
            .performScrollToNode(hasTestTag("external_analysis_export_confirm"))
        composeRule.onNodeWithTag("external_analysis_export_confirm").performClick()
        composeRule.onNodeWithTag("copy_external_analysis_button")
            .performScrollTo()
            .performClick()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val copied = clipboard.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            .orEmpty()
        assertTrue(copied.contains("南枫八字 · 外部分析材料 v1"))
        assertTrue(copied.contains("App 不联网、不自动发送"))
        assertTrue(copied.contains("【四柱与基础盘】"))
        assertFalse(copied.contains(alias))
        assertFalse(copied.contains(name))
        assertFalse(copied.contains(location))

        composeRule.onNodeWithTag("external_analysis_list")
            .performScrollToNode(hasTestTag("external_analysis_provider"))
        composeRule.onNodeWithTag("external_analysis_provider")
            .performTextReplacement("合成外部服务")
        composeRule.onNodeWithTag("external_analysis_model")
            .performTextReplacement("合成模型 v1")
        composeRule.onNodeWithTag("external_analysis_result")
            .performTextReplacement("这是一段合成外部分析，只用于 API 35 验收。")

        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("external_analysis_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("external_analysis_list")
            .performScrollToNode(hasTestTag("external_analysis_provider"))
        composeRule.onNodeWithTag("external_analysis_provider")
            .assertTextContains("合成外部服务", substring = true)
        composeRule.onNodeWithTag("external_analysis_model")
            .assertTextContains("合成模型 v1", substring = true)
        composeRule.onNodeWithTag("external_analysis_result")
            .assertTextContains("只用于 API 35 验收", substring = true)

        composeRule.onNodeWithTag("external_analysis_list")
            .performScrollToNode(hasTestTag("external_analysis_end"))
        composeRule.onNodeWithTag("external_analysis_import_confirm")
            .performClick()
        composeRule.onNodeWithTag("external_analysis_import_confirm").assertIsOn()
        composeRule.onNodeWithTag("save_external_analysis_button")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("external_analysis_error"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("external_analysis_error").assertDoesNotExist()
        composeRule.onNodeWithTag("case_detail_screen").assertIsDisplayed()
        composeRule.onAllNodes(hasText("来源：合成外部服务", substring = true))
            .onFirst()
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodes(hasText("不是南枫八字本机算法真值", substring = true))
            .onFirst()
            .assertIsDisplayed()
    }

    private fun createSyntheticCase(alias: String, name: String, location: String) {
        composeRule.onNodeWithTag("nav_chart").performClick()
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextReplacement(alias)
        composeRule.onNodeWithTag("case_name").performTextReplacement(name)
        composeRule.onNodeWithTag("sex_woman").performClick()
        composeRule.onNodeWithTag("birth_year").performTextReplacement("1996")
        composeRule.onNodeWithTag("birth_month").performTextReplacement("8")
        composeRule.onNodeWithTag("birth_day").performTextReplacement("15")
        composeRule.onNodeWithTag("birth_hour").performTextReplacement("9")
        composeRule.onNodeWithTag("birth_minute").performTextReplacement("20")
        composeRule.onNodeWithTag("birth_second").performTextReplacement("0")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextReplacement(location)
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
    }
}
