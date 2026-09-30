package com.munadir.interval.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * One motion vocabulary for the whole app.
 *
 * Everything is a spring rather than a duration. A tween always arrives at exactly the same
 * moment no matter how far it travelled, which is what makes interfaces feel mechanical; a
 * spring carries momentum, so a small movement settles fast and a large one takes its time.
 */
object Motion {

    /** Default for most state changes: quick, no visible overshoot. */
    val snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Larger travel — sheets, cards, anything crossing a lot of screen. */
    val gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** Rewards and confirmations, where a little overshoot reads as delight. */
    val bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * Tap handling with physical feedback: the surface dips under your finger and springs back.
 *
 * Replaces ripple on large surfaces. A ripple tells you *where* you touched, which you already
 * know; a scale tells you the whole element responded, which is what makes a card feel like an
 * object rather than a rectangle with a listener.
 */
fun Modifier.pressable(
    scaleTo: Float = 0.97f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleTo else 1f,
        animationSpec = Motion.snappy,
        label = "pressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    pressed = true
                    // Hold the dip until the finger lifts or the gesture is cancelled.
                    tryAwaitRelease()
                    pressed = false
                },
                onTap = { onClick() }
            )
        }
}

/** Press feedback for elements that keep their own ripple, e.g. Material buttons. */
@Composable
fun rememberPressScale(
    interactionSource: MutableInteractionSource,
    scaleTo: Float = 0.97f
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleTo else 1f,
        animationSpec = Motion.snappy,
        label = "pressScale"
    )
    return scale
}
