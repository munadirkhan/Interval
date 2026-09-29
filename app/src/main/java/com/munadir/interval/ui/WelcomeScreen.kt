package com.munadir.interval.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AVATARS = listOf("🦉", "🧠", "📚", "🦊", "🐙", "🌱", "⚡", "🎯")

/**
 * First run. Names the profile and picks an avatar.
 *
 * Deliberately not a login wall: there is no server to log into, and asking someone to make an
 * account before they have seen the app is friction with nothing behind it. The profile is real
 * though -- its id becomes the RevenueCat app user id, so a purchase belongs to the person
 * rather than to a reinstall.
 */
@Composable
fun WelcomeScreen(
    initialName: String = "",
    initialAvatar: String = AVATARS.first(),
    isEditing: Boolean = false,
    onContinue: (name: String, avatar: String) -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var avatar by remember { mutableStateOf(initialAvatar) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        Box(
            Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(avatar, fontSize = 56.sp)
        }

        Spacer(Modifier.height(28.dp))

        Text(
            if (isEditing) "Your profile" else "Welcome to Interval",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isEditing) "Change how you show up in the app."
            else "Everything stays on this device. No email, no password, no waiting.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 24) name = it },
            label = { Text("What should we call you?") },
            placeholder = { Text("Munadir") },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        Text(
            "PICK AN AVATAR",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AVATARS.chunked(4).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { option ->
                        AvatarChoice(
                            emoji = option,
                            selected = avatar == option,
                            onClick = { avatar = option },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = { onContinue(name.ifBlank { "Studier" }, avatar) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                if (isEditing) "Save" else "Let's go",
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (onCancel != null) {
            Spacer(Modifier.height(4.dp))
            androidx.compose.material3.TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun AvatarChoice(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = tween(180),
        label = "avatar"
    )
    Box(
        modifier
            .height(64.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .then(
                if (selected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(18.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 28.sp)
    }
}
