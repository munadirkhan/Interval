package com.munadir.interval.ui

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * The thinking orb.
 *
 * Shader orbs on the web get their softness from fragment maths. The equivalent here is a
 * stack of coloured lobes drawn on a Canvas and then run through a real Gaussian blur, which
 * dissolves the circle edges into each other and leaves something that genuinely churns rather
 * than obviously being three overlapping circles.
 *
 * `Modifier.blur` needs API 31. Below that it is a no-op, so a fallback layer draws the lobes
 * with much softer gradient falloff and lower alpha — less liquid, still not ugly.
 */
@Composable
fun AiOrb(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    active: Boolean = true
) {
    val canBlur = Build.VERSION.SDK_INT >= 31
    val transition = rememberInfiniteTransition(label = "orb")

    // Three orbits at co-prime-ish periods, so the lobes never settle into a visible loop.
    val speed = if (active) 1f else 0.32f
    val full = (2 * Math.PI).toFloat()

    val a by transition.animateFloat(
        initialValue = 0f,
        targetValue = full,
        animationSpec = infiniteRepeatable(
            animation = tween((4200 / speed).toInt(), easing = LinearEasing)
        ),
        label = "a"
    )
    val b by transition.animateFloat(
        initialValue = 0f,
        targetValue = full,
        animationSpec = infiniteRepeatable(
            animation = tween((6100 / speed).toInt(), easing = LinearEasing)
        ),
        label = "b"
    )
    val c by transition.animateFloat(
        initialValue = 0f,
        targetValue = full,
        animationSpec = infiniteRepeatable(
            animation = tween((8700 / speed).toInt(), easing = LinearEasing)
        ),
        label = "c"
    )

    val breathe by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween((2600 / speed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val accent = MaterialTheme.colorScheme.primary
    val violet = Color(0xFF9C6BFF)
    val cyan = Color(0xFF4CC2F0)
    val deep = Color(0xFF2B1D66)

    // Each lobe: colour, its own phase, orbit radius, size. Prime-ish speeds keep them from
    // ever settling into a visible repeating pattern.
    val lobes = remember(accent) {
        listOf(
            Lobe(accent, 0f, 0.30f, 0.62f),
            Lobe(violet, 2.0f, 0.36f, 0.58f),
            Lobe(cyan, 4.1f, 0.28f, 0.50f),
            Lobe(deep, 1.1f, 0.40f, 0.66f),
            Lobe(accent, 3.4f, 0.20f, 0.44f),
            Lobe(violet, 5.2f, 0.34f, 0.40f)
        )
    }

    Box(modifier.size(size), contentAlignment = Alignment.Center) {

        // ---- soft body -------------------------------------------------
        Canvas(
            Modifier
                .size(size)
                .then(if (canBlur) Modifier.blur(size * 0.13f) else Modifier)
        ) {
            val radius = this.size.minDimension / 2f
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            val body = radius * 0.72f * breathe

            // Base pool, so the lobes have something to sit in rather than floating on black.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(deep.copy(alpha = 0.9f), deep.copy(alpha = 0.15f), Color.Transparent),
                    center = centre,
                    radius = body
                ),
                radius = body,
                center = centre
            )

            lobes.forEach { lobe ->
                val phase = lobe.phase + when (lobe.color) {
                    accent -> a
                    violet -> b
                    else -> c
                }
                val position = Offset(
                    centre.x + cos(phase) * body * lobe.orbit,
                    centre.y + sin(phase) * body * lobe.orbit
                )
                val r = body * lobe.scale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            lobe.color.copy(alpha = if (canBlur) 0.85f else 0.45f),
                            lobe.color.copy(alpha = 0f)
                        ),
                        center = position,
                        radius = r
                    ),
                    radius = r,
                    center = position,
                    blendMode = BlendMode.Plus
                )
            }
        }

        // ---- crisp overlay ---------------------------------------------
        // Kept unblurred so the orb still has an edge and a highlight, which is what stops a
        // blurred blob from reading as an out-of-focus mistake.
        Canvas(Modifier.size(size)) {
            val radius = this.size.minDimension / 2f
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            val body = radius * 0.72f * breathe

            drawHalo(centre, radius, accent)
            drawRim(centre, body, a, accent)
            drawSpecular(centre, body, b)
        }
    }
}

private data class Lobe(
    val color: Color,
    val phase: Float,
    val orbit: Float,
    val scale: Float
)

/** Ambient bloom beyond the body, so the orb sits in light rather than on a flat ground. */
private fun DrawScope.drawHalo(centre: Offset, radius: Float, accent: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, accent.copy(alpha = 0.10f), Color.Transparent),
            center = centre,
            radius = radius
        ),
        radius = radius,
        center = centre
    )
}

/** A sweep-gradient rim, brightest where the lead lobe is, that rotates with it. */
private fun DrawScope.drawRim(centre: Offset, body: Float, phase: Float, accent: Color) {
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                accent.copy(alpha = 0.05f),
                accent.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.35f),
                accent.copy(alpha = 0.18f),
                accent.copy(alpha = 0.05f)
            ),
            center = centre
        ),
        radius = body,
        center = centre,
        style = Stroke(width = body * 0.045f)
    )
    // Second, tighter pass gives the rim a hot inner edge.
    drawCircle(
        color = Color.White.copy(alpha = 0.10f),
        radius = body * 0.97f,
        center = centre,
        style = Stroke(width = body * 0.02f)
    )
}

/** Off-centre highlight. The single cheapest trick for making a flat disc look spherical. */
private fun DrawScope.drawSpecular(centre: Offset, body: Float, phase: Float) {
    val position = Offset(
        centre.x + cos(phase * 0.4f - 0.9f) * body * 0.34f,
        centre.y + sin(phase * 0.4f - 0.9f) * body * 0.34f - body * 0.16f
    )
    val r = body * 0.42f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
            center = position,
            radius = r
        ),
        radius = r,
        center = position
    )
}

/** Compact three-dot indicator, for inline use where a full orb would be too loud. */
@Composable
fun ThinkingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(animation = tween(1100, easing = LinearEasing)),
        label = "phase"
    )
    val accent = MaterialTheme.colorScheme.primary

    Canvas(modifier.size(width = 34.dp, height = 10.dp)) {
        val r = size.height / 2.6f
        repeat(3) { i ->
            val local = ((phase - i) % 3f + 3f) % 3f
            val lift = if (local < 1f) 1f - local else 0f
            drawCircle(
                color = accent.copy(alpha = 0.35f + 0.65f * lift),
                radius = r * (0.8f + 0.35f * lift),
                center = Offset(r + i * (size.width - 2 * r) / 2f, size.height / 2f - lift * r * 0.7f)
            )
        }
    }
}
