package com.munadir.interval.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munadir.interval.data.Milestones
import com.munadir.interval.data.Prefs

@Composable
fun ProfileScreen(
    prefs: Prefs,
    isPro: Boolean,
    totalCards: Int,
    totalReviews: Int,
    totalXp: Int,
    streak: Int,
    retention: Float?,
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
    onRewards: () -> Unit,
    onUpgrade: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(prefs.profileAvatar, fontSize = 38.sp)
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    prefs.profileName.ifBlank { "Studier" },
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    if (prefs.profileCreatedAt > 0)
                        "Studying since ${rememberMonthYear(prefs.profileCreatedAt)}"
                    else "Welcome",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isPro) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            TextButton(onClick = onEditProfile) { Text("Edit") }
        }

        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat("$streak", "day streak", Modifier.weight(1f))
            MiniStat("$totalCards", "cards", Modifier.weight(1f))
            MiniStat(
                retention?.let { "${(it * 100).toInt()}%" } ?: "—",
                "recall",
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        if (!isPro) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickableNoRipple(onUpgrade)
                    .padding(20.dp)
            ) {
                Text(
                    "Interval Pro",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Unlimited cards, decks, stats and every accent.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "See plans →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        NavRow(
            "Rewards",
            Milestones.next(totalXp)
                ?.let { "%,d XP to ${it.title}".format(it.xp - totalXp) }
                ?: "Every milestone cleared",
            onRewards
        )
        Spacer(Modifier.height(10.dp))
        NavRow("Your stats", "Streaks, retention and the heatmap", onStats)
        Spacer(Modifier.height(10.dp))
        NavRow("Settings", "Theme, reminders, AI key, purchases", onSettings)

        Spacer(Modifier.height(24.dp))

        Text(
            "$totalReviews reviews all time",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickableNoRipple(onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
