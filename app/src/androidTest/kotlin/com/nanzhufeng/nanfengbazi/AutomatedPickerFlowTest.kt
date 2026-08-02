package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class AutomatedPickerFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun birthTimeAndBirthplaceUsePrefilledWheelSheets() {
        composeRule.onNodeWithTag("create_case_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("open_birth_datetime_picker")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("birth_datetime_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_year_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("birth_minute_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_birth_datetime").performClick()

        composeRule.onNodeWithTag("open_birthplace_picker")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("birthplace_picker_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("birthplace_region_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("birthplace_city_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("birthplace_district_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_birthplace").performClick()
    }

    @Test
    fun fourPillarsAndRangeUseSelectorsInsteadOfFormattedTyping() {
        composeRule.onNodeWithTag("open_four_pillars_lookup")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("four_pillars_wheel_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("lookup_year_pillar_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_four_pillars_wheels").performClick()
        composeRule.onNodeWithTag("four_pillars_lookup_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("open_lookup_year_range_picker")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("lookup_year_range_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("lookup_start_year_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("lookup_end_year_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_lookup_year_range").performClick()

        composeRule.onNodeWithTag("open_lookup_time_zone_picker")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("lookup_time_zone_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("lookup_time_zone_wheel").assertIsDisplayed()
    }
}
