package com.munadir.interval.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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

@Composable
fun StatsScreen(
    streak: Int,
    longestStreak: Int,
    retention: Float?,
    totalReviews: Int,
    reviewsToday: Int,
    totalXp: Int,
    dailyCounts: List<Int>,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stats", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Done") }
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigStat(
                value = "$streak",
                unit = if (streak == 1) "day" else "days",
                label = "Current streak",
                accent = true,
                modifier = Modifier.weight(1f)
            )
            BigStat(
                value = "$longestStreak",
                unit = if (longestStreak == 1) "day" else "days",
                label = "Longest",
                accent = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigStat(
                value = retention?.let { "${(it * 100).toInt()}" } ?: "—",
                unit = if (retention != null) "%" else "",
                label = "Retention",
                accent = false,
                modifier = Modifier.weight(1f)
            )
            BigStat(
                value = "$totalXp",
                unit = "XP",
                label = "Earned · $reviewsToday today",
                accent = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "LAST 30 DAYS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Heatmap(dailyCounts)

        Spacer(Modifier.height(20.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp)
        ) {
            Text(
                if (totalReviews == 0)
                    "Review one card and this page starts filling in."
                else
                    "$totalReviews reviews all time. Retention is the share you recalled correctly on the first try.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun BigStat(
    value: String,
    unit: String,
    label: String,
    accent: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (accent) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                color = if (accent) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            if (unit.isNotEmpty()) {
                Text(
                    " $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (accent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Thirty cells, oldest top-left. Intensity is relative to the busiest day in the window. */
@Composable
private fun Heatmap(counts: List<Int>) {
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val primary = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surface

    LazyVerticalGrid(
        columns = GridCells.Fixed(10),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp),
        userScrollEnabled = false
    ) {
        items(counts) { count ->
            Box(
                Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (count == 0) empty
                        else primary.copy(alpha = 0.25f + 0.75f * (count.toFloat() / max))
                    )
            )
        }
    }
}
