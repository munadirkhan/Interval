package com.munadir.interval.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munadir.interval.ai.Attachment
import com.munadir.interval.ai.DraftCard
import com.munadir.interval.ai.GenerateMode
import com.munadir.interval.data.CardType

/**
 * Describe a topic or paste your notes, get a study set.
 *
 * Nothing is saved until the user accepts it. A model that invents a wrong fact should cost a
 * tap to reject, not a silent bad card sitting in the rotation for a month.
 */
@Composable
fun GenerateCardsScreen(
    configured: Boolean,
    mode: GenerateMode,
    input: String,
    types: Set<CardType>,
    attachment: Attachment?,
    drafts: List<DraftCard>,
    working: Boolean,
    error: String?,
    remainingFreeSlots: Int?,
    onModeChange: (GenerateMode) -> Unit,
    onInputChange: (String) -> Unit,
    onToggleType: (CardType) -> Unit,
    onAttachFile: () -> Unit,
    onAttachPhoto: () -> Unit,
    onClearAttachment: () -> Unit,
    onGenerate: () -> Unit,
    onToggleDraft: (Int) -> Unit,
    onSaveAccepted: () -> Unit,
    onConnectKey: () -> Unit,
    onBack: () -> Unit
) {
    if (!configured) {
        AiNotConfigured(
            title = "Turn anything into a study set",
            body = "Name a topic or paste your notes, and get flashcards, multiple choice and " +
                "true/false questions back in seconds. Connect an OpenRouter key to switch it on.",
            onConnectKey = onConnectKey
        )
        return
    }

    val acceptedCount = drafts.count { it.accepted }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().imePadding()) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Create with AI", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "A topic, your notes, or a file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onBack) { Text("Close") }
            }

            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { ModeToggle(mode = mode, onChange = onModeChange) }

                item {
                    Text(
                        "QUESTION TYPES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CardType.entries.forEach { type ->
                            TypeChip(
                                label = type.label,
                                selected = type in types,
                                onClick = { onToggleType(type) }
                            )
                        }
                    }
                }

                if (drafts.isEmpty() && !working) {
                    item { Suggestions(mode = mode, onPick = onInputChange) }
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

                if (drafts.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$acceptedCount OF ${drafts.size} SELECTED",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(drafts.indices.toList()) { index ->
                        DraftRow(draft = drafts[index], onToggle = { onToggleDraft(index) })
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            AnimatedVisibility(visible = drafts.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    if (remainingFreeSlots != null && acceptedCount > remainingFreeSlots) {
                        Text(
                            "You have $remainingFreeSlots free slots left. " +
                                "We'll save what fits and show you Pro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = onSaveAccepted,
                        enabled = acceptedCount > 0,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            if (acceptedCount == 1) "Add 1 card" else "Add $acceptedCount cards",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Composer(
                input = input,
                mode = mode,
                attachment = attachment,
                working = working,
                canSend = input.isNotBlank() || attachment != null,
                onInputChange = onInputChange,
                onAttachFile = onAttachFile,
                onAttachPhoto = onAttachPhoto,
                onClearAttachment = onClearAttachment,
                onSend = onGenerate
            )
        }

        if (working) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AiOrb(size = 150.dp)
                    Spacer(Modifier.height(24.dp))
                    Text("Building your set", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Reading, picking what matters, writing questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- composer

/**
 * The prompt bar. A single rounded field with the attach control inside it and a filled send
 * button on the end -- the shape people already know from every AI chat app.
 */
@Composable
private fun Composer(
    input: String,
    mode: GenerateMode,
    attachment: Attachment?,
    working: Boolean,
    canSend: Boolean,
    onInputChange: (String) -> Unit,
    onAttachFile: () -> Unit,
    onAttachPhoto: () -> Unit,
    onClearAttachment: () -> Unit,
    onSend: () -> Unit
) {
    var attachMenu by remember { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

        if (attachment != null) {
            Row(
                Modifier
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📎", fontSize = 15.sp)
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        attachment.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Text(
                        attachment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "✕",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickableNoRipple(onClearAttachment)
                )
            }
        }

        AnimatedVisibility(visible = attachMenu) {
            Row(
                Modifier.padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttachOption("Document", "📄") { attachMenu = false; onAttachFile() }
                AttachOption("Photo", "📷") { attachMenu = false; onAttachPhoto() }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(26.dp))
                .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickableNoRipple { attachMenu = !attachMenu },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (attachMenu) "✕" else "+",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (input.isEmpty()) {
                    Text(
                        if (mode == GenerateMode.TOPIC) "Quiz me on the Krebs cycle…"
                        else "Paste your notes…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend && !working) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .then(
                        if (canSend && !working) Modifier.clickableNoRipple(onSend) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "↑",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (canSend && !working) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AttachOption(label: String, glyph: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(glyph, fontSize = 15.sp)
        Spacer(Modifier.size(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

// ---------------------------------------------------------------------- pieces

@Composable
private fun ModeToggle(mode: GenerateMode, onChange: (GenerateMode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        listOf(
            GenerateMode.TOPIC to "A topic",
            GenerateMode.NOTES to "My notes"
        ).forEach { (value, label) ->
            val selected = mode == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickableNoRipple { onChange(value) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickableNoRipple(onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Suggestions(mode: GenerateMode, onPick: (String) -> Unit) {
    val items = if (mode == GenerateMode.TOPIC) listOf(
        "The Krebs cycle",
        "Spanish travel phrases",
        "Big-O of common sorting algorithms",
        "Causes of the First World War"
    ) else listOf(
        "Paste a lecture slide",
        "Paste a definition list",
        "Paste a page of your textbook"
    )

    Column {
        Text(
            if (mode == GenerateMode.TOPIC) "TRY ONE" else "WORKS WELL WITH",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        items.forEach { suggestion ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .then(
                        if (mode == GenerateMode.TOPIC) Modifier.clickableNoRipple { onPick(suggestion) }
                        else Modifier
                    )
                    .padding(14.dp)
            ) {
                Text(
                    suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (mode == GenerateMode.TOPIC) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DraftRow(draft: DraftCard, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (draft.accepted) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(18.dp)
                ) else Modifier
            )
            .clickableNoRipple(onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (draft.accepted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (draft.accepted) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                draft.type.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                draft.front,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (draft.accepted) null else TextDecoration.LineThrough,
                color = if (draft.accepted) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when (draft.type) {
                    CardType.FLIP -> draft.back
                    else -> draft.choices.getOrNull(draft.correctIndex)
                        ?.let { "Answer: $it" }.orEmpty()
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
