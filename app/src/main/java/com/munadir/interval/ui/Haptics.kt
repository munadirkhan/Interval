package com.munadir.interval.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Thin wrapper over the platform's haptic constants.
 *
 * Compose's own HapticFeedbackType exposes only a couple of effects; the View constants give
 * the distinct flip / grade / complete feel the app wants, and they degrade gracefully on
 * devices that do not implement a given effect.
 */
class Haptics(private val view: View) {

    /** Card flip. Light and quick. */
    fun flip() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

    /** Grading a card. Slightly heavier, so a grade feels committed. */
    fun grade() = view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

    /** End of a session. */
    fun success() = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
