package com.munadir.interval.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** One award, identified by [id] so a repeat of the same amount still re-triggers the animation. */
data class Reward(val id: Long, val xp: Int, val combo: Int)

/** Base XP for a correct answer, before the streak bonus. */
const val XP_BASE = 10

/**
 * What a correct answer is worth, given how many have landed in a row.
 *
 * The bonus is deliberately coarse -- three tiers, not a curve. A reward you can predict is a
 * reward you can chase.
 */
fun xpFor(combo: Int): Int = when {
    combo >= 5 -> XP_BASE + 10
    combo >= 3 -> XP_BASE + 5
    else -> XP_BASE
}

/**
 * The "+15 XP" that rises off a correct answer, with a spray of accent-coloured particles.
 *
 * Drawn rather than animated with a library: a dozen particles on a Canvas costs nothing and
 * keeps the app at one external dependency.
 */
@Composable
fun RewardOverlay(
    reward: Reward?,
    modifier: Modifier = Modifier
) {
    if (reward == null) return

    val rise = remember(reward.id) { Animatable(0f) }
    val fade = remember(reward.id) { Animatable(0f) }
    val burst = remember(reward.id) { Animatable(0f) }

    LaunchedEffect(reward.id) {
        fade.snapTo(0f)
        rise.snapTo(0f)
        burst.snapTo(0f)
        // Pop in fast, drift up, fade out slowly. The asymmetry is what makes it feel snappy.
        fade.animateTo(1f, tween(140))
        burst.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        rise.animateTo(1f, tween(760, easing = FastOutSlowInEasing))
        fade.animateTo(0f, tween(260))
    }

    val accent = MaterialTheme.colorScheme.primary
    val particles = remember(reward.id) {
        List(14) { i ->
            val angle = (i / 14f) * 2f * Math.PI.toFloat() + Random.nextFloat() * 0.4f
            Triple(angle, 60f + Random.nextFloat() * 90f, 3f + Random.nextFloat() * 4f)
        }
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            particles.forEach { (angle, distance, radius) ->
                val d = distance * burst.value
                drawCircle(
                    color = accent.copy(alpha = (1f - burst.value) * fade.value),
                    radius = radius * (1f - burst.value * 0.5f),
                    center = Offset(
                        centre.x + cos(angle) * d,
                        centre.y + sin(angle) * d
                    )
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = fade.value
                translationY = -70f * rise.value
                val pop = 0.7f + 0.3f * minOf(1f, burst.value * 3f)
                scaleX = pop
                scaleY = pop
            }
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "+${reward.xp} XP",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            if (reward.combo >= 3) {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "🔥 ${reward.combo} in a row",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
