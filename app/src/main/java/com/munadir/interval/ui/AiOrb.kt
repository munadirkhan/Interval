package com.munadir.interval.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * The thinking orb: layered radial gradients orbiting a common centre, behind a soft rim.
 *
 * This is the Compose answer to the WebGL/shader orbs you see on the web. There is no GLSL and
 * no texture -- three translucent radial gradients drifting out of phase read as the same slow
 * organic churn, and it costs one Canvas and no dependency.
 *
 * Colours lean on the theme accent so the orb belongs to the app rather than looking like a
 * borrowed asset.
 */
@Composable
fun AiOrb(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    active: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "orb")

    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (active) 5200 else 14000, easing = LinearEasing)
        ),
        label = "spin"
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (active) 3400 else 11000, easing = LinearEasing)
        ),
        label = "drift"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (active) 1500 else 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val accent = MaterialTheme.colorScheme.primary
    val warm = MaterialTheme.colorScheme.secondary
    val cool = Color(0xFF4C7DF0)

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val radius = this.size.minDimension / 2f
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            val orbRadius = radius * 0.78f * pulse

            // Outer halo.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                    center = centre,
                    radius = radius
                ),
                radius = radius,
                center = centre
            )

            // The body, so the lobes have something to sit on.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.55f),
                        accent.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = centre,
                    radius = orbRadius
                ),
                radius = orbRadius,
                center = centre
            )

            // Three lobes, each on its own phase, added together with Plus so overlaps brighten.
            data class Lobe(val color: Color, val phase: Float, val distance: Float, val scale: Float)

            listOf(
                Lobe(accent, spin, 0.30f, 0.62f),
                Lobe(cool, spin + 2.1f + drift * 0.35f, 0.34f, 0.55f),
                Lobe(warm, drift - 1.2f, 0.26f, 0.48f)
            ).forEach { lobe ->
                val c = Offset(
                    centre.x + cos(lobe.phase) * orbRadius * lobe.distance,
                    centre.y + sin(lobe.phase) * orbRadius * lobe.distance
                )
                val r = orbRadius * lobe.scale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(lobe.color.copy(alpha = 0.60f), Color.Transparent),
                        center = c,
                        radius = r
                    ),
                    radius = r,
                    center = c,
                    blendMode = BlendMode.Plus
                )
            }

            // Rim light, brightest where the lead lobe sits.
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        accent.copy(alpha = 0.7f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                    center = centre
                ),
                radius = orbRadius,
                center = centre,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.05f)
            )
        }
    }
}

/**
 * A compact three-dot "typing" indicator, for inline use where a full orb would be too loud.
 */
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
