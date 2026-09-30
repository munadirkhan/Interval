package com.munadir.interval.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.munadir.interval.data.Card as StudyCard
import com.munadir.interval.data.CardType
import kotlinx.coroutines.launch

private val CorrectGreen = Color(0xFF3FBF7F)
private val WrongRed = Color(0xFFE5484D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    queue: List<StudyCard>,
    aiConfigured: Boolean,
    explaining: Boolean,
    explanation: String?,
    explainError: String?,
    onExplain: (StudyCard) -> Unit,
    onDismissExplanation: () -> Unit,
    onGrade: (StudyCard, Boolean) -> Unit,
    onUndo: (StudyCard) -> Unit,
    onAward: (Int) -> Unit,
    onFinish: (got: Int, missed: Int, xp: Int) -> Unit,
    onExit: () -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var got by remember { mutableIntStateOf(0) }
    var missed by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var sessionXp by remember { mutableIntStateOf(0) }
    var reward by remember { mutableStateOf<Reward?>(null) }

    // Per-card UI state, reset on every advance.
    var flipped by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<Int?>(null) }

    val history = remember { mutableStateListOf<StudyCard>() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(index) {
        if (index >= queue.size) onFinish(got, missed, sessionXp)
    }

    val card = queue.getOrNull(index) ?: return

    if (explaining || explanation != null || explainError != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissExplanation,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    "COACH",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                when {
                    explaining -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.size(10.dp))
                        Text("Working it out…", style = MaterialTheme.typography.bodyMedium)
                    }

                    explainError != null -> Text(
                        explainError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    else -> Text(explanation.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    fun advance() {
        flipped = false
        chosen = null
        index++
    }

    fun commit(gotIt: Boolean, offerUndo: Boolean) {
        history.add(card)
        onGrade(card, gotIt)

        if (gotIt) {
            got++
            combo++
            val earned = xpFor(combo)
            sessionXp += earned
            onAward(earned)
            reward = Reward(System.nanoTime(), earned, combo)
            haptics.success()
        } else {
            missed++
            combo = 0
            haptics.grade()
        }

        if (!offerUndo) return

        scope.launch {
            val result = snackbar.showSnackbar(
                message = if (gotIt) "Got it — back in ${StudyCard.nextLabelAfterPass(card.stage)}"
                else "Missed — back tomorrow",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                val previous = history.removeLastOrNull() ?: return@launch
                onUndo(previous)
                if (gotIt) {
                    got--
                    combo = (combo - 1).coerceAtLeast(0)
                } else {
                    missed--
                }
                index = (index - 1).coerceAtLeast(0)
                flipped = true
                chosen = null
            }
        }
    }

    Scaffold { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                ReviewHeader(
                    position = index + 1,
                    total = queue.size,
                    deck = card.deck,
                    combo = combo,
                    sessionXp = sessionXp,
                    onExit = onExit
                )

                Spacer(Modifier.height(12.dp))

                when (card.type) {
                    CardType.FLIP -> FlipBody(
                        card = card,
                        flipped = flipped,
                        aiConfigured = aiConfigured,
                        onFlip = { haptics.flip(); flipped = true },
                        onExplain = { onExplain(card) },
                        onGrade = { gotIt -> commit(gotIt, offerUndo = true); advance() },
                        modifier = Modifier.weight(1f)
                    )

                    CardType.MULTIPLE_CHOICE, CardType.TRUE_FALSE -> QuizBody(
                        card = card,
                        chosen = chosen,
                        aiConfigured = aiConfigured,
                        onChoose = { picked ->
                            if (chosen == null) {
                                chosen = picked
                                commit(picked == card.correctIndex, offerUndo = false)
                            }
                        },
                        onExplain = { onExplain(card) },
                        onContinue = { advance() },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            RewardOverlay(reward = reward)

            // Anchored to the top, not the bottom. A bottom snackbar sits exactly on top of
            // the grade buttons, so the next card can't be answered until it times out.
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 88.dp, start = 12.dp, end = 12.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------- header

@Composable
private fun ReviewHeader(
    position: Int,
    total: Int,
    deck: String,
    combo: Int,
    sessionXp: Int,
    onExit: () -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$position / $total", style = MaterialTheme.typography.titleMedium)
                if (deck.isNotBlank()) {
                    Text(
                        deck.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (sessionXp > 0) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "$sessionXp XP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.size(8.dp))
            }
            if (combo >= 2) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "🔥 $combo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.size(4.dp))
            }

            TextButton(onClick = onExit) {
                Text("Done", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))

        val fraction by animateFloatAsState(
            targetValue = (position - 1).toFloat() / total.coerceAtLeast(1),
            animationSpec = tween(320),
            label = "progress"
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// ---------------------------------------------------------------------- flip cards

@Composable
private fun FlipBody(
    card: StudyCard,
    flipped: Boolean,
    aiConfigured: Boolean,
    onFlip: () -> Unit,
    onExplain: () -> Unit,
    onGrade: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        FlipCard(
            front = card.front,
            back = card.back,
            flipped = flipped,
            onFlip = { if (!flipped) onFlip() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        if (flipped && aiConfigured) {
            TextButton(onClick = onExplain, modifier = Modifier.fillMaxWidth()) {
                Text("✨  Explain this", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }

        if (flipped) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradeButton(
                    label = "Missed it",
                    sublabel = "tomorrow",
                    primary = false,
                    onClick = { onGrade(false) },
                    modifier = Modifier.weight(1f)
                )
                GradeButton(
                    label = "Got it",
                    sublabel = StudyCard.nextLabelAfterPass(card.stage),
                    primary = true,
                    onClick = { onGrade(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Button(
                onClick = onFlip,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text("Show answer", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Rotates around Y. Only one face is ever drawn: past 90 degrees the front would show mirrored,
 * so the back takes over and is counter-rotated to read correctly.
 */
@Composable
private fun FlipCard(
    front: String,
    back: String,
    flipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A spring here matters more than anywhere else in the app: this is the gesture people
    // repeat hundreds of times, and a tween makes it feel like a slideshow.
    val angle by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = Motion.gentle,
        label = "flip"
    )
    val density = LocalDensity.current.density
    val showingBack = angle > 90f

    Box(
        modifier
            .graphicsLayer {
                rotationY = angle
                cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickableNoRipple(onFlip),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { if (showingBack) rotationY = 180f }
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showingBack) {
                Text(
                    back,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        front,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "TAP TO FLIP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * The interval goes on the button. Seeing "Got it · 7 days" before committing is what makes the
 * scheduler legible instead of magic.
 */
@Composable
private fun GradeButton(
    label: String,
    sublabel: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (primary) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (primary) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            modifier = modifier.height(62.dp)
        ) { content() }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = modifier.height(62.dp)
        ) { content() }
    }
}

// ---------------------------------------------------------------------- quiz cards

/**
 * Multiple choice and true/false.
 *
 * These grade themselves, so there is no self-assessment step -- but the answer is never just
 * marked wrong and moved past. The correct option is always revealed alongside the mistake,
 * because the moment right after getting something wrong is when it actually sticks.
 */
@Composable
private fun QuizBody(
    card: StudyCard,
    chosen: Int?,
    aiConfigured: Boolean,
    onChoose: (Int) -> Unit,
    onExplain: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val answered = chosen != null
    val wasCorrect = chosen == card.correctIndex

    Column(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        if (card.type == CardType.TRUE_FALSE) "TRUE OR FALSE" else "CHOOSE ONE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(card.front, style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(Modifier.height(16.dp))

            card.choices.forEachIndexed { i, option ->
                ChoiceRow(
                    label = option,
                    state = when {
                        !answered -> ChoiceState.IDLE
                        i == card.correctIndex -> ChoiceState.CORRECT
                        i == chosen -> ChoiceState.WRONG
                        else -> ChoiceState.MUTED
                    },
                    onClick = { onChoose(i) }
                )
                Spacer(Modifier.height(10.dp))
            }

            if (answered && card.back.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Text(
                        card.back,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        if (answered) {
            if (aiConfigured && !wasCorrect) {
                TextButton(onClick = onExplain, modifier = Modifier.fillMaxWidth()) {
                    Text("✨  Explain this", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (wasCorrect) CorrectGreen else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text(
                    if (wasCorrect) "Nice — continue" else "Got it, continue",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        } else {
            Spacer(Modifier.height(74.dp))
        }
    }
}

private enum class ChoiceState { IDLE, CORRECT, WRONG, MUTED }

@Composable
private fun ChoiceRow(
    label: String,
    state: ChoiceState,
    onClick: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val target = when (state) {
        ChoiceState.IDLE -> surface
        ChoiceState.CORRECT -> CorrectGreen.copy(alpha = 0.18f)
        ChoiceState.WRONG -> WrongRed.copy(alpha = 0.18f)
        ChoiceState.MUTED -> surface
    }
    val background by animateColorAsState(target, tween(240), label = "choiceBg")

    val borderColor = when (state) {
        ChoiceState.CORRECT -> CorrectGreen
        ChoiceState.WRONG -> WrongRed
        else -> Color.Transparent
    }

    val scale by animateFloatAsState(
        targetValue = if (state == ChoiceState.CORRECT) 1.02f else 1f,
        animationSpec = tween(240),
        label = "choiceScale"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(
                width = if (borderColor == Color.Transparent) 1.dp else 2.dp,
                color = if (borderColor == Color.Transparent) MaterialTheme.colorScheme.outline
                else borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickableNoRipple(onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (state == ChoiceState.MUTED) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        when (state) {
            ChoiceState.CORRECT -> Text("✓", style = MaterialTheme.typography.titleMedium, color = CorrectGreen)
            ChoiceState.WRONG -> Text("✕", style = MaterialTheme.typography.titleMedium, color = WrongRed)
            else -> Unit
        }
    }
}
