package com.nanzhufeng.nanfengbazi

import android.content.ContentUris
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CaseImageSystemFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createCaseSaveFourSectionPngToGalleryAndOpenShareChooser() {
        val caseName = "$ALIAS-${System.currentTimeMillis()}"
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val device = UiDevice.getInstance(instrumentation)
        val galleryIdsBefore = exportedGalleryImageIds()

        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("case_alias").performTextReplacement(caseName)
        composeRule.onNodeWithTag("sex_man").performClick()
        composeRule.onNodeWithTag("save_case").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_list_screen"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                    .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("form_error"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (
            composeRule.onAllNodes(hasTestTag("duplicate_candidates"))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("confirm_duplicate_save").performClick()
            composeRule.waitUntil(timeoutMillis = 20_000) {
                composeRule.onAllNodes(hasTestTag("case_list_screen"))
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
        val formErrors = composeRule.onAllNodes(hasTestTag("form_error"))
            .fetchSemanticsNodes()
        check(formErrors.isEmpty()) {
            "合成命例保存失败：${formErrors.joinToString { it.config.toString() }}"
        }
        composeRule.onNodeWithTag("case_search").performTextReplacement(caseName)
        composeRule.onNodeWithContentDescription("打开命例：$caseName")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasTestTag("case_detail_screen"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("toggle_case_management").performClick()
        composeRule.onNodeWithTag("save_case_image_to_gallery").performClick()
        composeRule.onNodeWithTag("confirm_case_image_export").performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            exportedGalleryImageIds().any { it !in galleryIdsBefore } ||
                composeRule.onAllNodes(hasTestTag("case_image_error"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        val imageErrors = composeRule.onAllNodes(hasTestTag("case_image_error"))
            .fetchSemanticsNodes()
        check(imageErrors.isEmpty()) {
            "图库写入失败：${imageErrors.joinToString { it.config.toString() }}"
        }

        val createdIds = exportedGalleryImageIds() - galleryIdsBefore
        assertTrue("图库中没有新增命盘长图", createdIds.isNotEmpty())
        val createdUris = createdIds.map { id ->
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        }
        try {
            val signature = resolver.openInputStream(createdUris.first())!!.use { input ->
                ByteArray(8).also { bytes -> check(input.read(bytes) == bytes.size) }
            }
            assertArrayEquals(
                byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10),
                signature,
            )

            composeRule.onNodeWithTag("toggle_case_management").performClick()
            composeRule.onNodeWithTag("share_case_image_button").performClick()
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
        } finally {
            createdUris.forEach { uri -> resolver.delete(uri, null, null) }
        }
    }

    private fun exportedGalleryImageIds(): Set<Long> {
        val resolver = InstrumentationRegistry.getInstrumentation()
            .targetContext.contentResolver
        return resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
            arrayOf("%_南枫八字命盘.png"),
            null,
        )?.use { cursor ->
            buildSet {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) add(cursor.getLong(idColumn))
            }
        }.orEmpty()
    }

    private companion object {
        const val ALIAS = "VX07API35"
    }
}
