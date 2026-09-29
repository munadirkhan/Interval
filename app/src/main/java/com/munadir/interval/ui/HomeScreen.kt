package com.munadir.interval.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munadir.interval.Config
import com.munadir.interval.data.Card as StudyCard
import java.util.Calendar

@Composable
fun HomeScreen(
    cards: List<StudyCard>,
    isPro: Boolean,
    streak: Int,
    totalXp: Int,
    profileName: String,
    profileAvatar: String,
    onAdd: () -> Unit,
    onGenerate: () -> Unit,
    onReview: () -> Unit,
    onEdit: (StudyCard) -> Unit,
    onDelete: (StudyCard) -> Unit,
    onUpgrade: () -> Unit,
    onTestReminder: () -> Unit
) {
    val due = cards.count { it.isDue }
    var menuFor by remember { mutableStateOf<StudyCard?>(null) }
    var confirmDelete by remember { mutableStateOf<StudyCard?>(null) }

    menuFor?.let { card ->
        AlertDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(card.front, style = MaterialTheme.typography.titleMedium) },
            text = { Text("Edit this card or remove it from rotation.") },
            confirmButton = {
                TextButton(onClick = { onEdit(card); menuFor = null }) { Text("Edit") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = card; menuFor = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    confirmDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this card?") },
            text = { Text(card.front) },
            confirmButton = {
                TextButton(onClick = { onDelete(card); confirmDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Keep") } }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(20.dp))
            Greeting(name = profileName, avatar = profileAvatar, streak = streak, totalXp = totalXp)
            Spacer(Modifier.height(18.dp))
        }

        item {
            if (due > 0) DueHero(due = due, total = cards.size, onReview = onReview)
            else CaughtUp(next = cards.minByOrNull { it.dueAt })
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile("＋", "Add a card", Modifier.weight(1f), onAdd)
                ActionTile("✨", "Generate", Modifier.weight(1f), onGenerate)
            }
        }

        if (!isPro) {
            item { FreeTierNotice(used = cards.size, onUpgrade = onUpgrade) }
        }

        if (cards.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "ALL CARDS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(cards.sortedBy { it.dueAt }, key = { it.id }) { card ->
            CardRow(card = card, onLongPress = { menuFor = card })
        }

        item {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onTestReminder) {
                Text(
                    "Send a test reminder (15s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Greeting(name: String, avatar: String, streak: Int, totalXp: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(avatar, fontSize = 22.sp)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                greetingFor(name),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Remember what you learn",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (streak > 0) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        "🔥 $streak",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            if (totalXp > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "$totalXp XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun greetingFor(name: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val part = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (name.isBlank()) part else "$part, $name"
}

@Composable
private fun DueHero(due: Int, total: Int, onReview: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(24.dp)
    ) {
        Text(
            "$due",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            if (due == 1) "card due now" else "cards due now",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            "$total in rotation",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onReview,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Review now", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** The screen you see most once the habit sticks. Worth designing properly. */
@Composable
private fun CaughtUp(next: StudyCard?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✓", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("All caught up", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            if (next == null) "Add a card and it will come back to you tomorrow."
            else "Nothing to review. The next card is due ${dueLabel(next).removePrefix("Due ").lowercase()}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActionTile(
    glyph: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(glyph, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun FreeTierNotice(used: Int, onUpgrade: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                // Clamped: a debug seed or an old install can exceed the limit, and
                // "6 of 5" reads as a bug even when the gate itself is working.
                "${used.coerceAtMost(Config.FREE_CARD_LIMIT)} of ${Config.FREE_CARD_LIMIT} free cards",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Pro removes the limit and unlocks stats",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onUpgrade) { Text("Upgrade") }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardRow(card: StudyCard, onLongPress: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StageDot(stage = card.stage)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(card.front, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
            Text(
                buildString {
                    append(dueLabel(card))
                    append(" · ")
                    append(StudyCard.labelFor(card.stage))
                    if (card.deck.isNotBlank()) {
                        append(" · ")
                        append(card.deck)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (card.isDue) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Deepens as a card climbs the ladder, so progress is visible at a glance. */
@Composable
private fun StageDot(stage: Int) {
    val alpha = 0.25f + (stage.toFloat() / StudyCard.INTERVALS.lastIndex) * 0.75f
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha.coerceIn(0.25f, 1f)))
    )
}

/**
 * Rounds up, not down. A card promoted to the 3-day rung is 3 days minus a few seconds away by
 * the time this renders, and flooring reads as "Due in 2 d" — which looks like a broken scheduler.
 */
internal fun dueLabel(card: StudyCard): String {
    if (card.isDue) return "Due now"
    val mins = ceilDiv(card.dueAt - System.currentTimeMillis(), 60_000)
    return when {
        mins < 60 -> "Due in $mins min"
        mins < 1440 -> "Due in ${ceilDiv(mins, 60)} h"
        else -> "Due in ${ceilDiv(mins, 1440)} d"
    }
}

private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1) / divisor
