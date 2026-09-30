package com.munadir.interval.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Clickable without a ripple, but not without feedback: the surface springs down under the
 * finger instead. See [pressable].
 *
 * Kept as the name every screen already calls so the whole app picked up the new behaviour in
 * one place rather than a hundred edits.
 */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = pressable(onClick = onClick)

/** Formats a millisecond timestamp as "March 2026", for profile "member since" lines. */
@Composable
fun rememberMonthYear(timestamp: Long): String {
    val formatter = remember { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()) }
    return remember(timestamp) { formatter.format(java.util.Date(timestamp)) }
}
