package com.munadir.interval.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munadir.interval.data.Milestone
import com.munadir.interval.data.Milestones

@Composable
fun RewardsScreen(totalXp: Int, onBack: () -> Unit) {
    val next = Milestones.next(totalXp)
    val progress by animateFloatAsState(
        targetValue = Milestones.progressToNext(totalXp),
        animationSpec = tween(600),
        label = "milestoneProgress"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Rewards",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBack) { Text("Done") }
        }

        Spacer(Modifier.height(20.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(24.dp)
        ) {
            Text(
                "$totalXp",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "total XP",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (next != null) {
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "${next.xp - totalXp} XP to ${next.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Every milestone cleared. Seriously, email me.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Milestones.ALL.forEach { milestone ->
            MilestoneRow(milestone = milestone, unlocked = totalXp >= milestone.xp, totalXp = totalXp)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Ten XP a correct answer, more on a streak.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun MilestoneRow(milestone: Milestone, unlocked: Boolean, totalXp: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (unlocked) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
            .then(
                // The punchline tier gets a border whether or not it is reachable, because it
                // is the one people will actually stop and read.
                if (milestone.isTheBit) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = if (unlocked) 1f else 0.4f),
                    RoundedCornerShape(20.dp)
                ) else Modifier
            )
            .padding(18.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (unlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (unlocked) "✓" else if (milestone.isTheBit) "🍔" else "🔒",
                fontSize = if (unlocked) 17.sp else 15.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(Modifier.size(14.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    milestone.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (unlocked) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "%,d XP".format(milestone.xp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                milestone.blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!unlocked) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "%,d to go".format(milestone.xp - totalXp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
