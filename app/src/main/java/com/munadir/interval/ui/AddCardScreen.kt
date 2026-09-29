package com.munadir.interval.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.munadir.interval.data.Card as StudyCard

@Composable
fun AddCardScreen(
    editing: StudyCard?,
    decks: List<String>,
    isPro: Boolean,
    onSave: (front: String, back: String, deck: String) -> Unit,
    onUpgrade: () -> Unit,
    onCancel: () -> Unit
) {
    var front by remember { mutableStateOf(editing?.front ?: "") }
    var back by remember { mutableStateOf(editing?.back ?: "") }
    var deck by remember { mutableStateOf(editing?.deck ?: "") }
    val canSave = front.isNotBlank() && back.isNotBlank()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            if (editing == null) "New card" else "Edit card",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            "The prompt on the front, what you want to recall on the back.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = front,
            onValueChange = { front = it },
            label = { Text("Front") },
            placeholder = { Text("What is the testing effect?") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = back,
            onValueChange = { back = it },
            label = { Text("Back") },
            placeholder = { Text("Retrieving an answer strengthens memory more than rereading.") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )

        Spacer(Modifier.height(20.dp))

        if (isPro) {
            OutlinedTextField(
                value = deck,
                onValueChange = { deck = it },
                label = { Text("Deck (optional)") },
                placeholder = { Text("Biology") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (decks.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    decks.forEach { name ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (deck == name) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { deck = if (deck == name) "" else name }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                name,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (deck == name) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onUpgrade)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Organise into decks", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Available in Pro",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("PRO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { onSave(front, back, deck) },
            enabled = canSave,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                if (editing == null) "Save card" else "Save changes",
                style = MaterialTheme.typography.labelLarge
            )
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(30.dp))
    }
}
