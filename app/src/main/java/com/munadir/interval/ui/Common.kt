package com.munadir.interval.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Clickable without the ripple.
 *
 * Used on surfaces that already respond visually -- a card that scales, a send button that
 * changes colour -- where a second ripple on top reads as noise.
 */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

/** Formats a millisecond timestamp as "March 2026", for profile "member since" lines. */
@Composable
fun rememberMonthYear(timestamp: Long): String {
    val formatter = remember { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()) }
    return remember(timestamp) { formatter.format(java.util.Date(timestamp)) }
}
