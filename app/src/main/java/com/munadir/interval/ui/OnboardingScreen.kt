package com.munadir.interval.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.exp

/**
 * Three pages: capture, schedule, notify. The order matches the order a new user meets the app.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onRequestNotifications: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    val last = 2

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp)
    ) {
        Column(Modifier.fillMaxSize()) {

            Spacer(Modifier.height(24.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        (fadeIn(tween(280))) togetherWith (fadeOut(tween(160)))
                    },
                    label = "onboarding"
                ) { index ->
                    when (index) {
                        0 -> OnboardPage(
                            art = { ForgettingCurve() },
                            title = "Capture it once",
                            body = "Write down the thing you want to remember. A definition, a formula, a line from a lecture."
                        )

                        1 -> OnboardPage(
                            art = { IntervalLadder() },
                            title = "We choose when",
                            body = "Interval brings it back after a day, then three, then a week, then a month. Each time you recall it, the gap widens."
                        )

                        else -> OnboardPage(
                            art = { BellArt() },
                            title = "We'll tell you",
                            body = "A notification when cards are ready, and a daily nudge at a time you pick. Tap it and you're straight into the session."
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (i == page) 22.dp else 7.dp, height = 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == page) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    if (page < last) {
                        if (page == 1) onRequestNotifications()
                        page++
                    } else {
                        onFinish()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    if (page < last) "Continue" else "Start studying",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OnboardPage(
    art: @Composable () -> Unit,
    title: String,
    body: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(200.dp), contentAlignment = Alignment.Center) { art() }
        Spacer(Modifier.height(44.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Ebbinghaus decay, drawn rather than shipped as an asset so it themes with the accent. */
@Composable
private fun ForgettingCurve() {
    val accent = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.outline
    val progress by animateFloatAsState(1f, tween(900), label = "curve")

    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height

        drawLine(dim, Offset(0f, h), Offset(w, h), strokeWidth = 2f)
        drawLine(dim, Offset(0f, 0f), Offset(0f, h), strokeWidth = 2f)

        // The curve you get without review.
        val decay = Path().apply {
            moveTo(0f, 8f)
            var x = 0f
            while (x <= w * progress) {
                val t = x / w
                lineTo(x, 8f + (h - 16f) * (1f - exp(-3.2f * t)))
                x += 6f
            }
        }
        drawPath(decay, dim, style = Stroke(width = 5f))

        // The curve you get with it: each review resets the decay from a higher floor.
        val reviewed = Path().apply {
            moveTo(0f, 8f)
            var x = 0f
            var floor = 8f
            var sinceReview = 0f
            while (x <= w * progress) {
                val t = sinceReview / w
                lineTo(x, floor + (h - floor - 16f) * (1f - exp(-3.2f * t)) * 0.45f)
                x += 6f
                sinceReview += 6f
                if (sinceReview > w / 3.4f) {
                    sinceReview = 0f
                    floor += 4f
                    lineTo(x, floor)
                }
            }
        }
        drawPath(reviewed, accent, style = Stroke(width = 7f))
    }
}

/** Four bars at 1, 3, 7, 30 -- the ladder, as a picture. */
@Composable
private fun IntervalLadder() {
    val accent = MaterialTheme.colorScheme.primary
    val steps = listOf("1d", "3d", "7d", "30d")

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        steps.forEachIndexed { i, label ->
            val h by animateFloatAsState(
                targetValue = 40f + i * 34f,
                animationSpec = tween(500, delayMillis = i * 90),
                label = "bar$i"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(width = 40.dp, height = h.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.35f + i * 0.2f))
                )
                Spacer(Modifier.height(8.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
    }
}

@Composable
private fun BellArt() {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(150.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        // Expanding rings, the same mark as the app icon.
        listOf(0.30f to 1f, 0.52f to 0.55f, 0.74f to 0.28f).forEach { (r, alpha) ->
            drawCircle(
                color = accent.copy(alpha = alpha),
                radius = size.minDimension * r / 2f,
                center = c,
                style = Stroke(width = 8f)
            )
        }
        drawCircle(color = accent, radius = size.minDimension * 0.07f, center = c)
        drawCircle(color = Color.Transparent, radius = 0f, center = c)
    }
}
