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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.munadir.interval.ai.AiMessage

@Composable
fun CoachScreen(
    messages: List<AiMessage>,
    thinking: Boolean,
    error: String?,
    configured: Boolean,
    avatar: String,
    onSend: (String) -> Unit,
    onClear: () -> Unit,
    onConnectKey: () -> Unit
) {
    if (!configured) {
        AiNotConfigured(
            title = "Meet your coach",
            body = "Ask anything about what you're studying, and get it explained in plain language. " +
                "Connect an OpenRouter key to switch it on — it takes about thirty seconds.",
            onConnectKey = onConnectKey
        )
        return
    }

    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, thinking) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Coach", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Ask about anything you're learning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (messages.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item { CoachIntro(avatar = avatar, onSuggestion = onSend) }
            }

            itemsIndexed(messages) { _, message ->
                MessageBubble(message)
            }

            if (thinking) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AiOrb(size = 34.dp)
                        Spacer(Modifier.size(10.dp))
                        ThinkingDots()
                    }
                }
            }

            if (error != null) {
                item {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Ask something…") },
                shape = RoundedCornerShape(20.dp),
                maxLines = 4,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(8.dp))
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (draft.isBlank() || thinking) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.primary
                    )
                    .then(
                        if (draft.isBlank() || thinking) Modifier
                        else Modifier.clickableNoRipple {
                            onSend(draft.trim())
                            draft = ""
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "↑",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (draft.isBlank() || thinking) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun CoachIntro(avatar: String, onSuggestion: (String) -> Unit) {
    val suggestions = listOf(
        "Explain spaced repetition like I'm five",
        "How should I study for an exam in 3 days?",
        "Why do I forget things I just read?"
    )

    Column(Modifier.padding(top = 24.dp)) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(avatar, fontSize = 36.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text("What are you working on?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "I can explain concepts, break down a card you're stuck on, or help you decide what to study.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        suggestions.forEach { suggestion ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickableNoRipple { onSuggestion(suggestion) }
                    .padding(16.dp)
            ) {
                Text(suggestion, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: AiMessage) {
    val fromUser = message.role == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.88f)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (fromUser) 20.dp else 6.dp,
                        bottomEnd = if (fromUser) 6.dp else 20.dp
                    )
                )
                .background(
                    if (fromUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
                .padding(16.dp)
        ) {
            Text(
                message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (fromUser) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Shown wherever an AI feature lives but no key is set.
 *
 * Designed rather than disabled: someone who clones this repo should see what the feature is
 * and how to switch it on, not a greyed-out button.
 */
@Composable
fun AiNotConfigured(
    title: String,
    body: String,
    onConnectKey: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 40.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onConnectKey,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Connect a key", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Free keys are available at openrouter.ai",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
