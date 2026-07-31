package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CaseImageSystemFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createCaseExportReadablePngAndOpenShareChooser() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeRule.onNodeWithTag("nav_chart").performClick()
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextReplacement(ALIAS)
        composeRule.onNodeWithTag("case_name").performTextReplacement("合成验收")
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("birth_year").performTextReplacement("2000")
        composeRule.onNodeWithTag("birth_month").performTextReplacement("2")
        composeRule.onNodeWithTag("birth_day").performTextReplacement("29")
        composeRule.onNodeWithTag("birth_hour").performTextReplacement("10")
        composeRule.onNodeWithTag("birth_minute").performTextReplacement("30")
        composeRule.onNodeWithTag("birth_second").performTextReplacement("0")
        composeRule.onNodeWithTag("birth_location")
            .performScrollTo()
            .performTextReplacement("合成验收地区")
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("form_error"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        val formErrors = composeRule.onAllNodes(hasTestTag("form_error"))
            .fetchSemanticsNodes()
        check(formErrors.isEmpty()) {
            "合成命例保存失败：${formErrors.joinToString { it.config.toString() }}"
        }
        composeRule.onNodeWithText("别名：$ALIAS")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("export_case_image_button")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("confirm_case_image_export").performClick()
        val saveButton = device.wait(
            Until.findObject(By.text(Pattern.compile("(?i)save|保存"))),
            15_000,
        )
        checkNotNull(saveButton) { "Android 系统创建文档页面未出现保存按钮" }
        saveButton.click()
        device.wait(
            Until.gone(By.pkg("com.google.android.documentsui")),
            15_000,
        )
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText("命盘长图已保存到所选系统位置。"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("case_image_error"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        val imageErrors = composeRule.onAllNodes(hasTestTag("case_image_error"))
            .fetchSemanticsNodes()
        check(imageErrors.isEmpty()) {
            "系统文件写入失败：${imageErrors.joinToString { it.config.toString() }}"
        }

        composeRule.onNodeWithTag("share_case_image_button")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("confirm_case_image_share").performClick()
        val chooserVisible = device.wait(
            Until.hasObject(By.textContains("分享命盘长图")),
            15_000,
        ) || device.hasObject(By.res("android:id/resolver_list"))
        assertTrue("Android 系统分享面板未打开", chooserVisible)
        device.pressBack()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("case_detail_screen").assertIsDisplayed()
    }

    private companion object {
        const val ALIAS = "VX07API35"
    }
}
