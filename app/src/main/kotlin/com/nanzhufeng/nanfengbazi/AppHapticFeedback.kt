package com.nanzhufeng.nanfengbazi

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

internal enum class AppHapticEvent {
    KEY_PRESS,
    SELECTION,
    SNAP,
    CONFIRM,
    SUCCESS,
    REJECT,
}

@Stable
internal class AppHapticFeedback(
    private val view: View,
) {
    fun perform(event: AppHapticEvent) {
        val constant = when (event) {
            AppHapticEvent.KEY_PRESS -> HapticFeedbackConstants.KEYBOARD_TAP
            AppHapticEvent.SELECTION,
            AppHapticEvent.SNAP,
            -> HapticFeedbackConstants.CLOCK_TICK
            AppHapticEvent.CONFIRM -> HapticFeedbackConstants.CONFIRM
            AppHapticEvent.SUCCESS -> HapticFeedbackConstants.CONFIRM
            AppHapticEvent.REJECT -> HapticFeedbackConstants.REJECT
        }
        view.performHapticFeedback(constant)
    }
}

@Composable
internal fun rememberAppHapticFeedback(): AppHapticFeedback {
    val view = LocalView.current
    return remember(view) { AppHapticFeedback(view) }
}
