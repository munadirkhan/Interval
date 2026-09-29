package com.munadir.interval.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class Tab(val label: String) {
    TODAY("Today"),
    COACH("Coach"),
    STATS("Stats"),
    YOU("You")
}

/**
 * Four destinations, hand-drawn icons.
 *
 * The icons are Canvas paths rather than a Material icon set: the whole app is one external
 * dependency and four glyphs are not worth breaking that for. Drawing them also means they
 * inherit the accent colour exactly.
 */
@Composable
fun BottomBar(
    selected: Tab,
    dueCount: Int,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tab.entries.forEach { tab ->
            TabButton(
                tab = tab,
                selected = tab == selected,
                badge = if (tab == Tab.TODAY && dueCount > 0) dueCount else null,
                onClick = { onSelect(tab) }
            )
        }
    }
}

@Composable
private fun TabButton(
    tab: Tab,
    selected: Boolean,
    badge: Int?,
    onClick: () -> Unit
) {
    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    val tint by animateColorAsState(if (selected) active else idle, tween(200), label = "tint")
    val lift by animateFloatAsState(if (selected) 1.08f else 1f, tween(200), label = "lift")

    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Canvas(
                Modifier
                    .size(24.dp)
                    .scale(lift)
            ) {
                when (tab) {
                    Tab.TODAY -> drawCardIcon(tint)
                    Tab.COACH -> drawSparkIcon(tint)
                    Tab.STATS -> drawBarsIcon(tint)
                    Tab.YOU -> drawPersonIcon(tint)
                }
            }
            if (badge != null) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

// ---------------------------------------------------------------------- icons

private fun DrawScope.drawCardIcon(color: Color) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.08f, h * 0.18f),
        size = Size(w * 0.84f, h * 0.64f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f),
        style = Stroke(width = w * 0.09f)
    )
    drawLine(
        color = color,
        start = Offset(w * 0.28f, h * 0.5f),
        end = Offset(w * 0.72f, h * 0.5f),
        strokeWidth = w * 0.09f
    )
}

private fun DrawScope.drawSparkIcon(color: Color) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.5f
    val long = w * 0.42f
    val short = w * 0.16f
    // A four-point star, drawn as two tapered crossing strokes.
    drawLine(color, Offset(cx, cy - long), Offset(cx, cy + long), strokeWidth = w * 0.13f)
    drawLine(color, Offset(cx - long, cy), Offset(cx + long, cy), strokeWidth = w * 0.13f)
    drawCircle(color, radius = short, center = Offset(cx, cy))
}

private fun DrawScope.drawBarsIcon(color: Color) {
    val w = size.width
    val h = size.height
    val barWidth = w * 0.16f
    listOf(0.45f, 0.72f, 0.95f).forEachIndexed { i, fraction ->
        val x = w * (0.18f + i * 0.28f)
        drawRoundRect(
            color = color,
            topLeft = Offset(x, h * (0.92f - fraction * 0.72f)),
            size = Size(barWidth, h * fraction * 0.72f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
        )
    }
}

private fun DrawScope.drawPersonIcon(color: Color) {
    val w = size.width
    val h = size.height
    drawCircle(
        color = color,
        radius = w * 0.17f,
        center = Offset(w * 0.5f, h * 0.32f),
        style = Stroke(width = w * 0.09f)
    )
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(w * 0.18f, h * 0.52f),
        size = Size(w * 0.64f, h * 0.56f),
        style = Stroke(width = w * 0.09f)
    )
}
